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

import com.itman.datastream.admin.service.*;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.admin.controller.domain.request.CreateTableLinkTaskRequest;
import com.itman.datastream.admin.controller.domain.request.QueryTableLinkTaskRequest;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.engine.holder.DataStreamHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;

@Slf4j
@Component
public class LinkTaskHandler extends AbstractHandler {
    private final ILinkSourceService linkSourceService;
    private final ILinkTargetService linkTargetService;
    private final IMetaService metaService;
    private final ITableLinkService tableLinkService;
    private final DataStreamHolder dataStreamHolder;


    public LinkTaskHandler(DataStreamConfig dataStreamConfig, DataSourceFactory dataSourceFactory, ILinkSourceService linkSourceService, ILinkTargetService linkTargetService, IMetaService metaService, ITableLinkService tableLinkService, DataStreamHolder dataStreamHolder, IMoveTargetService moveTargetService) {
        super(dataSourceFactory, dataStreamConfig, metaService, dataStreamHolder, moveTargetService);
        this.linkSourceService = linkSourceService;
        this.linkTargetService = linkTargetService;
        this.metaService = metaService;
        this.tableLinkService = tableLinkService;
        this.dataStreamHolder = dataStreamHolder;
    }

    public void insertLinkTaskTable(Long linkTaskId, List<LinkTaskTableEntity> linkTableList) throws DataStreamException {
        for (LinkTaskTableEntity iterator : linkTableList) {
            iterator.setLinkTaskTableId(metaService.querySequence(SEQ_LINK_TASK_TABLE_ID));
            iterator.setLinkTaskId(linkTaskId);
            metaService.insertLinkTaskTable(iterator);
        }
    }

    public void tableLinkTaskByService(TableLinkTaskEntity tableLinkTask) throws DataStreamException {
        List<String> insertSqlList = new ArrayList<>();
        List<String> updateSqlList = new ArrayList<>();
        List<LinkTaskTableEntity> linkTaskTableList = new ArrayList<>();

        generateInsertSqlByLink(tableLinkTask, insertSqlList, updateSqlList, linkTaskTableList);

        if (!CollectionUtils.isEmpty(insertSqlList)) {
            linkTargetService.syncDataList(tableLinkTask.getTargetDataBaseId(), insertSqlList);
        }

        if (!CollectionUtils.isEmpty(updateSqlList)) {
            linkSourceService.updateDataList(tableLinkTask.getSourceDataBaseId(), updateSqlList);
        }

        if (!CollectionUtils.isEmpty(linkTaskTableList)) {
            insertLinkTaskTable(tableLinkTask.getLinkTaskId(), linkTaskTableList);
        }
    }

    void generateInsertSqlByLink(final TableLinkTaskEntity tableLinkTask, List<String> insertSqlList, List<String> updateSqlList, List<LinkTaskTableEntity> linkTaskTableList) throws DataStreamException {
        List<TableLinkEntity> tableLinkList = tableLinkService.queryTableLink(TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID, tableLinkTask.getTableLinkId().toString(), 1, 10);
        if (CollectionUtils.isEmpty(tableLinkList)) {
            throw new DataStreamException(OPER_TASK_BACK_BY_SERVICE_TYPE_ERROR);
        }

        List<LinkNodeEntity> linkNodeList = tableLinkService.queryTableLinkNodeList(tableLinkTask.getTableLinkId());
        if (CollectionUtils.isEmpty(linkNodeList)) {
            throw new DataStreamException(OPER_TASK_LINK_CONFIG_SERVICE_TYPE_ERROR);
        }

        List<Map> businessDataList = new ArrayList<>(Collections.singletonList(Collections.singletonMap(FLOW_ROOT_PARENT_FIELD_BUSINESS_ID, tableLinkTask.getBusinessId())));

        recursionTableLinkNode(tableLinkTask, linkNodeList, -1L, businessDataList, insertSqlList, updateSqlList, linkTaskTableList);
    }

