-- =====================================================================
-- DataStream RBAC 内置数据初始化脚本
-- 适用于全新环境（datastream-h2-ddl.sql / datastream-mysql-ddl.sql 建表后执行）
-- 说明：
--   1. 内置系统管理员角色 role_code = SYSTEM_ADMIN（built_in = 1，不可删除/修改）
--   2. 内置系统管理员账号 admin（BCrypt 密文），绑定系统管理员角色
--   3. 内置菜单权限（permission_type = 1）与数据权限（permission_type = 2）目录
--   4. 系统管理员角色被授予全部权限
-- 注意：密文为 BCrypt 的 'admin'；运行时初始化组件会使用 PasswordEncoder 重新
--       生成，以避免不同 BCrypt 实现版本前缀（$2a/$2b/$2y）差异。
-- =====================================================================

-- 内置系统管理员角色
INSERT INTO data_stream_role (role_id, role_code, role_name, description, built_in, create_date)
VALUES (1, 'SYSTEM_ADMIN', '系统管理员', '内置系统管理员角色，拥有全部权限', 1, NOW());

-- 内置系统管理员账号（密码为 BCrypt 密文）
INSERT INTO data_stream_system_user (system_user_id, system_user_code, system_user_name, password, org_id, org_name, username, state, create_date)
VALUES (1, 'admin', '系统管理员', '$2a$10$B6d3gpHFtykC9mOAdEqAxOcQRIB/t4B7.UvkXKBkaegjF7JWBXl0i', NULL, NULL, 'admin', 1, NOW());

-- 绑定系统管理员用户与角色
INSERT INTO data_stream_user_role (user_role_id, system_user_id, role_id)
VALUES (1, 1, 1);

-- 内置菜单权限（permission_type = 1）
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (101, 'menu:overview',            '系统概览',   1, NULL, 1, 'overview',            1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (102, 'menu:task',                '任务管理',   1, NULL, 2, NULL,                  1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (103, 'menu:task-move',           '迁移任务',   1, 102,  1, 'taskManage',          1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (104, 'menu:task-link',           '链接任务',   1, 102,  2, 'tableLinkTask',       1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (105, 'menu:task-batch',          '批量任务',   1, 102,  3, 'batchMoveTask',       1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (106, 'menu:task-table',          '结构迁移',   1, 102,  4, 'tableManage',         1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (107, 'menu:data-search',         '数据检索',   1, NULL, 3, 'DataSearch',          1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (108, 'menu:config',              '配置管理',   1, NULL, 4, NULL,                  1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (109, 'menu:config-database',     '数据库配置', 1, 108,  1, 'dataBaseConfig',      1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (110, 'menu:config-tablelink',    '表链接配置', 1, 108,  2, 'tableLinkConfig',     1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (111, 'menu:config-file',         '文件配置',   1, 108,  3, 'fileFormatConfig',    1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (112, 'menu:config-mq',           'MQ配置',     1, 108,  4, 'mqConfig',            1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (113, 'menu:config-column-type',  '字段类型配置', 1, 108, 5, 'columnTypeConfig',    1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (114, 'menu:system',              '系统管理',   1, NULL, 5, NULL,                  1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (115, 'menu:system-login-log',    '登录日志',   1, 114,  1, 'loginLogs',           1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (116, 'menu:system-operation-log','操作日志',   1, 114,  2, 'operationLogs',       1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (117, 'menu:system-h2',           'H2管理',     1, 114,  3, 'h2Manage',            1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (118, 'menu:system-about',        '关于系统',   1, 114,  4, 'aboutTheSystem',      1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (119, 'menu:resource-monitor',    '资源监控',   1, NULL, 6, 'resourceMonitor',     1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (120, 'menu:user-management',     '用户管理',   1, 114,  5, 'userManage',          1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (121, 'menu:role-management',     '角色管理',   1, 114,  6, 'roleManage',          1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (122, 'menu:permission-management','权限管理',   1, 114,  7, 'permissionManage',    1);

-- 内置数据权限（permission_type = 2）
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (201, 'task:create',              '创建迁移任务', 2, NULL, 1, NULL, 1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (202, 'task:execute',             '执行迁移任务', 2, NULL, 2, NULL, 1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (203, 'task:stop',                '停止迁移任务', 2, NULL, 3, NULL, 1);
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in) VALUES (204, 'task:delete',              '删除迁移任务', 2, NULL, 4, NULL, 1);

-- 系统管理员角色授予全部菜单与数据权限
INSERT INTO data_stream_role_permission (role_permission_id, role_id, permission_id)
SELECT 1000 + permission_id, 1, permission_id FROM data_stream_permission;