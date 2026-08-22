## Context

现有系统只有认证、无授权：`SystemUser` 实体无角色/权限字段，[UserDetailsServiceImpl.java](file:///Users/jack2/Documents/soft/datastream/datastream-security/src/main/java/com/itman/datastream/security/handler/UserDetailsServiceImpl.java) 对 SSO 模式硬编码 `ROLE_TASK_ALL`，本地测试模式仅校验 admin/admin；[DsJwtUser.java](file:///Users/jack2/Documents/soft/datastream/datastream-security/src/main/java/com/itman/datastream/security/jwt/DsJwtUser.java) 同样固定单一权限。前端菜单为静态配置（[useMenuConfig.js](file:///Users/jack2/Documents/soft/datastream/datastream-ui/src/composables/useMenuConfig.js)），路由全量注册，无权限过滤。

工程结构约束（见 proposal.md Impact）：
- 数据访问：DAO 位于 `datastream-engine/.../engine/dao`，MyBatis XML 位于 `datastream-engine/src/main/resources/mapper/`，实体位于 `datastream-common/.../common/entity`。
- 业务/接口：Service 与 Controller 位于 `datastream-admin/.../admin`。
- 认证与安全：位于 `datastream-security`，`SecurityConfig` 使用 JWT 过滤器 + `UserDetailsService`，已有 `@LogOperate` 操作日志切面与 `data_stream_system_log` 表。
- 元数据库：H2 默认、可切 MySQL；DDL 分别维护 `doc/sql/datastream-h2-ddl.sql` 与 `doc/sql/datastream-mysql-ddl.sql`；主键由 `data_stream_sequence` 序列下发（历史教训：新序列须预置起始值，否则主键为 0）。

## Goals / Non-Goals

**Goals:**
- 引入基于角色的访问控制（RBAC）：用户-角色-权限（菜单权限 + 数据权限）三层模型。
- 数据库承载用户/角色/权限，登录时按账号加载角色与权限，替换硬编码权限。
- 后端接口级数据权限鉴权（如创建迁移任务 vs 仅执行迁移），系统管理员全通过。
- 前端按权限动态渲染菜单与路由控制。
- 复用并增强现有 `data_stream_system_log` / `@LogOperate` 实现详细操作审计。
- 内置系统管理员账号与角色，保证首次启动可用，兼容默认 admin/admin。

**Non-Goals:**
- 不做细粒度行级/字段级数据脱敏或行过滤（当前「数据权限」指功能操作级授权，非按行数据范围隔离）。
- 不改动 SSO 外部鉴权协议本身；SSO 模式保持可用，本变更新增数据库用户模式。
- 不做用户组织/机构树管理（保留现有 orgId/orgName 字段，暂不扩展组织树）。

## Decisions

### D1: RBAC 数据模型（5 张新表 + 序列）
采用经典 RBAC 模型，沿用现有 `data_stream_` 前缀与序列机制：
- `data_stream_system_user`（用户：system_user_id、system_user_code 唯一登录账号、system_user_name、password、org_id、org_name、username、state、create_date、update_date）
- `data_stream_role`（角色：role_id、role_code、role_name 唯一、description、built_in 内置标记）
- `data_stream_user_role`（用户-角色：user_role_id、system_user_id、role_id）
- `data_stream_permission`（权限资源：permission_id、permission_code 唯一、permission_name、permission_type 1=菜单/2=数据操作、parent_id、sort_no、route 菜单路由标识、built_in）
- `data_stream_role_permission`（角色-权限：role_permission_id、role_id、permission_id）

新序列 `SEQ_SYSTEM_USER_ID / SEQ_ROLE_ID / SEQ_USER_ROLE_ID / SEQ_PERMISSION_ID / SEQ_ROLE_PERMISSION_ID` 均预置起始值 50000（与既有配置约定一致）。同步维护 H2 与 MySQL 两份 DDL。
- 备选：仅扩展 `SystemUser` 实体 + 角色字段（多对多用逗号串）→ 关联查询与权限变更追溯困难，弃用。

### D2: 权限粒度与编码
权限分两类：菜单权限（permission_type=1，对应前端菜单/路由）与数据权限（permission_type=2，对应业务功能操作点，如 `task:create`、`task:execute`、`task:delete`）。权限编码使用 `域:动作` 形式（如 `task:create`），作为 Spring Security 授权标识。
- 备选：仅菜单权限 + 全量放行后端 → 无法满足「有的用户只能执行迁移」的需求，弃用。

### D3: 登录时加载权限，改造 UserDetailsService
在 `datastream-security` 新增数据库用户加载逻辑（新增 `SystemUserDao`/`AuthService` 供 security 模块引用）：`UserDetailsServiceImpl.loadUserByUsername` 增加数据库模式——按账号查用户、校验 BCrypt 密码、加载其全部角色的权限并集，返回携带 authorities（`ROLE_<roleCode>` + `PERM_<permissionCode>`）的 `DsJwtUser`。内置管理员角色额外授予 `ROLE_SYSTEM_ADMIN`。
- 修复 `DsJwtUser` 当前「对已存储密码再次 BCrypt 编码」的问题：数据库模式下直接使用存储的 BCrypt 哈希（用户创建/重置密码时一次性编码存储）。
- 保留 SSO 模式与本地测试模式作为可选配置，默认切换为数据库模式。
- 备选：在过滤器里逐请求查库 → 性能差且职责混乱，弃用。

### D4: 后端数据权限鉴权
启用 `@EnableGlobalMethodSecurity(prePostEnabled = true)`，对受保护的业务接口（如 TaskController 的创建/执行/删除迁移任务）加 `@PreAuthorize("@permissionService.hasPermission('task:create')")`。`PermissionService.hasPermission`：当前认证包含 `ROLE_SYSTEM_ADMIN` 则直接放行，否则校验 authorities 是否含 `PERM_<code>`。无权限抛 403 并返回明确错误信息。
- 备选：自研过滤器按 URL 匹配权限 → 与现有注解式 `@LogOperate` 风格不一致、URL 易漂移，弃用。

### D5: 前端动态菜单与路由控制
登录响应携带用户信息 + 角色 + 权限编码集合 + 允许的菜单索引/路由列表。前端：
- `useMenuConfig.js` 保留静态菜单字典；根据登录返回的允许索引过滤渲染（沿用现有数字索引与面包屑机制）。
- 路由守卫 `router.beforeEach` 增加权限校验：无权限路由跳转首页并提示无权限。
- Pinia `store/index.js` 增存角色、权限编码与允许菜单；`localStorage` 持久化。
- 无权限操作入口按权限编码隐藏或点击提示（复用按钮级 v-if）。
- 备选：后端下发完整菜单树动态注册路由 → 改动面大、与现有静态菜单/面包屑机制冲突，弃用。

### D6: 操作审计增强
复用 `data_stream_system_log` + `LogAspect` + `@LogOperate`：为关键业务操作补齐 `@LogOperate` 注解并写入 request_info/response_info、耗时与执行结果；登录日志（type=1）沿用现状。`SystemLogDao`/`SystemLogController` 查询能力扩展为支持 操作人、模块、时间范围、关键字 条件过滤；删除/修改日志接口不对普通用户开放（仅系统管理员可查询）。

### D7: 内置数据初始化
启动时（或 DDL 脚本 + 数据脚本）内置：
- 系统管理员账号 admin/admin（BCrypt 存储），绑定内置系统管理员角色；
- 系统管理员角色（role_code=SYSTEM_ADMIN，built_in=1，不可删除/修改）；
- 与现有静态菜单对应的内置菜单权限（菜单索引 → permission_code），以及任务创建/执行/删除等内置数据权限。

### D8: 管理页面与菜单
新增页面（沿用现有 系统管理 父菜单与 `system-manage/` 目录、Element Plus 表格/表单风格、`useColumnTypeConfig.js` 式 composable 封装）：
- 用户管理（列表/新增/编辑/禁用/启用/重置密码/删除，禁止操作内置 admin）
- 角色管理（列表/新增/编辑/删除/用户派发/权限授权，内置角色保护）
- 权限管理（菜单权限与数据权限资源树维护、角色-权限授权）
路由与菜单索引沿用 `useMenuConfig.js` 增量追加（系统管理父菜单下新增子菜单索引）。

## Risks / Trade-offs

- [密码编码历史问题] 现有 `DsJwtUser` 对密码重复 BCrypt 编码，若直接复用会导致数据库模式登录失败 → 数据库模式改为使用存储哈希，并在设计评审时确认本地测试模式兼容性。
- [H2/MySQL DDL 漂移] 双 DDL 文件易不一致 → 新增表与序列同时维护两份文件，并以 H2 默认库冒烟验证。
- [序列未预置] 新表若沿用未预置序列会出现主键 0 → 新增序列写入 `data_stream_sequence` 且起始值 50000。
- [权限变更即时性] 角色权限调整需重新登录生效（按 D3 登录时加载）→ 规格已明确「重新登录后生效」，并在前端提示。
- [既有接口放行] 为迁移任务等业务接口补 `@PreAuthorize` 时若遗漏，会出现越权 → 以内置数据权限清单为锚点逐一核对受保护接口并在任务中列校验清单。
- [SSO 兼容] SSO 模式外部返回的用户信息无角色 → SSO 模式默认授予内置普通角色或按账号本地映射，保证不阻断现有 SSO 登录（任务中明确处理）。

## Migration Plan

1. 新增 DDL（H2 + MySQL）与序列预置、内置数据脚本（admin、系统管理员角色、菜单/数据权限种子）。
2. 后端：实体/DAO/Mapper → security 加载改造 → PermissionService + 方法级鉴权 → 管理接口（用户/角色/权限）→ 操作审计增强。
3. 前端：登录响应权限存储 → 菜单/路由过滤 → 用户/角色/权限管理页面。
4. 验证：默认 admin 登录正常；普通用户仅执行迁移时创建任务被拒；无权限菜单不可见；操作日志含请求/响应与耗时。
5. 回滚：本变更以新增表与新增代码为主，不修改既有表结构；回滚即移除新表与相关代码，不影响原登录/SSO 路径。
