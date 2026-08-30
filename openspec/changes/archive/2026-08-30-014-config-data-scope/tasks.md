## 1. 权限与错误码基础设施

- [x] 1.1 在 `DataStreamErrorCode` 增加 `OPER_CONFIG_NOT_OWNER_ERROR("20132", "无权访问该配置数据")`
- [x] 1.2 确认 `PermissionService.isAdmin()` 与 `getCurrentUserCode()` 可复用于配置模块（已具备，仅校验）

## 2. 数据源配置记录工号与过滤

- [x] 2.1 `DataBaseEntity` 增加 `systemUserCode` 字段
- [x] 2.2 `DataStreamMapper.xml` 的 `dataBaseColumnList`、`dataBaseCondition`、`insertDataBase` 增加 `system_user_code`；`DataStreamMapper.java` 为 `getDataBaseCount`/`queryDataBase`/`queryDataBaseLikeOracle` 增加可空 `systemUserCode` 参数
- [x] 2.3 `DataStreamDao`、`IMetaService`、`MetaServiceImpl` 的 `queryDataBase`/`getDataBaseCount` 透传 `systemUserCode`
- [x] 2.4 `DataBaseHandler` 注入 `PermissionService`：`insertDataBase` 写入当前工号；`queryDataBase`/`getDataBaseCount` 计算并透传过滤值；`updateDataBase`/`delDataBase`/`onOffDataBase`/`checkDataBaseList`/`dataSearch` 增加归属校验
- [x] 2.5 任务创建链路的按 ID 解析（`TableInfoHandler`/`TableLinkHandler`/`DataMoveHandler`）显式传 `null`，保持既有行为

## 3. 概览统计按工号过滤

- [x] 3.1 `DataStreamMapper.xml` 的 `statMoveTaskCount`、`statLinkTaskCount`、`statMoveTaskCountGroupByDay(+Oracle)`、`statLinkTaskCountGroupByDay(+Oracle)`、`statMoveTaskCountGroupByType`、`statMoveTaskCountGroupByState` 增加工号过滤；`DataStreamMapper.java` 增加可空 `systemUserCode` 参数
- [x] 3.2 `DataStreamDao`、`IMetaService`、`MetaServiceImpl` 的统计方法透传 `systemUserCode`
- [x] 3.3 `DataBaseHandler.statSystemInfo` 计算过滤值并透传到全部统计方法与数据源总数

## 4. MQ 配置记录工号与过滤

- [x] 4.1 `MQConfigEntity` 增加 `systemUserCode` 字段
- [x] 4.2 `MQConfigMapper.xml` 的 `mqConfigColumnList`、`MQConfigWhereCon`、`insertMQConfig` 增加 `system_user_code`；修复 `mq_name`→`mq_config_name`；`MQConfigMapper.java` 增加可空 `systemUserCode` 参数
- [x] 4.3 `MQConfigDao`、`IMQConfigService`、`MQConfigServiceImpl` 透传 `systemUserCode`；`insertConfig` 写入当前工号；`getConfigById`/`updateConfig`/`deleteConfig`/`updateConfigOnLineFlagById` 增加归属校验
- [x] 4.4 `MQConfigServiceImpl` 注入 `PermissionService` 计算过滤值

## 5. 文件格式配置记录工号与过滤

- [x] 5.1 `FileFormatEntity` 增加 `systemUserCode` 字段
- [x] 5.2 `FileMapper.xml` 的 `fileFormatColumnList`、`fileFormatWhereCon`、`insertFileFormat` 增加 `system_user_code`；`FileMapper.java` 增加可空 `systemUserCode` 参数
- [x] 5.3 `FileDao`、`IFileService`、`FileServiceImpl` 透传 `systemUserCode`；`createFileFormat` 写入当前工号；`makeFileObject`/`checkFileFormat`/`modifyFileInstance`/`deleteFileInstance`/`copyFileInstance`/`updateFileFormatOnLineFlagById` 增加归属校验
- [x] 5.4 `FileServiceImpl` 注入 `PermissionService` 计算过滤值

## 6. 表链接配置记录工号与过滤

- [x] 6.1 `TableLinkEntity` 增加 `systemUserCode` 字段
- [x] 6.2 `TableLinkMapper.xml` 的 `tableLinkColumns`、`tableLinkWhereCon`、`insertTableLink` 增加 `system_user_code`；`TableLinkMapper.java` 增加可空 `systemUserCode` 参数
- [x] 6.3 `TableLinkDao`、`ITableLinkService`、`TableLinkServiceImpl` 透传 `systemUserCode`；`addTableLink` 写入当前工号；`queryTableLink(detail)`/`modifyTableLink`/`delTableLink`/`onOffTableLink` 增加归属校验
- [x] 6.4 `TableLinkServiceImpl` 注入 `PermissionService` 计算过滤值

## 7. 元数据库脚本

- [x] 7.1 `doc/sql/datastream-h2-ddl.sql` 与 `doc/sql/datastream-mysql-ddl.sql` 为 `data_stream_data_base`/`data_stream_mq_config`/`data_stream_file_format`/`data_stream_table_link` 增加 `system_user_code` 列
- [x] 7.2 新增 `doc/sql/datastream-config-data-scope-migration.sql` 提供存量库 ALTER TABLE 升级脚本

## 8. 验证

- [x] 8.1 后端编译通过（`mvn -q -pl datastream-admin -am compile`）
- [x] 8.2 启动应用，验证普通用户仅见本人配置、系统管理员可见全部、普通用户凭 ID 越权访问他人配置被拒绝、概览统计按工号隔离
