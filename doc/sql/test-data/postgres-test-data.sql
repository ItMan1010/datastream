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
-- PostgreSQL 测试表结构 - 用于跨数据库表结构迁移测试
-- 日期: 2026-01-08
-- ====================================================================

-- 清理已存在的表
DROP TABLE IF EXISTS test_child CASCADE;
DROP TABLE IF EXISTS test_parent CASCADE;
DROP TABLE IF EXISTS test_order_item CASCADE;
DROP TABLE IF EXISTS test_user_role CASCADE;
DROP TABLE IF EXISTS test_role CASCADE;
DROP TABLE IF EXISTS test_employee CASCADE;
DROP TABLE IF EXISTS postgres_test_table CASCADE;

-- ====================================================================
-- 1. PostgreSQL 整数类型测试表
-- ====================================================================
CREATE TABLE postgres_test_table (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 各种整数类型（PostgreSQL 没有 tinyint）
    small_col SMALLINT,         -- -32768 to 32767
    normal_col INTEGER,          -- -2147483648 to 2147483647
    big_col BIGINT,             -- -2^63 to 2^63-1

    -- 浮点数类型
    float_col REAL,              -- 单精度浮点 (float4)
    double_col DOUBLE PRECISION, -- 双精度浮点 (float8)
    decimal_col NUMERIC(20,5),   -- 精确小数

    -- 字符串类型
    varchar_col VARCHAR(100),
    char_col CHAR(10),
    text_col TEXT,

    -- 日期时间类型（带默认值）
    date_col DATE DEFAULT CURRENT_DATE,
    time_col TIME DEFAULT CURRENT_TIME,
    timestamp_col TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    timestamptz_col TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- 二进制类型
    bytea_col BYTEA,

    -- 其他类型
    bool_col BOOLEAN,
    json_col JSON,
    jsonb_col JSONB,
    uuid_col UUID DEFAULT gen_random_uuid(),
    array_col INT[],
    enum_col mood

) ;

-- 创建自定义枚举类型
CREATE TYPE mood AS ENUM ('sad', 'okay', 'happy');

-- 重新添加枚举列
ALTER TABLE postgres_test_table ADD COLUMN enum_col mood;

-- 插入测试数据
INSERT INTO postgres_test_table (small_col, normal_col, big_col,
                                  float_col, double_col, decimal_col,
                                  varchar_col, char_col, text_col,
                                  date_col, timestamp_col, bool_col,
                                  json_col, jsonb_col, array_col, enum_col)
VALUES (
    32767, 2147483647, 9223372036854775807,
    3.14, 3.14159265359, 12345.67890,
    'test varchar', 'fixed', 'long text content',
    CURRENT_DATE, CURRENT_TIMESTAMP, TRUE,
    '{"key": "value"}', '{"key": "value"}'::jsonb,
    ARRAY[1, 2, 3], 'happy'
);

INSERT INTO postgres_test_table (small_col, normal_col, big_col,
                                  float_col, decimal_col, varchar_col, bool_col)
VALUES (
    -32768, -2147483648, -9223372036854775808,
    -3.14, -99999.99999, 'negative values', FALSE
);

-- ====================================================================
-- 2. 外键约束测试表
-- ====================================================================

-- 主表
CREATE TABLE test_parent (
    parent_id SERIAL PRIMARY KEY,
    parent_name VARCHAR(50) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 子表 - 单列外键 + CASCADE
CREATE TABLE test_child (
    child_id SERIAL PRIMARY KEY,
    parent_id INTEGER,
    child_name VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_child_parent
        FOREIGN KEY (parent_id)
        REFERENCES test_parent(parent_id)
        ON DELETE CASCADE
);

-- 订单明细 - 复合外键 + SET NULL
CREATE TABLE test_order_item (
    order_id INTEGER,
    item_id INTEGER,
    product_id INTEGER,
    quantity INTEGER,
    price NUMERIC(10,2),
    PRIMARY KEY (order_id, item_id),
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
        REFERENCES test_parent(parent_id)
        ON DELETE SET NULL
);

-- 角色表
CREATE TABLE test_role (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL
);

-- 用户角色 - 多外键
CREATE TABLE test_user_role (
    user_id INTEGER,
    role_id INTEGER,
    assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
        REFERENCES test_parent(parent_id),
    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
        REFERENCES test_role(role_id)
);

-- 员工表 - 自引用外键
CREATE TABLE test_employee (
    emp_id SERIAL PRIMARY KEY,
    emp_name VARCHAR(50) NOT NULL,
    manager_id INTEGER,
    hire_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_manager
        FOREIGN KEY (manager_id)
        REFERENCES test_employee(emp_id)
);

-- 插入测试数据
INSERT INTO test_parent (parent_id, parent_name) VALUES (1, 'Parent 1'), (2, 'Parent 2'), (3, 'Product A'), (4, 'Product B');
INSERT INTO test_child (child_id, parent_id, child_name) VALUES (1, 1, 'Child 1.1'), (2, 1, 'Child 1.2'), (3, 2, 'Child 2.1');
INSERT INTO test_order_item (order_id, item_id, product_id, quantity, price) VALUES (1001, 1, 3, 10, 99.99), (1001, 2, 4, 5, 49.99);
INSERT INTO test_role (role_id, role_name) VALUES (1, 'Admin'), (2, 'User'), (3, 'Guest');
INSERT INTO test_user_role (user_id, role_id) VALUES (1, 1), (1, 2), (2, 2);
INSERT INTO test_employee (emp_id, emp_name, manager_id) VALUES (1, 'CEO', NULL), (2, 'CTO', 1), (3, 'CFO', 1), (4, 'Engineer', 2);

-- ====================================================================
-- 3. 验证查询
-- ====================================================================

-- 查看所有表
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;

-- 查看 postgres_test_table 结构
\d postgres_test_table

-- 查看外键约束
SELECT
    tc.table_name,
    kcu.constraint_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'public';

-- 查看测试数据
SELECT * FROM postgres_test_table;
SELECT * FROM test_parent;
SELECT * FROM test_child;
SELECT * FROM test_order_item;
SELECT * FROM test_user_role;
SELECT * FROM test_employee;
