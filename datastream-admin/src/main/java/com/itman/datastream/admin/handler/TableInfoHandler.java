/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itman.datastream.admin.handler;


import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.admin.service.ITableInfoService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.utils.OracleNumberMapper;
import com.itman.datastream.engine.route.DataBaseSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TableInfoHandler {
    private final DataBaseSource dataBaseSource;
    private final IMetaService metaService;
    private final DataSourceFactory dataSourceFactory;
    private final ITableInfoService tableInfoService;

    public IDatabaseAdapter matchDataBase(Integer dataSourceType) throws DataStreamException {
        return this.dataSourceFactory.matchDataBase(dataSourceType);
    }


    public List<TableInfoEntity> getTableList(Long dataBaseId) throws DataStreamException {
        log.info("开始获取表列表，数据源ID: {}", dataBaseId);

        List<DataBaseEntity> dataBaseList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ID, dataBaseId, null, 1, 10);
        if (CollectionUtils.isEmpty(dataBaseList)) {
            log.error("数据源不存在，ID: {}", dataBaseId);
            throw new DataStreamException(OPER_DATA_SOURCE_NOT_EXISTS_ERROR);
        }

        DataBaseEntity dataBase = dataBaseList.get(0);
        log.info("找到数据源: {}, 类型: {}, URL: {}", dataBase.getDataBaseName(), dataBase.getDataBaseType(), dataBase.getUrl());

        IDatabaseAdapter dataBaseAdapter = dataSourceFactory.matchDataBase(dataBase.getDataBaseType());

        dataBase.setDataPoolCount(1);
        dataBase.setKeyName(DATA_SEARCH_KEY_NAME);
        dataBase.setSqlValidationQuery(dataBaseAdapter.makeSqlValidationQuery());
        dataBase.setDriverClass(dataBaseAdapter.getDriverClass());
        String schemaName = parseSchemaNameJdbcUrl(dataBase.getUrl());
        dataBase.setSchemaName(schemaName);

        dataBaseSource.addDataBase(dataBaseList, null);

        List<TableInfoEntity> tableList = tableInfoService.getTableInfo(dataBaseId, dataBase);
        log.info("获取到表列表，数量: {}", tableList != null ? tableList.size() : 0);

        return tableList;
    }


    public List<String> buildCreateTableSql(TableInfoEntity tableInfo, String tableName, DataBaseEntity sourceDataSource, DataBaseEntity targetDataSource) throws DataStreamException {
        List<TableColumnEntity> columns = tableInfo.getColumns();
        DataBaseEnum sourceDb = DataBaseEnum.of(sourceDataSource.getDataBaseType());
        DataBaseEnum targetDb = DataBaseEnum.of(targetDataSource.getDataBaseType());

        List<ColumnTypeMapEntity> columnTypeMapList = null;
        Map<String, ColumnTypeMapEntity> columnTypeMapByA = null;
        if (!sourceDb.equals(targetDb)) {
            columnTypeMapList = metaService.queryColumnTypeMap(sourceDb.getName().toLowerCase(), targetDb.getName().toLowerCase());
            if (CollectionUtils.isEmpty(columnTypeMapList)) {
                throw new DataStreamException(OPER_DB_FIELD_TYPE_MAPPING_FETCH_ERROR);
            }

            columnTypeMapByA = new HashMap<>(columnTypeMapList.size());
            for (ColumnTypeMapEntity iterator : columnTypeMapList) {
                columnTypeMapByA.put(iterator.getColumnTypeNameA().toLowerCase(), iterator);
            }
        }

        List<ColumnTypeDefineEntity> columnTypeDefineList = metaService.queryColumnTypeDefine(sourceDb.getName().toLowerCase());
        if (CollectionUtils.isEmpty(columnTypeDefineList)) {
            throw new DataStreamException(OPER_QUERY_COLUMN_TYPE_DEFINE_ERROR);
        }

        Map<String, ColumnTypeDefineEntity> sourceColumnTypeDefineMapByName = new HashMap<>(columnTypeDefineList.size());
        for (ColumnTypeDefineEntity iterator : columnTypeDefineList) {
            sourceColumnTypeDefineMapByName.put(iterator.getColumnTypeName().toLowerCase(), iterator);
        }

        // 获取目标数据库的类型定义，用于判断是否需要长度参数
        List<ColumnTypeDefineEntity> targetColumnTypeDefineList = metaService.queryColumnTypeDefine(targetDb.getName().toLowerCase());
        if (CollectionUtils.isEmpty(targetColumnTypeDefineList)) {
            throw new DataStreamException(OPER_QUERY_COLUMN_TYPE_DEFINE_ERROR);
        }

        Map<String, ColumnTypeDefineEntity> targetColumnTypeDefineMapByName = new HashMap<>(targetColumnTypeDefineList.size());
        Map<Long, ColumnTypeDefineEntity> targetColumnTypeDefineMapById = new HashMap<>(targetColumnTypeDefineList.size());
        for (ColumnTypeDefineEntity iterator : targetColumnTypeDefineList) {
            targetColumnTypeDefineMapByName.put(iterator.getColumnTypeName().toLowerCase(), iterator);
            targetColumnTypeDefineMapById.put(iterator.getColumnTypeDefineId(), iterator);
        }

        StringBuilder createTableSql = new StringBuilder();
        // 1. CREATE TABLE 语句
        createTableSql.append("create table ").append(tableName).append(" (\n");

        // 2. 字段定义
        List<String> columnDefinitions = new ArrayList<>();
        for (TableColumnEntity column : columns) {
            StringBuilder columnDef = new StringBuilder();
            columnDef.append("  ").append(column.getColumnName()).append(" ");

            String sourceNormalizedTypeName = normalizeTypeName(column.getTypeName());
            String targetTypeName = null;
            ColumnTypeDefineEntity targetTypeDefine = null;

            if (sourceDb.equals(targetDb)) {
                targetTypeName = column.getTypeName().toLowerCase();
                targetTypeDefine = targetColumnTypeDefineMapByName.get(targetTypeName);
            } else {
                // 跨数据库迁移：使用映射表获取目标类型
                // 特殊处理：Oracle NUMBER 类型需要根据精度动态映射
                if (OracleNumberMapper.isOracleNumber(sourceDb, column.getTypeName())) {
                    targetTypeName = OracleNumberMapper.mapNumberType(column, targetDb);
                    log.info("Oracle NUMBER 智能映射: {} precision={} scale={} -> {}",
                            column.getColumnName(), column.getColumnSize(),
                            column.getDecimalDigits(), targetTypeName);
                } else if (!columnTypeMapByA.containsKey(sourceNormalizedTypeName.toLowerCase())) {
                    throw new DataStreamException(OPER_GET_TABLE_COLUMN_TYPE_DEFINE_ERROR.getCode(), "匹配字段类型失败,columnName[" + column.getColumnName() + "],typeName[" + sourceNormalizedTypeName.toLowerCase() + "],originalTypeName[" + column.getTypeName() + "]");
                } else {
                    ColumnTypeMapEntity typeMap = columnTypeMapByA.get(sourceNormalizedTypeName.toLowerCase());
                    targetTypeName = typeMap.getColumnTypeNameB();
                    // 通过 columnTypeDefineIdB 获取目标类型定义
                    if (typeMap.getColumnTypeDefineIdB() != null) {
                        targetTypeDefine = targetColumnTypeDefineMapById.get(typeMap.getColumnTypeDefineIdB());
                    }
                }
                columnDef.append(targetTypeName);
            }

            // 验证源类型定义存在
            if (!sourceColumnTypeDefineMapByName.containsKey(sourceNormalizedTypeName)) {
                throw new DataStreamException(OPER_DB_FIELD_TYPE_MATCH_ERROR.getCode(), "匹配字段类型定义失败,columnName[" + column.getColumnName() + "],typeName[" + sourceNormalizedTypeName + "],originalTypeName[" + column.getTypeName() + "]");
            }

            // 处理长度参数：使用目标数据库的类型定义来判断
            // 检查类型名是否已包含精度参数（如 decimal(10,2)），如果有则不再添加
            boolean hasPrecisionParams = targetTypeName != null && targetTypeName.contains("(");
            if (!hasPrecisionParams) {
                if (targetTypeDefine == null) {
                    // 如果没有通过映射表获取到，尝试通过类型名获取
                    targetTypeDefine = targetColumnTypeDefineMapByName.get(normalizeTypeName(targetTypeName).toLowerCase());
                }
                if (targetTypeDefine != null && targetTypeDefine.requiresLengthParam() && column.getColumnSize() != null) {
                    columnDef.append("(" + column.getColumnSize() + ")");
                }
            }

            // 处理NOT NULL
            if (column.getNullAble().equals(0)) {
                columnDef.append(" not null");
            }

            // 处理默认值
            if (!StringUtils.isEmpty(column.getColumnDef())) {
                String defaultValue = formatDefaultValue(column, targetDb);
                if (!StringUtils.isEmpty(defaultValue)) {
                    columnDef.append(" default ").append(defaultValue);
                }
            }

            // 处理自增列
            if ("YES".equalsIgnoreCase(column.getIsAutoIncrement())) {
                columnDef.append(appendAutoIncrement(targetDb));
            }

            //处理备注
            if(targetDb == DataBaseEnum.MYSQL){
                columnDef.append(matchDataBase(targetDataSource.getDataBaseType()).makeSqlComment(column.getRemarks()));
            }

            columnDefinitions.add(columnDef.toString());
        }
        createTableSql.append(String.join(",\n", columnDefinitions));
        createTableSql.append("\n)");

        List<String> createTableSQLList = new ArrayList<>();
        createTableSQLList.add(createTableSql.toString());

        //主键语句
        if (!CollectionUtils.isEmpty(tableInfo.getKeyColumnMap())) {
            for (Map.Entry<String, List<String>> entry : tableInfo.getKeyColumnMap().entrySet()) {
                String pkName = entry.getKey();
                String pkColumns = String.join(", ", entry.getValue());
                createTableSQLList.add(matchDataBase(targetDataSource.getDataBaseType()).makeSqlKeyColumn(tableName, pkName, pkColumns));
            }
        }

        //索引语句
        // Oracle/SQLServer/达梦等数据库在创建主键约束时会自动创建索引，需要跳过与主键列相同的索引
        boolean isAutoCreateIndexDb = targetDb == DataBaseEnum.ORACLE
                || targetDb == DataBaseEnum.SQLSERVER
                || targetDb == DataBaseEnum.DAMENG;

        if (!CollectionUtils.isEmpty(tableInfo.getIndexColumnMap())) {
            // 构建主键列集合，用于判断索引是否与主键重复
            List<String> pkColumnsList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(tableInfo.getKeyColumnMap())) {
                for (List<String> cols : tableInfo.getKeyColumnMap().values()) {
                    pkColumnsList.addAll(cols);
                }
            }

            for (Map.Entry<String, List<String>> entry : tableInfo.getIndexColumnMap().entrySet()) {
                String indexName = entry.getKey();
                List<String> indexColumns = entry.getValue();

                // 如果目标数据库会自动为主键创建索引，且索引列与主键列完全相同，则跳过
                if (isAutoCreateIndexDb && indexColumns.equals(pkColumnsList)) {
                    log.info("跳过与主键列相同的索引创建: indexName={}, columns={}", indexName, indexColumns);
                    continue;
                }

                String indexColumnsStr = String.join(", ", indexColumns);
                createTableSQLList.add(matchDataBase(targetDataSource.getDataBaseType()).makeSqlIndexColumn(tableName, indexName, indexColumnsStr));
            }
        }

        //字段备注
        if(targetDb == DataBaseEnum.POSTGRESQL || targetDb == DataBaseEnum.ORACLE){
            for (TableColumnEntity column : columns) {
                if (!StringUtils.isEmpty(column.getRemarks())) {
                    createTableSQLList.add(matchDataBase(targetDataSource.getDataBaseType()).makeSqlComment(tableName, column.getColumnName(), column.getRemarks()));
                }
            }
        }

        return createTableSQLList;
    }


    public List<TableColumnEntity> getTableColumnInfo(Long dataSourceId, String tableName) throws DataStreamException {
        List<DataBaseEntity> dataSourceList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ID, dataSourceId, null, 1, 10);
        if (CollectionUtils.isEmpty(dataSourceList)) {
            throw new DataStreamException(OPER_DATA_SOURCE_NOT_EXISTS_ERROR);
        }

        IDatabaseAdapter dataBase = dataSourceFactory.matchDataBase(dataSourceList.get(0).getDataBaseType());
        DataBaseEntity dataBaseEntity = dataSourceList.get(0);
        dataBaseEntity.setDataPoolCount(1);
        dataBaseEntity.setKeyName(DATA_SEARCH_KEY_NAME);
        dataBaseEntity.setSqlValidationQuery(dataBase.makeSqlValidationQuery());
        dataBaseEntity.setDriverClass(dataBase.getDriverClass());
        dataBaseEntity.setSchemaName(parseSchemaNameJdbcUrl(dataBaseEntity.getUrl()));
        dataBaseSource.addDataBase(dataSourceList, null);

        TableInfoEntity tableInfo = tableInfoService.fetchTableMetadata(dataBaseEntity.getDataBaseId(), dataBaseEntity, tableName);
        List<ColumnTypeTestEntity> columnTypeTestList = new ArrayList<>();
        for (TableColumnEntity iterator : tableInfo.getColumns()) {
            ColumnTypeTestEntity columnTypeTest = new ColumnTypeTestEntity();
            columnTypeTest.setColumnTypeTestId(metaService.querySequence(SEQ_COLUMN_TYPE_TEST_ID));
            columnTypeTest.setDatabaseType(DataBaseEnum.of(dataSourceList.get(0).getDataBaseType()).getName());
            columnTypeTest.setTableName(tableName);
            columnTypeTest.setColumnName(iterator.getColumnName());
            columnTypeTest.setColumnTypeName(iterator.getTypeName());
            columnTypeTest.setColumnStandardSize(iterator.getColumnSize());
            columnTypeTest.setRemark(iterator.getRemarks());
            columnTypeTestList.add(columnTypeTest);
        }
        metaService.insertColumnTypeTest(columnTypeTestList);
        return tableInfo.getColumns();
    }

    /**
     * 格式化默认值
     */
    private String formatDefaultValue(TableColumnEntity column, DataBaseEnum targetDb) {
        String defaultValue = column.getColumnDef();
        if (StringUtils.isEmpty(defaultValue)) {
            return null;
        }

        String upperValue = defaultValue.toUpperCase().trim();

        // 处理日期时间函数
        switch (upperValue) {
            case "SYSDATE":
                if (targetDb == DataBaseEnum.MYSQL || targetDb == DataBaseEnum.DORIS
                        || targetDb == DataBaseEnum.TIDB || targetDb == DataBaseEnum.OCEANBASE
                        || targetDb == DataBaseEnum.STARROCKS) {
                    return "CURRENT_TIMESTAMP";
                }
                if (targetDb == DataBaseEnum.POSTGRESQL || targetDb == DataBaseEnum.GAUSSDB) {
                    return "NOW()";
                }
                if (targetDb == DataBaseEnum.SQLSERVER) {
                    return "GETDATE()";
                }
                break;
            case "NOW()":
                if (targetDb == DataBaseEnum.ORACLE || targetDb == DataBaseEnum.DAMENG) {
                    return "SYSDATE";
                }
                if (targetDb == DataBaseEnum.SQLSERVER) {
                    return "GETDATE()";
                }
                break;
            case "CURRENT_TIMESTAMP":
                if (targetDb == DataBaseEnum.ORACLE || targetDb == DataBaseEnum.DAMENG) {
                    return "SYSDATE";
                }
                break;
            case "GETDATE()":
                if (targetDb == DataBaseEnum.ORACLE || targetDb == DataBaseEnum.DAMENG) {
                    return "SYSDATE";
                }
                if (targetDb == DataBaseEnum.MYSQL || targetDb == DataBaseEnum.POSTGRESQL) {
                    return "NOW()";
                }
                break;
        }

        // 处理字符串默认值（确保带引号）
        if (!defaultValue.startsWith("'") && !defaultValue.endsWith("'")) {
            // 判断是否为数值类型
            if (column.getColumnTypeClassify() != null
                    && column.getColumnTypeClassify().equals(COLUMN_TYPE_CLASSIFY_STRING)) {
                return "'" + defaultValue + "'";
            }
        }

        return defaultValue;
    }

    /**
     * 生成自增列定义
     */
    private String appendAutoIncrement(DataBaseEnum targetDb) {
        if (targetDb == DataBaseEnum.MYSQL || targetDb == DataBaseEnum.DORIS
                || targetDb == DataBaseEnum.TIDB || targetDb == DataBaseEnum.OCEANBASE
                || targetDb == DataBaseEnum.STARROCKS) {
            return " AUTO_INCREMENT";
        } else if (targetDb == DataBaseEnum.POSTGRESQL || targetDb == DataBaseEnum.GAUSSDB) {
            // PostgreSQL 使用 SERIAL，这里返回空，类型需要改成 SERIAL
            // 实际使用时需要替换类型名
            return "";
        } else if (targetDb == DataBaseEnum.SQLSERVER) {
            return " IDENTITY(1,1)";
        }
        // Oracle 需要单独创建序列和触发器，这里暂不处理
        return "";
    }
}
