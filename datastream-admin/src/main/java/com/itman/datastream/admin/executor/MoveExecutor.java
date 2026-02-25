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
package com.itman.datastream.admin.executor;

import cn.hutool.extra.spring.SpringUtil;
import com.itman.datastream.admin.handler.*;
import com.itman.datastream.admin.service.*;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.route.RegisterDataBase;
import com.itman.datastream.engine.systemlog.ISystemLogEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;
import static com.itman.datastream.common.utils.CommUtils.*;
import static com.itman.datastream.engine.context.ThreadContext.removeThreadLocalJob;
import static com.itman.datastream.engine.context.ThreadContext.setThreadLocalJob;

@Slf4j
@Component
@AllArgsConstructor
public class MoveExecutor {
    private final IMetaService metaService;
    private final IMoveSourceService moveSourceService;
    private final IMoveTargetService moveTargetService;
    private final DataMoveHandler dataMoveHandler;
    private final ISystemLogEvent systemLogEvent;
    private final TableInfoHandler tableInfoHandler;
    private final RegisterDataBase registerDataBase;
    private final IFileService fileService;
    private final DataBaseHandler dataBaseHandler;
    private final DataCdcHandler dataCdcHandler;
    private final IMQConfigService mqConfigService;
    private final DataMQHandler dataMQHandler;
    private final DataStreamConfig dataStreamConfig;


    private void jobLogbackEventMoveTask(Long taskId, String content) {
        systemLogEvent.jobLogbackEvent(JOB_TYPE_MOVE_TASK, taskId, content);
    }

    private void registerDataBase(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        List<DataBaseEntity> dataBaseList = new ArrayList<>();
        if (isDataBaseDataSource(dataMoveTask.getSourceObjectType())) {
            dataBaseList.add(dataMoveTask.getSourceDataBase());
            dataMoveTask.setSourceTableColumnTypeDefineList(dataBaseHandler.queryColumnTypeDefine(dataMoveTask.getSourceDataBase().getDataBaseType()));
        }

        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            dataBaseList.add(dataMoveTask.getTargetDataBase());
            dataMoveTask.setTargetTableColumnTypeDefineList(dataBaseHandler.queryColumnTypeDefine(dataMoveTask.getTargetDataBase().getDataBaseType()));
        }

        if (!CollectionUtils.isEmpty(dataBaseList)) {
            registerDataBase.registerDataSources(dataBaseList, dataMoveTask.getTaskId());
        }
    }

    @Async(TASK_WORKS_POOL_EXECUTOR)
    public void dispatchTaskProcess(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        dataMoveHandler.setTaskRunning(dataMoveTask.getTaskId(), dataMoveTask.getTaskType());

        String errorCode = null;
        String errorMessage = null;
        jobLogbackEventMoveTask(dataMoveTask.getTaskId(), "dispatchTaskProcess start");
        setThreadLocalJob(dataMoveTask.getTaskId(), JOB_TYPE_MOVE_TASK);

        try {
            registerDataBase(dataMoveTask);

            dataMoveHandler.loadTaskExtendParameters(dataMoveTask);

            //todo 后续需要抽象扩展能力，如果再增加其他类型怎么扩展
            if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_TABLE_MOVE)) {
                //表结构迁移
                tableMoveTaskProcess(dataMoveTask);
            } else if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_CDC)) {
                //数据增量迁移
                cdcMoveTaskProcess(dataMoveTask);
            } else {
                //数据批量迁移：源数据对象->目标数据对象
                dataMoveTaskProcess(dataMoveTask);
            }
        } catch (DataStreamException aie) {
            log.error("loadDataMoveTaskNotice: SystemException={}", aie);
            errorCode = aie.getErrCode();
            errorMessage = aie.getMessage();
            jobLogbackEventMoveTask(dataMoveTask.getTaskId(), getStackTraceAsString(aie));
        } catch (Exception e) {
            log.error("loadDataMoveTaskNotice: Exception={}", e);
            errorCode = DataStreamErrorCode.UNKNOWN_ERROR.getCode();
            errorMessage = DataStreamErrorCode.UNKNOWN_ERROR.getMessage();
            jobLogbackEventMoveTask(dataMoveTask.getTaskId(), getStackTraceAsString(e));
        }