    void recursionTableLinkNode(final TableLinkTaskEntity tableLinkTask, List<LinkNodeEntity> linkNodeList, Long parentFlowNodeId, List<Map> parentDatalist, List<String> insertSqlList, List<String> updateSqlList, List<LinkTaskTableEntity> linkTaskTableList) throws DataStreamException {
        List<LinkNodeEntity> linkNodeListByFilter = linkNodeList.stream().filter(x -> x.getParentLinkNodeId().equals(parentFlowNodeId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(linkNodeListByFilter)) {
            return;
        }

        for (LinkNodeEntity iterator : linkNodeListByFilter) {
            if (CollectionUtils.isEmpty(parentDatalist)) {
                addDataLinkTableList(iterator.getTableName(), iterator.getTableName(), 0, 0, linkTaskTableList);
            }

            //根据负节点实例数据记录数个执行
            for (Map dataIterator : parentDatalist) {
                List<Map> datalist = new ArrayList<>();
                makeSqlLinkNode(tableLinkTask, iterator, dataIterator, datalist, insertSqlList, updateSqlList, linkTaskTableList);
                //递归下节点数据
                recursionTableLinkNode(tableLinkTask, linkNodeList, iterator.getLinkNodeId(), datalist, insertSqlList, updateSqlList, linkTaskTableList);
            }
        }
    }

    public void checkSourceAndTargetColumnType(TableLinkTaskEntity tableLinkTask, List<TableColumnEntity> sourceColumnList, List<TableColumnEntity> targetColumnList) throws DataStreamException {
        for (TableColumnEntity iterator : sourceColumnList) {
            List<ColumnTypeDefineEntity> sourceColumnTypeDefineListTemp = tableLinkTask.getSourceColumnTypeDefineList().stream().filter(x -> x.getColumnTypeName().equals(iterator.getTypeName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(sourceColumnTypeDefineListTemp)) {
                throw new DataStreamException("xxxxx", "源表匹配字段类型失败");
            }
            BeanUtils.copyProperties(sourceColumnTypeDefineListTemp.get(0), iterator);
        }

        for (TableColumnEntity iterator : targetColumnList) {
            List<ColumnTypeDefineEntity> targetColumnTypeDefineListTemp = tableLinkTask.getTargetColumnTypeDefineList().stream().filter(x -> x.getColumnTypeName().equals(iterator.getTypeName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(targetColumnTypeDefineListTemp)) {
                throw new DataStreamException("xxxxx", "目标表匹配字段类型失败");
            }
            BeanUtils.copyProperties(targetColumnTypeDefineListTemp.get(0), iterator);
        }
    }

    void makeSqlLinkNode(final TableLinkTaskEntity tableLinkTask, LinkNodeEntity linkNode, Map parentDataMap, List<Map> dataRowList, List<String> insertSqlList, List<String> updateSqlList, List<LinkTaskTableEntity> linkTaskTableList) throws DataStreamException {
        String targetTableName = linkNode.getTableName();
        String sourceTableName = linkNode.getTableName();
        //获取源表字段信息
        List<TableColumnEntity> sourceColumnList = getTableColumnsFromLinkSource(tableLinkTask.getSourceDataSource(), sourceTableName);
        //获取目标表字段信息
        List<TableColumnEntity> targetColumnList = linkTargetService.getTableColumns(tableLinkTask.getTargetDataBaseId(), tableLinkTask.getTargetDataSource(), targetTableName);
        if (CollectionUtils.isEmpty(targetColumnList)) {
            throw new DataStreamException(OPER_TASK_TARGET_TABLE_NULL_ERROR.getCode(), "SchemaName={" + tableLinkTask.getTargetDataSource().getSchemaName() + "},targetObjectName={" + targetTableName + "}获取表字段失败");
        }

        List<TableColumnEntity> targetUpdateColumnList = targetColumnList.stream().filter(column -> column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_BACK_COUNT) || column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_BACK_TASK_ID)).collect(Collectors.toList());

        targetColumnList = targetColumnList.stream().filter(column -> !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID) && !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_BACK_COUNT) && !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_BACK_TASK_ID)).collect(Collectors.toList());

        //组装查询sql
        if (!parentDataMap.containsKey(linkNode.getParentFieldName().toLowerCase())) {
            if (!parentDataMap.containsKey(linkNode.getParentFieldName().toLowerCase())) {
                throw new DataStreamException(OPER_TABLE_COLUMNS_CONTAIN_DATA_ERROR);
            }
        }

        checkSourceAndTargetColumnType(tableLinkTask, sourceColumnList, targetColumnList);

        String fieldValue = String.valueOf(Optional.ofNullable(parentDataMap.get(linkNode.getParentFieldName().toLowerCase())).orElseGet(() -> String.valueOf(parentDataMap.get(linkNode.getParentFieldName().toLowerCase()))));
        String tableCondition = linkNode.getFieldName().toLowerCase() + " = " + fieldValue;
        String selectSql = super.matchDataBase(tableLinkTask.getSourceDataSource().getDataBaseType()).makeSqlSelectColumns(sourceTableName, tableCondition, sourceColumnList, false);

        dataRowList.addAll(linkSourceService.executeSelectMapListSql(tableLinkTask.getSourceDataBaseId(), selectSql));
        if (CollectionUtils.isEmpty(dataRowList)) {
            addDataLinkTableList(sourceTableName, targetTableName, 0, 0, linkTaskTableList);
            return;
        }

        //根据主键判断是否需要插入
        List<Map> dataRowListFilter = new ArrayList<>();
        List<String> targetKeyColumns = targetColumnList.stream().filter(x -> x.isKeyFlag()).map(a -> a.getColumnName()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(targetKeyColumns)) {
            dataRowListFilter.addAll(dataRowList);
        } else {
            dataRowListFilter.addAll(filterTargetDuplicateData(tableLinkTask.getTargetDataSource().getDataBaseId(), tableLinkTask.getTargetDataSource().getDataBaseType(), dataRowList, targetKeyColumns, targetColumnList, targetTableName));
        }

        if (!CollectionUtils.isEmpty(dataRowListFilter)) {
            //组装insert sql
            String insertSqlColumns = super.makeSqlInsertColumns(targetTableName, matchDataBase(tableLinkTask.getTargetDataSource().getDataBaseType()).makeSqlIgnore(), targetColumnList);
            insertSqlList.add(makeSqlBatchInsert(insertSqlColumns, null, tableLinkTask.getTargetDataSource().getDataBaseType(), targetColumnList, dataRowListFilter));

            //组装update sql
            if (!CollectionUtils.isEmpty(targetUpdateColumnList)) {
                updateSqlList.add(makeUpdateSql(targetTableName, tableLinkTask.getLinkTaskId(), tableCondition));
            }
        }

        //组装插入a_data_back_table表名、记录数据
        addDataLinkTableList(sourceTableName, targetTableName, dataRowList.size(), dataRowListFilter.size(), linkTaskTableList);
    }

    List<Map> filterTargetDuplicateData(final Long dataSourceId, final Integer dataSourceType, final List<Map> dataList, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final String tableName) throws DataStreamException {
        if (CollectionUtils.isEmpty(keyColumns)) {
            return new ArrayList<>();
        }

        return (dataList.size() > dataStreamConfig.getDataStreamParallelStreamSize()) ? dataList.parallelStream().filter(dataRecord -> {
            String keySelectSql = makeSqlSelectCountByKey(dataSourceType, dataRecord, keyColumns, tableColumns, tableName);
            try {
                return keySelectSql != null && linkTargetService.getTableRecordCount(dataSourceId, keySelectSql) == 0L;
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()) : dataList.stream().filter(dataRecord -> {
            String keySelectSql = makeSqlSelectCountByKey(dataSourceType, dataRecord, keyColumns, tableColumns, tableName);
            try {
                return keySelectSql != null && linkTargetService.getTableRecordCount(dataSourceId, keySelectSql) == 0L;
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private void addDataLinkTableList(String sourceTableName, String targetTableName, Integer selectCount, Integer insertCount, List<LinkTaskTableEntity> dataBackTableList) {
        LinkTaskTableEntity dataBackTable = new LinkTaskTableEntity();
        dataBackTable.setSelectTableName(sourceTableName);
        dataBackTable.setInsertTableName(targetTableName);
        dataBackTable.setSelectCount(selectCount);
        dataBackTable.setInsertCount(insertCount);
        dataBackTableList.add(dataBackTable);
    }

    public String makeUpdateSql(String tableName, Long backTaskTd, String tableCondition) {
        return String.format("update %s set %s = %s , %s = coalesce(%s, 0) + 1 where %s", tableName, TARGET_TABLE_ADD_COLUMNS_BACK_TASK_ID, backTaskTd, TARGET_TABLE_ADD_COLUMNS_BACK_COUNT, TARGET_TABLE_ADD_COLUMNS_BACK_COUNT, tableCondition);
    }

    public void createTableLinkTask(CreateTableLinkTaskRequest createTableLinkTaskRequest) throws DataStreamException {
        List<LinkNodeEntity> linkNodeList = tableLinkService.queryTableLinkNodeList(createTableLinkTaskRequest.getTableLinkId());
        if (CollectionUtils.isEmpty(linkNodeList)) {
            throw new DataStreamException(OPER_TASK_LINK_CONFIG_SERVICE_TYPE_ERROR);
        }

        TableLinkTaskEntity dataBackTask = new TableLinkTaskEntity();
        BeanUtils.copyProperties(createTableLinkTaskRequest, dataBackTask);
        dataBackTask.setLinkTaskId(metaService.querySequence(SEQ_MOVE_TASK_ID));
        dataBackTask.setState(TABLE_LINK_TASK_STATE_INIT);

        if (metaService.insertTableLinkTask(dataBackTask).equals(0)) {
            throw new DataStreamException(DataStreamErrorCode.OPER_BACK_TASK_CREATE_ERROR);
        }
    }


    public List<TableLinkTaskEntity> queryTableLinkTaskList(QueryTableLinkTaskRequest queryTableLinkTaskRequest) throws DataStreamException {
        List<TableLinkTaskEntity> tableLinkTaskList = new ArrayList<>();
        if (queryTableLinkTaskRequest.getQueryFlag().equals(QUERY_DATE_LINK_TASK_FLAG_BY_TASK_ID)) {
            Optional.ofNullable(queryTableLinkTaskRequest.getLinkTaskId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_TASK_ID_NULL_ERROR));
            tableLinkTaskList.addAll(metaService.queryTableLinkTaskByLinkTaskId(queryTableLinkTaskRequest.getLinkTaskId()));
        } else if (queryTableLinkTaskRequest.getQueryFlag().equals(QUERY_DATE_LINK_TASK_FLAG_BY_STATE)) {
            Optional.ofNullable(queryTableLinkTaskRequest.getQueryFlag()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_STATE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getPage()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_PAGE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getCount()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_COUNT_NULL_ERROR));
            tableLinkTaskList.addAll(metaService.queryTableLinkTaskByState(queryTableLinkTaskRequest.getState(), queryTableLinkTaskRequest.getPage(), queryTableLinkTaskRequest.getCount()));
        } else if (queryTableLinkTaskRequest.getQueryFlag().equals(QUERY_DATE_LINK_TASK_FLAG_BY_TIME)) {
            Optional.ofNullable(queryTableLinkTaskRequest.getBeginDate()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_BEGIN_DATE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getEndDate()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_END_DATE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getPage()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_PAGE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getCount()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_COUNT_NULL_ERROR));
            tableLinkTaskList.addAll(metaService.queryTableLinkTaskByDate(queryTableLinkTaskRequest.getBeginDate(), queryTableLinkTaskRequest.getEndDate(), queryTableLinkTaskRequest.getPage(), queryTableLinkTaskRequest.getCount()));
        } else {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_QUERY_FLAG_NULL_ERROR);
        }

        for (TableLinkTaskEntity iterator : tableLinkTaskList) {
            if (iterator.getTableLinkId() != null) {
                List<TableLinkEntity> tableLinkList = tableLinkService.queryTableLink(TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID, iterator.getTableLinkId().toString(), 1, 10);
                if (!CollectionUtils.isEmpty(tableLinkList)) {
                    iterator.setTableLinkName(tableLinkList.get(0).getTableLinkName());
                }
            }
        }
        return tableLinkTaskList;
    }

    public Integer queryTableLinkTaskCount(QueryTableLinkTaskRequest queryTableLinkTaskRequest) throws DataStreamException {
        Integer recordCount = 0;
        if (queryTableLinkTaskRequest.getQueryFlag().equals(QUERY_DATE_LINK_TASK_FLAG_BY_TASK_ID)) {
            Optional.ofNullable(queryTableLinkTaskRequest.getLinkTaskId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_TASK_ID_NULL_ERROR));
            List<TableLinkTaskEntity> tableLinkTaskList = metaService.queryTableLinkTaskByLinkTaskId(queryTableLinkTaskRequest.getLinkTaskId());
            if (CollectionUtils.isEmpty(tableLinkTaskList)) {
                recordCount = tableLinkTaskList.size();
            }
        } else if (queryTableLinkTaskRequest.getQueryFlag().equals(QUERY_DATE_LINK_TASK_FLAG_BY_STATE)) {
            Optional.ofNullable(queryTableLinkTaskRequest.getQueryFlag()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_STATE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getPage()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_PAGE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getCount()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_COUNT_NULL_ERROR));
            recordCount = metaService.queryTableLinkTaskByStateCount(queryTableLinkTaskRequest.getState());
        } else if (queryTableLinkTaskRequest.getQueryFlag().equals(QUERY_DATE_LINK_TASK_FLAG_BY_TIME)) {
            Optional.ofNullable(queryTableLinkTaskRequest.getBeginDate()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_BEGIN_DATE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getEndDate()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_END_DATE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getPage()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_PAGE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkTaskRequest.getCount()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_BACK_TASK_QUERY_COUNT_NULL_ERROR));
            recordCount = metaService.queryTableLinkTaskByDateCount(queryTableLinkTaskRequest.getBeginDate(), queryTableLinkTaskRequest.getEndDate());
        } else {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_QUERY_FLAG_NULL_ERROR);
        }

        return recordCount;
    }

    public String makeInsertSqlColumnsBySource(String tableName, Integer dataSourceType, List<TableColumnEntity> tableColumnList) throws DataStreamException {
        return super.makeSqlInsertColumns(tableName, matchDataBase(dataSourceType).makeSqlIgnore(), tableColumnList);
    }

    public List<TableColumnEntity> getTableColumnsFromLinkSource(DataBaseEntity dataSource, String tableName) throws DataStreamException {
        List<TableColumnEntity> sourceColumnList = linkSourceService.getTableColumns(dataSource.getDataBaseId(), dataSource, tableName);
        if (CollectionUtils.isEmpty(sourceColumnList)) {
            throw new DataStreamException(OPER_TASK_SOURCE_TABLE_NULL_ERROR.getCode(), "SchemaName={" + dataSource.getSchemaName() + "},sourceObjectName={" + tableName + "}获取表字段失败");
        }
        return sourceColumnList;
    }

    public Integer insertDataList(Long dataSourceId, String insertSql) throws DataStreamException {
        return linkSourceService.insertDataList(dataSourceId, insertSql);
    }

    public void operateTableLinkTask(Long linkTaskId, Integer operate) throws DataStreamException {
        switch (operate) {
            case OPERATE_DATE_MOVE_INFO_FLAG_BY_COPY:
                operateTableLinkTaskCopy(linkTaskId);
                break;
            default:
                throw new DataStreamException(DataStreamErrorCode.OPER_OPERATE_INFO_TYPE_ID_ERROR);
        }
    }

    private void operateTableLinkTaskCopy(Long linkTaskId) throws DataStreamException {
        List<TableLinkTaskEntity> tableLinkTaskList = metaService.queryTableLinkTaskByLinkTaskId(linkTaskId);
        if (CollectionUtils.isEmpty(tableLinkTaskList)) {
            throw new DataStreamException(OPER_QUERY_TASK_BY_ID_ERROR);
        }

        if (!Arrays.asList(DATA_STREAM_TASK_STATE_FINISH, DATA_STREAM_TASK_STATE_ERROR, DATA_STREAM_TASK_STATE_STOP).contains(tableLinkTaskList.get(0).getState())) {
            throw new DataStreamException(OPER_TASK_IS_RUN_NOT_COPY_ERROR);
        }


        Long copyTaskId = tableLinkTaskList.get(0).getLinkTaskId();
        tableLinkTaskList.get(0).setLinkTaskId(metaService.querySequence(SEQ_MOVE_TASK_ID));
        tableLinkTaskList.get(0).setState(0);
        tableLinkTaskList.get(0).setTaskDisc((tableLinkTaskList.get(0).getTaskDisc() == null ? "" : tableLinkTaskList.get(0).getTaskDisc()) + "【任务复制生成:" + copyTaskId + "】");

        if (metaService.insertTableLinkTask(tableLinkTaskList.get(0)) != 1) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_CREATE_INSERT_ERROR);
        }
    }
}
