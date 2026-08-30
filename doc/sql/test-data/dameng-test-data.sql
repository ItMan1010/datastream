-- ====================================================================
-- Dameng DM8 test table for cross-database table migration testing
-- Date: 2026-08-30
-- Note: Oracle-style types; instance params CHARSET=1(UTF-8)
--       LENGTH_IN_CHAR=1 CASE_SENSITIVE=0
-- Connect: SYSDBA / SYSDBA @ localhost:5236
-- ====================================================================

-- cleanup (including diagnostic temp tables)
DROP TABLE IF EXISTS dm_test_table;
DROP TABLE IF EXISTS t_ascii;
DROP TABLE IF EXISTS t_cn;
DROP TABLE IF EXISTS t_hex;
DROP TABLE IF EXISTS t_iso;
DROP TABLE IF EXISTS t_a;
DROP TABLE IF EXISTS t_b;
DROP TABLE IF EXISTS t_utf;
DROP TABLE IF EXISTS t_utf2;

-- main test table (covers common data types)
CREATE TABLE dm_test_table (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    name          VARCHAR2(100),
    tiny_num      NUMBER(2),
    small_num     NUMBER(4),
    normal_num    NUMBER(9),
    big_num       NUMBER(18),
    decimal_num   NUMBER(10,2),
    high_prec_num NUMBER(38,10),
    varchar_col   VARCHAR2(100),
    clob_col      CLOB,
    date_col      DATE DEFAULT SYSDATE,
    timestamp_col TIMESTAMP DEFAULT SYSDATE,
    blob_col      BLOB,
    varbinary_col VARBINARY(100),
    status        VARCHAR2(10) DEFAULT 'active'
);

-- insert test data
INSERT INTO dm_test_table (name, tiny_num, small_num, normal_num, big_num,
                           decimal_num, high_prec_num, varchar_col, clob_col,
                           date_col, timestamp_col, varbinary_col, status)
VALUES ('张三', 99, 9999, 999999999, 999999999999999999,
        12345.67, 12345.6789, '测试数据', '这是一段 CLOB 大文本内容',
        SYSDATE, SYSDATE, 0x0A1B2C3D, 'active');

INSERT INTO dm_test_table (name, tiny_num, small_num, normal_num, big_num,
                           decimal_num, high_prec_num, varchar_col, clob_col,
                           date_col, timestamp_col, varbinary_col, status)
VALUES ('李四', -12, -1234, -999999999, -999999999999999999,
        -99999.99, -12345.6789, 'negative values', 'clob data 2',
        SYSDATE, SYSDATE, 0x00FF00FF, 'inactive');

INSERT INTO dm_test_table (name, tiny_num, small_num, normal_num, big_num,
                           decimal_num, high_prec_num, varchar_col, clob_col,
                           date_col, timestamp_col, varbinary_col, status)
VALUES ('王五', 0, 0, 0, 0, 0, 0, 'zero', NULL,
        SYSDATE, SYSDATE, NULL, 'pending');

COMMIT;

-- ====================================================================
-- verify
-- ====================================================================
SELECT id, name, tiny_num, small_num, normal_num, big_num,
       decimal_num, high_prec_num, varchar_col, status
FROM dm_test_table;

-- show table structure
SELECT column_name, data_type, data_length
FROM user_tab_columns
WHERE table_name = 'DM_TEST_TABLE'
ORDER BY column_id;