//        List<DataMoveInfoEntity> dataSourceMoveInfoList = metaService.queryDataMoveInfo(dataMoveTask.getLinkTaskId(), MOVE_INFO_FLAG_SOURCE);
//        if (!CollectionUtils.isEmpty(dataSourceMoveInfoList) && dataSourceMoveInfoList.stream().anyMatch(x -> !x.getState().equals(DATA_STREAM_TASK_STATE_FINISH))) {
//            errorCode = DataStreamErrorCode.OPER_DATA_MOVE_TASK_THREAD_FAIL_ERROR.getCode();
//            errorMessage = DataStreamErrorCode.OPER_DATA_MOVE_TASK_THREAD_FAIL_ERROR.getMessage();
//        }
//
//        List<DataMoveInfoEntity> dataTargetMoveInfoList = metaService.queryDataMoveInfo(dataMoveTask.getLinkTaskId(), MOVE_INFO_FLAG_TARGET);
//        if (!CollectionUtils.isEmpty(dataTargetMoveInfoList) && dataTargetMoveInfoList.stream().anyMatch(x -> !x.getState().equals(DATA_STREAM_TASK_STATE_FINISH))) {
//            errorCode = OPER_MOVE_INFO_INSERT_FAIL_ERROR.getCode();
//            errorMessage = OPER_MOVE_INFO_INSERT_FAIL_ERROR.getMessage();
//        }

        if (!metaService.updateDataMoveTaskErrorInfo(dataMoveTask.getTaskId(), dataMoveTask.getTaskExecuteId(), DATA_STREAM_TASK_STATE_RUNNING, Objects.isNull(errorCode) ? DATA_STREAM_TASK_STATE_FINISH : DATA_STREAM_TASK_STATE_ERROR, errorCode, errorMessage).equals(1)) {
            log.error("updateDataMoveTaskErrorInfo update state finish error, linkTaskId={}", dataMoveTask.getTaskId());
        }

        if (Objects.isNull(errorCode)) {
            genDataCheckReserveTaskInstance(dataMoveTask);
        }

        jobLogbackEventMoveTask(dataMoveTask.getTaskId(), "dispatchTaskProcess end");

        removeThreadLocalJob();

        registerDataBase.releaseTaskDataSources(dataMoveTask.getTaskId());

        dataMoveHandler.clearTaskRunning(dataMoveTask.getTaskId());
    }

    void genDataCheckReserveTaskInstance(DataMoveTaskEntity dataMoveTask) {
        try {
            if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_CHECK) && !dataMoveTask.getTaskDisc().contains("genFromDataCheckId") && (dataMoveTask.getCheckMode() != null && dataMoveTask.getCheckMode().equals(DATA_CHECK_MODE_BIDIRECTIONAL))) {
                CreateTaskInstanceEntity createTaskInstance = new CreateTaskInstanceEntity();
                BeanUtils.copyProperties(dataMoveTask, createTaskInstance);
                createTaskInstance.setSourceObjectId(dataMoveTask.getTargetObjectId());
                createTaskInstance.setTargetObjectId(dataMoveTask.getSourceObjectId());
                createTaskInstance.setTaskDisc("genFromDataCheckId[" + dataMoveTask.getTaskId() + "]");
                createTaskInstance.setCheckMode(DATA_CHECK_MODE_FORWARD);

                dataMoveHandler.createTaskInstance(createTaskInstance);
            }
        } catch (DataStreamException e) {
            log.error("Exception={}", e);
        }
    }

    public void tableMoveTaskProcess(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        List<MoveTableEntity> moveTableList = metaService.queryMoveTable(dataMoveTask.getTaskId());
        List<MoveTableEntity> moveTableStateInitList = moveTableList.stream().filter(x -> {
            return x.getState().equals(DATA_STREAM_TASK_STATE_INIT) || x.getState().equals(DATA_STREAM_TASK_STATE_ERROR);
        }).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(moveTableStateInitList)) {
            for (MoveTableEntity iterator : moveTableStateInitList) {
                moveTableInfoToTarget(dataMoveTask, iterator);
            }
        }
    }

    public void cdcMoveTaskProcess(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        //source
        if (isMQDataSource(dataMoveTask.getSourceObjectType())) {
            MQConfigEntity mqConfigEntity = mqConfigService.getConfigById(dataMoveTask.getSourceObjectId());
            dataMoveTask.setSourceMQConfig(mqConfigEntity);
        }

        //target
        if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            //目标对象可能是文件对象
            FileFormatEntity fileFormat = fileService.makeFileObject(dataMoveTask.getTargetObjectId());
            dataMoveTask.setTargetFileFormat(fileFormat);
        } else if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            MQConfigEntity mqConfigEntity = mqConfigService.getConfigById(dataMoveTask.getTargetObjectId());
            dataMoveTask.setTargetMQConfig(mqConfigEntity);
        }

        if (isDataBaseDataSource(dataMoveTask.getSourceObjectType())) {
            dataCdcHandler.startDebeziumEngine(dataMoveTask);
        } else if (isMQDataSource(dataMoveTask.getSourceObjectType())) {
            dataMQHandler.startMQEngine(dataMoveTask);
        }
    }

    public void moveTableInfoToTarget(DataMoveTaskEntity dataMoveTask, MoveTableEntity moveTable) throws DataStreamException {
        String errorCode = null;
        String errorMsg = null;
        setThreadLocalJob(moveTable.getMoveTableId(), JOB_TYPE_MOVE_TABLE);

        try {
            //获取源表字段信息
            TableInfoEntity tableInfo = moveSourceService.fetchTableMetadata(dataMoveTask.getSourceDataBase().getDataBaseId(), dataMoveTask.getSourceDataBase(), moveTable.getSourceTableName());
            if (tableInfo == null || CollectionUtils.isEmpty(tableInfo.getColumns())) {
                throw new DataStreamException(DataStreamErrorCode.OPER_GET_TABLE_STRUCT_INFO_ERROR);
            }

            List<String> createTableSqlList = tableInfoHandler.buildCreateTableSql(tableInfo, moveTable.getSourceTableName(), dataMoveTask.getSourceDataBase(), dataMoveTask.getTargetDataBase());
            StringBuilder createTableSql = new StringBuilder();
            for (String iterator : createTableSqlList) {
                createTableSql.append(iterator).append("\n");
            }
            metaService.updateMoveTableSQL(moveTable.getMoveTableId(), createTableSql.toString());

            for (String iterator : createTableSqlList) {
                log.info("--------------------createTableSql={}", iterator);
                moveTargetService.executeMetaDbSql(dataMoveTask.getTargetObjectId(), iterator);
            }
        } catch (DataStreamException aie) {
            errorCode = aie.getErrCode();
            errorMsg = aie.getErrMsg();
            dataMoveHandler.jobLogbackEventTableInfo(moveTable.getMoveTableId(), getStackTraceAsString(aie));
        } catch (Exception e) {
            errorCode = UNKNOWN_ERROR.getCode();
            errorMsg = UNKNOWN_ERROR.getMessage();
            dataMoveHandler.jobLogbackEventTableInfo(moveTable.getMoveTableId(), getStackTraceAsString(e));
        }


        metaService.updateMoveTableErrorInfo(moveTable.getMoveTableId(), moveTable.getState(), errorCode == null ? DATA_STREAM_TASK_STATE_FINISH : DATA_STREAM_TASK_STATE_ERROR, errorCode, errorMsg);

        //只要有一个执行错误，整个任务就是失败，然后可以重处理
        if (errorCode != null) {
            throw new DataStreamException(errorCode, errorMsg);
        }
    }

    public void moveDataByDataNode(Integer step, String dataNodeName, DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        if (dataNodeName.equals("null")) {
            dataMoveTask.setSourceSelectSql(dataMoveTask.getSourceSelectSqlColumns());
        } else {
            if (dataMoveTask.getSourceDataSet() != null) {
                dataMoveTask.setSourceSelectSql(String.format(SQL_FORMAT_HINT_BALANCE_DATANODE, dataMoveTask.getSourceDataSet(), dataNodeName) + dataMoveTask.getSourceSelectSqlColumns());
            } else {
                dataMoveTask.setSourceSelectSql(String.format(SQL_FORMAT_HINT_DATANODE, dataNodeName) + dataMoveTask.getSourceSelectSqlColumns());
            }
        }

        DataMoveTaskEntity dataMoveTaskThread = new DataMoveTaskEntity();
        BeanUtils.copyProperties(dataMoveTask, dataMoveTaskThread);
        dataMoveTaskThread.setSourceDataNode(dataNodeName);

        // 获取断点信息
        DataMoveInfoEntity dataMoveInfoBreak = metaService.queryDataMoveInfo(dataMoveTaskThread.getTaskId(), MOVE_INFO_FLAG_SOURCE).stream()
                // 1. 过滤出指定数据节点的信息
                .filter(x -> dataNodeName.equals(x.getDataNode()))
                // 2. 使用max方法找出PageLoopCount最大的元素
                .max(Comparator.comparingInt(DataMoveInfoEntity::getPageLoopCount))
                // 3. 如果找不到符合条件的元素，max返回Optional.empty，此时orElse返回null
                .orElse(null);

        if (dataMoveInfoBreak != null) {
            dataMoveHandler.fetchAndUpdateNextSegmentValueForBreak(dataMoveInfoBreak, dataMoveTaskThread);
        } else {
            String currentValue = dataMoveTask.getTaskId() + "_init";
            dataMoveHandler.initCurrentValueLock(dataMoveTask.getTaskId(), currentValue, 0);
        }

        if (dataMoveTask.getSourceLoadStrategy().equals(LOAD_STRATEGY_BY_DATA_PART)) {
            //判断有唯一主键且通道1:1进行分发，进行数据分区分割:总数/通道数的取整
            dataMoveHandler.splitDataRangeForParallelProcessing(dataMoveTaskThread);
        }

        startThreadProcessor(dataMoveTaskThread);

        sleepWait(500, step, dataNodeName);
    }

    private void startThreadProcessor(final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        if (log.isDebugEnabled()) {
            log.debug("startThreadProcessor sourceFuture linkTaskId={},SourceThreadCount={}", dataMoveTask.getTaskId(), dataMoveTask.getSourcePropertiesThreadCount());
        }

        //初始化资源
        dataMoveHandler.initTaskResource(dataMoveTask);

        CompletableFuture<Void>[] sourceFuture = new CompletableFuture[dataMoveTask.getSourcePropertiesThreadCount()];
        for (int i = 0; i < dataMoveTask.getSourcePropertiesThreadCount(); i++) {
            final int threadId = i + 1;
            sourceFuture[i] = CompletableFuture.runAsync(() -> {
                dataMoveHandler.dataProducer(threadId, dataMoveTask);
            }, SpringUtil.getBean(SOURCE_WORKS_POOL_EXECUTOR));
        }

        if (log.isDebugEnabled()) {
            log.debug("startThreadProcessor targetFuture linkTaskId={},TargetThreadCount={}", dataMoveTask.getTaskId(), dataMoveTask.getTargetPropertiesThreadCount());
        }

        CompletableFuture<Void>[] targetFuture = new CompletableFuture[dataMoveTask.getTargetPropertiesThreadCount()];
        for (int i = 0; i < dataMoveTask.getTargetPropertiesThreadCount(); i++) {
            final int threadId = i + 1;
            targetFuture[i] = CompletableFuture.runAsync(() -> {
                dataMoveHandler.dataConsumer(threadId, dataMoveTask);
            }, SpringUtil.getBean(TARGET_WORKS_POOL_EXECUTOR));
        }

        //启动主线程做数据异步同步工作
        if (dataStreamConfig.getDataStreamMoveInfoAsyncEnable()) {
            log.info("dataStreamMoveInfoAsyncEnable linkTaskId={}", dataMoveTask.getTaskId());

            dataMoveHandler.syncDataMoveInfoRunning(dataMoveTask.getTaskId());
        }

        if (log.isDebugEnabled()) {
            log.debug("startThreadProcessor allOfSourceFuture linkTaskId={}", dataMoveTask.getTaskId());
        }
        CompletableFuture<Void> allOfSourceFuture = CompletableFuture.allOf(sourceFuture);
        try {
            allOfSourceFuture.join();
        } catch (Exception e) {
            log.error("Error occurred while waiting for dataProducerHandler to complete", e);
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_SOURCE_THREAD_AWAIT_ERROR);
        }
        dataMoveHandler.sourceSelectFinishLock(dataMoveTask.getTaskId());

        if (log.isDebugEnabled()) {
            log.debug("startThreadProcessor allOfSourceFuture2 linkTaskId={}", dataMoveTask.getTaskId());
        }

        CompletableFuture<Void> allOfTargetFuture = CompletableFuture.allOf(targetFuture);
        try {
            allOfTargetFuture.join();
        } catch (Exception e) {
            log.error("Error occurred while waiting for dataConsumerHandler to complete", e);
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_TARGET_THREAD_AWAIT_ERROR);
        }

        if (log.isDebugEnabled()) {
            log.debug("startThreadProcessor finish linkTaskId={}", dataMoveTask.getTaskId());
        }

        //统一释放资源
        dataMoveHandler.releaseTaskResource(dataMoveTask);
    }

    private void sleepWait(long time, Integer step, String dataNodeName) throws DataStreamException {
        try {
            log.info("canalMoveData -------------------nodeName ={}, step={},sleep for a while", dataNodeName, step);
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("canalMoveData -------------------sleep InterruptedException=", e);
            Thread.currentThread().interrupt();
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_THREAD_SLEEP_ERROR);
        }
    }

    private void checkTaskType(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        if (!Arrays.asList(DATA_STREAM_TASK_TYPE_DATA_MOVE, DATA_STREAM_TASK_TYPE_DATA_DEL, DATA_STREAM_TASK_TYPE_DATA_MOVE_DEL, DATA_STREAM_TASK_TYPE_DATA_CHECK).contains(dataMoveTask.getTaskType())) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_TYPE_NOT_EQUAL_ERROR);
        }
    }

    private void sourceTablePrepare(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        //统计数据库源端的记录数,如果断点续作没必要再统计
        if (dataMoveTask.getSourceObjectCount() == null || dataMoveTask.getSourceObjectCount().equals(0)) {
            long sourceTableRecordCount = moveSourceService.executeSelectRecordCountSql(dataMoveTask.getSourceDataBase().getDataBaseId(), dataMoveHandler.makeSourceDataSelectCountSql(dataMoveTask));
            if (sourceTableRecordCount == 0L) {
                throw new DataStreamException(DataStreamErrorCode.OPER_TASK_NO_SOURCE_DATA_ERROR);
            }

            String sourceKeysBegin = null;
            String sourceKeysEnd = null;
            if (dataMoveTask.getSourceLoadStrategy().equals(LOAD_STRATEGY_BY_DATA_PART)) {
                dataMoveTask.setSourceDataNode("null");
                if (dataMoveTask.getSourceTableKeysList().size() == 1) {
                    Map<String, String> minMaxValueMap = moveSourceService.executeSelectMapMaxMinSql(dataMoveTask.getSourceObjectId(), makeSourceSelectMaxMinSql(dataMoveTask));
                    sourceKeysBegin = (minMaxValueMap != null) ? minMaxValueMap.get("min_value") : null;
                    sourceKeysEnd = (minMaxValueMap != null) ? minMaxValueMap.get("max_value") : null;
                } else {
                    sourceKeysBegin = getSourceSelectMaxMinValue("asc", dataMoveTask);
                    sourceKeysEnd = getSourceSelectMaxMinValue("desc", dataMoveTask);
                }

                dataMoveTask.setSourceKeysBegin(sourceKeysBegin);
                dataMoveTask.setSourceKeysEnd(sourceKeysEnd);
            }

            String sourceTableKeys = dataMoveTask.getSourceTableKeysList().stream().map(TableColumnEntity::getColumnName).collect(Collectors.joining(","));
            metaService.updateDataMoveTaskTableCountMinMaxValue(dataMoveTask.getTaskId(), sourceTableRecordCount, sourceTableKeys, sourceKeysBegin, sourceKeysEnd);
            dataMoveTask.setSourceObjectCount(sourceTableRecordCount);
            dataMoveTask.setSourceObjectKeys(sourceTableKeys);
        }
    }


    public void dataMoveTaskProcess(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        checkTaskType(dataMoveTask);

        //ToDo 后续继续下沉抽象
        //-----------源端对象处理--begin-------------
        List<String> sourceTableNodeNameList = null;
        if (isDataBaseDataSource(dataMoveTask.getSourceObjectType())) {
            List<TableColumnEntity> sourceTableColumnAllList = moveSourceService.getTableColumns(dataMoveTask.getSourceDataBase().getDataBaseId(), dataMoveTask.getSourceDataBase(), dataMoveTask.getSourceObjectName());
            List<TableColumnEntity> sourceTableColumnList = sourceTableColumnAllList.stream().filter(column -> !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID) && !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_BACK_COUNT) && !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_BACK_TASK_ID)).collect(Collectors.toList());
            dataMoveTask.setSourceTableColumns(sourceTableColumnList);
            if (CollectionUtils.isEmpty(dataMoveTask.getSourceTableColumns())) {
                throw new DataStreamException(DataStreamErrorCode.OPER_TASK_SOURCE_TABLE_NULL_ERROR);
            }

            dataMoveTask.setSourceKeyColumns(dataMoveTask.getSourceTableColumns().stream().filter(x -> x.isKeyFlag()).map(a -> a.getColumnName()).collect(Collectors.toList()));
            dataMoveTask.setSourceTableKeysList(dataMoveTask.getSourceTableColumns().stream().filter(x -> x.isKeyFlag()).collect(Collectors.toList()));
            setSourceLoadStrategy(dataMoveTask, sourceTableColumnAllList);
            sourceTablePrepare(dataMoveTask);
            dataBaseHandler.copyTableColumnsTypeDefine(dataMoveTask.getSourceTableColumns(), dataMoveTask.getSourceTableColumnTypeDefineList());
            dataMoveTask.setSourceSelectSqlColumns(dataMoveHandler.makeSourceSelectSqlColumns(dataMoveTask));
            //按数据分片处理
            sourceTableNodeNameList = getListOfDataNodeNames(dataMoveTask.getSourceDataBase().getDataBaseId(), dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceDataBase().getDataBaseType(), dataMoveTask.getSourceDataNode());
        } else if (isFileDataSource(dataMoveTask.getSourceObjectType())) {
            //源文件处理
            FileFormatEntity fileFormat = fileService.makeFileObject(dataMoveTask.getSourceObjectId());
            dataMoveTask.setSourceFileFormat(fileFormat);

            Long sourceFileLineCount = dataMoveHandler.statFileLineCount(dataMoveTask.getSourceObjectType(), dataMoveTask.getSourceObjectName(), fileFormat.getLocalPath());
            metaService.updateDataMoveTaskTableCountMinMaxValue(dataMoveTask.getTaskId(), sourceFileLineCount, null, null, null);
            dataMoveTask.setSourceObjectCount(sourceFileLineCount);
        }
        //-----------源端对象处理--end-------------

        //----------目标端对象处理--begin----------
        String targetTableCountSql = null;
        Long targetTableCount = 0L;
        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            dataMoveTask.setTargetTableColumns(moveTargetService.getTableColumns(dataMoveTask.getTargetDataBase().getDataBaseId(), dataMoveTask.getTargetDataBase(), dataMoveTask.getTargetObjectName()));
            if (CollectionUtils.isEmpty(dataMoveTask.getTargetTableColumns())) {
                throw new DataStreamException(DataStreamErrorCode.OPER_TASK_TARGET_TABLE_NULL_ERROR);
            }

            dataMoveTask.setTargetKeyColumns(dataMoveTask.getTargetTableColumns().stream().filter(x -> x.isKeyFlag()).map(a -> a.getColumnName()).collect(Collectors.toList()));
            dataMoveTask.setTargetTableKeysList(dataMoveTask.getTargetTableColumns().stream().filter(x -> x.isKeyFlag()).collect(Collectors.toList()));
            dataMoveTask.setTargetInsertSqlColumns(dataMoveHandler.makeTargetInsertSqlColumns(dataMoveTask));
            //记录数据迁移前目标表记录数,如果断点重做没必要再统计
            targetTableCountSql = dataMoveHandler.makeTargetDataSelectCountSql(dataMoveTask);
            if (dataMoveTask.getTargetObjectBeginCount() == null || dataMoveTask.getTargetObjectBeginCount().equals(0)) {
                targetTableCount = moveTargetService.executeSelectRecordCountSql(dataMoveTask.getTargetObjectId(), targetTableCountSql);
                metaService.updateDataMoveTaskTableCount(dataMoveTask.getTaskId(), UPDATE_DATA_MOVE_TASK_COLUMN_TARGET_BEGIN_COUNT, targetTableCount);
            }
            dataMoveHandler.judgeTargetDataMoveInfoByInfoId(dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectId());

            dataBaseHandler.copyTableColumnsTypeDefine(dataMoveTask.getTargetTableColumns(), dataMoveTask.getTargetTableColumnTypeDefineList());
        } else if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            FileFormatEntity fileFormat = fileService.makeFileObject(dataMoveTask.getTargetObjectId());
            if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList()) && !CollectionUtils.isEmpty(fileFormat.getFileBody().getFileFieldList())) {
                for (FileSpecialEntity iteratorFileSpecial : fileFormat.getFileSpecialList()) {
                    for (FileFieldEntity iteratorFileSpecialField : iteratorFileSpecial.getFileFieldList()) {
                        if (iteratorFileSpecialField.getBelongFlag().equals(1) && iteratorFileSpecialField.getSumFieldName() != null) {
                            for (FileFieldEntity iteratorFileBodyField : fileFormat.getFileBody().getFileFieldList()) {
                                if (iteratorFileBodyField.getFieldName().equalsIgnoreCase(iteratorFileSpecialField.getSumFieldName())) {
                                    iteratorFileBodyField.setSumFieldName(iteratorFileSpecialField.getSumFieldName());
                                }
                            }
                        }
                    }
                }
            }
            dataMoveTask.setTargetFileFormat(fileFormat);
        } else if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            MQConfigEntity mqConfigEntity = mqConfigService.getConfigById(dataMoveTask.getTargetObjectId());
            dataMoveTask.setTargetMQConfig(mqConfigEntity);
        }
        //----------目标端对象处理--end----------

        //来源和目标综合判断
        if (isDataBaseDataSource(dataMoveTask.getSourceObjectType()) && isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            checkSourceAndTargetTableColumn(dataMoveTask.getSourceTableColumns(), dataMoveTask.getTargetTableColumns());
            validateSourceAndTargetKeyColumnsMatch(dataMoveTask.getTaskType(), dataMoveTask.getSourceKeyColumns(), dataMoveTask.getSourceDataBase(), dataMoveTask.getTargetKeyColumns(), dataMoveTask.getTargetDataBase());
        }

        List<DataMoveInfoEntity> dataMoveInfoIngList = metaService.queryDataMoveInfo(dataMoveTask.getTaskId(), MOVE_INFO_FLAG_SOURCE);
        boolean hasStopState = dataMoveInfoIngList.stream().anyMatch(x -> x.getState().equals(DATA_STREAM_TASK_STATE_STOP));
        if (!hasStopState) {
            if (CollectionUtils.isEmpty(sourceTableNodeNameList)) {
                moveDataByDataNode(1, "null", dataMoveTask);
            } else {
                //如果存在多节点数据分片
                for (int i = 0; i < sourceTableNodeNameList.size(); i++) {
                    String dataNodeName = sourceTableNodeNameList.get(i);
                    if (!CollectionUtils.isEmpty(dataMoveInfoIngList) && dataMoveInfoIngList.stream().filter(x -> x.getDataNode().equals(dataNodeName)).allMatch(x -> x.getState().equals(DATA_STREAM_TASK_STATE_FINISH))) {
                        continue;
                    }
                    moveDataByDataNode((i + 1), dataNodeName, dataMoveTask);
                }
            }
        }

        //统计目标端数据库记录数
        targetTableCount = 0L;
        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            targetTableCount = moveTargetService.executeSelectRecordCountSql(dataMoveTask.getTargetObjectId(), targetTableCountSql);
        }
        metaService.updateDataMoveTaskTableCount(dataMoveTask.getTaskId(), UPDATE_DATA_MOVE_TASK_COLUMN_TARGET_END_COUNT, targetTableCount);
    }


    private String getSourceSelectMaxMinValue(String orderBy, DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        List<Map> keyRowMapList = moveSourceService.executeSelectMapListSql(dataMoveTask.getSourceObjectId(), makeSourceSelectMaxMinSql(orderBy, dataMoveTask));
        if (keyRowMapList == null || keyRowMapList.isEmpty()) {
            return null;
        }

        return dataMoveHandler.generateKeyValuesString(keyRowMapList.get(0), dataMoveTask.getSourceTableKeysList());
    }

    public void setSourceLoadStrategy(DataMoveTaskEntity dataMoveTask, List<TableColumnEntity> sourceTableColumns) throws DataStreamException {
        if (Arrays.asList(DATA_STREAM_TASK_TYPE_DATA_DEL, DATA_STREAM_TASK_TYPE_DATA_MOVE_DEL).contains(dataMoveTask.getTaskType())) {
            if (CollectionUtils.isEmpty(dataMoveTask.getSourceKeyColumns())) {
                throw new DataStreamException(DataStreamErrorCode.OPER_TASK_TYPE_BY_PART_KEY_ERROR);
            }

            dataMoveTask.setSourceLoadStrategy(LOAD_STRATEGY_BY_DATA_PART);
            metaService.updateDataMoveTaskLoadStrategy(dataMoveTask.getTaskId(), dataMoveTask.getSourceLoadStrategy());
        } else if (Arrays.asList(DATA_STREAM_TASK_TYPE_DATA_MOVE, DATA_STREAM_TASK_TYPE_DATA_CHECK).contains(dataMoveTask.getTaskType()) && !CollectionUtils.isEmpty(dataMoveTask.getSourceKeyColumns())) {
            dataMoveTask.setSourceLoadStrategy(LOAD_STRATEGY_BY_DATA_PART);
            metaService.updateDataMoveTaskLoadStrategy(dataMoveTask.getTaskId(), dataMoveTask.getSourceLoadStrategy());
        }
    }

    public void checkSourceAndTargetTableColumn(List<TableColumnEntity> sourceTableColumns, List<TableColumnEntity> targetTableColumns) throws DataStreamException {
        if (sourceTableColumns.isEmpty()) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_CREATE_INSERT_ERROR);
        }
        if (log.isDebugEnabled()) {
            log.debug("checkSourceAndTargetTableColumn-sourceTableColumns:{}", sourceTableColumns);
        }

        if (targetTableColumns.isEmpty()) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TARGET_COLUMNS_IS_NULL_ERROR);
        }

        if (log.isDebugEnabled()) {
            log.debug("checkSourceAndTargetTableColumn-targetTableColumns:{}", targetTableColumns);
        }

        Map<String, String> targetTableColumnsNameMap = new HashMap<>();
        if (!targetTableColumns.isEmpty()) {
            targetTableColumnsNameMap.putAll(targetTableColumns.stream().collect(Collectors.toMap(TableColumnEntity::getColumnName, TableColumnEntity::getColumnName)));
        }

        //查询目标端表字段进行稽核
        for (TableColumnEntity iterator : sourceTableColumns) {
            if (!targetTableColumnsNameMap.containsKey(iterator.getColumnName())) {
                log.error("checkSourceAndTargetTableColumn.sourceTableColumns.ColumnName={} more", iterator.getColumnName());
                throw new DataStreamException(DataStreamErrorCode.OPER_TASK_COLUMNS_NOT_EQUAL_ERROR);
            }
        }
    }

    public void validateSourceAndTargetKeyColumnsMatch(Integer taskType, List<String> sourceKeyColumns, DataBaseEntity sourceDataSource, List<String> targetKeyColumns, DataBaseEntity targetDataSource) throws DataStreamException {
        if (sourceKeyColumns.isEmpty() && sourceDataSource.getTableKeyNotSupported() != null && sourceDataSource.getTableKeyNotSupported().equals(1)) {
            return;
        }

        if (Arrays.asList(DATA_STREAM_TASK_TYPE_DATA_DEL, DATA_STREAM_TASK_TYPE_DATA_MOVE_DEL).contains(taskType) && sourceKeyColumns.isEmpty()) {
            throw new DataStreamException(DataStreamErrorCode.OPER_SOURCE_KEY_COLUMNS_IS_NULL_ERROR);
        }

        if (targetKeyColumns.isEmpty() && targetDataSource.getTableKeyNotSupported() != null && targetDataSource.getTableKeyNotSupported().equals(1)) {
            return;
        }

        if (targetKeyColumns.isEmpty()) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TARGET_KEY_COLUMNS_IS_NULL_ERROR);
        }
    }

    List<String> getListOfDataNodeNames(Long dataSourceId, String sourceTableName, Integer dataSourceType, String defaultDataNode) throws DataStreamException {
        List<String> nodeNameList = new ArrayList<>();
        if (DATA_SOURCE_TYPE_SHARDING.equals(dataSourceType)) {
            if (defaultDataNode != null) {
                nodeNameList.add(defaultDataNode);
            } else {
                List<String> nodeNameListAll = moveSourceService.queryDataNodesByTableName(dataSourceId, sourceTableName);
                nodeNameList.addAll(nodeNameListAll);
            }
        } else {
            nodeNameList.add("null");
        }
        return nodeNameList;
    }

    public String makeSourceSelectMaxMinSql(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        String maxMinSql = dataMoveHandler.makeSqlMinMax(dataMoveTask.getSourceDataBase().getDataBaseType(), dataMoveTask.getSourceKeyColumns().get(0), dataMoveTask.getSourceObjectName());
        if (!Strings.isEmpty(dataMoveTask.getSourceObjectCondition())) {
            maxMinSql = maxMinSql + " where " + dataMoveTask.getSourceObjectCondition();
        }

        if (dataMoveTask.getSourceDataNode().equals("null")) {
            return maxMinSql;
        } else {
            if (dataMoveTask.getSourceDataSet() != null) {
                return String.format(SQL_FORMAT_HINT_BALANCE_DATANODE, dataMoveTask.getSourceDataSet(), dataMoveTask.getSourceDataNode()) + maxMinSql;
            } else {
                return String.format(SQL_FORMAT_HINT_DATANODE, dataMoveTask.getSourceDataNode()) + maxMinSql;
            }
        }
    }

    public String makeSourceSelectMaxMinSql(String orderBy, DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        String selectKeys = dataMoveTask.getSourceTableKeysList().stream().map(TableColumnEntity::getColumnName).collect(Collectors.joining(","));
        String orderByKeys = null;
        if (orderBy.equals("asc")) {
            orderByKeys = selectKeys;
        } else if (orderBy.equals("desc")) {
            orderByKeys = dataMoveTask.getSourceTableKeysList().stream().map(column -> column.getColumnName() + " DESC").collect(Collectors.joining(","));
        }

        String minSql = "SELECT " + selectKeys + " FROM " + dataMoveTask.getSourceObjectName();
        if (!Strings.isEmpty(dataMoveTask.getSourceObjectCondition())) {
            minSql = minSql + " WHERE " + dataMoveTask.getSourceObjectCondition();
        }

        minSql = minSql + " ORDER BY " + orderByKeys + " LIMIT 1";

        if (dataMoveTask.getSourceDataNode().equals("null")) {
            return minSql;
        } else {
            if (dataMoveTask.getSourceDataSet() != null) {
                return String.format(SQL_FORMAT_HINT_BALANCE_DATANODE, dataMoveTask.getSourceDataSet(), dataMoveTask.getSourceDataNode()) + minSql;
            } else {
                return String.format(SQL_FORMAT_HINT_DATANODE, dataMoveTask.getSourceDataNode()) + minSql;
            }
        }
    }

    public Integer getTaskRunningSize() {
        return dataMoveHandler.getTaskRunningSize();
    }
}
