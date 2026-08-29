## Why

DataStream 的核心价值是「数据不丢、不错、可稽核、增量不重不漏」，但当前 3.5 万余行生产代码仅有 1 个工具类测试（`CommUtilsTest`），真正承载业务的 `datastream-engine`（迁移/稽核/CDC）与 `datastream-admin`（Handler/Executor）零测试覆盖。核心正确性契约（字段映射保序、分页游标不重不漏、断点续传幂等、CDC offset 精确）从未被显式化，更无法被自动化验证；任何对分页游标、字段映射、断点续传、CDC offset 的改动都可能引入数据错误而不被发现。

## What Changes

- 新增能力 `engine-data-integrity`：显式化核心迁移引擎的数据完整性契约，并作为 spec 沉淀。
- 为核心链路建立契约测试 + 集成测试（H2 / Testcontainers 真实库），覆盖：字段映射、分页/分段游标、断点续传、稽核修复。
- 对 SQL 生成器 `AbstractSqlBuilder` 建立 golden master 快照测试，锁定各数据库方言输出。
- 引入测试依赖（JUnit 5、Testcontainers、AssertJ 等）并打通 Maven 测试执行链路。
- 建立「bug 修复 → 回归测试」的固化机制，将历史缺陷逐步转化为可回归用例。
- 为可测试性做必要的非行为性重构（不改变对外行为）。

## Capabilities

### New Capabilities

- `engine-data-integrity`: 核心迁移引擎的数据完整性契约——定义字段映射、分页游标、断点续传、稽核、CDC offset 等环节必须满足的正确性要求，并以自动化测试锁定。

### Modified Capabilities

（无）

## Impact

- 构建系统：`datastream-engine`、`datastream-admin`、`datastream-connectors` 的 pom.xml 引入测试依赖；Maven surefire 执行测试。
- 新增 spec：`openspec/specs/engine-data-integrity/spec.md`。
- 测试代码：新增 `datastream-engine/src/test`、`datastream-admin/src/test` 等测试源码集。
- 生产代码：仅做不改变行为的可测试性重构（如方法可见性、依赖注入点调整、SQL 生成器接口化）。
- 不涉及：对外 API 契约、数据格式、数据库 schema、前端行为。
