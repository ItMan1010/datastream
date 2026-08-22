## Why

当前「迁移任务 > 创建任务」的任务类型下拉框固定展示全部 6 种任务类型（数据迁移、数据清理、迁移清理、结构迁移、数据稽核、增量迁移），创建接口仅校验动作级权限 `task:create`。无法实现「按任务类型」的细粒度授权——例如某角色只被允许创建「数据迁移」和「数据清理」，其工号在创建任务时却仍能看到并提交其余类型。需要将任务类型纳入数据权限，实现任务类型级的可选项过滤与后端强制校验。

## What Changes

- 新增 6 个任务类型数据权限（`permission_type=2`），与 6 种任务类型一一对应，权限编码分别为 `task:type:migrate`（数据迁移）、`task:type:clean`（数据清理）、`task:type:migrate-clean`（迁移清理）、`task:type:structure`（结构迁移）、`task:type:data-check`（数据稽核）、`task:type:cdc`（增量迁移）。
- 角色授权的「数据权限」Tab 自动展示并支持勾选这 6 个任务类型权限（复用现有数据权限列表查询，无需新增授权入口）。
- 前端「创建任务」任务类型下拉框改为按当前用户已授权的任务类型权限动态过滤选项；系统管理员仍可见全部类型。
- 后端创建迁移任务接口 `createMoveTask` 的鉴权由动作级 `task:create` **替代**为按 `taskType` 校验对应任务类型权限（**BREAKING**：`task:create` 不再作为创建迁移任务的准入条件，拥有任一任务类型权限即可创建对应类型）。
- 存量兼容：一次性将持有 `task:create` 的角色自动补授全部 6 个任务类型权限，保证升级后原有用户行为不变。
- 授权范围仅限「迁移任务 > 创建任务」：`createTableLinkTask`（表链接任务）仍沿用 `task:create`，独立「结构迁移」页面不受本次任务类型权限约束。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `system-permission/data-permission`: 新增「任务类型级数据权限」需求——数据权限可细分到任务类型，后端按 `taskType` 校验对应类型权限，前端创建任务按已授权任务类型过滤下拉选项；并定义存量 `task:create` 向任务类型权限的兼容迁移。

## Impact

- 后端鉴权：`datastream-security` 的 `PermissionService` 新增 `hasTaskTypePermission(Integer taskType)`（管理员放行，普通用户按 `taskType → task:type:*` 编码校验），`TaskController.createMoveTask` 改用该鉴权。
- 权限种子数据：`doc/sql/datastream-rbac-seed.sql` 新增 6 个任务类型权限记录；新增存量环境迁移 SQL（幂等），将持有 `task:create` 的角色补授 6 个类型权限。
- 前端：`datastream-ui/src/constants/taskConstants.js` 维护「任务类型 → 权限编码」映射；`datastream-ui/src/views/taskmanage/components/TaskCreateDrawer.vue` 的任务类型下拉框改为按权限过滤渲染。
- 权限编码与任务类型映射需在前端与后端保持一致（见 design.md）。
