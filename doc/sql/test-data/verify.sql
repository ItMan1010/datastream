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
-- 跨数据库迁移验证脚本
-- 日期: 2026-01-08
-- 说明: 在目标数据库执行此脚本，验证迁移后的数据一致性
-- ====================================================================

-- ====================================================================
-- 1. 表结构验证
-- ====================================================================

-- 查看所有已迁移的表
-- MySQL: SHOW TABLES;
-- PostgreSQL: SELECT tablename FROM pg_tables WHERE schemaname = 'public';
-- Oracle: SELECT table_name FROM user_tables;

-- 查看表结构
-- MySQL: DESC oracle_test_table;
-- PostgreSQL: \d oracle_test_table
-- Oracle: DESC oracle_test_table;

-- ====================================================================
-- 2. 数据行数验证
-- ====================================================================

-- 验证各表数据行数
SELECT
    'oracle_test_table' AS table_name,
    COUNT(*) AS row_count
FROM oracle_test_table

UNION ALL

SELECT
    'test_parent' AS table_name,
    COUNT(*) AS row_count
FROM test_parent

UNION ALL

SELECT
    'test_child' AS table_name,
    COUNT(*) AS row_count
FROM test_child

UNION ALL

SELECT
    'test_order_item' AS table_name,
    COUNT(*) AS row_count
FROM test_order_item

UNION ALL

SELECT
    'test_user_role' AS table_name,
    COUNT(*) AS row_count
FROM test_user_role

UNION ALL

SELECT
    'test_employee' AS table_name,
    COUNT(*) AS row_count
FROM test_employee

ORDER BY table_name;

-- ====================================================================
-- 3. Oracle NUMBER 类型映射验证
-- ====================================================================

-- 检查整数类型映射是否正确
SELECT
    id,
    tiny_num,
    small_num,
    normal_num,
    big_num,
    huge_num
FROM oracle_test_table
ORDER BY id;

-- 检查小数类型映射是否正确
SELECT
    id,
    decimal_num,
    high_precision_num,
    overflow_num
FROM oracle_test_table
ORDER BY id;

-- 验证精度是否在范围内
-- MySQL: DECIMAL 最大精度 65
-- PostgreSQL: NUMERIC 最大精度 1000
SELECT
    id,
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    NUMERIC_PRECISION,
    NUMERIC_SCALE
-- MySQL: FROM information_schema.COLUMNS
-- PostgreSQL: FROM information_schema.columns
WHERE TABLE_NAME = 'oracle_test_table'
    AND COLUMN_NAME IN ('decimal_num', 'high_precision_num', 'overflow_num');

-- ====================================================================
-- 4. 外键约束验证
-- ====================================================================

-- MySQL 外键查询
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
    AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- PostgreSQL 外键查询
SELECT
    tc.table_name,
    kcu.constraint_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'public'
ORDER BY tc.table_name, kcu.constraint_name;

-- Oracle 外键查询
SELECT
    a.table_name,
    a.constraint_name,
    b.column_name,
    c.table_name AS referenced_table,
    d.column_name AS referenced_column,
    a.delete_rule,
    a.update_rule
FROM user_constraints a
JOIN user_cons_columns b
    ON a.constraint_name = b.constraint_name
JOIN user_constraints c
    ON a.r_constraint_name = c.constraint_name
JOIN user_cons_columns d
    ON c.constraint_name = d.constraint_name
WHERE a.constraint_type = 'R'
ORDER BY a.table_name, a.constraint_name;

-- ====================================================================
-- 5. 默认值验证
-- ====================================================================

-- MySQL 默认值查询
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN ('oracle_test_table', 'test_parent', 'test_child')
    AND COLUMN_DEFAULT IS NOT NULL;

-- PostgreSQL 默认值查询
SELECT
    table_name,
    column_name,
    data_type,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
    AND table_name IN ('oracle_test_table', 'test_parent', 'test_child')
    AND column_default IS NOT NULL;

-- ====================================================================
-- 6. 主键约束验证
-- ====================================================================

-- MySQL 主键查询
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
    AND CONSTRAINT_NAME = 'PRIMARY'
ORDER BY TABLE_NAME;

-- PostgreSQL 主键查询
SELECT
    tc.table_name,
    tc.constraint_name,
    kcu.column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.constraint_type = 'PRIMARY KEY'
    AND tc.table_schema = 'public'
ORDER BY tc.table_name;

-- ====================================================================
-- 7. 数据完整性验证（外键引用完整性）
-- ====================================================================

-- 验证 test_child 的 parent_id 是否都存在于 test_parent
SELECT
    'test_child 孤儿记录' AS check_name,
    COUNT(*) AS orphan_count
FROM test_child c
WHERE NOT EXISTS (
    SELECT 1 FROM test_parent p
    WHERE p.parent_id = c.parent_id
);

-- 验证 test_order_item 的 product_id 引用
SELECT
    'test_order_item 孤儿记录' AS check_name,
    COUNT(*) AS orphan_count
FROM test_order_item oi
WHERE oi.product_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM test_parent p
        WHERE p.parent_id = oi.product_id
    );

-- 验证自引用外键（test_employee）
SELECT
    'test_employee 无效引用' AS check_name,
    COUNT(*) AS invalid_count
FROM test_employee e
WHERE e.manager_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM test_employee m
        WHERE m.emp_id = e.manager_id
    );

-- ====================================================================
-- 8. 级联删除测试（可选，谨慎执行）
-- ====================================================================

-- 测试 CASCADE 删除
-- BEGIN;
-- DELETE FROM test_parent WHERE parent_id = 1;
-- 检查 test_child 中对应的记录是否被删除
-- ROLLBACK; -- 回滚测试

-- 测试 SET NULL
-- BEGIN;
-- DELETE FROM test_parent WHERE parent_id = 3;
-- 检查 test_order_item 中 product_id 是否变为 NULL
-- ROLLBACK; -- 回滚测试

-- ====================================================================
-- 9. 性能统计查询
-- ====================================================================

-- 各表数据量统计
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN (
        'oracle_test_table',
        'test_parent',
        'test_child',
        'test_order_item',
        'test_user_role',
        'test_employee'
    )
ORDER BY TABLE_ROWS DESC;

-- ====================================================================
-- 10. 类型映射一致性报告
-- ====================================================================

-- 生成类型映射报告（MySQL）
SELECT
    'oracle_test_table' AS table_name,
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    NUMERIC_PRECISION,
    NUMERIC_SCALE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'oracle_test_table'
ORDER BY ORDINAL_POSITION;

-- 生成类型映射报告（PostgreSQL）
SELECT
    'oracle_test_table' AS table_name,
    column_name,
    data_type,
    character_maximum_length,
    numeric_precision,
    numeric_scale,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
    AND table_name = 'oracle_test_table'
ORDER BY ordinal_position;
