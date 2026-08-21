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
-- 数据库类型定义和映射表（全量脚本）
-- 日期: 2026-01-08 初版
--       2026-08-20 合并 mysql-pg-ext 扩展脚本，成为自包含全量脚本:
--         1) 修正 PG time/timetz/timestamp/timestamptz/numeric/decimal 的
--            require_length_param=0，避免结构同步生成非法的 timestamp(19)
--            或丢失小数位的 numeric(20)
--         2) 补充 MySQL 全部字段类型定义(位/年份/枚举/集合/二进制/大对象/空间/无符号整数)
--         3) 补充 PG bytea/boolean/bool 定义及 MySQL -> PG 对应映射
-- 说明: 包含 MySQL、PostgreSQL、Oracle 的类型定义和映射关系
--       全新环境在执行 datastream-mysql-ddl.sql 建表后直接执行本脚本即可;
--       已执行过旧版 v2 + mysql-pg-ext 的存量环境数据与本脚本一致, 无需重复执行
-- 依据: MySQL Connector/J 8.0.33 getColumns 实测的 TYPE_NAME 与取值类型
-- ====================================================================

-- ====================================================================
-- 类型分类常量说明
-- type_category: 类型分类
--   NUMERIC_INTEGER       - 整数类型
--   NUMERIC_FIXED_POINT   - 定点数类型（精确小数）
--   NUMERIC_FLOATING_POINT - 浮点数类型（近似值）
--   STRING_SHORT          - 短字符串（有长度限制）
--   STRING_LONG           - 长字符串（无长度限制或很大）
--   DATETIME_DATE         - 日期类型
--   DATETIME_TIME         - 时间类型
--   DATETIME_TIMESTAMP    - 时间戳类型
--   BINARY                - 二进制类型
--   OTHER                 - 其他类型

-- 类型大类常量说明
-- column_type_classify: 类型大类
--   1      - 数字类型
--   2      - 字符串
--   3      - 日期日期
--   4      - 二进制类型
--   5      - 其他类型
-- ====================================================================

-- ====================================================================
-- MySQL 类型定义
-- ====================================================================

-- 数值类型（整数）
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(1, 'mysql', 1, 'tinyint', 3, 'NUMERIC_INTEGER', 3, NULL, NULL, -128, 127, 0, 0, '很小整数 -128~127'),
(2, 'mysql', 1, 'smallint', 5, 'NUMERIC_INTEGER', 5, NULL, NULL, -32768, 32767, 0, 0, '小整数 -32768~32767'),
(3, 'mysql', 1, 'mediumint', 9, 'NUMERIC_INTEGER', 9, NULL, NULL, -8388608, 8388607, 0, 0, '中等整数'),
(4, 'mysql', 1, 'int', 10, 'NUMERIC_INTEGER', 10, NULL, NULL, -2147483648, 2147483647, 0, 0, '标准整数'),
(5, 'mysql', 1, 'bigint', 20, 'NUMERIC_INTEGER', 20, NULL, NULL, -9223372036854775808, 9223372036854775807, 0, 0, '大整数'),
(100, 'mysql', 1, 'bit', 1, 'NUMERIC_INTEGER', NULL, NULL, NULL, NULL, 1, 0, 0, '位类型 BIT(1)/BOOLEAN JDBC报告为BIT');

-- 数值类型（无符号整数）
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(111, 'mysql', 1, 'tinyint unsigned', 3, 'NUMERIC_INTEGER', 3, NULL, NULL, 0, 255, 0, 0, '极小整数-无符号'),
(112, 'mysql', 1, 'smallint unsigned', 5, 'NUMERIC_INTEGER', 5, NULL, NULL, 0, 65535, 0, 0, '小整数-无符号'),
(113, 'mysql', 1, 'mediumint unsigned', 8, 'NUMERIC_INTEGER', 8, NULL, NULL, 0, 16777215, 0, 0, '中等整数-无符号'),
(114, 'mysql', 1, 'int unsigned', 10, 'NUMERIC_INTEGER', 10, NULL, NULL, 0, 4294967295, 0, 0, '标准整数-无符号'),
(115, 'mysql', 1, 'bigint unsigned', 20, 'NUMERIC_INTEGER', 20, NULL, NULL, 0, NULL, 0, 0, '大整数-无符号 JDBC取值为BigInteger 上限18446744073709551615超出有符号BIGINT存储范围');

