## Context

迁移任务列表查询链路为：`TaskController.queryDataMoveTaskList` → `DataMoveHandler.queryDataMoveTaskList/queryDataMoveTaskCount` → `MetaServiceImpl` → `DataStreamDao` → `DataStreamMapper`（`DataStreamMapper.xml`）。任务实例表 `data_stream_move_task` 已有 `system_user_code` 字段记录创建人登录账号，实体 `DataMoveTaskEntity.systemUserCode` 已映射该字段。

现有 RBAC 已具备系统管理员判定：`PermissionService` 通过当前认证的 `ROLE_SYSTEM_ADMIN` 权限放行，`DsJwtUser.getSystemUserInfo().getSystemUserCode()` 提供当前登录用户编码。本变更在这些既有能力之上补充行级过滤，不引入新的权限模型。

## Goals / Non-Goals

**Goals:**

- 用户侧迁移任务列表查询按创建人过滤，普通用户仅见本人任务，管理员见全部。
- 按 `taskId` 读取的任务详情/进度/执行明细/表结构明细做归属校验。
- 列表总数与列表数据保持一致（SQL 层过滤，保证分页正确）。

**Non-Goals:**

- 不修改数据迁移引擎内部（`StartExecutor` 等）对 `queryDataMoveTaskByState` 的调用语义，内部调用仍返回全部任务。
- 不改动前端页面；数据范围由后端强制，前端无新增隐藏逻辑。
- 不扩展数据范围到数据源、表结构等其他实体（仅覆盖迁移任务）。

## Decisions

### 1. 在 SQL 层做行级过滤，而非内存过滤

对任务列表与 count 查询的 mapper 方法增加可空 `systemUserCode` 参数，SQL 中以 `<if test="systemUserCode != null and systemUserCode != ''">and system_user_code = #{systemUserCode}</if>` 追加过滤条件。管理员时传 `null`，普通用户传当前用户编码。

- **理由**：分页查询需同时保证 count 与列表结果一致；内存过滤会破坏分页与总数统计。
- **备选**：在 `DataMoveHandler` 返回前对列表做内存过滤——已排除，因为无法正确计算分页总数。

### 2. 统一获取「当前用户」与「管理员」判定

在 `PermissionService` 增加 `isAdmin()` 与 `getCurrentUserCode()` 两个只读方法（不抛异常），复用 `SecurityContextHolder` 中 `DsJwtUser` 主体现有信息：管理员判定为当前认证含 `ROLE_SYSTEM_ADMIN`（沿用既有 `SYSTEM_ADMIN_AUTHORITY` 常量），用户编码取自 `getSystemUserInfo().getSystemUserCode()`。`DataMoveHandler` 注入 `PermissionService` 计算过滤值。

- **理由**：避免在 handler 中重复 SecurityContext 解析逻辑，与既有鉴权规则保持一致。
- **备选**：新建独立工具类——已排除，避免重复维护两套管理员判定。

### 3. 过滤值仅在用户侧查询路径注入

`DataMoveHandler.queryDataMoveTaskList/queryDataMoveTaskCount` 计算 `systemUserCode`（管理员为 `null`）后透传到 `MetaServiceImpl`/`DataStreamDao`/`DataStreamMapper`。内部引擎调用（如 `StartExecutor.queryDataMoveTaskByState`）继续传 `null`，保持返回全部任务。

- **理由**：同一 mapper 方法被用户侧与引擎侧复用，只有用户侧需要过滤；用可空参数区分，避免破坏引擎调度。

### 4. 详情/明细接口做归属校验

对 `queryTaskProgress`、`queryDataMoveInfoList`、`queryTableMoveList`（以及列表 `queryFlag=1` 按 taskId 查询）先 `queryTaskByTaskId(taskId)` 得到任务归属，若非管理员且 `task.systemUserCode` 不等于当前用户编码，抛出无权限错误，不返回数据。

- **理由**：列表过滤之外，仍需阻断普通用户凭 taskId 直接访问他人任务的详情，闭环数据范围。
- **备选**：仅在列表过滤、详情不做校验——已排除，存在越权访问漏洞。

## Risks / Trade-offs

- [风险] 遗漏某条任务查询路径导致仍可越权 → 缓解：将列表与 count 的各 queryFlag 分支统一纳入过滤，并在任务中逐条核对 mapper 方法。
- [风险] 可空参数新增导致签名变更影响引擎内部调用 → 缓解：内部调用显式传 `null`，并在改动后运行后端编译/启动验证。
- [风险] `system_user_code` 历史数据为空导致普通用户查询不到 → 缓解：仅当当前用户为普通用户且传入非空用户编码时过滤；管理员不触发过滤。
