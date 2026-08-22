## Why

当前用户无数据权限时，后端统一返回固定文案「无权限执行该操作」，前端 HTTP 拦截器又对 403 直接抛出原始 AxiosError，最终业务层展示为「生成任务失败：AxiosError: Request failed with status code 403」。用户无法得知自己缺少的是哪一个具体权限，排查与授权成本高，需要让无权限提示准确、可读，并明确给出缺失的具体权限。

## What Changes

- 后端权限校验失败时，返回信息中携带缺失权限的「权限编码」与「权限中文名称」，例如「无【创建迁移任务（task:create）】权限，请联系管理员」。
- 后端统一权限拒绝处理（accessDeniedHandler）不再返回固定文案，而是透出本次被拒绝操作所需的权限信息；无法解析到具体权限时回退为通用中文提示。
- 前端 HTTP 拦截器针对 403 状态码，从后端响应中提取可读的中文错误信息并向上层抛出，而非原始 AxiosError。
- 前端业务层无权限提示展示具体缺失权限，避免出现「AxiosError: Request failed with status code 403」这类无意义文案。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `system-permission/data-permission`: 强化「后端对受保护操作鉴权」「前端对无权限操作提示」两条需求——无权限拒绝必须返回并展示具体缺失的数据权限（编码 + 中文名），而非笼统提示。
- `frontend-http`: 强化「请求失败错误处理」——403 状态码时提取后端返回的可读中文错误信息向上层抛出，替代原始 AxiosError。

## Impact

- 后端鉴权与拒绝处理：`datastream-security` 的 `PermissionService`（携带缺失权限信息）、`SecurityConfig` 的 `accessDeniedHandler`（透出具体权限文案）。
- 权限名称来源：`data_stream_permission.permission_name` 的查询（复用 `datastream-engine` 的 `SystemPermissionDao`）。
- 前端 HTTP 拦截器：`datastream-ui/src/utils/fetch.js`（403 分支提取 `resultMsg`）。
- 前端业务提示：受影响的创建任务等操作（如 `datastream-ui/src/composables/useTaskCreate.js`）改为展示可读中文提示。
