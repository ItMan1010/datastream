-- DataStream 存量元数据库升级脚本：数据源 URL 列扩容
-- 背景：data_stream_data_base.url 原为 varchar(128)，带常规连接参数的 JDBC URL
--       （useSSL / characterEncoding / serverTimezone / allowPublicKeyRetrieval 等）
--       约 130~160 字符，插入时报 "Data too long for column 'url'"。
-- 变更：扩容至 varchar(512)，兼容已有数据，无需回滚。

-- MySQL 元数据库执行：
ALTER TABLE `data_stream_data_base`
    MODIFY COLUMN `url` VARCHAR(512) NOT NULL COMMENT '数据库链接';

-- H2 元数据库说明：H2 为文件库，随 datastream-h2-ddl.sql 全量建库，
-- 重新执行建库 DDL 即包含新列宽；如需对存量 H2 库原地升级，可执行：
-- ALTER TABLE data_stream_data_base ALTER COLUMN url VARCHAR(512) NOT NULL;
