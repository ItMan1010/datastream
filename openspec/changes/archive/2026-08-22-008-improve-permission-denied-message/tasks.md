## 1. 后端权限异常与名称查询

- [x] 1.1 在 `datastream-security` 新增 `PermissionDeniedException`（继承 `AccessDeniedException`），携带缺失权限的可读中文信息
- [x] 1.2 改造 `PermissionService`：新增私有 `checkPermission(String code)` 返回 boolean；`hasPermission` 失败时抛 `PermissionDeniedException`；`hasAnyPermission` 全部失败时抛组合异常
- [x] 1.3 在 `SystemPermissionDao` / `SystemPermissionMapper` 新增 `selectPermissionNameByCode`，按 `permission_code` 查 `permission_name`
- [x] 1.4 `PermissionService` 注入 DAO 查询权限中文名，加入惰性加载缓存，名称为空或查询失败时回退为「该操作」并保留权限编码

## 2. 后端统一拒绝处理

- [x] 2.1 修改 `SecurityConfig` 的 `accessDeniedHandler`：当异常为 `PermissionDeniedException` 时返回其 message，否则回退通用中文提示「无权限执行该操作」

## 3. 前端 403 提示

- [x] 3.1 修改 `datastream-ui/src/utils/fetch.js` 响应拦截器：新增 403 分支，从 `error.response.data.resultMsg` 提取中文信息并 `Promise.reject`，无响应体时回退通用中文提示

## 4. 验证

- [x] 4.1 后端编译通过（`mvn -pl datastream-security,datastream-engine -am compile`）
- [x] 4.2 使用无 `task:create` 权限的工号（如 jack）创建迁移任务，确认返回并展示「无【创建迁移任务（task:create）】权限，请联系管理员」类中文提示，不再出现 AxiosError 文案
- [x] 4.3 使用 `admin` 或具备权限的工号创建任务，确认正常创建不受影响