-- 数值类型（小数）
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(6, 'mysql', 1, 'float', NULL, 'NUMERIC_FLOATING_POINT', NULL, NULL, NULL, NULL, NULL, 0, 0, '单精度浮点数'),
(7, 'mysql', 1, 'double', NULL, 'NUMERIC_FLOATING_POINT', NULL, NULL, NULL, NULL, NULL, 0, 0, '双精度浮点数'),
(8, 'mysql', 1, 'decimal', NULL, 'NUMERIC_FIXED_POINT', 65, 30, NULL, NULL, NULL, 0, 1, '精确小数');

-- 日期时间类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(9, 'mysql', 3, 'date', 10, 'DATETIME_DATE', NULL, NULL, NULL, NULL, NULL, 0, 0, '日期 1000-01-01至9999-12-31'),
(10, 'mysql', 3, 'time', 8, 'DATETIME_TIME', NULL, NULL, NULL, NULL, NULL, 0, 0, '时间 -838:59:59至838:59:59'),
(11, 'mysql', 3, 'timestamp', 19, 'DATETIME_TIMESTAMP', NULL, NULL, NULL, NULL, NULL, 0, 0, '时间戳 1970至2038'),
(12, 'mysql', 3, 'datetime', 19, 'DATETIME_TIMESTAMP', NULL, NULL, NULL, NULL, NULL, 0, 0, '日期时间'),
(13, 'mysql', 3, 'datetimev2', NULL, 'DATETIME_TIMESTAMP', NULL, NULL, NULL, NULL, NULL, 0, 0, '高精度日期时间(Doris兼容类型)'),
(101, 'mysql', 3, 'year', 4, 'DATETIME_DATE', NULL, NULL, NULL, 1901, 2155, 0, 0, '年份 JDBC取值为DATE');

-- 字符串及文本类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(14, 'mysql', 2, 'varchar', NULL, 'STRING_SHORT', NULL, NULL, 65535, NULL, NULL, 0, 1, '可变字符串 最多65535字符'),
(15, 'mysql', 2, 'char', NULL, 'STRING_SHORT', NULL, NULL, 255, NULL, NULL, 0, 1, '定长字符串 最多255字符'),
(16, 'mysql', 2, 'tinytext', 255, 'STRING_LONG', NULL, NULL, 255, NULL, NULL, 0, 0, '短文本 最多255字符'),
(17, 'mysql', 2, 'text', 65535, 'STRING_LONG', NULL, NULL, 65535, NULL, NULL, 0, 0, '文本 最多65535字符'),
(18, 'mysql', 2, 'mediumtext', 16777215, 'STRING_LONG', NULL, NULL, 16777215, NULL, NULL, 0, 0, '中等文本 最多16777215字符'),
(19, 'mysql', 2, 'longtext', 4294967295, 'STRING_LONG', NULL, NULL, 4294967295, NULL, NULL, 0, 0, '长文本 最多4GB字符'),
(20, 'mysql', 5, 'json', NULL, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, 'JSON数据'),
(102, 'mysql', 2, 'enum', 255, 'STRING_SHORT', NULL, NULL, 255, NULL, NULL, 0, 0, '枚举类型'),
(103, 'mysql', 2, 'set', 64, 'STRING_SHORT', NULL, NULL, 64, NULL, NULL, 0, 0, '集合类型');

-- 二进制类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(104, 'mysql', 4, 'binary', 255, 'BINARY', NULL, NULL, 255, NULL, NULL, 0, 1, '定长二进制'),
(105, 'mysql', 4, 'varbinary', 65535, 'BINARY', NULL, NULL, 65535, NULL, NULL, 0, 1, '变长二进制'),
(106, 'mysql', 4, 'tinyblob', 255, 'BINARY', NULL, NULL, 255, NULL, NULL, 0, 0, '短二进制大对象'),
(107, 'mysql', 4, 'blob', 65535, 'BINARY', NULL, NULL, 65535, NULL, NULL, 0, 0, '二进制大对象'),
(108, 'mysql', 4, 'mediumblob', 16777215, 'BINARY', NULL, NULL, 16777215, NULL, NULL, 0, 0, '中等二进制大对象'),
(109, 'mysql', 4, 'longblob', 4294967295, 'BINARY', NULL, NULL, 4294967295, NULL, NULL, 0, 0, '长二进制大对象');

