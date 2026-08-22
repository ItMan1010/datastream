## 1. 权限种子与迁移 SQL

- [x] 1.1 在 `doc/sql/datastream-rbac-seed.sql` 新增 6 个 `task:type:*` 数据权限 INSERT（`permission_type=2`、`built_in=1`，编码与名称按 design.md 映射表）
- [x] 1.2 新增存量环境迁移 SQL（幂等）：插入 6 个类型权限后，将持有 `task:create` 权限的角色补授全部 6 个类型权限
- [x] 1.3 校验权限编码 `task:type:*` 唯一、`permission_type` 为 2，且不与既有权限编码冲突

## 2. 后端鉴权

- [x] 2.1 在 `PermissionService` 内定义 taskType → `task:type:*` 权限编码映射（与 design.md 一致）
- [x] 2.2 在 `PermissionService` 新增 `hasTaskTypePermission(Integer taskType)`：`isAdmin()` 放行；taskType 为空或无法映射时拒绝；否则复用 `checkPermission` 与 `buildSingleDeniedMessage` 抛缺失权限
- [x] 2.3 将 `TaskController.createMoveTask` 的 `@PreAuthorize` 由 `task:create` 改为 `@permissionService.hasTaskTypePermission(#createMoveTaskRequest.taskType)`
- [x] 2.4 保持 `TaskController.createTableLinkTask` 的 `task:create` 鉴权不变，确认表链接任务创建无回归

## 3. 前端过滤

- [x] 3.1 在 `taskConstants.js` 新增 `TASK_TYPE_PERMISSION_MAP`（value → permission_code），并为 `TASK_TYPE_OPTIONS` 各选项补充 `permission` 字段
- [x] 3.2 将 `TaskCreateDrawer.vue` 任务类型下拉框改为 `v-for` 渲染 `computed` 过滤后的选项（使用 `usePermission().hasPermission`）
- [x] 3.3 确认系统管理员（`getIsAdmin`）仍展示全部 6 种任务类型

## 4. 验证

- [x] 4.1 后端编译、启动通过（无鉴权注解 SpEL 解析错误）
- [x] 4.2 角色仅授权「数据迁移」「数据清理」后，该角色工号重新登录，创建任务下拉框仅见这两类
- [x] 4.3 无「结构迁移」权限的用户直接提交创建结构迁移请求，被后端拒绝并返回含中文名与编码 `task:type:structure` 的提示
- [x] 4.4 存量持有 `task:create` 的角色执行迁移 SQL 后，其工号仍可见全部 6 种任务类型
- [x] 4.5 系统管理员可见全部任务类型，且表链接任务创建不受影响
