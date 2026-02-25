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
-- Oracle 测试表结构 - 用于跨数据库表结构迁移测试
-- 日期: 2026-01-08
-- ====================================================================

-- 清理已存在的表
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE test_child CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE test_parent CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE oracle_test_table CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/

-- ====================================================================
-- 1. Oracle NUMBER 精度测试表
-- ====================================================================
CREATE TABLE oracle_test_table (
    -- 主键
    id NUMBER(10) PRIMARY KEY,

    -- 各种精度的 NUMBER 类型（用于测试智能映射）
    tiny_num NUMBER(2),          -- 映射到 MySQL tinyint, PostgreSQL smallint
    small_num NUMBER(4),         -- 映射到 smallint
    normal_num NUMBER(9),        -- 映射到 int / integer
    big_num NUMBER(18),          -- 映射到 bigint
    huge_num NUMBER(25),         -- 映射到 decimal(25,0) / numeric(25,0)
    very_huge_num NUMBER(100),   -- 映射到 decimal(65,0) / numeric(1000,0)

    -- 小数类型
    decimal_num NUMBER(10,2),    -- 标准小数
    high_precision_num NUMBER(38,10),
    overflow_num NUMBER(70,5),   -- 精度超限测试

    -- 字符串类型
    varchar_col VARCHAR2(100),
    varchar_max VARCHAR2(4000),
    clob_col CLOB,

    -- 日期时间类型（带默认值）
    date_col DATE DEFAULT SYSDATE,
    timestamp_col TIMESTAMP DEFAULT SYSDATE,
    timestamp_tz_col TIMESTAMP WITH TIME ZONE DEFAULT SYSDATE,

    -- 二进制类型
    blob_col BLOB,
    raw_col RAW(100),

    -- 自动增长列
    auto_id NUMBER(20) GENERATED ALWAYS AS IDENTITY
);

-- 插入测试数据
INSERT INTO oracle_test_table (id, tiny_num, small_num, normal_num, big_num,
                                huge_num, very_huge_num, decimal_num,
                                high_precision_num, overflow_num,
                                varchar_col, varchar_max, clob_col,
                                date_col, timestamp_col, blob_col, raw_col)
VALUES (
    1, 99, 9999, 999999999, 999999999999999999,
    9999999999999999999999999, POWER(10, 99), 12345.67,
    12345.6789, 12345.67,
    'test varchar', RPAD('x', 4000, 'x'), 'clob data',
    SYSDATE, SYSDATE, UTL_RAW.CAST_TO_RAW('blob data'), 'A1B2C3'
);

INSERT INTO oracle_test_table (id, tiny_num, small_num, normal_num, big_num,
                                huge_num, decimal_num, varchar_col)
VALUES (
    2, -128, -32768, -2147483648, -9223372036854775808,
    12345678901234567890, -99999.99, 'negative values'
);

-- ====================================================================
-- 2. 外键约束测试表
-- ====================================================================

-- 主表
CREATE TABLE test_parent (
    parent_id NUMBER(10) PRIMARY KEY,
    parent_name VARCHAR2(50) NOT NULL,
    created_date DATE DEFAULT SYSDATE
);

-- 子表 - 单列外键 + CASCADE
CREATE TABLE test_child (
    child_id NUMBER(10) PRIMARY KEY,
    parent_id NUMBER(10),
    child_name VARCHAR2(50),
    created_date DATE DEFAULT SYSDATE,
    CONSTRAINT fk_child_parent
        FOREIGN KEY (parent_id)
        REFERENCES test_parent(parent_id)
        ON DELETE CASCADE
);

-- 订单明细 - 复合外键 + SET NULL
CREATE TABLE test_order_item (
    order_id NUMBER(10),
    item_id NUMBER(5),
    product_id NUMBER(10),
    quantity NUMBER(5),
    price NUMBER(10,2),
    CONSTRAINT pk_order_item PRIMARY KEY (order_id, item_id),
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
        REFERENCES test_parent(parent_id)
        ON DELETE SET NULL
);

