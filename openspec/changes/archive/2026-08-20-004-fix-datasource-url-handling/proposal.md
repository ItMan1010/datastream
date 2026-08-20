## Why

通过界面配置 MySQL 数据源（带完整连接参数的 JDBC URL，如 `jdbc:mysql://127.0.0.1:13307/dbtest1?useSSL=false&...&allowPublicKeyRetrieval=true`）时，"连接测试"必然失败、新增也失败。存在两个实际缺陷：

1. **URL 追加参数分隔符错误**：后端在连接测试/任务执行前给 URL 追加 `socketTimeout=5000` 时，分隔符判断逻辑有误。当 URL 已含 `&` 且不以 `?` 结尾时，不补 `&`，直接拼出 `...allowPublicKeyRetrieval=truesocketTimeout=5000`，MySQL 驱动报 "The value 'truesocketTimeout=5000' is not acceptable"（错误码 POOL_005 创建数据库连接失败）。该缺陷同样存在于 `TableLinkHandler` 与 `DataMoveHandler` 中。
2. **元数据库 `url` 列长度不足**：`data_stream_data_base.url` 为 `varchar(128)`，带常规参数的 JDBC URL（约 130~160 字符）插入时报 "Data too long for column 'url'"。

## What Changes

- 在 `CommUtils` 新增通用方法 `appendUrlParam(url, paramName, paramValue)`：按 URL 结尾字符（`?`/`&`）与是否已含 `?` 自动选择分隔符，同名参数已存在时不重复追加。
- 用该方法替换 3 处手写的 `socketTimeout=5000` 追加逻辑：
  - `DataBaseController.testDataBase`（连接测试）
  - `TableLinkHandler.testTableLink`（表链接测试）
  - `DataMoveHandler`（迁移任务源端注册）
- `data_stream_data_base.url` 列长度由 `varchar(128)` 扩为 `varchar(512)`：
  - 更新建库 DDL：`doc/sql/datastream-mysql-ddl.sql`、`doc/sql/datastream-h2-ddl.sql`
  - 新增存量库升级脚本 `doc/sql/datastream-datasource-url-alter.sql`
  - 对运行中的元数据库执行 ALTER
- 各调用点原有的 Oracle 排除逻辑、已有参数跳过逻辑保持不变（仅修复分隔符与长度问题）。

## Capabilities

### New Capabilities
- `datasource-config`: 数据源配置管理——JDBC URL 参数追加规则与 URL 存储长度约束。

### Modified Capabilities
<!-- 无：auth-session / frontend-http / ui-theme 的需求不受影响 -->

## Impact

- **代码**：`datastream-common`（CommUtils 新增工具方法）、`datastream-admin`（DataBaseController / TableLinkHandler / DataMoveHandler 三处调用点替换）。
- **数据库**：元数据库 `data_stream_data_base` 表结构变更（url 列扩容），需对存量环境执行 ALTER；新建环境直接使用新 DDL。
- **API**：无接口签名变化；`/api/database/testDataBase`、`/api/database/addDataBase` 等行为从"特定 URL 下失败"修复为"成功"。
- **兼容性**：已存入的短 URL 不受影响；varchar 扩容为在线 DDL，无数据丢失风险。
