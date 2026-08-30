# dameng-connector Specification

## Purpose

提供达梦数据库（DM8）连接器能力，使 DataStream 能够将达梦作为数据迁移、数据清理、数据稽核、表结构迁移、链表迁移等场景的源端或目标端，并生成与达梦兼容的 SQL 方言与表元数据。

## Requirements

### Requirement: 达梦数据源识别与连接器选择
系统 SHALL 识别数据源类型 15（达梦），将 `jdbc:dm://host:port` 形式的 URL 解析为数据库类型 `dm`，并将该类型的数据源匹配到达梦连接器（SQL 方言适配器与表元数据解析器）。

#### Scenario: 达梦 URL 解析为 dm 类型
- **WHEN** 提交的数据源 URL 为 `jdbc:dm://localhost:5236` 且数据源类型为达梦
- **THEN** 系统将该 URL 的数据库类型解析为 `dm`，与数据源类型 15 对应的 `dm` 一致，校验通过

#### Scenario: 达梦数据源匹配到达梦连接器
- **WHEN** 系统按数据源类型 15 匹配 SQL 方言适配器与表元数据解析器
- **THEN** 返回达梦连接器实现，而非其他数据库连接器

### Requirement: 达梦连接与验证
达梦数据源 SHALL 使用达梦 JDBC 驱动建连，连接验证查询为 `select 1 from dual`；达梦连接测试 MUST NOT 向 URL 追加 `socketTimeout` 参数。

#### Scenario: 达梦连接测试成功
- **WHEN** 用户对达梦数据源执行连接测试（URL `jdbc:dm://localhost:5236`、账号 SYSDBA）
- **THEN** 系统使用达梦驱动与验证查询 `select 1 from dual` 建连，返回成功

#### Scenario: 达梦连接测试不追加 socketTimeout
- **WHEN** 达梦数据源执行连接测试
- **THEN** 系统不向达梦 URL 追加 `socketTimeout` 参数

### Requirement: 达梦 SQL 方言生成
达梦方言生成的 SQL SHALL 使用 Oracle 兼容函数：日期转字符串用 `to_char`、字符串转日期用 `to_date`、系统时间用 `sysdate`、空值函数用 `nvl`、序列取值用 `seq.nextval`、列注释用 `comment on column`；分页取数使用 `limit` 子句。

#### Scenario: 达梦分页 SQL 使用 limit
- **WHEN** 达梦作为源端按分页策略生成取数 SQL
- **THEN** 生成的 SQL 包含 `limit offset,count` 形式的分页子句，且能完整覆盖全部行

#### Scenario: 达梦日期与系统时间函数
- **WHEN** 达梦方言生成日期转字符串、字符串转日期、系统时间、空值 SQL
- **THEN** 分别生成 `to_char`、`to_date`、`sysdate`、`nvl` 函数

### Requirement: 达梦表元数据解析
系统 SHALL 能列出达梦 schema 下的表列表，并解析指定表的列信息（列名、类型、长度、精度、小数位、是否可空、默认值、是否主键）；达梦的 schema 由连接用户名确定（达梦 URL 不含 database 路径）。

#### Scenario: 列出达梦 schema 下的表
- **WHEN** 查询达梦数据源 `SYSDBA` 用户下的表列表
- **THEN** 返回该用户拥有的表（如 `dm_test_table`）

#### Scenario: 解析达梦表列信息
- **WHEN** 解析达梦表 `dm_test_table` 的列信息
- **THEN** 返回全部列，且 `NUMBER` 列保留精度/小数位、`VARCHAR2` 列保留长度、主键列标记为主键

### Requirement: 达梦表结构迁移
达梦作为表结构迁移源端时，系统 SHALL 依据源/目标类型映射生成目标库 DDL；达梦作为目标端时，主键约束 SHALL 使用显式命名，并在主键约束自动建索引时跳过与主键列相同的索引。

#### Scenario: 达梦表结构迁移到 MySQL
- **WHEN** 将达梦表（含 NUMBER/VARCHAR2/CLOB/BLOB/DATE/TIMESTAMP 列）结构迁移到 MySQL
- **THEN** 生成 MySQL DDL，其中 NUMBER 按精度映射为对应整数/小数类型、VARCHAR2 映射为 varchar、CLOB 映射为长文本、BLOB 映射为二进制、日期映射为日期时间类型

#### Scenario: 达梦作为目标端创建主键
- **WHEN** 达梦作为目标端创建含主键的表
- **THEN** 生成的主键约束 SQL 使用显式约束名（`alter table ... add constraint ... primary key`），且不重复创建与主键列相同的索引

### Requirement: 达梦数据迁移
达梦作为数据迁移源端或目标端时，系统 SHALL 正确读写数据，包括 NUMBER（精度与小数位）、DATE/TIMESTAMP、CLOB/BLOB/VARBINARY 及中文内容，且不丢失或损坏数据。

#### Scenario: 达梦数据迁移到目标库
- **WHEN** 将达梦表数据（含中文、日期、CLOB、BLOB、VARBINARY、不同精度 NUMBER）迁移到目标库
- **THEN** 目标库记录数与源端一致，且各字段值等价（日期时间、二进制、中文内容无损坏）