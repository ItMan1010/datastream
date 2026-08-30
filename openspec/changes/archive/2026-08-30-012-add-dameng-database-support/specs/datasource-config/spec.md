## MODIFIED Requirements

### Requirement: JDBC URL 参数追加必须保持 URL 合法

系统在连接测试、表链接测试、迁移任务源端注册等场景向 JDBC URL 追加连接参数（如 `socketTimeout`）时，MUST 依据 URL 当前形态选择分隔符，生成语法合法的 URL：

- URL 以 `?` 或 `&` 结尾：直接追加参数，不再补分隔符；
- URL 已含 `?` 且以普通字符结尾：追加 `&` + 参数；
- URL 不含 `?`：追加 `?` + 参数；
- URL 中已存在同名参数：不做任何追加（幂等）；
- 追加参数的判断仅以 URL 当前内容为准，不得依赖"URL 是否包含 `&`"这类与结尾无关的条件。

各调用点原有的数据库类型排除规则 MUST 保持不变：Oracle 与达梦不追加 `socketTimeout`，其余数据库类型按上述规则追加。

#### Scenario: 带多个参数且以普通值结尾的 URL 连接测试成功
- **WHEN** 用户在"连接测试"中提交 URL `jdbc:mysql://127.0.0.1:13307/dbtest1?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`（MySQL 类型）
- **THEN** 系统内部使用 `...&allowPublicKeyRetrieval=true&socketTimeout=5000` 建连，连接测试返回成功，而不是将参数拼为 `truesocketTimeout=5000` 导致 POOL_005 错误

#### Scenario: 以 & 结尾的 URL 不产生重复分隔符
- **WHEN** 提交的 URL 以 `&` 结尾（如 `jdbc:mysql://host:3306/db?useSSL=false&`）
- **THEN** 追加后 URL 为 `...&socketTimeout=5000`，不出现 `&&`

#### Scenario: 不含 ? 的裸 URL
- **WHEN** 提交的 URL 不带任何查询参数（如 `jdbc:mysql://host:3306/db`）
- **THEN** 追加后 URL 为 `...db?socketTimeout=5000`

#### Scenario: 已含 socketTimeout 的 URL 保持不变
- **WHEN** 提交的 URL 已包含 `socketTimeout` 参数（任意位置、任意大小写形式为 `socketTimeout=` 前缀匹配）
- **THEN** 系统不重复追加该参数

#### Scenario: Oracle URL 不追加 socketTimeout
- **WHEN** 数据库类型为 Oracle
- **THEN** 连接测试场景下系统不向其 URL 追加 `socketTimeout` 参数

#### Scenario: 达梦 URL 不追加 socketTimeout
- **WHEN** 数据库类型为达梦
- **THEN** 连接测试场景下系统不向其 URL 追加 `socketTimeout` 参数
