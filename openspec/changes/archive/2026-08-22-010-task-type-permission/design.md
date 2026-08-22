## Context

现有 RBAC 已具备完整链路：`data_stream_permission(permission_type=2)` 存储数据权限，`data_stream_role_permission` 关联角色与权限，`data_stream_user_role` 关联用户与角色，用户最终权限为其全部角色权限的并集。登录时 `UserDetailsServiceImpl` 通过 `selectPermissionCodesByUserId` 将权限编码写入 `SystemUser.permissions`，前端 `mainStore.getLoginPermissions` 与 `usePermission().hasPermission(code)` 已可直接读取。

后端创建迁移任务接口 `TaskController.createMoveTask` 现用 `@PreAuthorize("@permissionService.hasPermission('task:create')")` 做动作级鉴权；`PermissionService` 已提供 `hasPermission` / `hasAnyPermission` / `isAdmin`，且 `isAdmin()` 对系统管理员与兼容 SSO 全量角色放行。任务类型取值已由 `DataStreamConstant` 定义为 1~6 常量，前端由 `taskConstants.js` 的 `TASK_TYPE_OPTIONS` 维护。动机详见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 将任务类型（6 种）纳入数据权限，实现创建迁移任务的「任务类型级」授权。
- 后端按 `taskType` 强制校验对应类型权限，前端按授权过滤下拉选项，两端权限编码与类型映射一致。
- 存量 `task:create` 持有角色平滑升级到 6 个类型权限，不破坏现有用户行为。

**Non-Goals:**

- 不改动「表链接任务」的创建鉴权（`createTableLinkTask` 仍使用 `task:create`）。
- 不约束独立的「结构迁移」菜单页面（`menu:task-table` / `tableManage` 路由），仅约束「迁移任务 > 创建任务」下拉框。
- 不改变权限变更的生效机制：仍沿用「重新登录后生效」，不做运行时动态刷新。

## Decisions

### 1. 权限编码采用语义化 `task:type:*`

新增 6 个数据权限（`permission_type=2`），编码与任务类型映射如下：

| taskType | 常量（DataStreamConstant） | 前端名称 | permission_code | permission_name |
|---|---|---|---|---|
| 1 | DATA_STREAM_TASK_TYPE_DATA_MOVE | 数据迁移 | `task:type:migrate` | 数据迁移 |
| 2 | DATA_STREAM_TASK_TYPE_DATA_DEL | 数据清理 | `task:type:clean` | 数据清理 |
| 3 | DATA_STREAM_TASK_TYPE_DATA_MOVE_DEL | 迁移清理 | `task:type:migrate-clean` | 迁移清理 |
| 4 | DATA_STREAM_TASK_TYPE_TABLE_MOVE | 结构迁移 | `task:type:structure` | 结构迁移 |
| 5 | DATA_STREAM_TASK_TYPE_DATA_CHECK | 数据稽核 | `task:type:data-check` | 数据稽核 |
| 6 | DATA_STREAM_TASK_TYPE_DATA_CDC | 增量迁移 | `task:type:cdc` | 增量迁移 |

- **理由**：语义编码可读、自描述，与既有 `task:create`/`task:execute` 命名风格一致，且 `permission_name` 直接供角色授权弹窗与无权限提示展示。
- **备选**：数字编码 `task:type:1`~`task:type:6` —— 更省一个映射，但可读性差、无权限提示不直观，已排除。

### 2. 后端按 taskType 鉴权，替代 `task:create`（仅 createMoveTask）

在 `PermissionService` 新增只读方法 `hasTaskTypePermission(Integer taskType)`：`isAdmin()` 直接返回 `true`；否则依据 taskType 映射出 `task:type:*` 编码，复用现有 `checkPermission` 校验，无权限时复用 `buildSingleDeniedMessage` 抛出携带中文名与编码的 `PermissionDeniedException`；taskType 为空或无法映射时拒绝。

`TaskController.createMoveTask` 的注解改为 `@PreAuthorize("@permissionService.hasTaskTypePermission(#createMoveTaskRequest.taskType)")`。`createTableLinkTask` 保持 `task:create` 不变。

- **理由**：前端过滤只是体验，安全边界必须落在后端；复用 `PermissionService` 与既有 SpEL 参数名解析（`@LogOperate` 已使用 `#createMoveTaskRequest.*`）。
- **备选**：在 Controller 方法体内编程式校验 —— 可读性略差且偏离现有 `@PreAuthorize` 风格，已排除。

### 3. 前端任务类型选项按权限过滤

`taskConstants.js` 新增 `TASK_TYPE_PERMISSION_MAP`（value → permission_code）并为 `TASK_TYPE_OPTIONS` 各选项补 `permission` 字段；`TaskCreateDrawer.vue` 将写死的 6 个 `el-option` 改为 `v-for` 渲染 `computed` 过滤后的选项，过滤条件为 `usePermission().hasPermission(opt.permission)`（`hasPermission` 对管理员直接返回 `true`）。

- **理由**：登录响应已携带权限编码，前端无需新增接口；过滤逻辑集中在抽屉组件，改动最小。
- **备选**：新增「查询当前用户任务类型」接口实时取数 —— 引入额外请求且与既有「重新登录生效」机制不一致，已排除。

### 4. 存量 `task:create` 数据迁移升级

新增一段迁移 SQL：插入 6 个类型权限记录后，将所有已持有 `task:create` 权限的角色补授这 6 个类型权限（`INSERT ... SELECT` 按 role_id + 类型权限 id 批量写入）。系统管理员因 `isAdmin()` 本就放行，无需额外处理。

- **理由**：一次性数据迁移保证升级后原 `task:create` 持有者仍可见/可创建全部 6 种类型，向后兼容。
- **备选**：代码层「task:create 视同全部类型」—— 引入隐藏特例逻辑，长期维护成本高，已排除。

## Risks / Trade-offs

- [风险] 前端与后端「taskType → 权限编码」映射不一致导致过滤与鉴权错位 → 缓解：两端均以同一张映射表为准，任务中列明需同步修改的常量文件，并在 apply 后做联调验证。
- [风险] SpEL 参数名 `#createMoveTaskRequest` 解析失败导致鉴权不生效 → 缓解：项目已在 `@LogOperate` 中复用同一参数名，沿用已验证的编译参数；apply 后验证无权限用户被拒、有权限用户放行。
- [风险] 存量环境未执行迁移 SQL 导致普通用户看不到任何任务类型 → 缓解：迁移 SQL 与种子脚本一并纳入交付，并在部署步骤中明确要求先执行迁移再重启。
- [风险] `task:create` 语义分裂（迁移任务已不再依赖，但表链接任务仍依赖）造成理解混淆 → 缓解：在 design 与代码注释中明确 `task:create` 仅保留给表链接任务，属有意为之；后续如需统一可另立变更。

## Migration Plan

1. 执行迁移 SQL：新增 6 个 `task:type:*` 权限记录，并将存量 `task:create` 角色补授 6 个类型权限（幂等设计，避免重复插入）。
2. 部署后端与前端代码。
3. 回滚：保留 `task:create` 权限与 `createTableLinkTask` 鉴权不变；如需回退，将 `createMoveTask` 的 `@PreAuthorize` 恢复为 `task:create` 并移除前端过滤即可，权限数据无需删除。
