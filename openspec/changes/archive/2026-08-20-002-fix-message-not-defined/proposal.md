## Why

前端「查询任务」等请求在返回非 2xx 状态码（如 401、500）时，控制台/界面提示 `查询任务失败：ReferenceError: Message is not defined`。根因是 `datastream-ui/src/utils/fetch.js` 的响应拦截器错误分支引用了未导入的全局标识符 `Message`，而文件顶部实际导入的是 `ElMessage`。错误处理本身抛异常，既掩盖了真实的后端错误信息，也会在 401 时无法正确跳转登录页。

## What Changes

- 修复 `datastream-ui/src/utils/fetch.js` 响应拦截器错误处理分支，将三处 `Message` 引用改为已导入的 `ElMessage`：
  - `Message.closeAll()` → `ElMessage.closeAll()`
  - `Message.error('登录已失效，请重新登录！')` → `ElMessage.error('登录已失效，请重新登录！')`

## Capabilities

### New Capabilities

- `frontend-http`: 定义前端 HTTP 请求/响应拦截器的行为规范——请求鉴权头注入、响应错误提示、401 失效跳转登录等可观测行为。

### Modified Capabilities

<!-- 无既有 capability 需要修改 -->

## Impact

- **前端 `datastream-ui`**（唯一影响）：
  - `src/utils/fetch.js`（响应拦截器错误处理分支）
- 后端无改动。
