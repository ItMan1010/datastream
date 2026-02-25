-- ====================================================================
-- Oracle → MySQL 表结构迁移测试配置
-- 日期: 2026-01-08
-- 说明: 配置数据源和类型映射
-- ====================================================================

-- ====================================================================
-- 1. 数据源配置
-- ====================================================================

-- Oracle 数据源配置
INSERT INTO data_stream_source (
    source_name,
    source_type,
    source_url,
    source_driver,
    source_username,
    source_password,
    source_host,
    source_port,
    source_sid,
    status,
    create_time,
    update_time
) VALUES (
    'Oracle Test DB',
    3,  -- Oracle
    'jdbc:oracle:thin:@//127.0.0.1:1521/helowin',
    'oracle.jdbc.OracleDriver',
    'root',
    'root',
    '127.0.0.1',
    1521,
    'helowin',
    1,  -- 正常
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    source_url = VALUES(source_url),
    source_username = VALUES(source_username),
    source_password = VALUES(source_password),
    update_time = NOW();

-- MySQL 数据源配置
INSERT INTO data_stream_source (
    source_name,
    source_type,
    source_url,
    source_driver,
    source_username,
    source_password,
    source_host,
    source_port,
    source_database,
    status,
    create_time,
    update_time
) VALUES (
    'MySQL Target DB',
    2,  -- MySQL
    'jdbc:mysql://127.0.0.1:13306/debezium?useSSL=false',
    'com.mysql.cj.jdbc.Driver',
    'root',
    'root',
    '127.0.0.1',
    13306,
    'debezium',
    1,  -- 正常
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    source_url = VALUES(source_url),
    source_username = VALUES(source_username),
    source_password = VALUES(source_password),
    update_time = NOW();

-- ====================================================================
-- 2. 查询数据源 ID（后续使用）
-- ====================================================================

SELECT source_id, source_name, source_type
FROM data_stream_source
WHERE source_name IN ('Oracle Test DB', 'MySQL Target DB');

-- ====================================================================
-- 3. Oracle 类型定义（NUMBER 重点测试）
-- ====================================================================

-- 清理旧数据
DELETE FROM data_stream_column_type_define WHERE database_type = 3 AND column_type_name IN (
    'NUMBER', 'VARCHAR2', 'DATE', 'TIMESTAMP', 'CLOB', 'BLOB', 'CHAR'
);

-- 插入 Oracle 类型定义
INSERT INTO data_stream_column_type_define (
    column_type_define_id,
    database_type,
    column_type_classify,
    column_type_name,
    column_standard_size,
    remark,
    type_category,
    max_precision,
    max_scale,
    character_max_length,
    require_length_param
) VALUES
-- NUMBER 类型（多种精度）
(1, 3, 1, 'NUMBER', NULL, 'Oracle 通用数字类型', 'NUMERIC_FIXED_POINT', 38, 127, NULL, 0),
(2, 3, 2, 'VARCHAR2', 4000, '可变长字符串', 'STRING_LONG', NULL, NULL, 4000, 1),
(3, 3, 5, 'DATE', NULL, '日期类型', 'DATETIME_TIMESTAMP', NULL, NULL, NULL, 0),
(4, 3, 5, 'TIMESTAMP', NULL, '时间戳', 'DATETIME_TIMESTAMP', 9, 9, NULL, 0),
(5, 3, 6, 'CLOB', NULL, '大文本', 'STRING_LONG', NULL, NULL, 1073741824, 0),
(6, 3, 7, 'BLOB', NULL, '二进制大对象', 'BINARY', NULL, NULL, 1073741824, 0),
(7, 3, 2, 'CHAR', 2000, '固定长字符串', 'STRING_SHORT', NULL, NULL, 2000, 1);

-- ====================================================================
-- 4. MySQL 类型定义
-- ====================================================================

-- 清理旧数据
DELETE FROM data_stream_column_type_define WHERE database_type = 2 AND column_type_name IN (
    'tinyint', 'smallint', 'int', 'bigint', 'decimal', 'varchar',
    'date', 'datetime', 'timestamp', 'text', 'blob', 'char'
);

-- 插入 MySQL 类型定义
INSERT INTO data_stream_column_type_define (
    column_type_define_id,
    database_type,
    column_type_classify,
    column_type_name,
    column_standard_size,
    remark,
    type_category,
    max_precision,
    max_scale,
    character_max_length,
    min_value,
    max_value,
    require_length_param
) VALUES
-- 整数类型
(101, 2, 1, 'tinyint', 1, '很小的整数', 'NUMERIC_INTEGER', 3, NULL, NULL, -128, 127, 0),
(102, 2, 1, 'smallint', 2, '小整数', 'NUMERIC_INTEGER', 5, NULL, NULL, -32768, 32767, 0),
(103, 2, 1, 'int', 4, '普通整数', 'NUMERIC_INTEGER', 10, NULL, NULL, -2147483648, 2147483647, 0),
(104, 2, 1, 'bigint', 8, '大整数', 'NUMERIC_INTEGER', 19, NULL, NULL, -9223372036854775808, 9223372036854775807, 0),
-- 小数类型
(105, 2, 3, 'decimal', NULL, '精确小数', 'NUMERIC_FIXED_POINT', 65, 30, NULL, NULL, NULL, 1),
-- 字符串类型
(106, 2, 2, 'varchar', 255, '可变长字符串', 'STRING_LONG', NULL, NULL, 65535, NULL, NULL, 1),
(107, 2, 2, 'char', 1, '固定长字符串', 'STRING_SHORT', NULL, NULL, 255, NULL, NULL, 1),
(108, 2, 2, 'text', NULL, '长文本', 'STRING_LONG', NULL, NULL, 65535, NULL, NULL, 0),
-- 日期时间类型
(109, 2, 5, 'date', NULL, '日期', 'DATETIME_DATE', NULL, NULL, NULL, NULL, NULL, 0),
(110, 2, 5, 'datetime', NULL, '日期时间', 'DATETIME_TIMESTAMP', NULL, NULL, NULL, NULL, NULL, 0),
(111, 2, 5, 'timestamp', NULL, '时间戳', 'DATETIME_TIMESTAMP', NULL, NULL, NULL, NULL, NULL, 0),
-- 二进制类型
(112, 2, 7, 'blob', NULL, '二进制大对象', 'BINARY', NULL, NULL, 65535, NULL, NULL, 0);

-- ====================================================================
-- 5. Oracle → MySQL 类型映射配置
-- ====================================================================

-- 清理旧映射
DELETE FROM data_stream_column_type_map WHERE column_type_define_id_a IN (
    SELECT column_type_define_id FROM data_stream_column_type_define
    WHERE database_type = 3 AND column_type_name IN ('NUMBER', 'VARCHAR2', 'DATE', 'TIMESTAMP', 'CLOB', 'BLOB', 'CHAR')
);

-- 插入类型映射（使用 Oracle NUMBER 智能映射规则）
INSERT INTO data_stream_column_type_map (
    column_type_map_id,
    column_type_define_id_a,
    column_type_define_id_b,
    match_level,
    precision_conversion_rule,
    conversion_warning,
    is_reversible
) VALUES
-- NUMBER → 多种 MySQL 类型（使用智能映射）
(1, 1, 101, 3, 'ORACLE_NUMBER_MAPPER', NULL, 0),
(2, 1, 102, 3, 'ORACLE_NUMBER_MAPPER', NULL, 0),
(3, 1, 103, 3, 'ORACLE_NUMBER_MAPPER', NULL, 0),
(4, 1, 104, 3, 'ORACLE_NUMBER_MAPPER', NULL, 0),
(5, 1, 105, 2, 'ORACLE_NUMBER_MAPPER', NULL, 0),
-- 字符串类型
(6, 2, 106, 1, 'COPY', NULL, 1),
(7, 7, 107, 1, 'COPY', NULL, 1),
-- CLOB → TEXT
(8, 5, 108, 2, NULL, 'CLOB 转 TEXT 可能有长度限制', 0),
-- BLOB → BLOB
(9, 6, 112, 1, 'COPY', NULL, 1),
-- DATE → DATETIME
(10, 3, 110, 2, NULL, 'DATE 缺少时间部分', 0),
-- TIMESTAMP → DATETIME
(11, 4, 110, 1, 'COPY', NULL, 1);

COMMIT;

-- ====================================================================
-- 6. 验证配置
-- ====================================================================

-- 查看 Oracle 类型定义
SELECT * FROM data_stream_column_type_define WHERE database_type = 3;

-- 查看 MySQL 类型定义
SELECT * FROM data_stream_column_type_define WHERE database_type = 2;

-- 查看类型映射
SELECT
    a.column_type_name AS oracle_type,
    b.column_type_name AS mysql_type,
    m.match_level,
    m.precision_conversion_rule,
    m.conversion_warning
FROM data_stream_column_type_map m
JOIN data_stream_column_type_define a ON m.column_type_define_id_a = a.column_type_define_id
JOIN data_stream_column_type_define b ON m.column_type_define_id_b = b.column_type_define_id
WHERE a.database_type = 3 AND b.database_type = 2;
