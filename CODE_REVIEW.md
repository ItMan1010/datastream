# DataStream 项目代码审查与优化建议

> 审查范围：后端核心引擎与执行链路、连接器实现、安全与通用层、前端 UI 四个方向
> 审查方式：静态代码审查 + 交叉检索
> 审查日期：2026-08-19

---

## 一、概述

本项目（DataStream 数据处理平台）整体架构设计思路清晰：采用「`@RouteSource` 注解 + `RouteAspect` 切面 + `RouteHolder` ThreadLocal + `DataBaseSource`（`AbstractRoutingDataSource` 动态路由）」的自定义多数据源方案，配合 handler/executor 分层、线程池调度、Debezium CDC 做增量同步，连接器模式扩展数据源。

但审查发现存在 **70+ 个问题点**，其中包含若干**确定性 bug、认证绕过、数据静默丢失、连接池泄漏、SQL 注入风险**等实质性问题，需按优先级逐步处理。

### 问题统计（按严重程度）

| 级别 | 含义 | 数量（约） |
|------|------|-----------|
| P0 | 必须立即修复（安全漏洞 / 数据丢失 / 认证绕过） | 12+ |
| P1 | 高优先级（正确性 bug / 资源泄漏 / 并发竞态） | 18+ |
| P2 | 中优先级（前端安全 / 内存泄漏 / 配置隐患） | 15+ |
| P3 | 低优先级（性能 / 可维护性 / 架构 / 依赖升级） | 25+ |

---

## 二、P0 级 — 必须立即修复

### 2.1 认证与安全漏洞

#### P0-1 【认证绕过】会话校验逻辑倒置
- **文件**：`datastream-engine/src/main/java/com/itman/datastream/engine/systemlog/impl/SystemLogServiceImpl.java`（98-123 行）
- **问题**：`isTokenExpiration` 在查不到会话记录时返回 `false`（语义为"未过期"）。攻击者用弱密钥伪造 JWT（不在 `data_stream_session` 表），`selectSystemSession` 返回 null → 判定未过期 → 认证通过。同时登出后（state=2）同 token 再查询返回 null → 又判定未过期 → 登出失效，token 仍可复用。
- **建议**：语义反转——查不到会话时返回 `true`（视为过期/无效）；登出后对 state!=1 一律拒绝。

#### P0-2 【弱密钥】JWT 签名密钥硬编码且强度极弱
- **文件**：`datastream-security/src/main/java/com/itman/datastream/security/constant/SecurityConstant.java`（29 行）
- **问题**：`SECRET = "jwt818"`（6 字符），硬编码在源码，可秒级暴力破解；拿到源码/jar 即可伪造合法 Token。
- **建议**：从环境变量/配置中心读取，长度 ≥ 256 bit；不同环境不同密钥，支持轮换。

#### P0-3 【无过期时间】JWT 未设置 expiration
- **文件**：`datastream-security/src/main/java/com/itman/datastream/security/jwt/DsJwtToken.java`（39-47 行）
- **问题**：`createToken` 只 `setIssuedAt`，无 `setExpiration`，token 过期完全依赖数据库会话逻辑（而该逻辑又有 P0-1 漏洞）。
- **建议**：补充 `.setExpiration(...)`，并在校验时强制校验 `exp`。

#### P0-4 【密码形同明文】数据库密码 AES-ECB + 硬编码密钥
- **文件**：`datastream-common/src/main/java/com/itman/datastream/common/utils/AESUtils.java`（28-29 行）
- **问题**：`AES/ECB/PKCS5Padding` + 密钥 `"1234567812345678"` 硬编码，且与前端完全一致。拿到 jar/前端包即可解密所有数据源密码。
- **建议**：改用 AES-GCM/CBC + 随机 IV；密钥外部注入（环境变量/配置中心/KMS），禁止硬编码。

