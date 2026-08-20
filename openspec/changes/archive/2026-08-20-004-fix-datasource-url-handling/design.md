## Context

`socketTimeout=5000` 的追加逻辑在 3 处独立手写，分隔符判断各有缺陷（详见 proposal.md - Why）：

- `DataBaseController#testDataBase`：以"URL 是否含 `&`"+"结尾是否为 `?`"判断，URL 含 `&` 且以普通值结尾时漏补 `&`；
- `TableLinkHandler#testTableLink`：仅以"URL 是否含 `&`"判断，含 `&` 时一律不补分隔符；
- `DataMoveHandler`（源端注册）：固定追加 `&`，URL 以 `?` 结尾时产生 `?&`（可运行但不整洁）。

JDBC URL 解析工具（parseJdbcUrl/parseSchemaNameJdbcUrl 等）已集中在 `datastream-common` 的 `CommUtils`，三处调用方（admin 模块）均已依赖该类。`data_stream_data_base.url` 在 MySQL/H2 两份建库 DDL 中均为 `varchar(128)`。

## Goals / Non-Goals

**Goals:**
- 一处实现、三处复用的 URL 参数追加规则，覆盖全部 5 种 URL 形态（见 specs）。
- url 列扩容至 `varchar(512)`，覆盖新建库 DDL、存量升级脚本、运行中元数据库。

**Non-Goals:**
- 不重构 `?` 的预追加逻辑以外的 URL 组装方式（如统一 URL Builder）。
- 不调整 Oracle 的排除规则（连接测试/表链接测试跳过 socketTimeout；迁移源端注册现状不加类型判断，保持不变）。
- 不处理其他表的 URL 类字段（如 `url_path` 日志字段）。

## Decisions

**1. 工具方法落在 `CommUtils`，签名 `appendUrlParam(String url, String paramName, String paramValue)`**
- 备选：各调用点就地修复（3 处复制粘贴，规则仍可能漂移）；或放入 `DataStreamConstant`（常量类不适合放行为）。选 CommUtils 与既有 JDBC URL 工具同址，便于发现与测试。
- 规则（与 spec 场景一一对应）：结尾为 `?`/`&` → 直接追加；已含 `?` → 补 `&`；无 `?` → 补 `?`；URL 已含 `paramName=` → 原样返回（幂等，调用点无需再自行判断 contains）。
- `url` 为 null/空时原样返回，不抛异常（防御式，调用点输入来自元数据库）。

**2. 调用点替换为最小 diff**
- `DataBaseController#testDataBase` / `TableLinkHandler#testTableLink`：删除"补 `?`"与"补 socketTimeout"两个 if 块，合并为一次 `CommUtils.appendUrlParam(url, "socketTimeout", "5000")` 调用（保留 Oracle 排除外层判断）。
- `DataMoveHandler` 源端注册：两段 if 同样合并为一次工具调用；该处原本无 Oracle 判断，维持现状不做行为变更。
- `"5000"` 以字面量保留在调用点（各处现状即字面量，不引入新常量）。

**3. url 列扩容 128 → 512**
- 512 覆盖最长常见形态（多参数 MySQL URL ≈160 字符，Oracle TNS 长连接串可达 300+），且远小于 varchar 行为变化阈值；1024 属过度预留。
- 同步修改 `doc/sql/datastream-mysql-ddl.sql:50`、`doc/sql/datastream-h2-ddl.sql:78`。
- 新增 `doc/sql/datastream-datasource-url-alter.sql` 供存量库执行（含 MySQL ALTER；H2 元数据库为文件库，随 DDL 重建，脚本中注明）。

## Risks / Trade-offs

- [工具方法幂等判断用 `paramName=` 前缀匹配，极端情况下 URL 中参数名作为值的一部分出现会误判已存在] → 现网参数（socketTimeout）作为值出现的可能性极低；且误判结果是不追加，行为安全（驱动侧无该参数仅丢失超时保护，不产生非法 URL）。
- [存量元数据库若遗漏执行 ALTER，长 URL 新增仍会失败] → 升级脚本放入 doc/sql 并在 README 无维护约定的前提下，通过发布包脚本提示；本次实施会对运行中元数据库直接执行。
- [varchar 扩容在线 DDL 锁表] → 该表为配置表，行数极少（个位数），MySQL `ALTER ... MODIFY` 瞬时完成，风险可忽略。

## Migration Plan

1. 代码合入并发布；
2. 对存量元数据库执行 `doc/sql/datastream-datasource-url-alter.sql`（幂等：重复执行报列已是 512 的告警，无害）；
3. 回滚：代码可独立回滚；列扩容无需回滚（512 兼容 128 数据）。