-- 角色表
CREATE TABLE test_role (
    role_id NUMBER(5) PRIMARY KEY,
    role_name VARCHAR2(50) NOT NULL
);

-- 用户角色 - 多外键 + RESTRICT
CREATE TABLE test_user_role (
    user_id NUMBER(10),
    role_id NUMBER(5),
    assigned_date DATE DEFAULT SYSDATE,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
        REFERENCES test_parent(parent_id),
    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
        REFERENCES test_role(role_id)
);

-- 员工表 - 自引用外键
CREATE TABLE test_employee (
    emp_id NUMBER(10) PRIMARY KEY,
    emp_name VARCHAR2(50) NOT NULL,
    manager_id NUMBER(10),
    hire_date DATE DEFAULT SYSDATE,
    CONSTRAINT fk_employee_manager
        FOREIGN KEY (manager_id)
        REFERENCES test_employee(emp_id)
);

-- 插入测试数据
INSERT INTO test_parent (parent_id, parent_name) VALUES (1, 'Parent 1');
INSERT INTO test_parent (parent_id, parent_name) VALUES (2, 'Parent 2');
INSERT INTO test_parent (parent_id, parent_name) VALUES (3, 'Product A');
INSERT INTO test_parent (parent_id, parent_name) VALUES (4, 'Product B');

INSERT INTO test_child (child_id, parent_id, child_name) VALUES (1, 1, 'Child 1.1');
INSERT INTO test_child (child_id, parent_id, child_name) VALUES (2, 1, 'Child 1.2');
INSERT INTO test_child (child_id, parent_id, child_name) VALUES (3, 2, 'Child 2.1');

INSERT INTO test_order_item (order_id, item_id, product_id, quantity, price) VALUES (1001, 1, 3, 10, 99.99);
INSERT INTO test_order_item (order_id, item_id, product_id, quantity, price) VALUES (1001, 2, 4, 5, 49.99);
INSERT INTO test_order_item (order_id, item_id, product_id, quantity, price) VALUES (1002, 1, 3, 20, 99.99);

INSERT INTO test_role (role_id, role_name) VALUES (1, 'Admin');
INSERT INTO test_role (role_id, role_name) VALUES (2, 'User');
INSERT INTO test_role (role_id, role_name) VALUES (3, 'Guest');

INSERT INTO test_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO test_user_role (user_id, role_id) VALUES (1, 2);
INSERT INTO test_user_role (user_id, role_id) VALUES (2, 2);

INSERT INTO test_employee (emp_id, emp_name, manager_id) VALUES (1, 'CEO', NULL);
INSERT INTO test_employee (emp_id, emp_name, manager_id) VALUES (2, 'CTO', 1);
INSERT INTO test_employee (emp_id, emp_name, manager_id) VALUES (3, 'CFO', 1);
INSERT INTO test_employee (emp_id, emp_name, manager_id) VALUES (4, 'Engineer', 2);

-- ====================================================================
-- 3. 验证查询
-- ====================================================================

-- 查看所有表
SELECT table_name FROM user_tables ORDER BY table_name;

-- 查看 oracle_test_table 结构
DESC oracle_test_table;

-- 查看外键约束
SELECT
    a.table_name,
    a.constraint_name,
    a.constraint_type,
    b.table_name AS referenced_table,
    b.constraint_name AS pk_constraint
FROM user_constraints a
LEFT JOIN user_constraints b
    ON a.r_constraint_name = b.constraint_name
WHERE a.constraint_type = 'R'
ORDER BY a.table_name, a.constraint_name;

-- 查看测试数据
SELECT * FROM oracle_test_table;
SELECT * FROM test_parent;
SELECT * FROM test_child;
SELECT * FROM test_order_item;
SELECT * FROM test_user_role;
SELECT * FROM test_employee;

COMMIT;
