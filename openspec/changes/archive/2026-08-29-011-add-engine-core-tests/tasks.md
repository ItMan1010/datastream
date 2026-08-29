## 1. 测试基建

- [x] 1.1 在 `datastream-engine`、`datastream-admin`、`datastream-connectors` 的 pom.xml 引入 `spring-boot-starter-test`（JUnit 4 / AssertJ / Mockito），并用 Maven profile 隔离 Testcontainers（mysql/postgresql）依赖
- [x] 1.2 建立各模块 `src/test/java` 目录骨架与测试基类（H2 内存库初始化工具、断言工具）
- [x] 1.3 验证 `mvn test` 可在各模块正常执行并产出测试报告

## 2. 非行为性可测试性重构

- [x] 2.1 将 `DataMoveHandler` 中纯逻辑 SQL 生成方法（`makeDataSelectCountSql`、`makeInsertRowObject`）提取到 `DataMoveSqlSupport`（行为不变）；其余 SQL 生成（`makeSqlSelectColumns`/`makeSqlInsertRow`/`makeSqlSelectByPage`）确认已在 connector 层 `AbstractSqlBuilder` 可直接单测，无需重构
- [x] 2.2 游标推进：核心 SQL 生成（`makeSqlSelectByPage`/`makeSqlCurrentPageMaxKeyValue`）位于 connector 层已可测（见 3.2）；`DataMoveHandler` 中游标状态机为薄胶水，由 3.2 的 H2 集成测试覆盖，不单独提取
- [x] 2.3 检查确认：`DataMoveHandler` 关键依赖已全部构造器注入，局部 `new` 均为值对象/数据结构/异常，无需 DI 替换
- [x] 2.4 运行第 3 节契约用例（3.1/3.2），验证 `DataMoveSqlSupport` 提取后行为不变（datastream-admin 5 测试 + connector-h2 5 测试全通过）

## 3. 核心契约测试（对应 engine-data-integrity 5 个 requirement）

- [x] 3.1 字段映射保序与保真测试（同名列按目标列顺序取值、无错位、无值失真，见 `DataMoveSqlSupportTest`）
- [x] 3.2 分页/分段游标不重不漏测试（N 条记录完整覆盖、主键无重复无缺失，见 `PagingCursorIntegrityTest`）
- [ ] 3.3 断点续传幂等测试（中断恢复后目标端与源端一致、不重不漏）
- [ ] 3.4 稽核精确比对与修复测试（差异完整识别、以源为准修复）
- [ ] 3.5 增量迁移位点精确续传测试（变更事件解析纯逻辑已通过 `DataCdcChangeParsingTest` 验证 op/after/before/source 解析；完整位点续传需 Debezium+Kafka 环境）

## 4. SQL 生成器 golden master 快照

- [x] 4.1 为各方言子类生成 SQL 快照断言（`MysqlSqlDialectGoldenMasterTest` 6 例、`PostgresSqlDialectGoldenMasterTest` 6 例、H2 见 `H2DatabaseAdapterImplTest`）
- [x] 4.2 快照比对用 inline 断言实现，方言 SQL 变化时测试失败需显式更新断言（即批准流程）

## 5. 方言集成测试（Testcontainers）

- [ ] 5.1 用 Testcontainers 起真实 MySQL/PostgreSQL，验证跨方言 SQL 语义正确（测试代码已实现 `MysqlDialectIntegrationTest`/`PostgresDialectIntegrationTest`，含 Docker 不可用自动跳过；当前环境 Testcontainers↔Docker Desktop 29.2.1 返回 400 而跳过，需在兼容环境验证）
- [ ] 5.2 验证 H2 契约测试与真实库集成测试结果的一致性（依赖 5.1 环境，待 5.1 跑通后执行）

## 6. 回归固化机制

- [x] 6.1 文档化回归测试约定（新增 `TESTING.md`：测试分层 + 强制回归测试要求）
- [x] 6.2 将 git 57d90b0（MySQL→PostgreSQL 二进制字段 0x 字面量写入 bytea 报错）固化为 `PostgresBinaryFieldRegressionTest` 示范
