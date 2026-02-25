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
-- MySQL 测试表结构 - 用于跨数据库表结构迁移测试
-- 日期: 2026-01-08
-- ====================================================================

-- 清理已存在的表
DROP TABLE IF EXISTS test_child;
DROP TABLE IF EXISTS test_parent;
DROP TABLE IF EXISTS test_order_item;
DROP TABLE IF EXISTS test_user_role;
DROP TABLE IF EXISTS test_role;
DROP TABLE IF EXISTS test_employee;
DROP TABLE IF EXISTS mysql_test_table;

-- ====================================================================
-- 1. MySQL 整数类型测试表
-- ====================================================================
CREATE TABLE mysql_test_table (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 各种整数类型
    tiny_col TINYINT,           -- -128 to 127
    tiny_unsigned TINYINT UNSIGNED,  -- 0 to 255
    small_col SMALLINT,         -- -32768 to 32767
    medium_col MEDIUMINT,       -- -8388608 to 8388607
    normal_col INT,             -- -2147483648 to 2147483647
    big_col BIGINT,             -- -2^63 to 2^63-1

    -- 浮点数类型
    float_col FLOAT,            -- 单精度浮点
    double_col DOUBLE,          -- 双精度浮点
    decimal_col DECIMAL(20,5),  -- 精确小数

    -- 字符串类型
    varchar_col VARCHAR(100),
    char_col CHAR(10),
    tinytext_col TINYTEXT,
    text_col TEXT,
    mediumtext_col MEDIUMTEXT,
    longtext_col LONGTEXT,

    -- 日期时间类型（带默认值）
    date_col DATE,
    datetime_col DATETIME DEFAULT CURRENT_TIMESTAMP,
    timestamp_col TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    year_col YEAR,

    -- 二进制类型
    binary_col BINARY(10),
    varbinary_col VARBINARY(100),
    blob_col BLOB,
    tinyblob_col TINYBLOB,
    mediumblob_col MEDIUMBLOB,
    longblob_col LONGBLOB,

    -- 其他类型
    bool_col BOOL,
    json_col JSON,
    enum_col ENUM('A', 'B', 'C'),
    set_col SET('X', 'Y', 'Z')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO mysql_test_table (tiny_col, tiny_unsigned, small_col, medium_col,
                               normal_col, big_col, float_col, double_col,
                               decimal_col, varchar_col, char_col, text_col,
                               date_col, datetime_col, bool_col, json_col,
                               enum_col, set_col)
VALUES (
    127, 255, 32767, 8388607,
    2147483647, 9223372036854775807, 3.14, 3.14159265359,
    12345.67890, 'test varchar', 'fixed', 'long text content',
    CURDATE(), NOW(), TRUE, '{"key": "value"}',
    'B', 'X,Y'
);

INSERT INTO mysql_test_table (tiny_col, small_col, normal_col, big_col,
                               float_col, decimal_col, varchar_col, bool_col)
VALUES (
    -128, -32768, -2147483648, -9223372036854775808,
    -3.14, -99999.99999, 'negative values', FALSE
);

-- ====================================================================
-- 2. 外键约束测试表
-- ====================================================================

-- 主表
CREATE TABLE test_parent (
    parent_id INT PRIMARY KEY AUTO_INCREMENT,
    parent_name VARCHAR(50) NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 子表 - 单列外键 + CASCADE
CREATE TABLE test_child (
    child_id INT PRIMARY KEY AUTO_INCREMENT,
    parent_id INT,
    child_name VARCHAR(50),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_child_parent
        FOREIGN KEY (parent_id)
        REFERENCES test_parent(parent_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 订单明细 - 复合外键 + SET NULL
CREATE TABLE test_order_item (
    order_id INT,
    item_id INT,
    product_id INT,
    quantity INT,
    price DECIMAL(10,2),
    PRIMARY KEY (order_id, item_id),
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
        REFERENCES test_parent(parent_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- 角色表
CREATE TABLE test_role (
    role_id INT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB;

-- 用户角色 - 多外键
CREATE TABLE test_user_role (
    user_id INT,
    role_id INT,
    assigned_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
        REFERENCES test_parent(parent_id),
    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
        REFERENCES test_role(role_id)
) ENGINE=InnoDB;

-- 员工表 - 自引用外键
CREATE TABLE test_employee (
    emp_id INT PRIMARY KEY AUTO_INCREMENT,
    emp_name VARCHAR(50) NOT NULL,
    manager_id INT,
    hire_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_manager
        FOREIGN KEY (manager_id)
        REFERENCES test_employee(emp_id)
) ENGINE=InnoDB;

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
SHOW TABLES;

-- 查看 mysql_test_table 结构
DESC mysql_test_table;

-- 查看外键约束
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
    AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 查看测试数据
SELECT * FROM mysql_test_table;
SELECT * FROM test_parent;
SELECT * FROM test_child;
SELECT * FROM test_order_item;
SELECT * FROM test_user_role;
SELECT * FROM test_employee;
