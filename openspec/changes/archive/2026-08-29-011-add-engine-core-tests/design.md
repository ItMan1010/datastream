## Context

- 动机见 `proposal.md`（核心引擎零测试）。
- 现有约束：JDK 1.8、Spring Boot 2.1.4、Maven 多模块；引擎使用自研 `ConnectionPoolManager` 管理数据源连接；SQL 生成集中在 `AbstractSqlBuilder` 及其方言子类；迁移主逻辑集中在 `DataMoveHandler`（1803 行）。
- 本变更只引入测试与测试基建，**不改变生产系统对外行为**。

## Goals / Non-Goals

**Goals:**
- 建立可被 Maven 自动执行的测试骨架（单元/契约/集成三层），并打通 CI 入口。
- 用测试锁定 `engine-data-integrity` 的 5 个核心正确性契约（字段映射、分页游标、断点续传、稽核、CDC 位点）。
- 为 SQL 生成器建立 golden master 快照，锁定各方言输出。
- 建立「bug 修复必须附带回归测试」的固化机制。

**Non-Goals:**
- 不追求覆盖率数字目标，聚焦高风险核心链路。
- 不重构业务逻辑、不改变任何对外行为（可测试性重构仅限非行为性改动）。
- 不新增数据源类型、不做性能/压测、不动前端。

## Decisions

### D1 测试框架：JUnit 4 + AssertJ + Mockito
- 选用 Spring Boot 2.1.4 `spring-boot-starter-test` 自带体系（JUnit 4.12 + AssertJ + Mockito），与现有技术栈一致、零额外依赖与学习成本。
- 说明：Spring Boot 2.1.4 的 starter-test 默认提供 JUnit 4（不含 JUnit 5）；为 JDK 1.8 兼容与零额外依赖，选用 JUnit 4，断言统一用 AssertJ。
- 备选 JUnit 5 / TestNG：JUnit 5 需额外引入 junit-jupiter 依赖、增大依赖面，暂不采用。

### D2 测试分层与数据库策略：H2 做契约测试，Testcontainers 做方言集成测试
- 单元/契约测试用 H2 内存库（快、无外部依赖、CI 友好），覆盖字段映射、游标分页、断点续传等纯逻辑链路。
- 方言集成测试用 Testcontainers 起真实 MySQL/PostgreSQL，验证跨方言 SQL 语义正确（H2 无法覆盖的方言差异）。
- 备选：全部用 H2（省 Docker 但丢失方言真实性）；依赖固定外部库（环境脆弱、不可复现），均放弃。
- 落地约束：Testcontainers 依赖 Docker；若目标环境无 Docker 或与 JDK 1.8 不兼容，集成测试标记为可选执行（见 Risks）。

### D3 golden master 快照测试锁定 SQL 方言输出
- 对 `AbstractSqlBuilder` 各方言子类生成 SQL 采用「批准文件 + 差异比对」快照模式，方言微调时走显式批准流程而非静默通过。
- 备选：逐条手工断言 SQL 字符串（脆弱、方言多时难维护），放弃。

### D4 非行为性可测试性重构
- 在不改行为前提下，将 `DataMoveHandler` 中 SQL 生成、游标推进等可独立验证的片段抽为可测方法/组件，并用依赖注入替换局部 `new` 以便 mock。
- 边界：仅做等价重构，配合 spec 的 5 个契约用例回归验证行为不变。

### D5 回归测试固化机制
- 约定：任何针对迁移/稽核/CDC 的 bug 修复，必须附带一条能复现该缺陷的回归测试（先红后绿），纳入对应模块测试集。

## Risks / Trade-offs

- [Testcontainers 需 Docker，且 JDK 1.8 兼容性需验证] → 集成测试用 JUnit 4 `Assume` 检测 Docker 可用性，不可用时自动跳过（而非失败），降级为仅 H2 契约测试，不影响主测试套件。
- [实际遇到的环境兼容性] → ① Testcontainers 1.17.6 的旧 JNA 不含 arm64 mac native lib（`UnsatisfiedLinkError`），已升级 JNA 5.13.0 解决；② Testcontainers 1.19.8 的 docker-java 与 Docker Desktop 29.2.1 的 socket 代理 `/info` 返回 400，导致集成测试在当前环境跳过——属环境限制，测试代码已正确降级，需在 Linux CI 或匹配版本环境验证。
- [自研连接池使集成测试连接管理复杂] → 优先测纯逻辑与 SQL 生成，连接层测试用独立短生命周期连接，不依赖池的全局状态。
- [现有代码耦合度高，可测试性重构可能牵动较多文件] → 严格限定为非行为性改动，并以契约用例兜底验证。
- [golden master 快照会因方言 SQL 微调频繁变化] → 缩小快照粒度到关键方言差异点，配合明确批准流程。
