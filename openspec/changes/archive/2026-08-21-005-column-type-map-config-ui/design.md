## Context

两张表 `data_stream_column_type_define` 与 `data_stream_column_type_map` 目前只有只读查询路径：`DataStreamMapper.queryColumnTypeDefine` / `queryColumnTypeMap`（[DataStreamMapper.xml](file:///Users/jack2/Documents/soft/datastream/datastream-engine/src/main/resources/mapper/DataStreamMapper.xml#L1149-L1192)），被结构迁移 / 类型映射引擎消费。数据由 `doc/sql/datastream-column-type-map-v2.sql` 一次性脚本初始化，ID 来自序列 `seq_column_type_define_id` / `seq_column_type_map_id`。本 change 目标是新增界面化 CRUD，菜单挂在「配置管理」下（前端 [useMenuConfig.js](file:///Users/jack2/Documents/soft/datastream/datastream-ui/src/composables/useMenuConfig.js) 的父菜单索引 `14`），并保留现有只读路径不变。需求确认结果：**单个菜单 + 双 Tab**；启用三种保护（防删除被引用、禁止删除内置、映射外键校验）；**不加一键重置**，内置数据改由脚本维护。

参考的既有 CRUD 模式：MQ 配置（[MQConfigController.java](file:///Users/jack2/Documents/soft/datastream/datastream-admin/src/main/java/com/itman/datastream/admin/controller/MQConfigController.java)，Controller+Dao，含 `request/response` domain 与 `@LogOperate`）与文件格式（[FileServiceImpl.java](file:///Users/jack2/Documents/soft/datastream/datastream-admin/src/main/java/com/itman/datastream/admin/service/impl/FileServiceImpl.java)，Service 分层、`querySequence` 生成 ID）。前端参考 `fileformat` / `mq-manage`（View + Composable + `config/api.js` 常量 + 路由）。

## Goals / Non-Goals

**Goals:**

- 在「配置管理」下新增「字段类型配置」页面，双 Tab 分别管理类型定义与类型映射，支持增删改查。
- 提供保护约束：禁止删除被映射引用的类型定义、禁止删除库内置定义与映射、映射新增/编辑时校验外键存在性并提示匹配级别警告。
- 完全复用现有实体与只读查询，不改变结构迁移/映射引擎的既有行为。
- 新增操作日志（沿用 `@LogOperate`）与前后端一致的分页/详情交互风格。

**Non-Goals:**

- 不提供一键重置内置默认数据的能力。
- 不改变两张表的表结构 / 初始化脚本 / 现有只读 SQL。
- 不引入新的前端依赖或数据库序列机制。

## Decisions

### 决策 1：后端新增独立 CRUD 模块，而不是复用 `DataStreamMapper`
新建 `ColumnTypeConfigController`（`/api/columnTypeConfig`）+ `IColumnTypeConfigService` + impl + `ColumnTypeConfigDao`/`ColumnTypeConfigMapper`。数据量小（定义约数十行、映射约百行），Mapper 提供全量列表 + 按 ID 详情 + 插入 + 更新 + 删除 + 引用计数校验查询即可，无需真实分页 SQL——分页可在内存/SQL 层面按既有模式处理（沿用 `queryFlag`/`queryValue` + `page`/`count` 风格，与 `QueryMqRowsRequest` 一致）。

- **替代方案**：直接扩展 `DataStreamMapper`。**未选**：该 Mapper 承载引擎核心查询，混入管理端写操作会增大耦合与回归风险。

### 决策 2：内置数据判定采用「ID + 数据库类型」静态集合，发布为常量
内置定义/映射由 `datastream-column-type-map-v2.sql` 预置。为兼容 H2/MySQL 两种元数据库且不新增表字段，用一个静态集合（`Set<Long>`）记录脚本内置的 `column_type_define_id`（如 1~20、100~115、21~42、200~202、60~79 等）与 `column_type_map_id`，作为「库内置」判定依据。

- **替代方案**：为表加 `is_builtin` 字段。**未选**：需改 DDL 与初始化脚本，且存量数据不一致，违背「不改变表结构」的 Non-Goal。
- **权衡**：若脚本新增内置 ID，需要同步更新常量集合。设计上把该集合集中在一个常量类（如 `ColumnTypeBuiltInConstant`），并添加注释指向脚本来源，降低维护遗漏风险。

### 决策 3：映射模块以关联展示代替裸 ID，删除时做双向引用校验
映射列表复用 `queryColumnTypeMap` 的 join 写法（带 `database_type_a/name_a/b`），前端下拉框直接展示可读的「数据库类型.类型名称」。删除类型定义前，服务端校验其在 `column_type_map` 的 `column_type_define_id_a` 或 `column_type_define_id_b` 是否仍被引用，被引用则拒绝。

### 决策 4：新增记录沿用现有 ID 生成机制
插入新类型定义/映射的 ID 通过 `dataStreamDao.querySequence('seq_column_type_define_id')` / `('seq_column_type_map_id')` 获取，与文件格式模块一致，保证 H2/MySQL 均可用，避免 `MAX(id)+1` 在并发下的冲突。

> 注意：这两个新序列此前并无对应序列定义。MySQL 需在 `data_stream_sequence` 表预置
> `SEQ_COLUMN_TYPE_DEFINE_ID` / `SEQ_COLUMN_TYPE_MAP_ID` 行（否则 `nextseq` 返回 0，导致新增主键为 0）；
> H2 需创建同名 `CREATE SEQUENCE`。起始值设在 50000，避开脚本内置 ID（类型定义上限 202、类型映射上限 315）
> 与后续扩展。已同步更新 `doc/sql/datastream-mysql-ddl.sql` 与 `doc/sql/datastream-h2-ddl.sql`。

### 决策 5：前端复用既有配置页约定
- 视图 `src/views/column-type/index.vue`：`el-tabs` 双 Tab，两个 Tab 各自为查询区 + `el-table` + 增删改按钮 + 详情/编辑 `el-dialog`。
- 逻辑拆到 `useColumnTypeConfig.js`（参照 `useFileFormatManage.js`）。
- 在 `config/api.js` 新增 `/api/columnTypeConfig/*` 常量；路由增加 `/datastream/config/columnTypeConfig`；在 `useMenuConfig.js` 给 `14`（配置管理）新增子菜单项与面包屑、图标。

## Risks / Trade-offs

- **内置 ID 集合维护**：脚本新增内置类型/映射后，常量集合若未同步会导致保护失效 → 把集合集中到单一常量类并注释脚本来源，作为实施任务中的显式检查点。
- **映射引用校验时序**：删除定义与删除/修改映射是不同接口，校验与提交存在极小并发窗口 → 元数据表为低频配置，接受由数据库主键约束兜底，服务端单次事务内完成「校验+删除」。
- **H2 与 MySQL 方言差异**：分页/序列若写法不同会导致某端失败 → 复用与既有 Mapper 相同的内置函数与 `querySequence` 机制，保障双端一致。
- **新增定义被映射引擎忽略**：用户新增自定义类型定义后，结构迁移的映射规则由映射表驱动，新增定义需配套新增映射才会被引擎采用 → 在类型映射 Tab 提供基于数据库类型字段的过滤与联动，降低遗漏。

## Migration Plan

- 无表结构变更、无既有数据迁移需求。
- 需为两个新序列补充序列定义（数据库层面，一次性）：MySQL 执行 `INSERT INTO data_stream_sequence` 预置
  `SEQ_COLUMN_TYPE_DEFINE_ID`/`SEQ_COLUMN_TYPE_MAP_ID`（起始 50000）；H2 执行 `CREATE SEQUENCE ... START WITH 50000`。
  新建环境按 `datastream-mysql-ddl.sql` / `datastream-h2-ddl.sql` 全量初始化即可。存量环境的 `data_stream_sequence`
  请手工补一次上述补丁，否则新增会得到主键 0。
- 部署顺序：后端先部署（新增接口），前端再部署（路由/菜单/页面）；两者独立可回滚——回滚只需移除前端菜单项与后端 Controller 即可，不影响存量只读查询与引擎。
- 内置数据仍由现有初始化脚本保证，新环境照旧执行 `datastream-column-type-map-v2.sql`，无需额外脚本。

## Open Questions

无。内置数据判定范围（H2/MySQL 通用）与保护粒度已通过对本 change 的规格确认明确，不涉及会改变方案或任务拆分的未知项。