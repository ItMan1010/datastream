## Why

当前迁移任务列表对所有登录用户返回全部任务数据，普通用户可看到他人创建的任务，缺少按创建人的数据范围隔离。需要在既有 RBAC（角色/权限）基础上补充行级数据范围控制，使普通用户仅可见本人创建的任务，系统管理员可见全部任务。

## What Changes

- 对迁移任务列表查询（`queryDataMoveTaskList` 及其 count）按创建人进行行级过滤：普通用户仅返回 `system_user_code` 等于当前登录用户的记录，系统管理员返回全部记录。
- 对按 `taskId` 读取任务详情/进度/执行明细/表结构明细的接口补充归属校验，防止普通用户通过任务 ID 越权访问他人任务数据。
- 后端提供获取当前登录用户标识与管理员判定的能力，供任务查询链路复用，判定规则与现有 RBAC 一致（`SYSTEM_ADMIN` 角色）。

## Capabilities

### New Capabilities

- `system-permission/data-scope`: 迁移任务数据的行级数据范围控制，普通用户仅可见本人创建的任务，系统管理员可见全部任务。

### Modified Capabilities

<!-- 无 -->

## Impact

- 后端 `datastream-admin`：`TaskController`、`DataMoveHandler`、`MetaServiceImpl`、`IMetaService`、`DataStreamDao` 及任务查询相关方法需透传创建人过滤条件。
- 后端 `datastream-engine`：`DataStreamMapper` / `DataStreamMapper.xml` 中任务列表与 count 的查询 SQL 增加 `system_user_code` 过滤条件。
- 后端 `datastream-security`：复用或扩展 `PermissionService` 以支持获取当前登录用户与管理员判定。
- 前端 `datastream-ui`：无需改动；数据范围由后端强制过滤，前端无需额外隐藏逻辑。