-- 其他类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(110, 'mysql', 5, 'geometry', 65535, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, '空间几何类型 JDBC取值为WKB字节');

-- ====================================================================
-- PostgreSQL 类型定义
-- 注意: time/timetz/timestamp/timestamptz/numeric/decimal 的
--       require_length_param=0, 避免结构同步时生成非法的 timestamp(19)
--       或丢失小数位的 numeric(20)
-- ====================================================================

-- 数值类型（整数）
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(21, 'postgresql', 1, 'int2', 5, 'NUMERIC_INTEGER', 5, NULL, NULL, -32768, 32767, 0, 0, '小整数'),
(22, 'postgresql', 1, 'int4', 11, 'NUMERIC_INTEGER', 10, NULL, NULL, -2147483648, 2147483647, 0, 0, '标准整数'),
(23, 'postgresql', 1, 'int8', 20, 'NUMERIC_INTEGER', 19, NULL, NULL, -9223372036854775808, 9223372036854775807, 0, 0, '大整数'),
(24, 'postgresql', 1, 'smallserial', 5, 'NUMERIC_INTEGER', 5, NULL, NULL, 1, 32767, 0, 0, '自增小整数'),
(25, 'postgresql', 1, 'bigserial', 20, 'NUMERIC_INTEGER', 19, NULL, NULL, 1, 9223372036854775807, 0, 0, '自增大整数');

-- 数值类型（小数）
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(26, 'postgresql', 1, 'numeric', NULL, 'NUMERIC_FIXED_POINT', 1000, 30, NULL, NULL, NULL, 0, 0, '精确小数 任意精度'),
(27, 'postgresql', 1, 'decimal', NULL, 'NUMERIC_FIXED_POINT', 1000, 30, NULL, NULL, NULL, 0, 0, '精确小数 numeric同义词'),
(28, 'postgresql', 1, 'real', NULL, 'NUMERIC_FLOATING_POINT', NULL, NULL, NULL, NULL, NULL, 0, 0, '单精度浮点数'),
(29, 'postgresql', 1, 'float4', NULL, 'NUMERIC_FLOATING_POINT', NULL, NULL, NULL, NULL, NULL, 0, 0, '单精度浮点数'),
(30, 'postgresql', 1, 'float8', NULL, 'NUMERIC_FLOATING_POINT', NULL, NULL, NULL, NULL, NULL, 0, 0, '双精度浮点数');

-- 日期时间类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(31, 'postgresql', 3, 'date', 10, 'DATETIME_DATE', NULL, NULL, NULL, NULL, NULL, 0, 0, '日期'),
(32, 'postgresql', 3, 'time', 8, 'DATETIME_TIME', NULL, NULL, NULL, NULL, NULL, 0, 0, '时间'),
(33, 'postgresql', 3, 'timetz', NULL, 'DATETIME_TIME', NULL, NULL, NULL, NULL, NULL, 0, 0, '带时区时间'),
(34, 'postgresql', 3, 'timestamp', NULL, 'DATETIME_TIMESTAMP', NULL, NULL, NULL, NULL, NULL, 0, 0, '时间戳'),
(35, 'postgresql', 3, 'timestamptz', NULL, 'DATETIME_TIMESTAMP', NULL, NULL, NULL, NULL, NULL, 0, 0, '带时区时间戳'),
(36, 'postgresql', 5, 'interval', NULL, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, '时间间隔');

-- 字符串及文本类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(37, 'postgresql', 2, 'varchar', NULL, 'STRING_SHORT', NULL, NULL, NULL, NULL, NULL, 0, 1, '可变字符串'),
(38, 'postgresql', 2, 'char', NULL, 'STRING_SHORT', NULL, NULL, NULL, NULL, NULL, 0, 1, '定长字符串'),
(39, 'postgresql', 2, 'bpchar', NULL, 'STRING_SHORT', NULL, NULL, NULL, NULL, NULL, 0, 1, '定长字符串 char类型别名'),
(40, 'postgresql', 2, 'text', NULL, 'STRING_LONG', NULL, NULL, NULL, NULL, NULL, 0, 0, '大段文本内容'),
(41, 'postgresql', 2, 'name', NULL, 'STRING_SHORT', NULL, NULL, 64, NULL, NULL, 0, 0, '内部对象名'),
(42, 'postgresql', 5, 'uuid', 36, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, '通用唯一标识');

