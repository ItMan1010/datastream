## Why

数据库字段类型定义（`data_stream_column_type_define`）与类型映射关系（`data_stream_column_type_map`）当前仅能通过 SQL 脚本在 `doc/sql/datastream-column-type-map-v2.sql` 中手工维护，平台没有任何可视化操作入口。当新增数据源类型或调整映射规则时，需要研发直接改库，效率低且易出错。需要将这些基础配置界面化，纳入「配置管理」菜单，让运维/实施人员可以在页面上进行增删改查。

## What Changes

- 新增一个前端配置页面，放在「配置管理」菜单下，页面内以两个 Tab 组织：
  - **类型定义** Tab：管理 `data_stream_column_type_define`（数据库字段类型名称定义）。
  - **类型映射** Tab：管理 `data_stream_column_type_map`（数据库字段类型名称映射）。
- 后端新增对两张表的 CRUD 接口（分页查询、详情、新增、编辑、删除、批量删除）。
- 新增数据保护约束，避免破坏被结构迁移/类型映射引擎依赖的只读数据：
  - 禁止删除被映射表引用（`column_type_define_id_a` / `column_type_define_id_b`）的类型定义。
  - 禁止删除库内置类型（由 SQL 脚本预置的 mysql/pg/oracle 内置定义与映射）。
  - 新增/编辑映射时校验引用的类型定义存在，并根据 `match_level` 分类给出警告提示。
- 内置默认数据仍由 SQL 脚本初始化，界面只做后续维护，不提供一键重置。

## Capabilities

### New Capabilities

- `config/column-type-config`: 提供数据库字段类型定义与类型映射的界面化配置能力，含只读引用保护、内置数据保护与映射外键校验，菜单归属「配置管理」。

### Modified Capabilities

<!-- 无既有 capability 需要修改。类型定义/映射的读写均由 SQL 变更脚本维护，本 change 仅新增管理界面与受保护的写操作，不改变现有只读读取语义。 -->

## Impact

- **后端 `datastream-admin`**：
  - 新增 `ColumnTypeConfigController`（路由 `/api/columnTypeConfig`）及相关 request/response domain。
  - 新增 `IColumnTypeConfigService` 与实现，承载分页/CRUD 及保护校验逻辑。
- **后端 `datastream-engine`**：
  - 新增/扩展 `ColumnTypeConfigMapper.java` 与 `ColumnTypeConfigMapper.xml`（或复用 `DataStreamMapper`），提供两表的 CRUD SQL；保留现有 `queryColumnTypeDefine` / `queryColumnTypeMap` 只读方法不变。
- **后端 `datastream-common`**：
  - 复用已有实体 `ColumnTypeDefineEntity` / `ColumnTypeMapEntity`（含映射关联展示所需的类型名称字段）。
- **前端 `datastream-ui`**：
  - 新增视图 `src/views/column-type/index.vue`（含双 Tab 与两套 CRUD 交互）。
  - 新增 Composable `useColumnTypeConfig.js`。
  - 注册路由 `/datastream/config/columnTypeConfig` 与后端 API。
  - 更新菜单配置 `useMenuConfig.js`（配置管理 `14` 下新增子菜单、面包屑、图标）。
- **文档**：无需修改底层初始化脚本。