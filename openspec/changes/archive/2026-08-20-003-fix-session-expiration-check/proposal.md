## Why

在数据库中删除会话（`data_stream_session` 记录）或注销登录（state 置为 2）后，前端继续操作仍不报 401，请求被正常放行。根因是 `SystemLogServiceImpl.isTokenExpiration` 在 `selectSystemSession` 返回 `null` 时错误地返回 `false`（未过期），把「会话不存在」当成了「token 有效」，导致 JWT 过滤器继续认证并放行，登录失效校验形同虚设。

## What Changes

- 修复 `datastream-engine` 中 `SystemLogServiceImpl.isTokenExpiration` 的判断逻辑：当查询不到有效的会话记录（`systemSession == null`）时，返回 `true`（已失效）而非 `false`。
- 使「删除会话」或「注销后 state 不为 1」这两种情况都能正确触发 401，前端据此跳转登录并提示「登录已失效」。

## Capabilities

### New Capabilities

- `auth-session`: 定义后端登录会话的有效性校验与失效处理行为——会话记录不存在（被删除或已注销）时，token 应被判定为已失效并返回 401。

### Modified Capabilities

<!-- 无既有 capability 需要修改 -->

## Impact

- **后端 `datastream-engine`**（唯一影响）：
  - `src/main/java/com/itman/datastream/engine/systemlog/impl/SystemLogServiceImpl.java`（`isTokenExpiration` 方法）
- 前端无改动。
