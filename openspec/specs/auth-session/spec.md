## Purpose

定义后端登录会话的有效性校验行为，确保会话记录不存在（被删除或已注销）时，token 被判定为已失效并返回 401。

## Requirements

### Requirement: 会话不存在视为已失效

后端在校验 token 有效性时，若数据库中不存在对应的有效会话记录（`state = 1`），SHALL 判定该 token 已失效并返回 401，MUST NOT 继续放行请求。

#### Scenario: 会话被删除后请求被拒绝
- **WHEN** 数据库中的会话记录被删除或 state 不为 1
- **THEN** 系统判定 token 已失效，返回 401，前端跳转登录页并提示「登录已失效」
