--Licensed to the Apache Software Foundation (ASF) under one or more
--contributor license agreements.  See the NOTICE file distributed with
--this work for additional information regarding copyright ownership.
--The ASF licenses this file to You under the Apache License, Version 2.0
--(the "License"); you may not use this file except in compliance with
--the License.  You may obtain a copy of the License at
--
--http://www.apache.org/licenses/LICENSE-2.0
--
--Unless required by applicable law or agreed to in writing, software
--distributed under the License is distributed on an "AS IS" BASIS,
--WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--See the License for the specific language governing permissions and
--limitations under the License.

-- ====================================================================
-- 数据库表结构类型映射表字段扩展脚本
-- 日期: 2026-01-08
-- 说明: 为 data_stream_column_type_define 和 data_stream_column_type_map 表新增字段
-- ====================================================================

-- ====================================================================
-- 一、data_stream_column_type_define 表新增字段
-- ====================================================================

-- 类型分类：整数/小数/浮点/字符串/日期/二进制/布尔/json/数组/uuid/其他
ALTER TABLE data_stream_column_type_define
ADD COLUMN type_category VARCHAR(50) DEFAULT NULL COMMENT '类型分类：整数/小数/浮点/字符串/日期/二进制/布尔/json/数组/uuid/其他';

-- 最大精度（整数位数）
ALTER TABLE data_stream_column_type_define
ADD COLUMN max_precision INT DEFAULT NULL COMMENT '最大精度（整数位数）';

-- 最大小数位数
ALTER TABLE data_stream_column_type_define
ADD COLUMN max_scale INT DEFAULT NULL COMMENT '最大小数位数';

-- 字符串最大长度
ALTER TABLE data_stream_column_type_define
ADD COLUMN character_max_length BIGINT DEFAULT NULL COMMENT '字符串最大长度';

-- 最小值
ALTER TABLE data_stream_column_type_define
ADD COLUMN min_value BIGINT DEFAULT NULL COMMENT '最小值';

-- 最大值
ALTER TABLE data_stream_column_type_define
ADD COLUMN max_value BIGINT DEFAULT NULL COMMENT '最大值';

-- 是否支持字符集（如 nvarchar）
ALTER TABLE data_stream_column_type_define
ADD COLUMN is_national_flag INT DEFAULT 0 COMMENT '是否支持字符集（如 nvarchar）';

-- 是否必须指定长度参数
ALTER TABLE data_stream_column_type_define
ADD COLUMN require_length_param INT DEFAULT 0 COMMENT '是否必须指定长度参数';

-- ====================================================================
-- 二、data_stream_column_type_map 表新增字段
-- ====================================================================

-- 匹配级别：1-精确匹配，2-兼容匹配，3-降级匹配
ALTER TABLE data_stream_column_type_map
ADD COLUMN match_level INT DEFAULT 1 COMMENT '匹配级别：1-精确匹配，2-兼容匹配，3-降级匹配';

-- 精度转换规则表达式
ALTER TABLE data_stream_column_type_map
ADD COLUMN precision_conversion_rule VARCHAR(200) DEFAULT NULL COMMENT '精度转换规则表达式';

-- 长度转换规则表达式
ALTER TABLE data_stream_column_type_map
ADD COLUMN length_conversion_rule VARCHAR(200) DEFAULT NULL COMMENT '长度转换规则表达式';

-- 转换警告信息
ALTER TABLE data_stream_column_type_map
ADD COLUMN conversion_warning VARCHAR(500) DEFAULT NULL COMMENT '转换警告信息';

-- 是否可逆转换（双向无损）
ALTER TABLE data_stream_column_type_map
ADD COLUMN is_reversible INT DEFAULT 1 COMMENT '是否可逆转换（双向无损）';

-- ====================================================================
-- 三、为现有数据设置默认值
-- ====================================================================

-- 为 data_stream_column_type_define 表的现有记录设置默认值
UPDATE data_stream_column_type_define SET
    type_category =
        CASE column_type_classify
            WHEN 1 THEN
                CASE column_type_name
                    WHEN 'float' THEN 'NUMERIC_FLOATING_POINT'
                    WHEN 'double' THEN 'NUMERIC_FLOATING_POINT'
                    WHEN 'decimal' THEN 'NUMERIC_FIXED_POINT'
                    ELSE 'NUMERIC_INTEGER'
                END
            WHEN 2 THEN
                CASE column_type_name
                    WHEN 'tinytext' THEN 'STRING_SHORT'
                    WHEN 'text' THEN 'STRING_LONG'
                    WHEN 'mediumtext' THEN 'STRING_LONG'
                    WHEN 'longtext' THEN 'STRING_LONG'
                    ELSE 'STRING_SHORT'
                END
            WHEN 3 THEN
                CASE column_type_name
                    WHEN 'date' THEN 'DATETIME_DATE'
                    WHEN 'time' THEN 'DATETIME_TIME'
                    ELSE 'DATETIME_TIMESTAMP'
                END
            WHEN 4 THEN 'BINARY'
            ELSE 'OTHER'
        END,
    require_length_param =
        CASE column_type_name
            WHEN 'varchar' THEN 1
            WHEN 'char' THEN 1
            WHEN 'nvarchar' THEN 1
            WHEN 'nchar' THEN 1
            ELSE 0
        END,
    is_national_flag =
        CASE column_type_name
            WHEN 'nvarchar' THEN 1
            WHEN 'nchar' THEN 1
            WHEN 'nvchar2' THEN 1
            ELSE 0
        END