#### P0-5 【弱加密】登录密码 DES/ECB + 前后端同步硬编码密钥
- **文件**：`datastream-security/src/main/java/com/itman/datastream/security/utils/DsDesCipherUtils.java`（35-50 行）、`SecurityConstant.java`（33 行）
- **问题**：DES 为 56 位老旧算法，ECB 模式不安全；密钥 `"task-manage-3826"` 前后端硬编码，形同明文。
- **建议**：登录走 HTTPS + 服务端 BCrypt/Argon2 单向哈希，移除前端对称加密。

#### P0-6 【白名单过宽】`indexOf("login")` 模糊放行
- **文件**：`datastream-security/src/main/java/com/itman/datastream/security/filter/JWTAuthorizationFilter.java`（54-66 行）
- **问题**：URL 中出现 `login` 子串即无需认证，`/api/xxxlogin`、`/foo-login-bar` 等都被放行。
- **建议**：改为精确匹配登录路径或 AntPathRequestMatcher 白名单集合。

#### P0-7 【H2 风险】老版本 + 远程控制台 + 弱口令 + 白名单放行
- **文件**：`application-h2.yml`（12-23 行）、`pom.xml`（55 行 h2 1.4.193）、`SecurityConfig.java`（111 行）
- **问题**：H2 1.4.193（2016 年，存在 RCE 等 CVE）+ 控制台 `web-allow-others: true` + 默认 `root/123456` + 认证白名单放行 `/h2`，可未授权访问甚至 RCE。
- **建议**：生产关闭控制台、移除白名单、升级 H2 到 2.x。

### 2.2 数据正确性 / 数据丢失

#### P0-8 【数据静默丢失】同步迁移模式空实现
- **文件**：`datastream-admin/src/main/java/com/itman/datastream/admin/handler/DataMoveHandler.java`（873-884 行）
- **问题**：`SOURCE_SEND_MODE_SYNC` 分支核心逻辑被注释，`dataActualCount` 恒为 0，同步模式下既不删源也不插目标，数据静默丢失。
- **建议**：恢复实现，或若已废弃则在任务创建时显式拒绝该 sendMode。

#### P0-9 【offset 丢失】Debezium offset 异步写库即回调成功
- **文件**：`datastream-engine/src/main/java/com/itman/datastream/engine/debezium/DatabaseOffsetBackingStore.java`（122-160 行）
- **问题**：`set` 用 `CompletableFuture.runAsync` 异步写库，却立即 `callback.onCompletion(null, null)`；进程崩溃时 offset 未落库，重启重复消费/丢数据。写库失败仅 log，不通过 callback 上报。
- **建议**：`onCompletion` 移入异步任务完成后调用；失败回调 `onCompletion(error, ...)`。

#### P0-10 【offset 回放不完整】多分区只查第一个 key
- **文件**：`DatabaseOffsetBackingStore.java`（96-104 行）
- **问题**：`get` 只用 `offsetKeys.get(0)` 查一次，其余分区 key 不返回，无法完整回放。
- **建议**：遍历所有 key 分别查询并组装完整 result。

#### P0-11 【MQ 启动即停】消费者绑定后立即解绑
- **文件**：`datastream-admin/src/main/java/com/itman/datastream/admin/handler/DataMQHandler.java`（113-119 行）
- **问题**：`bindConsumerDestination` 绑定后，`startMQMonitorThread` 又立即 `stopSingleEngine`，MQ 消费无法工作（疑似严重 bug，需确认调用时序）。
- **建议**：核对 start/stop 时序，去掉多余的立即 stop。

### 2.3 SQL 注入

#### P0-12 【SQL 注入】多处 `${}` 字符串拼接 SQL
- **文件**：`datastream-engine/src/main/resources/mapper/DataStreamMapper.xml`（几乎全部动态 SQL 使用 `${sqlLimit}`、`${selectSql}`、`${insertSql}` 等）
- **问题**：表名/条件来自任务配置（用户可写），`${}` 不做预编译与转义，存在注入面。
- **建议**：结构化参数改 `#{}`；标识符（表名/列名）做白名单校验或方言化转义。

