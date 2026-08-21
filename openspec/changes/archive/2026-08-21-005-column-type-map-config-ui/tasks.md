## 1. 后端基础设施（datastream-common / datastream-engine）

- [x] 1.1 在 `datastream-common` 新建常量类 `ColumnTypeBuiltInConstant`，以 `Set<Long>` 声明脚本预置的内置 `column_type_define_id` 与 `column_type_map_id` 集合（依据 `doc/sql/datastream-column-type-map-v2.sql` 中所有表内 INSERT 的 ID），并注释标明出处与「脚本新增 ID 需同步更新」的维护提示。
- [x] 1.2 在 `datastream-engine` 新建 `ColumnTypeConfigMapper.java`，声明接口：定义表全量查询/按ID查询/插入/更新/删除、映射表全量查询/按ID查询/插入/更新/删除、按 ID 统计该定义被映射表作为 `column_type_define_id_a` 或 `b` 引用的计数。
- [x] 1.3 新增 `datastream-engine/src/main/resources/mapper/ColumnTypeConfigMapper.xml`，实现上述 SQL：定义与映射的查询沿用 `DataStreamMapper.queryColumnTypeMap` 的 join 别名写法（含 `database_type_a/name_a/b` 可读列）；插入使用 `#{id}` 占位（ID 由上层 `querySequence` 提供）。
- [x] 1.4 在 `DataStreamMapper`/`DataStreamDao` 中确认 `querySequence` 可用，或按需暴露 `querySequence('seq_column_type_define_id')` 与 `('seq_column_type_map_id')` 取号方法。
- [x] 1.5 新建 `ColumnTypeConfigDao`（引擎层 `dao` 包），封装 Mapper 方法，抛 `DataStreamException` 统一异常。

## 2. 后端服务层（datastream-admin）

- [x] 2.1 新建 `IColumnTypeConfigService` 接口及 `ColumnTypeConfigServiceImpl`，实现：定义表分页/详情/新增/编辑/删除；映射表分页/详情/新增/编辑/删除；新增/编辑时校验必填项与唯一性（数据库类型+类型名称）；映射新增/编辑校验源/目标定义存在性并在 `match_level != 1` 时提示转换警告。
- [x] 2.2 在服务层实现删除保护：删除定义前校验被引用计数与内置集合，被引用或内置则抛异常；删除映射前校验是否属于内置集合，内置则拒绝。
- [x] 2.3 新建 request/response domain：`QueryTypeDefineRowsRequest/Response`、`TypeDefineInfo/Add/Modify/Del Request+Response`、`QueryTypeMapRowsRequest/Response`、`TypeMapInfo/Add/Modify/Del Request+Response`（参照 `QueryMqRowsRequest` / `AddMqConfigResponse` 风格，响应含 `errorCode`/`errorMsg`）。
- [x] 2.4 新建 `ColumnTypeConfigController`（路由 `/api/columnTypeConfig`），映射定义与映射两套接口，添加 `@LogOperate` 操作日志注解（新增/修改/删除）。

## 3. 前端（datastream-ui）

- [x] 3.1 在 `src/config/api.js` 新增 `COLUMN_TYPE_*` / `TYPE_MAP_*` 常量，指向 `/api/columnTypeConfig/*` 后端接口。
- [x] 3.2 新建 `src/api/columnType.js` 封装后端请求函数（分页查询、详情、新增、编辑、删除）。
- [x] 3.3 新建 `useColumnTypeConfig.js` Composable（参照 `useFileFormatManage.js`），管理两套列表分页、查询、详情/编辑 dialog 状态与增删改调用，含删除前的确认与错误提示。
- [x] 3.4 新建视图 `src/views/column-type/index.vue`：`el-tabs` 双 Tab（「类型定义」「类型映射」），各 Tab 含查询区、`el-table`、新增/编辑/删除操作与 `el-dialog` 表单；映射 Tab 的下拉选项以「数据库类型.类型名称」展示，并在 `match_level` 非精确时展示警告。
- [x] 3.5 在 `src/router/index.js` 注册路由 `/datastream/config/columnTypeConfig`，懒加载上述视图，name 与现有规范一致。
- [x] 3.6 在 `src/composables/useMenuConfig.js` 给配置管理父菜单（`14`）新增子菜单项（`menuNameArr`、`menuDescArr`、`menuIconArr`）、`crumbArr` 面包屑及 `defaultOpeneds` 展开项。

## 4. 验证

- [x] 4.1 后端编译通过（`mvn -q clean compile`）并启动，H2 与 MySQL 两套元数据库下验证两套 CRUD 接口均正常返回。
- [x] 4.2 接口用例验证：重复新增同（数据库类型+类型名称）被拒绝；删除被映射引用或内置的定义被拒绝；删除内置映射被拒绝；非精确映射新增/编辑返回警告提示；未被引用且非内置的定义/映射可正常删除。
- [x] 4.3 前端构建通过，进入「配置管理 → 字段类型配置」，双 Tab 增删改查可用，删除保护与映射联动提示生效；操作后日志记录正确写入。
- [x] 4.4 回归验证现有 `queryColumnTypeDefine` / `queryColumnTypeMap` 只读路径与结构迁移流程不受影响。