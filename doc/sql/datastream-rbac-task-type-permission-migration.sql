-- =====================================================================
-- DataStream RBAC 任务类型权限迁移脚本（存量环境，幂等）
-- 适用于已部署旧版本、需升级「任务类型级数据权限」的存量环境。
-- 作用：
--   1. 新增 6 个任务类型数据权限（permission_type = 2，编码 task:type:*）
--   2. 将持有「创建迁移任务 task:create」权限的角色补授全部 6 个任务类型权限，
--      保证升级后原有用户创建任务行为不变。
-- 说明：脚本可重复执行，已存在的权限与授权会被跳过，不产生重复数据。
-- =====================================================================

-- 1. 新增 6 个任务类型数据权限（如已存在则跳过）
INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in)
SELECT 205, 'task:type:migrate',       '数据迁移', 2, NULL, 5,  NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM data_stream_permission WHERE permission_code = 'task:type:migrate');

INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in)
SELECT 206, 'task:type:clean',         '数据清理', 2, NULL, 6,  NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM data_stream_permission WHERE permission_code = 'task:type:clean');

INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in)
SELECT 207, 'task:type:migrate-clean', '迁移清理', 2, NULL, 7,  NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM data_stream_permission WHERE permission_code = 'task:type:migrate-clean');

INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in)
SELECT 208, 'task:type:structure',     '结构迁移', 2, NULL, 8,  NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM data_stream_permission WHERE permission_code = 'task:type:structure');

INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in)
SELECT 209, 'task:type:data-check',    '数据稽核', 2, NULL, 9,  NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM data_stream_permission WHERE permission_code = 'task:type:data-check');

INSERT INTO data_stream_permission (permission_id, permission_code, permission_name, permission_type, parent_id, sort_no, route, built_in)
SELECT 210, 'task:type:cdc',           '增量迁移', 2, NULL, 10, NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM data_stream_permission WHERE permission_code = 'task:type:cdc');

-- 2. 将持有 task:create 的角色补授全部 6 个任务类型权限（如已授权则跳过）
--    role_permission_id 采用高位定值公式，避免与内置数据（1000+）及序列（50000+）冲突。
INSERT INTO data_stream_role_permission (role_permission_id, role_id, permission_id)
SELECT 900000000 + rp.role_id * 1000 + p.permission_id, rp.role_id, p.permission_id
FROM data_stream_role_permission rp
JOIN data_stream_permission pc ON pc.permission_id = rp.permission_id AND pc.permission_code = 'task:create'
JOIN data_stream_permission p ON p.permission_code IN
    ('task:type:migrate', 'task:type:clean', 'task:type:migrate-clean',
     'task:type:structure', 'task:type:data-check', 'task:type:cdc')
WHERE NOT EXISTS (
    SELECT 1 FROM data_stream_role_permission x
    WHERE x.role_id = rp.role_id AND x.permission_id = p.permission_id
);
