-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- ====================================================================
-- MySQL 全字段类型测试表 - 用于 MySQL -> PostgreSQL 结构同步与数据迁移测试
-- 说明: 覆盖 MySQL 8.0 全部数据类型(整数/浮点/定点/位/布尔/字符串/
--       文本/枚举/集合/二进制/大对象/日期时间/年份/JSON/空间类型)
-- 执行库: dbtest1 (MySQL 源库)
-- ====================================================================

DROP TABLE IF EXISTS test_mysql_all_types;

CREATE TABLE test_mysql_all_types (
    id                   BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键-大整数自增',
    f_tinyint            TINYINT         NOT NULL COMMENT '极小整数',
    f_tinyint_unsigned   TINYINT UNSIGNED NOT NULL COMMENT '极小整数-无符号',
    f_smallint           SMALLINT        NOT NULL COMMENT '小整数',
    f_smallint_unsigned  SMALLINT UNSIGNED NOT NULL COMMENT '小整数-无符号',
    f_mediumint          MEDIUMINT       NOT NULL COMMENT '中等整数',
    f_mediumint_unsigned MEDIUMINT UNSIGNED NOT NULL COMMENT '中等整数-无符号',
    f_int                INT             NOT NULL COMMENT '标准整数',
    f_int_unsigned       INT UNSIGNED    NOT NULL COMMENT '标准整数-无符号',
    f_bigint             BIGINT          NOT NULL COMMENT '大整数',
    f_bigint_unsigned    BIGINT UNSIGNED NOT NULL COMMENT '大整数-无符号',
    f_decimal            DECIMAL(20,5)   NOT NULL COMMENT '定点小数',
    f_float              FLOAT           NOT NULL COMMENT '单精度浮点数',
    f_double             DOUBLE          NOT NULL COMMENT '双精度浮点数',
    f_bit                BIT(1)          NOT NULL COMMENT '位类型',
    f_bool               BOOLEAN         NOT NULL COMMENT '布尔-TINYINT1别名',
    f_char               CHAR(10)        NOT NULL COMMENT '定长字符串',
    f_varchar            VARCHAR(100)    NOT NULL COMMENT '变长字符串',
    f_tinytext           TINYTEXT        NOT NULL COMMENT '短文本',
    f_text               TEXT            NOT NULL COMMENT '文本',
    f_mediumtext         MEDIUMTEXT      NOT NULL COMMENT '中等文本',
    f_longtext           LONGTEXT        NOT NULL COMMENT '长文本',
    f_enum               ENUM('A','B','C') NOT NULL COMMENT '枚举类型',
    f_set                SET('X','Y','Z') NOT NULL COMMENT '集合类型',
    f_binary             BINARY(16)      NOT NULL COMMENT '定长二进制',
    f_varbinary          VARBINARY(64)   NOT NULL COMMENT '变长二进制',
    f_tinyblob           TINYBLOB        NOT NULL COMMENT '短二进制大对象',
    f_blob               BLOB            NOT NULL COMMENT '二进制大对象',
    f_mediumblob         MEDIUMBLOB      NOT NULL COMMENT '中等二进制大对象',
    f_longblob           LONGBLOB        NOT NULL COMMENT '长二进制大对象',
    f_date               DATE            NOT NULL COMMENT '日期',
    f_time               TIME            NOT NULL COMMENT '时间',
    f_datetime           DATETIME        NOT NULL COMMENT '日期时间',
    f_timestamp          TIMESTAMP       NOT NULL COMMENT '时间戳',
    f_year               YEAR            NOT NULL COMMENT '年份',
    f_json               JSON            NOT NULL COMMENT 'JSON类型',
    f_geometry           GEOMETRY        NOT NULL COMMENT '空间几何类型',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MySQL全字段类型测试表';

-- ====================================================================
-- 模拟数据: 常规值 / 边界最小值 / 边界最大值 + 特殊字符
-- ====================================================================

-- 行1: 常规典型值
INSERT INTO test_mysql_all_types
(f_tinyint, f_tinyint_unsigned, f_smallint, f_smallint_unsigned,
 f_mediumint, f_mediumint_unsigned, f_int, f_int_unsigned,
 f_bigint, f_bigint_unsigned, f_decimal, f_float, f_double,
 f_bit, f_bool, f_char, f_varchar, f_tinytext, f_text,
 f_mediumtext, f_longtext, f_enum, f_set,
 f_binary, f_varbinary, f_tinyblob, f_blob, f_mediumblob, f_longblob,
 f_date, f_time, f_datetime, f_timestamp, f_year, f_json, f_geometry)
VALUES
(1, 100, 1000, 10000,
 100000, 1000000, 100000000, 3000000000,
 9000000000, 18000000000, 12345.67890, 3.14, 3.14159265358979,
 b'1', TRUE, 'CHAR-01', '常规变长字符串Hello', '短文本TinyText', '文本Text内容',
 '中等文本MediumText内容', '长文本LongText内容', 'A', 'X',
 UNHEX('0123456789ABCDEF'), UNHEX('DEADBEEF'), UNHEX('AABBCC'), UNHEX('CAFEF00D'),
 UNHEX('00112233'), UNHEX('0102030405'),
 '2026-08-20', '10:30:45', '2026-08-20 10:30:45', '2026-08-20 10:30:45', 2026,
 '{"name": "常规测试", "score": 99.5, "tags": ["a", "b"]}',
 ST_GeomFromText('POINT(116.397 39.909)'));

-- 行2: 边界最小值
INSERT INTO test_mysql_all_types
(f_tinyint, f_tinyint_unsigned, f_smallint, f_smallint_unsigned,
 f_mediumint, f_mediumint_unsigned, f_int, f_int_unsigned,
 f_bigint, f_bigint_unsigned, f_decimal, f_float, f_double,
 f_bit, f_bool, f_char, f_varchar, f_tinytext, f_text,
 f_mediumtext, f_longtext, f_enum, f_set,
 f_binary, f_varbinary, f_tinyblob, f_blob, f_mediumblob, f_longblob,
 f_date, f_time, f_datetime, f_timestamp, f_year, f_json, f_geometry)
VALUES
(-128, 0, -32768, 0,
 -8388608, 0, -2147483648, 0,
 -9223372036854775808, 0, -99999.99999, -1.5, -1.23456789,
 b'0', FALSE, 'MIN', '边界最小值-负数测试', 'min', '文本最小值',
 '中等文本最小值', '长文本最小值', 'B', 'Y',
 UNHEX('00000000000000000000000000000000'), UNHEX('00'), UNHEX('00'), UNHEX('0000'),
 UNHEX('000000'), UNHEX('0000000000'),
 '1900-01-01', '00:00:00', '1900-01-01 00:00:00', '1970-01-01 08:00:01', 1901,
 '{"flag": false, "count": 0}',
 ST_GeomFromText('POINT(0 0)'));

-- 行3: 边界最大值 + 中文/Emoji/长内容
INSERT INTO test_mysql_all_types
(f_tinyint, f_tinyint_unsigned, f_smallint, f_smallint_unsigned,
 f_mediumint, f_mediumint_unsigned, f_int, f_int_unsigned,
 f_bigint, f_bigint_unsigned, f_decimal, f_float, f_double,
 f_bit, f_bool, f_char, f_varchar, f_tinytext, f_text,
 f_mediumtext, f_longtext, f_enum, f_set,
 f_binary, f_varbinary, f_tinyblob, f_blob, f_mediumblob, f_longblob,
 f_date, f_time, f_datetime, f_timestamp, f_year, f_json, f_geometry)
VALUES
(127, 255, 32767, 65535,
 8388607, 16777215, 2147483647, 4294967295,
 9223372036854775807, 18446744073709551615, 99999.99999, 123.456, 1.7976931348623157e308,
 b'1', TRUE, 'MAX最大10字', '边界最大值中文测试🎉emoji', '短文本最大值', CONCAT('文本最大值-', REPEAT('长', 500)),
 CONCAT('中等文本最大值-', REPEAT('中', 1000)), CONCAT('长文本最大值-', REPEAT('超', 2000)), 'C', 'X,Y,Z',
 UNHEX('FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF'), UNHEX(REPEAT('FF', 32)), UNHEX(REPEAT('BB', 100)),
 UNHEX(REPEAT('CC', 1000)), UNHEX(REPEAT('DD', 10000)), UNHEX(REPEAT('EE', 10000)),
 '9999-12-31', '23:59:59', '9999-12-31 23:59:59', '2038-01-01 03:14:07', 2155,
 '{"name": "最大值测试", "active": true, "score": 100, "nested": {"k": [1, 2, 3]}}',
 ST_GeomFromText('LINESTRING(0 0, 1 1, 2 2)'));

-- 验证
SELECT id, f_tinyint, f_tinyint_unsigned, f_smallint, f_smallint_unsigned,
       f_mediumint, f_mediumint_unsigned, f_int, f_int_unsigned, f_bigint,
       f_bigint_unsigned, f_decimal, f_float, f_double, f_bool, f_enum, f_set,
       f_date, f_time, f_datetime, f_timestamp, f_year
FROM test_mysql_all_types;
