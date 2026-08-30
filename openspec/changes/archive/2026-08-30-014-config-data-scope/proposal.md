## Why

当前行级数据范围（变更 009）仅覆盖迁移任务（`data_stream_move_task`）：普通用户仅可见本人创建的任务，系统管理员可见全部。但配置管理数据（数据源、MQ 配置、文件格式、表链接）既未记录创建人工号（`system_user_code`），也未做行级过滤，普通用户可查看/修改/删除/复用他人配置；概览页统计接口也未按工号过滤，普通用户能看到全平台的任务与数据源统计。需要在既有 RBAC 基础上，将工号数据范围扩展到配置管理与概览统计，使普通用户仅见本人数据、系统管理员可见全部。

## What Changes

- 配置管理数据（数据源、MQ 配置、文件格式、表链接）在新增时记录创建人工号 `system_user_code`（后端从认证上下文取值，不信任前端传参）。
- 配置管理列表/总数/明细查询按工号行级过滤：普通用户仅返回 `system_user_code` 等于当前登录工号的记录，系统管理员返回全部。
- 配置明细的修改/删除/上下线/测试/复制等按 ID 操作补充归属校验，普通用户越权访问他人配置时返回无权限错误。
- 概览统计（数据源总数、迁移任务总数/运行数、回迁任务总数/运行数、按日趋势、任务类型分布、任务状态分布）按工号过滤，普通用户仅统计本人数据。
- 迁移任务查询（列表/总数/详情归属校验）已由变更 009 完成，本次仅保持既有行为，不做重复改造。
- 字段类型定义/映射（`data_stream_column_type_define` / `data_stream_column_type_map`）为平台级共享参考数据（被迁移引擎全局消费），不纳入工号过滤范围。

## Capabilities

### New Capabilities

- `system-permission/config-data-scope`: 配置管理数据的工号行级数据范围控制——配置数据记录创建人工号，普通用户仅见本人配置，系统管理员可见全部。

### Modified Capabilities

- `overview-task-statistics`: 概览统计接口增加工号过滤行为，普通用户仅统计本人任务与数据源数据。

## Impact

- 后端 `datastream-common`：`DataBaseEntity`、`MQConfigEntity`、`FileFormatEntity`、`TableLinkEntity` 增加 `systemUserCode` 字段；`DataStreamErrorCode` 增加配置无权限错误码。
- 后端 `datastream-engine`：`DataStreamMapper(.xml)`、`MQConfigMapper(.xml)`、`FileMapper(.xml)`、`TableLinkMapper(.xml)` 及对应 Dao 为配置查询增加 `system_user_code` 列/过滤/插入；概览统计 SQL 增加工号过滤。
- 后端 `datastream-admin`：`DataBaseHandler`、`IMetaService`/`MetaServiceImpl`、`MQConfigServiceImpl`、`FileServiceImpl`、`TableLinkServiceImpl`（或对应 Handler）注入 `PermissionService`，计算过滤值、记录工号并做归属校验。
- 元数据库脚本 `doc/sql/datastream-h2-ddl.sql`、`doc/sql/datastream-mysql-ddl.sql` 及新增迁移脚本为配置表增加 `system_user_code` 列。
- 前端 `datastream-ui`：无需改动；数据范围由后端强制过滤，前端不新增隐藏逻辑。

## Non-Goals

- 不对字段类型定义/映射（共享参考数据）做工号过滤。
- 不修改迁移引擎内部（`StartExecutor`、`loadDataSourceByMoveTasks` 等）读取配置的调用语义，引擎内部读取仍返回全部数据。
- 不引入新的权限模型，复用既有 `SYSTEM_ADMIN` 角色判定与 `PermissionService` 能力。
