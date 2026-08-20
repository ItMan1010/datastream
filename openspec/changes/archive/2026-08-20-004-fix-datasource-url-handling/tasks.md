## 1. 通用工具方法

- [x] 1.1 在 `datastream-common` 的 `CommUtils` 中新增 `appendUrlParam(String url, String paramName, String paramValue)`：结尾 `?`/`&` 直接追加、已含 `?` 补 `&`、无 `?` 补 `?`、已含 `paramName=` 幂等返回、null/空 URL 原样返回
- [x] 1.2 为该方法补充单元测试，覆盖 spec 中 5 个场景（多参数普通值结尾、`&` 结尾、无 `?`、已含同名参数、null/空）

## 2. 调用点替换

- [x] 2.1 `DataBaseController#testDataBase`：删除两段手写 if（补 `?`、补 socketTimeout），改为 Oracle 排除判断内调用 `CommUtils.appendUrlParam(url, "socketTimeout", "5000")`
- [x] 2.2 `TableLinkHandler#testTableLink`：同样替换为工具方法调用（保留 Oracle 排除）
- [x] 2.3 `DataMoveHandler` 源端注册处（约 1332-1338 行）：两段 if 合并为一次工具方法调用（维持无 Oracle 判断的现状）
- [x] 2.4 全局检索 `socketTimeout` 确认无遗漏调用点

## 3. 元数据库 DDL 与升级脚本

- [x] 3.1 `doc/sql/datastream-mysql-ddl.sql`：`data_stream_data_base.url` 改为 `varchar(512)`
- [x] 3.2 `doc/sql/datastream-h2-ddl.sql`：同上改为 `VARCHAR(512)`
- [x] 3.3 新增 `doc/sql/datastream-datasource-url-alter.sql` 存量库升级脚本（MySQL ALTER + H2 说明）
- [x] 3.4 对运行中元数据库（127.0.0.1:13307/datastream）执行 ALTER 并验证列类型为 varchar(512)

## 4. 构建与回归测试

- [x] 4.1 `mvn -pl datastream-common,datastream-admin -am compile`（或整体 `mvn compile -DskipTests`）通过；运行 CommUtils 单元测试
- [x] 4.2 重启后端服务，通过界面用完整带参 URL（155 字符，不以 `&` 结尾）新增数据源并连接测试，验证成功且无 POOL_005 / Data too long 错误
- [x] 4.3 回归验证已配置数据源（ID=4 dbtest1）连接测试仍成功；后端日志无拼接异常 URL
