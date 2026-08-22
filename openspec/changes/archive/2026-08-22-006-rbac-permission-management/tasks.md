## 1. 数据模型与 DDL

- [x] 1.1 在 `doc/sql/datastream-h2-ddl.sql` 与 `doc/sql/datastream-mysql-ddl.sql` 新增表：`data_stream_system_user`、`data_stream_role`、`data_stream_user_role`、`data_stream_permission`、`data_stream_role_permission`（字段与 D1 设计一致，含 built_in/state/create_date/update_date）
- [x] 1.2 在 `data_stream_sequence` 预置新序列：`SEQ_SYSTEM_USER_ID`、`SEQ_ROLE_ID`、`SEQ_USER_ROLE_ID`、`SEQ_PERMISSION_ID`、`SEQ_ROLE_PERMISSION_ID`，起始值 50000（H2 与 MySQL 两份 DDL 同步）
- [x] 1.3 编写内置数据初始化脚本：内置系统管理员账号 admin（BCrypt 密码 admin）、内置系统管理员角色（role_code=SYSTEM_ADMIN）、内置菜单权限（对应现有静态菜单索引与路由）与内置数据权限（task:create、task:execute、task:delete 等），并写入 `data_stream_role_permission`

## 2. 后端实体、DAO 与 Mapper

- [x] 2.1 在 `datastream-common` 新增实体：`SystemUserEntity`、`RoleEntity`、`UserRoleEntity`、`PermissionEntity`、`RolePermissionEntity`（沿用现有 entity 命名规范）
- [x] 2.2 在 `datastream-engine/.../dao` 新增 `SystemUserDao`/`RoleDao`/`PermissionDao`（或合并的权限管理 DAO），含用户 CRUD、角色 CRUD、用户-角色、角色-权限关联读写
- [x] 2.3 在 `datastream-engine/src/main/resources/mapper/` 新增对应 MyBatis XML（`SystemUserMapper.xml` 等），SQL 兼容 H2 与 MySQL，分页沿用现有 `makeSqlLimit` 模式
- [x] 2.4 关联操作与删除保护 SQL：删除用户/角色时级联清理关联表；查询用户时一次返回其全部角色与权限编码

## 3. 认证与权限加载（datastream-security）

- [x] 3.1 新增数据库用户加载：`UserDetailsServiceImpl` 增加数据库模式，按账号查询用户、校验 BCrypt 密码、加载全部角色权限并集作为 authorities（`ROLE_<roleCode>` + `PERM_<permissionCode>`，内置管理员加 `ROLE_SYSTEM_ADMIN`）
- [x] 3.2 修正 `DsJwtUser` 密码处理：数据库模式直接使用存储的 BCrypt 哈希，避免重复编码；携带 roles/permissions 供登录响应使用
- [x] 3.3 登录成功响应返回用户信息、角色列表、权限编码集合与允许的菜单索引/路由列表
- [x] 3.4 兼容既有 SSO 模式与本地测试模式：SSO 模式映射内置普通角色，本地测试模式（admin/admin）可用内置数据或映射管理员角色

## 4. 数据权限后端鉴权

- [x] 4.1 启用方法级安全：`SecurityConfig` 增加 `@EnableGlobalMethodSecurity(prePostEnabled = true)`
- [x] 4.2 新增 `PermissionService.hasPermission(code)`：`ROLE_SYSTEM_ADMIN` 直接放行，否则校验 `PERM_<code>`；无权限返回明确错误信息（403/业务错误码）
- [x] 4.3 为受保护业务接口加 `@PreAuthorize("@permissionService.hasPermission('...')")`：迁移任务创建（task:create）、执行（task:execute）、删除（task:delete）等，逐接口核对内置数据权限清单
- [x] 4.4 校验受保护接口完整性：以内置数据权限清单为锚点，核对所有应鉴权接口均已加注解，无遗漏越权

## 5. 权限管理接口（datastream-admin）

- [x] 5.1 用户管理接口：新增/编辑/禁用/启用/删除/重置密码/分页查询；账号唯一校验；内置 admin 禁止删除、禁用与降权
- [x] 5.2 角色管理接口：角色新增/编辑/删除/分页查询；角色名称唯一；被使用角色与内置系统管理员角色禁止删除/修改；用户-角色派发接口
- [x] 5.3 权限资源接口：菜单权限树与数据权限列表查询、权限资源维护（内置资源保护）、角色-权限授权接口
- [x] 5.4 上述 Controller/Service 沿用现有命名与返回结构（`ResponseEntity` + 错误码/错误信息），关键操作加 `@LogOperate`

## 6. 操作审计增强

- [x] 6.1 增强 `LogAspect`/`SystemLog`：记录 request_info、response_info、耗时与执行结果（成功/失败），写入 `data_stream_system_log`
- [x] 6.2 为关键业务操作补齐 `@LogOperate` 注解（用户/角色/权限管理、迁移任务创建/执行/删除等）
- [x] 6.3 扩展 `SystemLogDao`/`SystemLogController` 查询：支持按操作人、模块、时间范围、关键字条件过滤（登录/操作日志）
- [x] 6.4 日志安全：日志删除/修改接口仅系统管理员可用，普通用户无权限（接口鉴权校验）

## 7. 前端（datastream-ui）

- [x] 7.1 登录响应权限存储：`store/index.js` 增存角色、权限编码集合、允许菜单索引；`localStorage` 持久化
- [x] 7.2 菜单与路由权限过滤：`useMenuConfig.js`/布局按允许菜单索引渲染；`router.beforeEach` 拦截无权限路由并提示；无权限操作按钮隐藏或点击提示
- [x] 7.3 用户管理页面：列表/新增/编辑/禁用/启用/重置密码/删除（内置 admin 保护提示），沿用现有页面布局与 Element Plus 风格
- [x] 7.4 角色管理页面：角色列表/新增/编辑/删除、用户派发、权限授权（内置角色保护提示）
- [x] 7.5 权限管理页面：菜单权限树与数据权限列表、角色-权限授权界面
- [x] 7.6 新增权限相关 API 封装与 composable（沿用 `useColumnTypeConfig.js` 风格），在 系统管理 父菜单下追加新菜单索引与路由

## 8. 验证

- [x] 8.1 后端编译与接口冒烟：用户/角色/权限 CRUD、登录返回权限、内置保护规则生效
- [x] 8.2 数据权限验证：仅「执行迁移」权限用户创建任务被拒、执行任务成功；系统管理员全通过
- [x] 8.3 菜单权限验证：无权限菜单不可见、路由访问被拦截；权限变更后重新登录生效
- [x] 8.4 操作审计验证：登录与关键操作生成详细日志（含请求/响应/耗时/结果），支持条件查询，普通用户不可删除日志
- [x] 8.5 兼容性验证：默认 admin/admin 登录正常；SSO 模式与本地测试模式不回归；H2 与 MySQL 两份 DDL 一致、序列主键正确
