## Context

参见 proposal.md「Why」。达梦 DM8 的 SQL 方言高度兼容 Oracle（to_char / to_date / sysdate / dual / rownum / nvl / comment on column / all_* 系统视图），同时支持 MySQL 风格 limit m,n 与多行 values 插入——两者均已在本机 DM8 实例（qinchz/dm8-arm64，端口 5236）实测验证。达梦 JDBC 驱动 com.dameng:Dm8JdbcDriver18:8.1.1.49 已安装至本地 Maven 仓库，驱动类为 dm.jdbc.driver.DmDriver。

现有连接器通过 Spring @Component 注册，由 DataSourceFactory 注入 List<IDatabaseAdapter> / List<ITableMetaApi> 后按 chooseDS(dataSourceType) 匹配；新增连接器只需在 starter 依赖中加入该模块即可被自动发现。

## Goals / Non-Goals

**Goals:**
- 让达梦可作为迁移源/目标参与数据迁移、数据清理、数据稽核、表结构迁移、链表迁移与数据检索。
- 保证 NUMBER 的精度/小数位在跨库结构迁移时被正确映射（区别于现有 Oracle DAO 仅取 data_length 的局限）。
- 保证达梦表结构的 NOT NULL、默认值、列注释、主键信息可被保留。

**Non-Goals:**
- 达梦增量迁移（CDC）——Debezium 目前仅支持 MySQL 源端，本变更不扩展达梦 CDC。
- 达梦作为系统元数据库——元数据库仍限定 MySQL/Oracle/PostgreSQL/H2。
- 修复现有 Oracle 连接器的元数据精度损失问题（仅对达梦做正确实现）。

## Decisions

### 决策 1：达梦 SQL 方言适配器继承 NonOracleSqlBuilder
DamengDatabaseAdapterImpl extends NonOracleSqlBuilder，复用在 AbstractSqlBuilder 中已按 DataBaseEnum.DAMENG 处理的日期/二进制/主键/外键分支，并覆写日期与空值函数为 Oracle 兼容写法（to_char / to_date / sysdate / nvl）、分页为 limit m,n。

- 备选：继承 OracleSqlBuilder（rownum 分页 + insert all）。达梦虽也支持，但 limit 更简单、语义更直观，且 DM8 原生支持 limit m,n 与多行 values 插入，实测无误。
- 理由：NonOracleSqlBuilder 的 makeSqlBatchInsert 生成 insert into ... values (...),(...)，达梦实测支持；分页 limit 实测支持。仅需覆写达梦缺失的 MySQL 风格日期函数。

### 决策 2：达梦表元数据解析采用「DAO + Oracle 兼容视图」，而非 JDBC DatabaseMetaData
DamengTableMetaResolver extends JdbcTableMetaResolver，覆写 getTableColumns 与 getTableInfo，通过 DamengTableMetaDao 查询达梦 all_tab_columns / all_col_comments / all_constraints / all_cons_columns / all_tables / all_tab_comments / user_segments 等 Oracle 兼容视图。

- 备选：复用 JdbcTableMetaResolver 默认的 JDBC 元数据路径。达梦 getColumns 仅返回 22 个标准列、缺少 IS_AUTOINCREMENT / IS_GENERATEDCOLUMN，而父类 getColumns 会读取 IS_AUTOINCREMENT 导致 DMException: 无效的列名，故弃用。
- 理由：达梦 all_tab_columns 提供 data_precision / data_scale / nullable / data_default 等完整信息，比 JDBC 元数据更可靠，且可完整保留 NUMBER 精度、NOT NULL 与默认值（这是表结构迁移正确性的关键）。

### 决策 3：达梦 schema 由连接用户名确定
达梦 JDBC URL（jdbc:dm://host:port）不含 database 路径，schema 即登录用户（如 SYSDBA），与 Oracle 同策略。TableInfoServiceImpl 增加 schema 解析助手，对 Oracle 与达梦统一使用 userName 作为 schema。

- 理由：达梦默认实例 CASE_SENSITIVE=0，但查询 all_* 视图需按 owner 过滤，owner 即用户名。

### 决策 4：修复 DataBaseEnum.DAMENG URL 正则并在 CommUtils 登记达梦
达梦 URL 正则补 type 命名分组（jdbc:(?<type>dm)://...），使 parseJdbcUrl 返回 dm；在 CommUtils 登记 jdbcUrlDataBaseTypeMap[15]=dm 与 dataSourceCategoryMap[15]=DATABASE。

- 理由：现有 DataBaseEnum.DAMENG 正则缺 type 分组，parseJdbcUrl 遍历到该枚举时会抛 IllegalArgumentException（No group with name <type>），必须修复。

### 决策 5：达梦连接测试不追加 socketTimeout
DataBaseController.testDataBase 的排除条件由「非 Oracle」扩展为「非 Oracle 且非达梦」，与 Oracle 同策略。

- 理由：达梦 JDBC 无 socketTimeout 参数，追加无意义。

### 决策 6：达梦内置类型定义/映射使用独立 ID 段
达梦类型定义使用 column_type_define_id ∈ 400..、类型映射使用 column_type_map_id ∈ 400..，避免与 MySQL(1..115)、PostgreSQL(21..202)、Oracle(60..79) 及映射(1..111,300..315) 冲突；并同步 ColumnTypeBuiltInConstant 两个内置集合。

- 理由：沿用既有「库内置数据删除保护」机制，仅新增数据、不改行为。

## Risks / Trade-offs

- [达梦 all_col_comments 视图名称与 Oracle 略有差异] → 实现时以 disql 实测为准，必要时退回 user_col_comments（按当前登录用户），列注释在无注释场景下为空、不影响迁移正确性。
- [达梦 NUMBER data_precision 为空（无精度声明）] → 按 Oracle 惯例回退为默认精度处理（OracleNumberMapper 对 null 精度回退 bigint）。
- [达梦 JDBC 驱动的 DatabaseMetaData 能力有限] → 已在决策 2 中规避（不走 JDBC 元数据路径）。
- [达梦试用授权临近到期（2026-09-13）] → 集成测试仅在可达时执行，不可达时自动跳过（assumeNoException），不影响常规构建。
- [多行 values 插入在达梦低版本可能不支持] → 已在本机 DM8 实测通过；若遇到旧版本，可退化为逐行插入（不改规格）。

## Migration Plan

1. 部署后端：新增 connector-dameng 模块随 starter 打包发布；达梦驱动 Dm8JdbcDriver18 随依赖打入发行包（若发行包未含则需手动放入 lib）。
2. 数据库脚本：在元数据库执行更新后的 doc/sql/datastream-column-type-map-v2.sql（幂等：达梦 INSERT 使用固定 ID，重复执行需先清理旧达梦数据，或提供达梦专属增量片段）。
3. 前端：重新构建 datastream-ui 使达梦类型选项生效。
4. 回滚：移除 connector-dameng 依赖并回退相关常量/脚本改动即可，不影响已有数据源。