-- 二进制类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(200, 'postgresql', 4, 'bytea', NULL, 'BINARY', NULL, NULL, 1073741824, NULL, NULL, 0, 0, '二进制大对象');

-- 其他类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(201, 'postgresql', 5, 'boolean', 1, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, '布尔类型'),
(202, 'postgresql', 5, 'bool', 1, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, '布尔类型 boolean别名');

-- ====================================================================
-- Oracle 类型定义
-- ====================================================================

-- 数值类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(60, 'oracle', 1, 'NUMBER', 22, 'NUMERIC_INTEGER', 38, 127, NULL, NULL, NULL, 0, 0, '通用数字类型 需根据精度动态映射'),
(61, 'oracle', 1, 'BINARY_FLOAT', 4, 'NUMERIC_FLOATING_POINT', NULL, NULL, NULL, NULL, NULL, 0, 0, '单精度浮点数'),
(62, 'oracle', 1, 'BINARY_DOUBLE', 8, 'NUMERIC_FLOATING_POINT', NULL, NULL, NULL, NULL, NULL, 0, 0, '双精度浮点数'),
(79, 'oracle', 1, 'FLOAT', NULL, 'NUMERIC_FLOATING_POINT', 38, NULL, NULL, NULL, NULL, 0, 0, '浮点数 NUMBER的子类型');

-- 日期时间类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(63, 'oracle', 3, 'DATE', 7, 'DATETIME_TIMESTAMP', NULL, NULL, NULL, NULL, NULL, 0, 0, '日期时间 包含世纪年月日时分秒'),
(64, 'oracle', 3, 'TIMESTAMP', 11, 'DATETIME_TIMESTAMP', NULL, 9, NULL, NULL, NULL, 0, 1, '时间戳 支持小数秒'),
(75, 'oracle', 5, 'INTERVAL YEAR TO MONTH', NULL, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, '年月至期间隔'),
(76, 'oracle', 5, 'INTERVAL DAY TO SECOND', NULL, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, '日至秒间隔');

-- 字符串类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(65, 'oracle', 2, 'VARCHAR2', NULL, 'STRING_SHORT', NULL, NULL, 4000, NULL, NULL, 0, 1, '可变字符串 最多4000字节'),
(66, 'oracle', 2, 'NVARCHAR2', NULL, 'STRING_SHORT', NULL, NULL, 4000, NULL, NULL, 1, 1, '可变UNICODE字符串'),
(67, 'oracle', 2, 'CHAR', NULL, 'STRING_SHORT', NULL, NULL, 2000, NULL, NULL, 0, 1, '定长字符串 最多2000字节'),
(68, 'oracle', 2, 'NCHAR', NULL, 'STRING_SHORT', NULL, NULL, 2000, NULL, NULL, 1, 1, '定长UNICODE字符串'),
(69, 'oracle', 2, 'CLOB', NULL, 'STRING_LONG', NULL, NULL, 4294967295, NULL, NULL, 0, 0, '字符大对象 最大4GB'),
(70, 'oracle', 2, 'NCLOB', NULL, 'STRING_LONG', NULL, NULL, 4294967295, NULL, NULL, 1, 0, 'UNICODE字符大对象');

-- 二进制类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(71, 'oracle', 4, 'BLOB', NULL, 'BINARY', NULL, NULL, 4294967295, NULL, NULL, 0, 0, '二进制大对象 最大4GB'),
(72, 'oracle', 4, 'BFILE', NULL, 'BINARY', NULL, NULL, 4294967295, NULL, NULL, 0, 0, '外部二进制文件'),
(73, 'oracle', 4, 'RAW', NULL, 'BINARY', NULL, NULL, 2000, NULL, NULL, 0, 0, '原始二进制数据 最多2000字节'),
(74, 'oracle', 4, 'LONG RAW', NULL, 'BINARY', NULL, NULL, 2147483647, NULL, NULL, 0, 0, '长原始二进制数据 最多2GB');