#### P0-13 【SQL 注入】字符串值零转义
- **文件**：`datastream-connectors/connector-common/src/main/java/com/itman/datastream/connectors/common/AbstractSqlBuilder.java`（114-129 行）
- **问题**：`makeSqlKeyColumnSelect` 对字符串值 `columnName + "=" + "'" + columnValue + "'"` 未调用 `escapeSqlString`，用户数据含 `' OR '1'='1` 会改变语义。该方法被 select/delete/update 广泛调用。
- **建议**：字符串值必须 `escapeSqlString`；更好是改用 PreparedStatement。

#### P0-14 【SQL 注入】数据检索直接拼接 tableName/queryCondition
- **文件**：`datastream-admin/src/main/java/com/itman/datastream/admin/handler/DataBaseHandler.java`（224-227、257、311-327 行）
- **问题**：`tableName`、`queryCondition` 来自请求参数直接拼接 SQL。
- **建议**：表名/字段名白名单校验，条件参数化。

---

## 三、P1 级 — 高优先级

### 3.1 并发与资源泄漏

#### P1-1 【连接池泄漏】动态数据源并发注册竞态
- **文件**：`datastream-engine/src/main/java/com/itman/datastream/engine/route/DataBaseSource.java`（95-143 行）
- **问题**：`containsKey → 创建 → put` 非原子，两个任务并发注册同一 key 会各自创建 Druid 池，后写覆盖先写，被遗弃的池永不 close，连接泄漏。
- **建议**：`computeIfAbsent` + 同步锁保护注册与 `afterPropertiesSet`。

#### P1-2 【资源未释放】各 Executor/Handler 清理不在 finally
- **文件**：`MoveExecutor.java`（97-151 行）、`LinkTaskExecutor.java`（96 行）、`DataCheckHandler.java`（127-178 行）、`DataMoveHandler.java` 多处
- **问题**：`registerDataBase`、`releaseTaskDataSources`、ThreadLocal 清理、连接关闭散落在正常路径，异常时泄漏连接池引用/ThreadLocal 残留/任务状态卡 RUNNING。
- **建议**：统一 `try/finally` 保证清理。

#### P1-3 【静默丢任务】4 个线程池使用 DiscardPolicy
- **文件**：`TaskPoolConfig.java`、`EventPoolConfig.java`、`SourcePoolConfig.java`、`TargetPoolConfig.java`（各 52-53 行）
- **问题**：队列满时直接丢弃任务且无日志，数据丢失无感知。
- **建议**：改 `AbortPolicy`（显式抛异常）或 `CallerRunsPolicy`（背压），至少记录 WARN/ERROR。

#### P1-4 【跨线程路由失效】ThreadLocal 在 parallelStream/@Async 下失效
- **文件**：`DataBaseSource.java`（72-73 行）、`LinkTaskHandler.java`（205-219 行）、`AbstractHandler.java`
- **问题**：路由 key 依赖 ThreadLocal，`parallelStream`/`@Async` 子线程拿不到 key 返回 null，fallback 到元数据库，可能「写错库」。
- **建议**：跨线程前显式传递 dataSourceId，避免在 parallelStream 内执行依赖路由的 DAO。

#### P1-5 【文件句柄泄漏】全局静态缓存 Reader/Writer
- **文件**：`connector-file/.../impl/TextFileApiImpl.java`（51-52、80-121、265-288 行）
- **问题**：Reader/Writer 缓存在静态 Map，异常或调用方漏 release 时句柄永久泄漏。
- **建议**：改用 try-with-resources 或任务级实例字段，release 幂等 + 异常兜底。

#### P1-6 【Excel 流泄漏】FileInputStream 匿名传入不持有引用
- **文件**：`connector-file/.../impl/ExcelFileApiImpl.java`（222-233 行）
- **问题**：构造 Workbook 过程中异常时 InputStream 泄漏。
- **建议**：显式 `try (InputStream in = new FileInputStream(...))`。

#### P1-7 【Kafka 泄漏】consumer 构造异常不关闭
- **文件**：`connector-kafka/.../KafkaAdapterImpl.java`（209-271 行）
- **问题**：consumer 创建后 seek 等过程异常时直接抛异常，consumer 未进缓存无法被 cleanup 关闭。
- **建议**：异常时 `consumer.close()`。

