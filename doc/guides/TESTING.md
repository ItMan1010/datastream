# 测试约定

本文档定义 DataStream 的测试分层与回归测试约定（由 change `011-add-engine-core-tests` 建立）。

## 测试分层

| 层级 | 位置 | 用途 | 运行方式 |
|------|------|------|----------|
| 单元/契约测试 | `datastream-connectors/connector-*`、`datastream-admin` | 字段映射、游标分页、SQL 生成的正确性契约，用 H2 内存库 | `mvn test` 默认执行 |
| golden master 快照 | 各 connector 模块 | 锁定各方言 SQL 输出，方言变化时测试失败需显式更新断言（即批准） | `mvn test` 默认执行 |
| 集成测试 | `-Pintegration-tests` profile | 用 Testcontainers 起真实 MySQL/PostgreSQL 验证跨方言语义；达梦集成测试直连本机 DM8 容器（localhost:5236，不可达时自动跳过） | `mvn test -Pintegration-tests`（需 Docker） |

## 运行测试

```bash
# 单模块（含依赖）
mvn test -pl datastream-connectors/connector-h2 -am

# 集成测试（需 Docker）
mvn test -Pintegration-tests
```

> 说明：历史构建通过 `maven.test.skip=true` 与 surefire `<skip>true</skip>` 跳过了测试，
> 本 change 已将其移除，测试随构建默认执行。

## 回归测试约定（强制）

任何针对**数据迁移 / 数据稽核 / 增量迁移（CDC）** 的 bug 修复，必须满足：

1. **附带回归测试**：修复同时新增一条能复现该缺陷的测试（先红后绿），归入对应模块测试集。
2. **标注来源**：测试注释注明对应的缺陷现象或 commit（示例见 `PostgresBinaryFieldRegressionTest`）。
3. **不重不漏**：回归测试必须断言「修复前失败、修复后通过」的行为边界。

## 已知测试基类

- `AbstractH2Test`（connector-h2）：H2 内存库连接/建表/计数工具，供契约测试复用。