-- 其他类型
INSERT INTO data_stream_column_type_define (column_type_define_id, database_type, column_type_classify, column_type_name, column_standard_size, type_category, max_precision, max_scale, character_max_length, min_value, max_value, is_national_flag, require_length_param, remark) VALUES
(77, 'oracle', 5, 'ROWID', 10, 'OTHER', NULL, NULL, NULL, NULL, NULL, 0, 0, '行地址'),
(78, 'oracle', 5, 'UROWID', NULL, 'OTHER', NULL, NULL, 4000, NULL, NULL, 0, 0, '通用行地址');

-- ====================================================================
-- 类型映射：MySQL -> PostgreSQL
-- ====================================================================

-- 数值类型映射（精确匹配，可逆）
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(1, 1, 21, 1, NULL, NULL, NULL, 1),  -- tinyint -> int2
(2, 2, 21, 2, NULL, NULL, '范围: MySQL ±32768 vs PG ±32767', 0),  -- smallint -> int2 (MySQL范围更大)
(3, 3, 22, 1, NULL, NULL, NULL, 1),  -- mediumint -> int4
(4, 4, 22, 1, NULL, NULL, NULL, 1),  -- int -> int4
(5, 5, 23, 1, NULL, NULL, NULL, 1),  -- bigint -> int8
(6, 6, 29, 2, NULL, NULL, '精度可能损失', 0),  -- float -> float4 (精度降低)
(7, 7, 30, 1, NULL, NULL, NULL, 1),  -- double -> float8
(8, 8, 26, 1, NULL, NULL, NULL, 1),  -- decimal -> numeric
(300, 100, 201, 2, NULL, NULL, 'BIT(1)/BOOLEAN映射为boolean; BIT(n>1)需人工确认', 0),  -- bit -> boolean
(311, 111, 21, 1, NULL, NULL, NULL, 1),  -- tinyint unsigned -> int2
(312, 112, 22, 1, NULL, NULL, NULL, 1),  -- smallint unsigned -> int4
(313, 113, 22, 1, NULL, NULL, NULL, 1),  -- mediumint unsigned -> int4
(314, 114, 23, 1, NULL, NULL, NULL, 1),  -- int unsigned -> int8
(315, 115, 26, 2, NULL, NULL, '无符号大整数映射为numeric', 0);  -- bigint unsigned -> numeric

-- 日期时间类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(9, 9, 31, 1, NULL, NULL, NULL, 1),  -- date -> date
(10, 10, 32, 1, NULL, NULL, NULL, 1),  -- time -> time
(11, 11, 34, 1, NULL, NULL, '范围: MySQL到2038 vs PG更广', 0),  -- timestamp -> timestamp
(12, 12, 34, 1, NULL, NULL, NULL, 1),  -- datetime -> timestamp
(13, 13, 34, 1, NULL, NULL, NULL, 1),  -- datetimev2 -> timestamp
(301, 101, 31, 2, NULL, NULL, 'YEAR取值为DATE, 映射为date', 0);  -- year -> date

-- 字符串类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(14, 14, 37, 1, NULL, 'min(len, 65535)', NULL, 1),  -- varchar -> varchar (PG无长度限制通常)
(15, 15, 38, 1, NULL, NULL, NULL, 1),  -- char -> char
(16, 16, 40, 1, NULL, NULL, NULL, 1),  -- tinytext -> text
(17, 17, 40, 1, NULL, NULL, NULL, 1),  -- text -> text
(18, 18, 40, 1, NULL, NULL, NULL, 1),  -- mediumtext -> text
(19, 19, 40, 1, NULL, NULL, NULL, 1),  -- longtext -> text
(20, 20, 40, 2, NULL, NULL, 'PostgreSQL有原生JSON类型但此处映射到TEXT', 0),  -- json -> text (降级)
(302, 102, 37, 2, NULL, 'min(len, 65535)', '枚举映射为varchar', 0),  -- enum -> varchar
(303, 103, 37, 2, NULL, 'min(len, 65535)', '集合映射为varchar', 0);  -- set -> varchar