#### P1-8 【重复消费】Kafka offset 保存语义错误
- **文件**：`KafkaAdapterImpl.java`（298-299 行）
- **问题**：保存 `record.offset()`（已处理消息），重启后从已处理消息再次消费。
- **建议**：保存 `record.offset() + 1`，且每条消息处理成功后再保存。

### 3.2 正确性 Bug

#### P1-9 【Oracle 精度丢失】NUMBER 精度/标度解析错误
- **文件**：`connector-oracle/.../OracleTableMetaResolver.java`（63-66 行）、`OracleTableMetaDao.java`（30-35 行）
- **问题**：用 `DATA_LENGTH`（字节长度）当精度，`decimalDigits` 恒 null，导致 `NUMBER(10,2)` 被映射成整数，金额/小数数据截断或类型错误。
- **建议**：DAO 增查 `data_precision`、`data_scale`，resolver 正确设置 columnSize/decimalDigits。

#### P1-10 【PG 日期函数错误】使用 Oracle 小写格式串
- **文件**：`connector-postgres/.../PostgresDatabaseAdapterImpl.java`（40-47、61-62 行）
- **问题**：`to_timestamp('...','yyyymmddhh24missms')` 是 Oracle 格式，PG 需大写 `YYYYMMDDHH24MISSMS`，小写不识别/语义错误。
- **建议**：PG 使用大写格式串。

#### P1-11 【H2 驱动错误】驱动类写成 MySQL 驱动
- **文件**：`connector-h2/.../H2DatabaseAdapterImpl.java`（118-121 行）
- **问题**：`getDriverClass()` 返回 `com.mysql.jdbc.Driver`，应为 `org.h2.Driver`（复制粘贴错误，H2 连接会失败）。
- **建议**：改为 `org.h2.Driver`。

#### P1-12 【统计错误】maxCost 计算写错
- **文件**：`DataMoveHandler.java`（356 行）
- **问题**：`currentCost > getMaxCost() ? currentCost : getMinCost()`，else 分支误取 minCost，最大耗时统计失真并污染监控数据。
- **建议**：改为 `currentCost > getMaxCost() ? currentCost : getMaxCost()`。

#### P1-13 【计数反了】链表任务 count 逻辑错误
- **文件**：`LinkTaskHandler.java`（283-290 行）
- **问题**：`if (isEmpty) { recordCount = list.size(); }` 空时 size()=0，非空时保持 0，按 taskId 查询 count 恒为 0。
- **建议**：改为 `if (!isEmpty) recordCount = list.size();`。

#### P1-14 【除零/NPE】channelId 除数可能为 null/0
- **文件**：`DataMoveHandler.java`（1096-1098 行）
- **问题**：`virtualId % dataStreamQueueChannel + 1`，channel 为 null/0 时 NPE/除零。
- **建议**：入口校验 channel > 0。

#### P1-15 【字符串比较错误】`!= "null"` 引用比较
- **文件**：`DataMoveHandler.java`（302 行）
- **问题**：`getCurrentValue() != "null"` 引用比较永远不可控，分页游标判断会错误。
- **建议**：`!"null".equals(...)` 或 `Objects.equals(...)`。

#### P1-16 【未实现】PG/H2 的 getTableInfo 空实现
- **文件**：`PostgresTableMetaResolver.java`（42-45 行）、`H2TableMetaResolver.java`（42-45 行）
- **问题**：两库无法列出表清单，功能不完整。
- **建议**：参照 MySQL/Oracle 增加 information_schema.tables 查询。

#### P1-17 【未实现】数据修复不等分支未实现
- **文件**：`DataCheckHandler.java`（193-202 行）
- **问题**：`DATA_CHECK_RESULT_NOTEQUAL` 分支是 `//todo update`，`recordResult` 仍为 0，随后抛错，「修复数据不等」功能不可用。
- **建议**：补齐实现。

