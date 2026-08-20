## 1. 修复响应拦截器错误引用

- [x] 1.1 将 `datastream-ui/src/utils/fetch.js` 响应拦截器错误处理分支中的三处 `Message` 引用替换为 `ElMessage`（`Message.closeAll()` → `ElMessage.closeAll()`，`Message.error('登录已失效，请重新登录！')` → `ElMessage.error('登录已失效，请重新登录！')`）
- [x] 1.2 确认文件顶部已从 `element-plus` 导入 `ElMessage`，无需新增导入

## 2. 验证

- [x] 2.1 运行前端构建/静态检查，确认无 `ReferenceError` 或语法错误
- [x] 2.2 验证非 401 失败仅关闭提示并向上层传递错误，不再抛出 `Message is not defined`
- [x] 2.3 验证 401 场景清除 token、跳转登录页并显示「登录已失效，请重新登录！」提示
