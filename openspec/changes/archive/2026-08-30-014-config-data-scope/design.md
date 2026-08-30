## Context

配置管理共 5 个模块：数据源、表链接、文件格式、MQ、字段类型。经排查，字段类型定义/映射（`data_stream_column_type_define` / `data_stream_column_type_map`）为脚本预置、被迁移引擎全局消费的平台级共享参考数据，不应按工号隔离；其余 4 个模块（数据源 `data_stream_data_base`、表链接 `data_stream_table_link`、文件格式 `data_stream_file_format`、MQ `data_stream_mq_config`）均为用户自建配置，且当前既无 `system_user_code` 列，也无行级过滤与归属校验。

既有迁移任务数据范围（变更 009）已沉淀可复用能力：`PermissionService.isAdmin()` 判定系统管理员，`PermissionService.getCurrentUserCode()` 读取当前登录工号；SQL 层用 `<if test="systemUserCode != null and systemUserCode != ''">and system_user_code = #{systemUserCode}</if>` 做行级过滤；详情按 ID 访问时先查归属再做校验（`validateTaskOwner`）。

本变更将上述模式复制到 4 个配置模块与概览统计，不引入新的权限模型。

## Goals / Non-Goals

**Goals:**

- 4 类用户自建配置（数据源、表链接、文件格式、MQ）新增时记录创建人工号（后端取值，不信任前端）。
- 配置列表/总数查询按工号过滤，普通用户仅见本人配置，管理员见全部，总数与分页结果一致。
- 配置明细（详情/修改/删除/上下线/测试/复制）做归属校验，普通用户越权访问他人配置返回无权限错误。
- 概览统计（数据源数、迁移/回迁任务总数与运行数、按日趋势、任务类型/状态分布）按工号过滤。

**Non-Goals:**

- 字段类型定义/映射（共享参考数据）不做工号过滤。
- 引擎内部读取配置（`loadDataSourceByMoveTasks` → `getDataBaseById`、`getMQConfig`、`selectFileFormat` 等）仍返回全部数据，不注入过滤。
- 不引入新的权限模型；管理员判定沿用 `ROLE_SYSTEM_ADMIN` 与兼容的 `ROLE_TASK_ALL`。

## Decisions

### 1. 在 SQL 层做列表/总数过滤，而非内存过滤

对每个配置模块的 list 与 count 查询追加 `systemUserCode` 可空参数，公共 WHERE 片段中追加判空过滤；管理员传 `null`，普通用户传当前工号。

- **理由**：与迁移任务（009）一致，保证分页总数与列表结果一致；内存过滤会破坏分页。
- **备选**：返回前内存过滤——排除，无法正确计算总数。

### 2. 工号在创建时由后端写入，不信任前端

配置新增时在 Service 层调用 `permissionService.getCurrentUserCode()` 写入 `systemUserCode`；复制操作复用创建路径，因此复制品记为「当前操作人」。

- **理由**：配置请求对象（`AddDataBaseRequest`、`AddFileFormatRequest` 等）不含工号字段；后端从认证上下文取值可防止伪造。
- **备选**：前端传工号（现有任务模块做法）——不采用，存在伪造风险。

### 3. 配置模块在 ServiceImpl 注入 PermissionService（无 Handler 层）

数据源经 `DataBaseHandler`（有 Handler 层）计算过滤值；MQ、文件格式、表链接的 CRUD 为 Controller → ServiceImpl 直连，在 ServiceImpl 注入 `PermissionService` 作为唯一过滤/写值/校验入口。

- **理由**：各模块所有方法汇聚于 ServiceImpl，单点注入避免在多个 Controller 重复解析 SecurityContext。
- **备选**：新建工具类——与既有 `PermissionService` 重复，排除。

### 4. 明细按 ID 操作走「先查归属再校验」

对详情/修改/删除/上下线/测试/复制等按 ID 路径，先按 ID 取记录，若非管理员且 `systemUserCode` 不等于当前工号，抛 `OPER_CONFIG_NOT_OWNER_ERROR`（新增错误码），不返回/不改动数据。

- **理由**：列表过滤之外仍需阻断普通用户凭 ID 越权，闭环数据范围。
- **备选**：仅过滤列表、明细不校验——存在越权漏洞，排除。

### 5. 数据源查询链路的过滤边界

`queryDataBase` / `getDataBaseCount` 增加 `systemUserCode` 参数并在 SQL 过滤；`DataBaseHandler` 的配置列表/总数/概览数据源数/按 ID 归属校验均注入过滤值。引擎内部经 `getDataBaseById`（独立方法）读取数据源连接信息，保持不变、不过滤。任务创建链路中按 ID 解析数据源（`TableInfoHandler` / `TableLinkHandler` / `DataMoveHandler`）传 `null` 保持既有行为——数据源下拉已由过滤后的列表接口提供。

- **理由**：配置管理与概览属于用户侧查询路径，需过滤；引擎与任务创建解析属内部路径，避免破坏调度与建链流程。
- **备选**：任务创建解析也过滤——会扩大改动面并可能影响复用公共数据源场景，本次不做。

### 6. 概览统计统一按工号过滤

`statSystemInfo` 计算一次过滤值，透传到 `statMoveTaskCount`、`statLinkTaskCount`、`statMoveTaskCountGroupByDay`、`statLinkTaskCountGroupByDay`、`statMoveTaskCountGroupByType`、`statMoveTaskCountGroupByState` 及数据源总数查询，SQL 统一追加工号过滤。

- **理由**：概览页所有指标应口径一致，避免普通用户看到全平台聚合。

## Risks / Trade-offs

- [风险] 遗漏某条配置查询/操作路径导致仍可越权 → 缓解：任务清单逐模块核对 list/count/detail/mutation 路径，SQL 片段复用覆盖多分支。
- [风险] 历史配置 `system_user_code` 为空导致普通用户查不到 → 缓解：仅当普通用户且传入非空工号时过滤；历史数据由管理员可见，可后续补写。
- [风险] `DataSourceSelect.vue` 与列表共用过滤后接口，普通用户创建任务时仅能选本人数据源/文件/MQ → 视为符合数据范围预期的行为，管理员不受限。
- [风险] 可空参数新增导致签名变更影响内部调用 → 缓解：内部调用显式传 `null`，改动后运行后端编译验证。