#### P1-18 【编码不一致】文件读写编码不统一
- **文件**：`TextFileApiImpl.java`（58、90、274-275、360-371 行）
- **问题**：读用 `FileReader`（平台默认编码），写用 UTF-8；`RandomAccessFile` 定位按 UTF-8 算、写入用默认编码，多字节内容错位。
- **建议**：读写统一 `StandardCharsets.UTF_8`。

---

## 四、P2 级 — 中优先级

### 4.1 前端安全

#### P2-1 【硬编码密钥】加密密钥散落三处
- **文件**：`datastream-ui/src/config/index.js`（28 行）、`src/config/api.js`（100 行）、`src/utils/crypto.js`（23 行）
- **问题**：`ENCRYPT_KEY = '1234567812345678'` 重复定义，固定弱密钥，与后端一致，可解密所有存储的数据库密码。
- **建议**：密钥收敛为单一来源，由后端加密/下发，前端不参与密码加密。

#### P2-2 【XSS】v-html 渲染未转义 SQL
- **文件**：`datastream-ui/src/views/taskmanage/components/TaskObserveDrawer.vue`（158、319-350 行）
- **问题**：`highlightSql` 只对关键字/字符串做正则包裹，未对原始 SQL 做 HTML 实体转义，表名/列名含 `<img onerror=...>` 会执行，存储型 XSS。
- **建议**：先 `escapeHtml` 再高亮，或接入 DOMPurify。

#### P2-3 【XSS】markdown 渲染未消毒
- **文件**：`datastream-ui/src/views/system-manage/AboutSystem.vue`（19、52-57 行）
- **问题**：`marked()` 默认不消毒 HTML，直接 `v-html`。
- **建议**：接入 dompurify。

#### P2-4 【token 泄露】完整 token 打印到控制台
- **文件**：`datastream-ui/src/utils/fetch.js`（29-104 行多处）、`JWTAuthorizationFilter.java`（51 行）
- **问题**：前后端都把完整 token/headers 打印到日志/控制台。
- **建议**：删除所有 token 相关日志。

#### P2-5 【认证失效】前端路由无权限控制
- **文件**：`datastream-ui/src/router/index.js`（136-150 行）
- **问题**：`beforeEach` 无条件 `next()` 放行，所有页面未登录可访问；`checkToken()` 只判断 sessionStorage 有无 token 字符串，不做有效性校验，可伪造绕过。
- **建议**：路由 meta 标注 requiresAuth，统一校验重定向；token 由后端接口验证。

#### P2-6 【token 泄露】token 走 URL query
- **文件**：`datastream-ui/src/router/index.js`（139-148 行）
- **问题**：`?token=` 出现在 URL，留痕历史/日志/Referer。
- **建议**：改 POST body 或 cookie/sessionStorage。

### 4.2 前端内存泄漏

#### P2-7 【监听器泄漏】VirtualScroll 注册/移除引用不一致
- **文件**：`datastream-ui/src/views/components/VirtualScroll.vue`（106、296 行）
- **问题**：加 `handleScroll`，移除 `onScroll`，不是同一引用，scroll 监听永远移除不掉；`this.timer` 未 clearTimeout。
- **建议**：统一引用，beforeUnmount 中清理。

#### P2-8 【监听器泄漏】flowUtil 滚轮监听只加不移
- **文件**：`datastream-ui/src/utils/flowUtil.js`（101-116 行）
- **问题**：每次 getInstance 都 addEventListener('wheel')，从不 remove，反复进入表链接页叠加监听器。
- **建议**：保存 handler 引用，resetNodeInfo 中移除。

#### P2-9 【定时器泄漏】keep-alive 停用后自动刷新未停
- **文件**：`datastream-ui/src/views/taskmanage/dataMoveTask.vue`（295-310、427-429 行）
- **问题**：`onDeactivated` 空实现，切走 Tab 后 setInterval 后台继续每 20 秒发请求。
- **建议**：onDeactivated 中 clearInterval 并置空。

### 4.3 配置隐患

#### P2-10 【明文密码】元数据库账号密码硬编码
- **文件**：`application.properties`（10-12 行）
- **问题**：`root/root123` 明文写配置，注释里还保留多组历史账号。
- **建议**：环境变量占位符注入，删除历史账号注释。

