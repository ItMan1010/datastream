-- =====================================================================
-- DataStream 配置管理工号数据范围迁移脚本（存量环境）
-- 适用于已部署旧版本、需升级「配置数据记录工号 + 按工号行级过滤」的存量环境。
-- 作用：为 6 张配置表增加 system_user_code（创建人工号）列。
--   - data_stream_data_base           数据源配置
--   - data_stream_table_link          表链接配置
--   - data_stream_file_format         文件格式配置
--   - data_stream_mq_config           MQ 配置
--   - data_stream_column_type_define  字段类型定义（记录工号，共享参考数据不参与过滤）
--   - data_stream_column_type_map     字段类型映射（记录工号，共享参考数据不参与过滤）
-- 说明：
--   1. 下方语句按默认元数据库 H2（MODE MYSQL）与 MySQL 语法编写；
--   2. 脚本可重复执行，若列已存在会报错，请在执行前确认列是否已存在，
--      或按需改为数据库对应的幂等写法（如 MySQL 8 的 ADD COLUMN IF NOT EXISTS）。
-- =====================================================================

-- 1. 数据源配置
ALTER TABLE data_stream_data_base ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码（创建人工号）';

-- 2. 表链接配置
ALTER TABLE data_stream_table_link ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码（创建人工号）';

-- 3. 文件格式配置
ALTER TABLE data_stream_file_format ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码（创建人工号）';

-- 4. MQ 配置
ALTER TABLE data_stream_mq_config ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码（创建人工号）';

-- 5. 字段类型定义（共享参考数据：记录工号，不参与行级过滤）
ALTER TABLE data_stream_column_type_define ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码（创建人工号）';

-- 6. 字段类型映射（共享参考数据：记录工号，不参与行级过滤）
ALTER TABLE data_stream_column_type_map ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码（创建人工号）';

-- =====================================================================
-- 其他元数据库方言参考（按实际元数据库选用其一）：
-- PostgreSQL / 达梦:
--   ALTER TABLE data_stream_data_base ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL;
--   ALTER TABLE data_stream_table_link ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL;
--   ALTER TABLE data_stream_file_format ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL;
--   ALTER TABLE data_stream_mq_config ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL;
--   ALTER TABLE data_stream_column_type_define ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL;
--   ALTER TABLE data_stream_column_type_map ADD COLUMN system_user_code VARCHAR(256) DEFAULT NULL;
-- Oracle:
--   ALTER TABLE data_stream_data_base ADD (system_user_code VARCHAR2(256));
--   ALTER TABLE data_stream_table_link ADD (system_user_code VARCHAR2(256));
--   ALTER TABLE data_stream_file_format ADD (system_user_code VARCHAR2(256));
--   ALTER TABLE data_stream_mq_config ADD (system_user_code VARCHAR2(256));
--   ALTER TABLE data_stream_column_type_define ADD (system_user_code VARCHAR2(256));
--   ALTER TABLE data_stream_column_type_map ADD (system_user_code VARCHAR2(256));
-- =====================================================================