-- 二进制类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(304, 104, 200, 1, NULL, NULL, NULL, 1),  -- binary -> bytea
(305, 105, 200, 1, NULL, NULL, NULL, 1),  -- varbinary -> bytea
(306, 106, 200, 1, NULL, NULL, NULL, 1),  -- tinyblob -> bytea
(307, 107, 200, 1, NULL, NULL, NULL, 1),  -- blob -> bytea
(308, 108, 200, 1, NULL, NULL, NULL, 1),  -- mediumblob -> bytea
(309, 109, 200, 1, NULL, NULL, NULL, 1),  -- longblob -> bytea
(310, 110, 200, 3, NULL, NULL, '空间类型以WKB字节迁移到bytea, 不保留空间语义', 0);  -- geometry -> bytea (降级)

-- ====================================================================
-- 类型映射：Oracle -> MySQL
-- ====================================================================

-- 数值类型映射
-- 注意：Oracle NUMBER 类型由 OracleNumberMapper 智能处理，这里仅做基础映射记录
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(21, 60, 4, 3, 'ORACLE_NUMBER_MAPPER', NULL, '由OracleNumberMapper根据精度动态映射', 0),  -- NUMBER -> int (标记为特殊处理)
(22, 60, 5, 3, 'ORACLE_NUMBER_MAPPER', NULL, '由OracleNumberMapper根据精度动态映射', 0),  -- NUMBER -> bigint (标记为特殊处理)
(23, 60, 8, 3, 'ORACLE_NUMBER_MAPPER', NULL, '由OracleNumberMapper根据精度动态映射', 0),  -- NUMBER -> decimal (标记为特殊处理)
(24, 61, 6, 1, NULL, NULL, NULL, 1),  -- BINARY_FLOAT -> float
(25, 62, 7, 1, NULL, NULL, NULL, 1),  -- BINARY_DOUBLE -> double
(26, 79, 6, 1, NULL, NULL, NULL, 1);  -- FLOAT -> float

-- 日期时间类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(27, 63, 12, 1, NULL, NULL, 'Oracle DATE包含时间，MySQL DATETIME对应', 1),  -- DATE -> datetime
(28, 64, 11, 1, NULL, NULL, '范围不同', 0),  -- TIMESTAMP -> timestamp
(30, 75, 12, 3, NULL, NULL, 'INTERVAL类型需应用程序处理', 0),  -- INTERVAL YEAR TO MONTH -> datetime (降级)
(31, 76, 12, 3, NULL, NULL, 'INTERVAL类型需应用程序处理', 0);  -- INTERVAL DAY TO SECOND -> datetime (降级)

-- 字符串类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(32, 65, 14, 1, NULL, 'min(len, 65535)', 'Oracle按字节计算', 1),  -- VARCHAR2 -> varchar
(33, 66, 14, 1, NULL, 'min(len, 65535)', NULL, 1),  -- NVARCHAR2 -> varchar
(34, 67, 15, 1, NULL, 'min(len, 255)', NULL, 1),  -- CHAR -> char
(35, 68, 15, 1, NULL, 'min(len, 255)', NULL, 1),  -- NCHAR -> char
(36, 69, 19, 1, NULL, NULL, NULL, 1),  -- CLOB -> longtext
(37, 70, 19, 1, NULL, NULL, NULL, 1);  -- NCLOB -> longtext

-- 二进制类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(38, 71, 19, 1, NULL, NULL, NULL, 1),  -- BLOB -> longtext (MySQL LONGBLOB对应longtext)
(39, 73, 17, 1, NULL, 'min(len, 65535)', NULL, 1);  -- RAW -> blob (较小二进制数据)

-- ====================================================================
-- 类型映射：PostgreSQL -> MySQL
-- ====================================================================

-- 数值类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(51, 21, 2, 1, NULL, NULL, 'PG smallint范围略大', 0),  -- int2 -> smallint
(52, 22, 4, 1, NULL, NULL, NULL, 1),  -- int4 -> int
(53, 23, 5, 1, NULL, NULL, NULL, 1),  -- int8 -> bigint
(54, 26, 8, 1, 'min(p, 65)', 'min(s, 30)', NULL, 1),  -- numeric -> decimal
(55, 27, 8, 1, 'min(p, 65)', 'min(s, 30)', NULL, 1),  -- decimal -> decimal
(56, 28, 6, 2, NULL, NULL, '精度可能损失', 0),  -- real -> float
(57, 29, 6, 2, NULL, NULL, '精度可能损失', 0),  -- float4 -> float
(58, 30, 7, 1, NULL, NULL, NULL, 1);  -- float8 -> double