#### P2-11 【注入防护弱化】Druid 允许多语句执行
- **文件**：`application.properties`（39-40 行）
- **问题**：`multi-statement-allow=true` 削弱 wall 对堆叠注入的拦截。
- **建议**：改为 false。

#### P2-12 【信息泄露】异常信息直接回显客户端
- **文件**：`JWTAuthorizationFilter.java`（75 行）、多个 Controller（KafkaAdapterController、MQConfigController 等）
- **问题**：`e.getMessage()` 直接返回前端，可能暴露连接串、内部路径。
- **建议**：统一错误码/友好提示，具体异常只写日志。

#### P2-13 【日志过宽】root=DEBUG + show-sql
- **文件**：`application.properties`（90 行）、`logback-spring.xml`（89 行）、`application-h2.yml`（31 行）
- **问题**：DEBUG 输出 SQL、请求参数、连接串等敏感信息。
- **建议**：生产 root=INFO/WARN，关闭 show-sql。

#### P2-14 【全放行开关】dataStreamPermit 一键放行
- **文件**：`SecurityConfig.java`（63-64、101-104 行）
- **问题**：误设 true 时所有接口放行。
- **建议**：删除或仅限本地 profile + 启动告警。

#### P2-15 【默认弱口令】测试账号 admin/admin 打开
- **文件**：`application.properties`（78-80 行）、`UserDetailsServiceImpl.java`（52-55 行）、`LoginView.vue`（141-142 行）
- **问题**：`test.mode=true` + 默认 admin/admin 预填，生产若忘关闭即弱口令后门。
- **建议**：默认 false，测试账号不写默认弱口令。

---

## 五、P3 级 — 低优先级

### 5.1 性能 / 包体积

#### P3-1 【包体积大】全量引入重依赖
- **文件**：`datastream-ui/src/main.js`（20-35、65-67 行）
- **问题**：Element Plus 全量 + 全部图标 + echarts + jsplumb + jquery 全打主包，无法 tree-shaking。
- **建议**：unplugin-vue-components 按需引入；图标单独 import；echarts 用 core + 按需；jsplumb/jQuery 懒加载。

#### P3-2 【缓存无上限】keep-alive 无 max/include
- **文件**：`datastream-ui/src/views/layout/index.vue`（196-209 行）
- **问题**：访问过的每个 Tab（含 echarts/jsplumb/大表格）常驻内存，长期内存增长。
- **建议**：加 `:max` 或 `:include`。

#### P3-3 【BLOB 性能】字节转十六进制用 String.format
- **文件**：`AbstractSqlBuilder.java`（457-463 行）
- **问题**：每字节一次 String.format，大 BLOB 百万次格式化 CPU 消耗巨大。
- **建议**：查表法或位运算。

#### P3-4 【大对象 OOM】Clob/Blob 一次性读入内存
- **文件**：`AbstractSqlBuilder.java`（320-352 行）
- **问题**：`getSubString(1,len)`/`getBytes(1,len)` 整个读入内存，超 Integer.MAX_VALUE 截断。
- **建议**：分块流式读取或 PreparedStatement setBlob/setClob。

### 5.2 架构 / 可扩展性

#### P3-5 【SPI 未实现】扩展连接器需改 starter 硬编码
- **文件**：`DataStreamApplication.java`（31-42 行）、`datastream-starter/pom.xml`（59-84 行）
- **问题**：所谓 SPI/auto-service 实际不存在（`META-INF/spring.factories` 为空文件），新增连接器需改父 pom + starter pom + @MapperScan + 枚举。
- **建议**：引入真正的 @AutoService + ServiceLoader 或 @ConditionalOnClass 条件装配，DAO 扫描下沉到各模块。

#### P3-6 【无效配置】@MapperScan 引用不存在的 postgres.dao 包
- **文件**：`DataStreamApplication.java`（41 行）
- **问题**：`com.itman.datastream.connectors.postgres.dao` 不存在，暴露装配与实现脱节。
- **建议**：补实现或移除该包。