WHERE type_category IS NULL;

-- 为 MySQL 类型设置精度和范围值
UPDATE data_stream_column_type_define SET
    max_precision = 3,
    min_value = -128,
    max_value = 255
WHERE database_type = 'mysql' AND column_type_name = 'tinyint';

UPDATE data_stream_column_type_define SET
    max_precision = 5,
    min_value = -32768,
    max_value = 65535
WHERE database_type = 'mysql' AND column_type_name = 'smallint';

UPDATE data_stream_column_type_define SET
    max_precision = 10,
    min_value = -2147483648,
    max_value = 2147483647
WHERE database_type = 'mysql' AND column_type_name = 'int';

UPDATE data_stream_column_type_define SET
    max_precision = 20,
    min_value = -9223372036854775808,
    max_value = 9223372036854775807
WHERE database_type = 'mysql' AND column_type_name = 'bigint';

UPDATE data_stream_column_type_define SET
    max_precision = 65,
    max_scale = 30
WHERE database_type = 'mysql' AND column_type_name = 'decimal';

UPDATE data_stream_column_type_define SET
    character_max_length = 65535
WHERE database_type = 'mysql' AND column_type_name = 'varchar';

UPDATE data_stream_column_type_define SET
    character_max_length = 255
WHERE database_type = 'mysql' AND column_type_name = 'char';

UPDATE data_stream_column_type_define SET
    character_max_length = 255
WHERE database_type = 'mysql' AND column_type_name = 'tinytext';

UPDATE data_stream_column_type_define SET
    character_max_length = 65535
WHERE database_type = 'mysql' AND column_type_name = 'text';

UPDATE data_stream_column_type_define SET
    character_max_length = 16777215
WHERE database_type = 'mysql' AND column_type_name = 'mediumtext';

UPDATE data_stream_column_type_define SET
    character_max_length = 4294967295
WHERE database_type = 'mysql' AND column_type_name = 'longtext';

-- 为 PostgreSQL 类型设置精度和范围值
UPDATE data_stream_column_type_define SET
    max_precision = 5,
    min_value = -32768,
    max_value = 32767
WHERE database_type = 'postgresql' AND column_type_name = 'int2';

UPDATE data_stream_column_type_define SET
    max_precision = 10,
    min_value = -2147483648,
    max_value = 2147483647
WHERE database_type = 'postgresql' AND column_type_name = 'int4';

UPDATE data_stream_column_type_define SET
    max_precision = 19,
    min_value = -9223372036854775808,
    max_value = 9223372036854775807
WHERE database_type = 'postgresql' AND column_type_name = 'int8';

UPDATE data_stream_column_type_define SET
    max_precision = 1000,
    max_scale = 30
WHERE database_type = 'postgresql' AND (column_type_name = 'numeric' OR column_type_name = 'decimal');

-- 为 Oracle 类型设置精度和范围值
UPDATE data_stream_column_type_define SET
    max_precision = 38,
    max_scale = 127
WHERE database_type = 'oracle' AND column_type_name = 'NUMBER';

UPDATE data_stream_column_type_define SET
    max_precision = 38,
    max_scale = 127
WHERE database_type = 'oracle' AND column_type_name = 'FLOAT';

UPDATE data_stream_column_type_define SET
    character_max_length = 4000
WHERE database_type = 'oracle' AND column_type_name = 'VARCHAR2';

UPDATE data_stream_column_type_define SET
    character_max_length = 4000
WHERE database_type = 'oracle' AND column_type_name = 'CHAR';

-- 为 data_stream_column_type_map 表的现有记录设置默认值
UPDATE data_stream_column_type_map SET
    match_level = 1,
    is_reversible = 1
WHERE match_level IS NULL;

-- 为需要转换警告的映射设置警告信息
UPDATE data_stream_column_type_map SET
    conversion_warning = '精度可能损失',
    match_level = 2
WHERE column_type_define_id_a = 6 AND column_type_define_id_b = 29;  -- mysql float -> postgres float4

UPDATE data_stream_column_type_map SET
    match_level = 2
WHERE column_type_define_id_a IN (16, 17, 18, 19);  -- mysql text类型 -> postgres text (长度不同)

-- ====================================================================
-- 四、验证查询
-- ====================================================================

-- 验证 data_stream_column_type_define 表结构
SELECT
    column_type_define_id,
    database_type,
    column_type_name,
    type_category,
    max_precision,
    max_scale,
    character_max_length,
    min_value,
    max_value,
    is_national_flag,
    require_length_param
FROM data_stream_column_type_define
ORDER BY database_type, column_type_define_id;

-- 验证 data_stream_column_type_map 表结构
SELECT
    a.column_type_map_id,
    b.database_type AS db_a,
    b.column_type_name AS type_a,
    c.database_type AS db_b,
    c.column_type_name AS type_b,
    a.match_level,
    a.conversion_warning,
    a.is_reversible
FROM data_stream_column_type_map a
JOIN data_stream_column_type_define b ON a.column_type_define_id_a = b.column_type_define_id
JOIN data_stream_column_type_define c ON a.column_type_define_id_b = c.column_type_define_id
ORDER BY a.match_level, a.column_type_map_id;
