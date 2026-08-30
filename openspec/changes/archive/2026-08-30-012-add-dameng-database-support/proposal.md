## Why

DataStream 目前支持 MySQL、Oracle、PostgreSQL、H2、Doris 等数据库，但国产数据库达梦（DM8）已在国内政企/金融场景广泛使用，用户存在将达梦库的表结构与数据迁移到 MySQL/PostgreSQL（或反向）的强需求。当前代码虽已预置 `DataBaseEnum.DAMENG` 枚举与部分 `DataBaseEnum.DAMENG` 分支，但缺少可运行的达梦连接器，达梦数据源在界面上无法配置、无法建连、无法迁移。

## What Changes

- 新增 `connector-dameng` 连接器模块（Maven 模块 + 注册进 connectors/starter 依赖），实现 `IDatabaseAdapter`（达梦 SQL 方言）与 `ITableMetaApi`（达梦表元数据解析）两个 SPI 实现。
- 新增达梦 JDBC 驱动依赖 `com.dameng:Dm8JdbcDriver18`（驱动类 `dm.jdbc.driver.DmDriver`），并纳入根 POM 依赖管理。
- 在 `DataStreamConstant` 增加 `DATA_SOURCE_TYPE_DAMENG=15`，并在 `CommUtils` 登记达梦的 JDBC URL 类型映射（`dm`）与数据源分类（数据库）。
- 修复 `DataBaseEnum.DAMENG` 的 URL 正则（补 `type` 命名分组，使 `parseJdbcUrl` 能正确识别 `jdbc:dm://...`）。
- 达梦 schema 处理：达梦 URL 无 database 路径，schema 由连接用户名确定（与 Oracle 同策略），更新 `TableInfoServiceImpl` 使其对达梦使用 `userName` 作为 schema。
- 达梦连接测试：不追加 `socketTimeout`（达梦 JDBC 无该参数），与 Oracle 同策略。
- 前端数据源类型下拉与类型映射新增「达梦」选项（value=15）。
- 在字段类型配置脚本 `doc/sql/datastream-column-type-map-v2.sql` 增加达梦内置类型定义与类型映射（达梦 ↔ MySQL / PostgreSQL / Oracle），并同步 `ColumnTypeBuiltInConstant` 内置 ID 集合（新增数据，沿用既有「库内置数据删除保护」行为，不改变该能力的行为契约）。
- 同步更新系统文档（README、doc/README、doc/guides/TESTING）与测试数据说明。

## Capabilities

### New Capabilities

- `dameng-connector`: 达梦(DM8)数据库连接器能力——涵盖达梦 SQL 方言适配、表元数据解析、连接器注册与驱动加载、URL 解析、schema 解析，以及达梦作为迁移源/目标时的表结构与数据迁移行为。

### Modified Capabilities

- `datasource-config`: 达梦数据源连接测试时 URL 处理行为（达梦不追加 `socketTimeout`，与 Oracle 同策略）。

## Impact

- 后端代码：`datastream-connectors`（新增 `connector-dameng` 模块）、`datastream-common`（`DataStreamConstant`、`CommUtils`、`ColumnTypeBuiltInConstant`）、`datastream-admin`（`TableInfoServiceImpl`、`DataBaseController` 连接测试排除达梦）、`datastream-starter`（依赖）。
- 构建配置：根 `pom.xml`、`datastream-connectors/pom.xml`、`datastream-starter/pom.xml`、`connector-dameng/pom.xml`。
- 前端：`datastream-ui` 数据库类型常量与数据源详情表单。
- 数据库脚本：`doc/sql/datastream-column-type-map-v2.sql`（新增达梦类型定义与映射）。
- 依赖：达梦 JDBC 驱动 `com.dameng:Dm8JdbcDriver18:8.1.1.49`（本地 Maven 仓库已安装）。
- 测试：`connector-dameng` 单元测试（SQL 方言 golden master）、集成测试（连接真实 DM8 容器验证表结构迁移与数据迁移）。