#### P3-7 【歧义不报错】连接器选择 findFirst 静默取第一个
- **文件**：`DataSourceFactory.java`（37-53 行）
- **问题**：多匹配时不报歧义错误，依赖 List 注入顺序。
- **建议**：多匹配时抛明确异常。

### 5.3 可维护性 / 死代码

#### P3-8 【死代码】前端 taskStore.js 空壳
- **文件**：`datastream-ui/src/store/taskStore.js`
- **问题**：全项目无引用，与 useTaskManage 重复，逻辑空壳。
- **建议**：删除。

#### P3-9 【噪音】.bak 文件残留、调试日志
- **文件**：`datastream-ui/src/views/components/TableLinkTemplate.vue.style.bak`、`fetch.js`（20+ 处 console.log）
- **建议**：删除 .bak；日志收敛到 DEV 分支。

#### P3-10 【依赖老旧】Spring Boot / fastjson / jjwt / H2
- **文件**：`pom.xml`（22、46、55 行）、`datastream-security/pom.xml`（40 行）
- **问题**：Spring Boot 2.1.4（停维护）、fastjson 1.x（EOL）、jjwt 0.9.1、H2 1.4.193。
- **建议**：评估升级 Spring Boot、替换 fastjson 为 jackson/fastjson2、升级 jjwt/H2。

#### P3-11 【拼写错误】MQTypeEnum 中 Kafka 拼成 KAKFA
- **文件**：`MQTypeEnum.java`（37 行）、`KafkaAdapterImpl.java`（49、56 行）
- **建议**：改为 KAFKA。

#### P3-12 【编码问题】PG 类型归一化不完整
- **文件**：`CommUtils.java`（221-228 行）
- **问题**：`normalizeTypeName` 只去 `(`，不能处理 `timestamp with time zone`、`character varying` 等 PG 类型。
- **建议**：增加 PG 类型别名映射。

---

## 六、修复优先级路线图

```
第一阶段（安全加固，P0）
  ├─ 修复认证绕过（isTokenExpiration 语义反转）
  ├─ JWT 密钥外部化 + 增加 exp
  ├─ 收紧白名单（移除 indexOf("login")）
  ├─ 关闭/隔离 H2 控制台 + 升级 H2
  └─ 密码加密改 AES-GCM/密钥托管，替换硬编码密钥

第二阶段（数据正确性，P0/P1）
  ├─ 恢复 MQ/同步迁移实现
  ├─ Debezium offset 同步落库 + 多分区回放
  ├─ SQL 注入：参数化 + 转义 + 白名单
  ├─ Oracle/PG/H2 适配器正确性修复
  └─ 连接池并发注册、资源释放 finally 化

第三阶段（前端与架构，P2/P3）
  ├─ XSS 防护、token 日志清理、路由鉴权
  ├─ 内存泄漏修复（VirtualScroll/flowUtil/定时器）
  ├─ 按需引入、keep-alive 上限
  └─ SPI 真正落地、依赖升级、死代码清理
```

---

## 七、附录：质量良好的模块（无需改动）

- `RouteSource.java`：注解定义简洁清晰
- `RouteConfig.java`：元数据库 Druid 数据源配置规范
- `PoolInfo.java`：AtomicInteger + ConcurrentHashMap 管理引用计数（仅 decrementRef 需加下限）
- `ConnectionWrapper.java`：原子状态管理设计良好
- `MoveSourceServiceImpl.java` / `IMoveSourceService.java`：接口路由注解约定清晰、薄委托
- `DataSourceFactory.java`：策略匹配逻辑清晰（除 findFirst 歧义外）
- `MoveExecutor.sleepWait`：正确恢复中断状态
- 前端 `useEventBus.js` / `useDebouncedFn` / `useResourceMonitor.js`：正确做 onUnmounted 清理
- 前端路由懒加载 + `build.sourcemap: false`
- 前端 `storage.js`：统一封装 localStorage/sessionStorage 带 try/catch

---

> 本文档所有问题均附文件相对路径（相对项目根目录）与关键行号，可直接定位到代码。可作为后续重构与修复的工作清单。
