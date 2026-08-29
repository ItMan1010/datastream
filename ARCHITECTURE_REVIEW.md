# DataStream 架构优化分析报告

> 分析视角：资深架构师
> 分析对象：DataStream 数据工具（约 3.5 万行 Java 生产代码 + Vue 3 前端）
> 分析日期：2026-08-29

---

## 结论先行

当前工程最需要优化的三个点，按「对业务价值与风险的影响」排序：

| 优先级 | 优化点 | 性质 |
|:---:|------|------|
| 1 | 核心数据引擎几乎零自动化测试 | 正确性无保障（致命） |
| 2 | 核心领域逻辑塌缩进「上帝类」、缺乏分层与策略抽象 | 可维护性危机（慢性病） |
| 3 | 自研连接池 + EOL 技术栈 + 动态 SQL 注入面 | 安全与稳定性风险（爆发点） |

---

## 1. 核心引擎几乎零自动化测试（最严重）

### 证据

- 生产代码 **35,724 行**（不含 target/），测试文件只有 **1 个**：
  `datastream-common/src/test/java/com/itman/datastream/common/utils/CommUtilsTest.java`（一个工具类测试）。
- 真正承载业务价值的模块——`datastream-engine`（迁移/稽核/CDC，7,154 行）、
  `datastream-admin`（Handler/Executor，17,201 行）——**零测试**。
- 前端 `package.json` 里有 `@vue/test-utils` 依赖，但没有 test runner（无 vitest/jest），也没有 test script。

### 为什么这是第一优先级

这是一个**数据搬运工具**，核心价值就是「数据不丢、不错、可稽核、增量不重不漏」。
当前的演化方式（`dataProducer` / `dataProducer1` / `dataProducer2` 这类带编号的方法）
说明代码是靠「改 bug → 复制一段」长出来的。每次改动都可能在以下环节引入数据错误，
而**没有任何回归测试兜底**：

- 分页/分段游标推进
- 字段映射
- 断点续传
- CDC offset 管理

### 建议

- 优先为核心链路建立**契约测试 + 集成测试**（用 H2 / Testcontainers 起真实库），覆盖：
  - 字段映射
  - 分页/分段游标（`fetchAndUpdateNextPageRowNumLock`、`splitDataRangeForParallelProcessing`）
  - 断点续传
  - 稽核修复
- 对 SQL 生成器（`AbstractSqlBuilder`，568 行）做 **golden master / 快照测试**，锁住各数据库方言的输出。
- 把每个已修复的 bug 固化成一条回归测试，逐步把「正确性」变成可证明的东西。

---

## 2. 上帝类与职责耦合，缺乏领域分层

### 证据

- `datastream-admin/src/main/java/com/itman/datastream/admin/handler/DataMoveHandler.java` **1803 行**，
  一个类同时干了：
  - 任务资源初始化 / 释放
  - 分页/分段游标状态机
  - 数据生产（Producer）
  - 数据消费（Consumer）
  - SQL 拼接（`makeSourceSelectSqlColumns`、`makeTargetInsertSqlColumns`、`makeDataInsertObject`）
  - 任务创建（`createTableMoveTask`、`createDataMoveTask`）
  - 任务查询（`queryTaskProgress`、`queryDataMoveTaskList`）
  - 权限校验（`validateTaskOwner`）
- `DataStreamDao.java` 1021 行、`ConnectionPoolManager.java` 728 行，三者合计 **3,552 行 ≈ 全工程 10%**。
- 方法命名 `dataProducer` / `dataProducer1` / `dataProducer2`、`dataConsumer` / `dataConsumer1` / `dataConsumer2`
  是典型的「复制粘贴演化」，而非抽象出的策略。

### 为什么严重

「上帝类」意味着：改一个数据源方言要动迁移主流程；生产者与消费者、游标推进与 SQL 生成、
业务编排与权限检查全都缠绕在一起。这是 bug 的温床，也让第 1 点「补测试」举步维艰——测不动、也测不全。

### 建议

- 沿领域边界拆分：
  - `SourceReader`（取数）
  - `TargetWriter`（写数）
  - `SegmentIterator`（分段游标状态机）
  - `TaskOrchestrator`（任务编排）
  - 查询与权限拆到 Service 层
- 用**策略模式**统一各数据源方言（MySQL/Oracle/PostgreSQL/H2/文件），消灭 `xxx1/xxx2` 编号复制。
- 让 `DataStreamDao` 回归纯持久化职责，SQL 拼装全部收敛到 `AbstractSqlBuilder` 及其子类。

---

## 3. 安全与稳定性：自研连接池 + EOL 技术栈 + 动态 SQL 注入面

### 证据

- **自研连接池** `ConnectionPoolManager`（728 行）：用 `while (...) { Thread.sleep(100); }`
  忙等轮询来等连接，自己实现空闲清理、泄漏检测、有效性校验——而项目**已经引入
  `druid-spring-boot-starter` 却不用**。自研连接池是连接泄漏、并发竞争、性能毛刺的高发区。
- **过时技术栈**：
  - `Spring Boot 2.1.4.RELEASE`（2019 年，已 EOL）
  - `Debezium 1.9.5.Final`（较老）
  - `fastjson 1.2.83`（存在历史反序列化漏洞，noneautotype 仅为缓解）
- **SQL 注入面**：mapper XML 里 **182 处 ${} 动态拼接**，其中相当一部分是**值拼接**：
  - `state in (${state})`
  - `create_date between ${beginDate} and ${endDate}`
  本应使用 #{} 预编译参数，却用了字符串拼接。

### 建议

- 删除自研 `ConnectionPoolManager`，统一走 Druid/HikariCP（多数据源场景 HikariCP 更轻）。
- 升级 Spring Boot（≥ 2.7 或 3.x）与 Debezium；`fastjson` 迁移到 `fastjson2` 或 Jackson。
- SQL 改造分两类：
  - **值**：一律改 #{} 预编译。
  - **表名/字段名/SQL 片段**（`${sqlLimit}`、`${sysdate}`）：必须做**白名单校验 + 方言抽象**，
    杜绝用户输入直达 SQL。

---

## 总结

> 先补测试锁住「数据正确性」，再拆「上帝类」恢复可维护性，
> 最后清掉「自研连接池 + EOL 依赖 + 注入面」这组安全稳定隐患。
> 三者按此顺序推进，风险递减、收益递增。

---

## 附：关键文件清单

| 文件 | 行数 | 问题 |
|------|:---:|------|
| `datastream-admin/.../handler/DataMoveHandler.java` | 1803 | 上帝类：生产/消费/SQL/编排/查询/权限混杂 |
| `datastream-engine/.../dao/DataStreamDao.java` | 1021 | DAO 过大，职责不纯 |
| `datastream-engine/.../debezium/FileOffsetBackingStoreParser.java` | 820 | CDC offset 解析逻辑集中 |
| `datastream-engine/.../jdbc/ConnectionPoolManager.java` | 728 | 自研连接池，忙等轮询 |
| `datastream-admin/.../handler/DataCdcHandler.java` | 661 | CDC 处理逻辑厚重 |
| `datastream-admin/.../executor/MoveExecutor.java` | 649 | 执行器逻辑厚重 |
| `datastream-admin/.../service/impl/MetaServiceImpl.java` | 626 | Service 层厚重 |
| `datastream-connectors/.../common/AbstractSqlBuilder.java` | 568 | SQL 方言生成核心，需测试锁定 |

> 注：以上行数为 2026-08-29 分析时点统计，不含 target/ 编译产物。
