## 1. 修复会话失效判断逻辑

- [x] 1.1 修改 `datastream-engine/src/main/java/com/itman/datastream/engine/systemlog/impl/SystemLogServiceImpl.java` 的 `isTokenExpiration` 方法：将 `Objects.isNull(systemSession)` 分支的返回值由 `false` 改为 `true`

## 2. 验证

- [x] 2.1 编译后端模块，确认无语法/编译错误
- [x] 2.2 验证删除 `data_stream_session` 记录后，携带原 token 的请求返回 401
- [x] 2.3 验证注销登录（state 置为 2）后，携带原 token 的请求返回 401
