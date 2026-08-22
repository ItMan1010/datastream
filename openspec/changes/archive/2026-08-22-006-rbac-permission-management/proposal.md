## Why

当前系统只有登录认证（JWT + 会话）与基于 `@LogOperate` 的零散操作日志，没有完整的权限管理体系：`SystemUser` 实体无角色/权限字段，`UserDetailsServiceImpl` 为所有用户硬编码单一权限 `ROLE_TASK_ALL`，前端菜单完全静态渲染。任何登录用户都能访问全部功能，无法按用户区分「谁可以创建迁移任务、谁只能执行迁移」，也无法做精细化的操作留痕，无法满足企业级使用要求。

## What Changes

- 新增系统用户管理：管理员（最高权限为系统管理员）可新增用户、编辑、禁用/启用、删除用户、重置密码；用户信息包含登录账号、显示名、状态、所属角色等。
- 新增角色管理：角色的增删改查，以及用户与角色的派发关系（多对多）；内置系统管理员角色，不可删除。
- 新增菜单权限：维护菜单/权限资源树，将菜单权限授予角色；前端根据当前用户权限动态渲染菜单、控制路由与页面入口，无权限菜单不可见。
- 新增数据权限：对业务功能级操作进行授权（例如「创建数据迁移任务」「仅执行数据迁移」），后端在接口层拦截校验，无权限用户被拒绝并返回明确错误。
- 登录流程改造：登录成功后按用户角色加载其权限集，随登录响应返回，供前端渲染与后端鉴权使用；`UserDetailsServiceImpl` 不再为所有用户硬编码 `ROLE_TASK_ALL`。
- 操作审计增强：每个登录用户的关键操作都详细记录（操作人、模块、内容、对象标识、IP、浏览器、URL、请求/响应信息、耗时、结果），登录日志与操作日志分类存储、支持查询。
- 新增「系统管理」下的权限管理相关菜单与页面：用户管理、角色管理、菜单权限、数据权限（沿用现有 `系统管理` 父菜单，复用现有 `useMenuConfig.js` / 路由结构）。

## Capabilities

### New Capabilities
- `system-permission/user-management`: 系统用户管理（新增/编辑/禁用/启用/删除、重置密码、内置管理员保护）
- `system-permission/role-management`: 角色管理（角色 CRUD、用户-角色派发、内置管理员角色保护）
- `system-permission/menu-permission`: 菜单权限（权限资源/菜单树维护、角色-菜单授权、前端动态菜单与路由控制）
- `system-permission/data-permission`: 数据权限（业务功能操作级授权、后端接口鉴权拦截）
- `system-permission/operation-audit`: 操作审计（登录与操作日志的详细记录、存储与查询）

### Modified Capabilities
- 无（现有 `auth-session` 仅涉及会话有效性，不改变其需求；权限加载与鉴权行为由上述新能力覆盖）

## Impact

- **后端**：
  - `datastream-security`：`SystemUser` 增加角色/权限相关字段；`UserDetailsServiceImpl` 从数据库按账号加载用户、角色与权限，替换硬编码 `ROLE_TASK_ALL`；`DsJwtUser` 承载权限；JWT 过滤器/安全配置增加接口鉴权入口。
  - `datastream-admin`：新增用户、角色、权限管理 Controller/Service/DAO；新增数据权限拦截校验；`LogAspect`/`SystemLog` 增强请求、响应、耗时等详细记录。
  - 元数据库（H2/MySQL）新增表：`data_stream_system_user`、`data_stream_role`、`data_stream_user_role`、`data_stream_permission`、`data_stream_role_permission`（或等价建模），并预设序列；同步 `doc/sql/datastream-h2-ddl.sql` 与 `doc/sql/datastream-mysql-ddl.sql`。
- **前端**（`datastream-ui`）：新增用户管理、角色管理、菜单权限、数据权限页面；登录后按权限动态渲染菜单与路由；新增权限相关 API 封装。
- **数据初始化**：内置系统管理员账号（admin）与系统管理员角色，保证平台首次启动可用。
- **配置**：`auth.local.systemUserAuth` 相关测试模式逻辑需兼容既有默认账号 admin/admin。
