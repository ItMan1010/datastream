## 1. 构建与依赖

- [x] 1.1 新建 connector-dameng 模块目录与 pom.xml（parent=datastream-connectors，依赖 connector-common + 达梦 JDBC 驱动 com.dameng:Dm8JdbcDriver18）
- [x] 1.2 在根 pom.xml dependencyManagement 增加达梦驱动依赖（version 8.1.1.49）
- [x] 1.3 在 datastream-connectors/pom.xml 的 modules 增加 connector-dameng
- [x] 1.4 在 datastream-starter/pom.xml 增加 connector-dameng 依赖

## 2. 达梦连接器实现

- [x] 2.1 新建 DamengDatabaseAdapterImpl（继承 NonOracleSqlBuilder，chooseDS=15，达梦方言：to_char/to_date/sysdate/nvl/limit/seq.nextval/comment on column，驱动类 dm.jdbc.driver.DmDriver）
- [x] 2.2 新建 DamengTableColumnsEntity（columnName/dataType/dataLength/dataPrecision/dataScale/nullable/dataDefault/comments）
- [x] 2.3 新建 DamengTableMetaDao（getDamengTableColumns / getDamengPrimaryKeys / getDamengTableInfo，基于 all_tab_columns/all_col_comments/all_constraints/all_cons_columns/all_tables/all_tab_comments/user_segments）
- [x] 2.4 新建 DamengTableMetaResolver（继承 JdbcTableMetaResolver，chooseDS=15，覆写 getTableColumns/getTableInfo，映射 TableColumnEntity 的 precision/scale/nullAble/columnDef/remarks）

## 3. 公共常量与 URL/schema 处理

- [x] 3.1 DataStreamConstant 增加 DATA_SOURCE_TYPE_DAMENG=15
- [x] 3.2 修复 DataBaseEnum.DAMENG URL 正则（补 type=dm 命名分组）
- [x] 3.3 CommUtils 登记 jdbcUrlDataBaseTypeMap[15]=dm 与 dataSourceCategoryMap[15]=DATABASE
- [x] 3.4 TableInfoServiceImpl 增加 schema 解析助手，使 Oracle 与达梦统一使用 userName 作为 schema
- [x] 3.5 DataBaseController.testDataBase 将 socketTimeout 排除条件扩展为「非 Oracle 且非达梦」

## 4. 字段类型配置脚本

- [x] 4.1 在 doc/sql/datastream-column-type-map-v2.sql 增加达梦内置类型定义（ID 400..：NUMBER/INT/BIGINT/DECIMAL/FLOAT/DOUBLE/VARCHAR2/VARCHAR/CHAR/CLOB/TEXT/BLOB/VARBINARY/BINARY/DATE/TIMESTAMP 等）
- [x] 4.2 增加达梦 ↔ MySQL、达梦 ↔ PostgreSQL、达梦 ↔ Oracle 类型映射（map ID 400..）
- [x] 4.3 同步 ColumnTypeBuiltInConstant 两个内置集合（BUILTIN_TYPE_DEFINE_IDS / BUILTIN_TYPE_MAP_IDS 加入达梦 ID）

## 5. 前端 UI

- [x] 5.1 databaseConstants.js 增加达梦类型映射/选项/颜色（value=15）
- [x] 5.2 DataBaseDetail.vue 数据源类型下拉增加达梦选项（value=15）

## 6. 单元测试

- [x] 6.1 新建 DamengSqlDialectGoldenMasterTest（锁定 limit/to_char/to_date/sysdate/nvl/seq.nextval/comment 等方言输出）
- [x] 6.2 新建 DamengDatabaseAdapterImplTest（chooseDS、validation query、driver class 等契约断言）

## 7. 集成测试（真实 DM8 容器）

- [x] 7.1 新建 DamengDialectIntegrationTest（连接 localhost:5236，分页 limit 覆盖全部行；不可达时 assumeNoException 跳过）
- [x] 7.2 新建 DamengTableMetaResolverIntegrationTest（解析 DM_TEST_TABLE 列信息：NUMBER 精度/小数位、主键、类型名）
- [x] 7.3 表结构迁移集成验证（达梦源 → MySQL 目标，验证 NUMBER 精度映射与生成 DDL 正确）
- [x] 7.4 数据迁移集成验证（达梦源数据含中文/日期/CLOB/BLOB/VARBINARY/不同精度 NUMBER 迁移到目标，断言记录数与字段值等价）

- [x] 3.6 在 DataStreamApplication @MapperScan 增加 com.itman.datastream.connectors.dameng.dao（应用启动验证时发现的必要注册，否则 DamengTableMetaDao 无法被 MyBatis 扫描）

## 8. 文档同步

- [x] 8.1 更新 README.md（支持数据源增加达梦、模块结构增加 connector-dameng）
- [x] 8.2 更新 doc/README.md（SQL 脚本说明）
- [x] 8.3 更新 doc/guides/TESTING.md（达梦集成测试说明）
- [x] 8.4 更新 doc/sql/test-data/dameng-test-data.sql 说明注释（如必要）