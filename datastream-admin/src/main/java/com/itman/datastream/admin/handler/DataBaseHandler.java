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


import com.itman.datastream.admin.controller.domain.response.DataSearchResponse;
import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.engine.jdbc.ConnectionPoolManager;
import com.itman.datastream.engine.route.DataBaseSource;
import com.itman.datastream.admin.controller.domain.request.DataSearchRequest;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.admin.service.IDataSearchService;
import com.itman.datastream.security.service.PermissionService;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.utils.AESUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.genPageRow;
import static com.itman.datastream.common.utils.CommUtils.parseSchemaNameJdbcUrl;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataBaseHandler {
    private final DataBaseSource dataBaseSource;
    private final IMetaService metaService;
    private final DataSourceFactory dataSourceFactory;
    private final IDataSearchService dataSearchService;
    private final ConnectionPoolManager poolManager;
    private final PermissionService permissionService;


    public void testDataDase(DataBaseEntity dataDase) throws DataStreamException {
        dataDase.setSqlValidationQuery(dataSourceFactory.matchDataBase(dataDase.getDataBaseType()).makeSqlValidationQuery());
        dataDase.setDriverClass(dataSourceFactory.matchDataBase(dataDase.getDataBaseType()).getDriverClass());

        poolManager.executeSqlValidationQuery(dataDase.getSqlValidationQuery(), dataDase.getUrl(), dataDase.getUserName(), AESUtils.decrypt(dataDase.getPassWord()));
    }

    public List<DataBaseEntity> queryDataBase(Integer queryFlag, Long queryValue, Integer state, Integer page, Integer count) throws DataStreamException {
        return metaService.queryDataBase(queryFlag, queryValue, state, page, count, currentUserScope());
    }

    public Integer getDataBaseCount(Integer queryFlag, Long queryValue, Integer state) throws DataStreamException {
        return metaService.getDataBaseCount(queryFlag, queryValue, state, currentUserScope());
    }

    public Long insertDataBase(DataBaseEntity dataBase) throws DataStreamException {
        dataBase.setDataBaseId(metaService.querySequence(SEQ_DATA_BASE_ID));
        dataBase.setState(COMMON_STATE_OFFLINE);
        dataBase.setSystemUserCode(permissionService.getCurrentUserCode());
        if (metaService.insertDataBase(dataBase).equals(0)) {
            throw new DataStreamException(OPER_INSERT_DATA_BASE_FAIL_ERROR);
        }

        return dataBase.getDataBaseId();
    }

    /**
     * 计算当前用户的数据范围过滤值：管理员返回 null（不过滤），普通用户返回当前工号。
     */
    private String currentUserScope() {
        return permissionService.isAdmin() ? null : permissionService.getCurrentUserCode();
    }

    /**
     * 校验配置归属：非管理员访问他人配置时抛出无权限错误。
     */
    private void checkDataBaseOwner(DataBaseEntity dataBase) throws DataStreamException {
        if (permissionService.isAdmin()) {
            return;
        }
        String currentUserCode = permissionService.getCurrentUserCode();
        if (StringUtils.isEmpty(currentUserCode) || !currentUserCode.equals(dataBase.getSystemUserCode())) {
            throw new DataStreamException(OPER_CONFIG_NOT_OWNER_ERROR);
        }
    }

    public Long updateDataBase(DataBaseEntity dataBase) throws DataStreamException {
        List<DataBaseEntity> dataBaseList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ID, dataBase.getDataBaseId(), null, 1, 10, null);
        if (CollectionUtils.isEmpty(dataBaseList)) {
            throw new DataStreamException(OPER_QUERY_DATA_BASE_FAIL_ERROR);
        }
        checkDataBaseOwner(dataBaseList.get(0));

        if (!dataBaseList.get(0).getState().equals(COMMON_STATE_OFFLINE)) {
            throw new DataStreamException(OPER_DATA_BASE_STATE_NOT_OFF_ERROR);
        }

        //todo 后续考虑是否需要把更新数据备份一下，可以之前任务按照老的数据源迁移
        if (metaService.updateDataBase(dataBase).equals(0)) {
            throw new DataStreamException(OPER_UPDATE_DATA_BASE_FAIL_ERROR);
        }

        return dataBase.getDataBaseId();
    }

    public Long delDataBase(Long dataBaseId) throws DataStreamException {
        List<DataBaseEntity> dataBaseList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ID, dataBaseId, null, 1, 10, null);
        if (CollectionUtils.isEmpty(dataBaseList)) {
            throw new DataStreamException(OPER_QUERY_DATA_BASE_FAIL_ERROR);
        }
        checkDataBaseOwner(dataBaseList.get(0));

        if (!dataBaseList.get(0).getState().equals(COMMON_STATE_OFFLINE)) {
            throw new DataStreamException(OPER_DATA_BASE_STATE_NOT_OFF_ERROR);
        }

        //todo 后续考虑是否需要把更新数据备份一下，可以之前任务按照老的数据源迁移
        if (metaService.updateDataBaseState(dataBaseId, COMMON_STATE_DELETED).equals(0)) {
            throw new DataStreamException(OPER_UPDATE_DATA_BASE_FAIL_ERROR);
        }

        return dataBaseId;
    }

    private void checkDataBaseList(Long dataBaseId, Integer state) throws DataStreamException {
        List<DataBaseEntity> dataBaseList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ID, dataBaseId, null, 1, 10, null);
        if (CollectionUtils.isEmpty(dataBaseList)) {
            throw new DataStreamException(OPER_QUERY_DATA_BASE_FAIL_ERROR);
        }
        checkDataBaseOwner(dataBaseList.get(0));

        if (state.equals(COMMON_STATE_ONLINE) && !dataBaseList.get(0).getState().equals(COMMON_STATE_OFFLINE)) {
            throw new DataStreamException(OPER_DATA_BASE_STATE_NOT_OFF_ERROR);
        } else if (state.equals(COMMON_STATE_OFFLINE) && !dataBaseList.get(0).getState().equals(COMMON_STATE_ONLINE)) {
            throw new DataStreamException(OPER_DATA_SOURCE_STATE_NOT_ON_ERROR);
        }
    }

    private void checkDataBaseIsWorking(Long dataSourceId, Integer state) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskAllList = new ArrayList<>();
        addDataMoveTaskEntityAllList(dataMoveTaskAllList, DATA_STREAM_TASK_STATE_RUNNING);
        addDataMoveTaskEntityAllList(dataMoveTaskAllList, DATA_STREAM_TASK_STATE_INIT);

        if (!CollectionUtils.isEmpty(dataMoveTaskAllList)) {
            List<Long> taskIdList = new ArrayList<>();
            for (DataMoveTaskEntity dataMoveTaskEntity : dataMoveTaskAllList) {
                if (dataMoveTaskEntity.getSourceObjectId().equals(dataSourceId)) {
                    taskIdList.add(dataMoveTaskEntity.getTaskId());
                }
                if (dataMoveTaskEntity.getTargetDataBase().equals(dataSourceId)) {
                    taskIdList.add(dataMoveTaskEntity.getTaskId());
                }
            }

            if (!CollectionUtils.isEmpty(taskIdList)) {
                String result = taskIdList.stream().map(String::valueOf).collect(Collectors.joining(","));
                throw new DataStreamException(OPER_DATA_BASE_HAVE_TASK_RUN_ERROR.getCode(), "该数据连接配置正在被迁移任务[" + result + "]使用，请先停止迁移任务!");
            }
        }
    }

    public Long onOffDataBase(Long dataBaseId, Integer state) throws DataStreamException {
        checkDataBaseList(dataBaseId, state);

        if (state.equals(COMMON_STATE_OFFLINE)) {
            checkDataBaseIsWorking(dataBaseId, state);
        }

        //todo 后续考虑是否需要把更新数据备份一下，可以之前任务按照老的数据源迁移
        if (metaService.updateDataBaseState(dataBaseId, state).equals(0)) {
            throw new DataStreamException(OPER_UPDATE_DATA_BASE_FAIL_ERROR);
        }

        return dataBaseId;
    }

    private void addDataMoveTaskEntityAllList(List<DataMoveTaskEntity> dataMoveTaskEntityAllList, Integer state) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskEntityList = metaService.queryDataMoveTaskByState(state, 1, 100, null);
        if (!CollectionUtils.isEmpty(dataMoveTaskEntityList)) {
            dataMoveTaskEntityAllList.addAll(dataMoveTaskEntityAllList);
        }
    }

    public void statSystemInfo(Integer days, StatSystemInfoEntity statSystemInfoEntity) throws DataStreamException {
        String systemUserCode = currentUserScope();
        Integer dataBaseCount = 0;
        List<DataBaseEntity> dataBaseList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ALL, null, null, 1, 1000, systemUserCode);
        if (!CollectionUtils.isEmpty(dataBaseList)) {
            dataBaseCount = dataBaseList.size();
        }

        //查询数据连接总
        statSystemInfoEntity.setDataSourceCount(dataBaseCount);
        //数据迁移任务总数
        statSystemInfoEntity.setMoveTaskSumCount(metaService.statMoveTaskCount(null, systemUserCode));
        //数据迁移运行总数
        statSystemInfoEntity.setMoveTaskRunCount(metaService.statMoveTaskCount(DATA_STREAM_TASK_STATE_INIT + "," + DATA_STREAM_TASK_STATE_RUNNING, systemUserCode));
        //数据回迁任务总数
        statSystemInfoEntity.setLinkTaskSumCount(metaService.statLinkTaskCount(null, systemUserCode));
        //数据回迁运行总数
        statSystemInfoEntity.setLinkTaskRunCount(metaService.statLinkTaskCount(DATA_STREAM_TASK_STATE_INIT + "," + DATA_STREAM_TASK_STATE_RUNNING, systemUserCode));
        //查询迁移执行总数
        statSystemInfoEntity.setMoveTaskDayCountList(metaService.statMoveTaskCountGroupByDay(days, systemUserCode));
        statSystemInfoEntity.setLinkTaskDayCountList(metaService.statLinkTaskCountGroupByDay(days, systemUserCode));
        //按任务类型统计迁移任务数
        statSystemInfoEntity.setTaskTypeCountList(metaService.statMoveTaskCountGroupByType(systemUserCode));
        //按任务状态统计迁移任务数
        statSystemInfoEntity.setTaskStateCountList(metaService.statMoveTaskCountGroupByState(systemUserCode));
    }

    public void dataSearch(DataSearchRequest dataSearchRequest, DataSearchResponse dataSearchResponse) throws DataStreamException {
        //获取数据源
        List<DataBaseEntity> dataBaseList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ID, dataSearchRequest.getDataSourceId(), null, 1, 10, null);
        if (CollectionUtils.isEmpty(dataBaseList)) {
            throw new DataStreamException(OPER_QUERY_DATA_BASE_FAIL_ERROR);
        }
        checkDataBaseOwner(dataBaseList.get(0));

        IDatabaseAdapter dataBase = dataSourceFactory.matchDataBase(dataBaseList.get(0).getDataBaseType());

        dataBaseList.get(0).setDataPoolCount(1);
        dataBaseList.get(0).setKeyName(DATA_SEARCH_KEY_NAME);
        dataBaseList.get(0).setSqlValidationQuery(dataBase.makeSqlValidationQuery());
        dataBaseList.get(0).setDriverClass(dataBase.getDriverClass());
        dataBaseList.get(0).setSchemaName(parseSchemaNameJdbcUrl(dataBaseList.get(0).getUrl()));
        dataBaseSource.addDataBase(dataBaseList, null);

        //todo 优化并行执行
        if (dataSearchRequest.getFlag().equals(1) || dataSearchRequest.getFlag().equals(3)) {
            StringBuffer selectSql = new StringBuffer();
            selectSql.append("select count(1) from ").append(dataSearchRequest.getTableName());
            if (!StringUtils.isEmpty(dataSearchRequest.getQueryCondition())) {
                selectSql.append(" where ").append(dataSearchRequest.getQueryCondition()).append(" ");
            }

            dataSearchResponse.setRecordSum(dataSearchService.executeSelectRecordCountSql(dataSearchRequest.getDataSourceId(), selectSql.toString()));
        }

        if (dataSearchRequest.getFlag().equals(2) || dataSearchRequest.getFlag().equals(3)) {
            //查询表
            List<TableColumnEntity> tableColumnEntityList = dataSearchService.getTableColumns(dataSearchRequest.getDataSourceId(), dataBaseList.get(0), dataSearchRequest.getTableName());
            if (CollectionUtils.isEmpty(tableColumnEntityList)) {
                throw new DataStreamException(OPER_QUERY_TABLE_FROM_DB_ERROR);
            }

            List<ColumnTypeDefineEntity> columnTypeDefineList = queryColumnTypeDefine(dataBaseList.get(0).getDataBaseType());
            copyTableColumnsTypeDefine(tableColumnEntityList, columnTypeDefineList);

            dataSearchResponse.setTableColumnNameList(tableColumnEntityList.stream().map(a -> a.getColumnName()).collect(Collectors.toList()));

            List<String> selectColumnNameList = new ArrayList<>(tableColumnEntityList.size());
            for (TableColumnEntity iterator : tableColumnEntityList) {
                selectColumnNameList.add(iterator.getColumnName());
            }

            String tableKey = null;
            List<TableColumnEntity> tableKeysColumnList = tableColumnEntityList.stream().filter(x -> x.isKeyFlag()).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(tableKeysColumnList)){
                List<String> tableKeysList = tableKeysColumnList.stream().map(a -> a.getColumnName()).collect(Collectors.toList());
                tableKey = String.join(",", tableKeysList);
            }

            String selectSql = makeSelectSql(dataBaseList.get(0).getDataBaseType(), dataSearchRequest.getPage(), dataSearchRequest.getCount(), dataSearchRequest.getTableName(), dataSearchRequest.getQueryCondition(), selectColumnNameList, tableKey);
            //查询
            List<Map> dataRowList = dataSearchService.executeSelectMapListSql(dataSearchRequest.getDataSourceId(), selectSql);

            // 如果是 Oracle，将 Map 的 key 转换为小写（Oracle 返回大写字段名）
            if (dataBaseList.get(0).getDataBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) {
                if (!CollectionUtils.isEmpty(dataRowList)) {
                    // 检查第一个 Map 的 key 是否为大写
                    String firstKey = dataRowList.get(0).keySet().iterator().next().toString();
                    if (Character.isUpperCase(firstKey.charAt(0))) {
                        List<Map> resultList = new ArrayList<>();
                        for (Map<String, Object> map : dataRowList) {
                            resultList.add(convertKeysToLowerCase(map));
                        }
                        dataRowList = resultList;
                    }
                }
            }

            // 格式化时间字段为可读字符串
            if (!CollectionUtils.isEmpty(dataRowList)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (Map<String, Object> row : dataRowList) {
                    for (TableColumnEntity column : tableColumnEntityList) {
                            Object value = row.get(column.getColumnName());
                            if (value != null) {
                                String formattedValue;
                                // java.util.Date 及其子类（java.sql.Date, java.sql.Timestamp）
                                if (value instanceof Date) {
                                    formattedValue = dateFormat.format((Date) value);
                                }
                                // java.time.LocalDateTime (Java 8+)
                                else if (value instanceof LocalDateTime) {
                                    formattedValue = ((LocalDateTime) value).format(dateTimeFormatter);
                                }
                                // 其他类型（包括字符串）直接用 toString
                                else {
                                    formattedValue = value.toString();
                                    // 如果是 ISO 8601 格式（包含 T），替换为空格
                                    if (formattedValue.contains("T")) {
                                        formattedValue = formattedValue.replace("T", " ");
                                    }
                                }
                                row.put(column.getColumnName(), formattedValue);
                            }
                    }
                }
            }

            dataSearchResponse.setDataRecordList(dataRowList);
        }
    }

    private String makeSelectSql(Integer dataBaseType, Integer page, Integer count, String tableName, String tableCondition, List<String> selectColumnNameList, String tableKey) throws DataStreamException {
        StringBuffer selectSql = new StringBuffer();
        selectSql.append("SELECT ").append(selectColumnNameList.stream().collect(Collectors.joining(","))).append(" FROM ").append(tableName).append(" ");

        if (!StringUtils.isEmpty(tableCondition)) {
            selectSql.append(" WHERE ").append(tableCondition).append(" ");
        }

        if (tableKey != null) {
            selectSql.append("ORDER BY ").append(tableKey).append(" ");
        }

        if (dataBaseType.equals(DATA_SOURCE_TYPE_ORACLE)) {
            return String.format("SELECT * FROM (SELECT row_.*, rownum rn FROM ( %s ) row_ WHERE rownum <= %d) WHERE rn>%s", selectSql, (genPageRow(page, count) + count), genPageRow(page, count));
        } else {
            return selectSql.append(dataSourceFactory.matchDataBase(dataBaseType).makeSqlLimit(genPageRow(page, count), count)).toString();
        }
    }

    public List<ColumnTypeDefineEntity> queryColumnTypeDefine(Integer dataBaseType) throws DataStreamException {
        List<ColumnTypeDefineEntity> columnTypeDefineList = metaService.queryColumnTypeDefine(DataBaseEnum.of(dataBaseType).getName().toLowerCase());
        if (CollectionUtils.isEmpty(columnTypeDefineList)) {
            throw new DataStreamException(OPER_QUERY_COLUMN_TYPE_DEFINE_ERROR);
        }

        return columnTypeDefineList;
    }

    public void copyTableColumnsTypeDefine(List<TableColumnEntity> tableColumns, List<ColumnTypeDefineEntity> tableColumnTypeDefineList) throws DataStreamException {
        if (CollectionUtils.isEmpty(tableColumns)) {
            return;
        }

        for (TableColumnEntity iterator : tableColumns) {
            List<ColumnTypeDefineEntity> tableColumnTypeDefineListTemp = tableColumnTypeDefineList.stream().filter(x -> x.getColumnTypeName().equalsIgnoreCase(iterator.getTypeName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(tableColumnTypeDefineListTemp)) {
                throw new DataStreamException(OPER_GET_TABLE_COLUMN_TYPE_DEFINE_ERROR);
            }
            BeanUtils.copyProperties(tableColumnTypeDefineListTemp.get(0), iterator);
        }
    }

    /**
     * 将 Map 的 key 转换为小写（用于处理 Oracle 返回的大写字段名）
     */
    private Map<String, Object> convertKeysToLowerCase(Map<String, Object> originalMap) {
        Map<String, Object> resultMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
            // 将key转换为小写，value保持不变
            resultMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return resultMap;
    }
}