-- 日期时间类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(61, 31, 9, 1, NULL, NULL, NULL, 1),  -- date -> date
(62, 32, 10, 1, NULL, NULL, NULL, 1),  -- time -> time
(63, 34, 12, 1, NULL, NULL, '范围不同', 0),  -- timestamp -> datetime
(64, 35, 12, 1, NULL, NULL, '带时区转无时区', 0);  -- timestamptz -> datetime

-- 字符串类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(71, 37, 14, 1, NULL, 'min(len, 65535)', NULL, 1),  -- varchar -> varchar
(72, 38, 15, 1, NULL, 'min(len, 255)', NULL, 1),  -- char -> char
(73, 40, 17, 1, NULL, NULL, NULL, 1);  -- text -> tinytext (降级，或text)

-- ====================================================================
-- 类型映射：MySQL -> Oracle
-- ====================================================================

-- 数值类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(81, 1, 60, 3, NULL, NULL, '由OracleNumberMapper处理', 0),  -- tinyint -> NUMBER
(82, 2, 60, 3, NULL, NULL, '由OracleNumberMapper处理', 0),  -- smallint -> NUMBER
(83, 4, 60, 3, NULL, NULL, '由OracleNumberMapper处理', 0),  -- int -> NUMBER
(84, 5, 60, 3, NULL, NULL, '由OracleNumberMapper处理', 0),  -- bigint -> NUMBER
(85, 6, 61, 1, NULL, NULL, NULL, 1),  -- float -> BINARY_FLOAT
(86, 7, 62, 1, NULL, NULL, NULL, 1),  -- double -> BINARY_DOUBLE
(87, 8, 60, 3, NULL, NULL, '由OracleNumberMapper处理', 0);  -- decimal -> NUMBER

-- 日期时间类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(91, 9, 63, 1, NULL, NULL, 'MySQL仅日期 vs Oracle包含时间', 0),  -- date -> DATE
(92, 10, 76, 3, NULL, NULL, '需应用程序处理', 0),  -- time -> INTERVAL DAY TO SECOND
(93, 11, 64, 1, NULL, NULL, NULL, 1),  -- timestamp -> TIMESTAMP
(94, 12, 63, 1, NULL, NULL, NULL, 1);  -- datetime -> DATE (Oracle DATE包含时间)

-- 字符串类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(101, 14, 65, 1, NULL, 'min(len, 4000)', NULL, 1),  -- varchar -> VARCHAR2
(102, 15, 67, 1, NULL, 'min(len, 2000)', NULL, 1),  -- char -> CHAR
(103, 16, 69, 1, NULL, NULL, NULL, 1),  -- tinytext -> CLOB
(104, 17, 69, 1, NULL, NULL, NULL, 1),  -- text -> CLOB
(105, 18, 69, 1, NULL, NULL, NULL, 1),  -- mediumtext -> CLOB
(106, 19, 69, 1, NULL, NULL, NULL, 1);  -- longtext -> CLOB

-- 二进制类型映射
INSERT INTO data_stream_column_type_map (column_type_map_id, column_type_define_id_a, column_type_define_id_b, match_level, precision_conversion_rule, length_conversion_rule, conversion_warning, is_reversible) VALUES
(111, 20, 69, 3, NULL, NULL, 'JSON类型映射到CLOB', 0);  -- json -> CLOB (降级)

-- ====================================================================
-- 验证查询
-- ====================================================================

-- 查看所有类型定义
SELECT
    column_type_define_id,
    database_type,
    column_type_name,
    type_category,
    max_precision,
    max_scale,
    character_max_length,
    min_value,
    max_value
FROM data_stream_column_type_define
ORDER BY database_type, column_type_define_id;

-- 查看所有映射关系
SELECT
    m.column_type_map_id,
    a.database_type AS source_db,
    a.column_type_name AS source_type,
    a.type_category AS source_category,
    b.database_type AS target_db,
    b.column_type_name AS target_type,
    b.type_category AS target_category,
    m.match_level,
    m.conversion_warning
FROM data_stream_column_type_map m
JOIN data_stream_column_type_define a ON m.column_type_define_id_a = a.column_type_define_id
JOIN data_stream_column_type_define b ON m.column_type_define_id_b = b.column_type_define_id
ORDER BY m.match_level, m.column_type_map_id;
