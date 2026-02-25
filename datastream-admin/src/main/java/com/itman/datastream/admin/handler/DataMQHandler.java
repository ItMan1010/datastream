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
import com.itman.datastream.admin.service.IMoveTargetService;
import com.itman.datastream.common.api.ConsumerMessageHandler;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.holder.DataStreamHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;
import static com.itman.datastream.common.utils.CommUtils.*;


@Component
@Slf4j
public class DataMQHandler extends AbstractHandler {
    private final IMetaService metaService;
    private final DataStreamHolder dataStreamHolder;

    private final ConcurrentHashMap<Long, AtomicLong> MQSourceRunTimeRecordCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> MQTargetRunTimeRecordCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, DebeziumTableEntity>> MQTableMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Exception> MQErrorResultMap = new ConcurrentHashMap<>();


    public DataMQHandler(DataSourceFactory dataSourceFactory, DataStreamConfig dataStreamConfig, IMoveTargetService moveTargetService, IMetaService metaService, DataStreamHolder dataStreamHolder) {
        super(dataSourceFactory, dataStreamConfig, metaService, dataStreamHolder, moveTargetService);
        this.metaService = metaService;
        this.dataStreamHolder = dataStreamHolder;
    }

    public void startMQEngine(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        MQConfigEntity sourceMQConfig = dataMoveTask.getSourceMQConfig();
        Integer messageFormat = sourceMQConfig.getMessageFormat();
        String delimiter = sourceMQConfig.getDelimiter();
        Long taskId = dataMoveTask.getTaskId();
        String destination = dataMoveTask.getSourceObjectName();
        String bootstrapServers = sourceMQConfig.getBootstrapServers();
        String groupId = "consumer_task_group_" + taskId;
        Long targetDataSourceId = dataMoveTask.getTargetObjectId();
        DataBaseEntity targetDataBase = dataMoveTask.getTargetDataBase();
        List<ColumnTypeDefineEntity> targetTableColumnTypeDefineList = dataMoveTask.getTargetTableColumnTypeDefineList();

        DataMoveInfoEntity sourceDataMoveInfo = obtainSourceDataMoveInfo(1, dataMoveTask);
        DataMoveInfoEntity targetDataMoveInfo = obtainTargetDataMoveInfo(1, dataMoveTask);

        ConsumerMessageHandler messageHandler = (message, topicName, partition, offset) -> {
            try {
                MQMessageConsumer(dataMoveTask, message);
                MQSourceRunTimeRecordCountMap.computeIfAbsent(taskId, k -> new AtomicLong(0)).incrementAndGet();

                // 如果目标是数据库，更新目标计数
                if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
                    MQTargetRunTimeRecordCountMap.computeIfAbsent(taskId, k -> new AtomicLong(0)).incrementAndGet();
                }

                log.info("**************** linkTaskId:" + taskId + ",Topic: " + topicName + ",Partition: " + partition + ",Offset: " + offset + ",Message: " + message);
            } catch (DataStreamException e) {
                MQErrorResultMap.put(taskId, e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                MQErrorResultMap.put(taskId, e);
                throw e;
            }
        };

        // 停止回调：更新任务状态
        Runnable onStopped = () -> {
            Long sourceInfoId = sourceDataMoveInfo.getInfoId();
            Long targetInfoId = targetDataMoveInfo.getInfoId();
            try {
                if (MQErrorResultMap.containsKey(taskId)) {
                    // 发生错误，更新为 ERROR 状态
                    log.info("MQ 消费者异常停止: linkTaskId={}", taskId);
                 } else {
                    // 正常停止（被手动停止），不更新状态
                    log.info("MQ 消费者正常停止: linkTaskId={}", taskId);
                }
            } catch (Exception e) {
                log.error("更新 MQ 任务状态失败: linkTaskId={}", taskId, e);
            }
        };

        // 绑定消费者（非阻塞）
        matchMQ(dataMoveTask.getSourceObjectType()).bindConsumerDestination(taskId, destination, bootstrapServers, groupId, null, messageHandler, onStopped);

        // 启动监控线程
        startMQMonitorThread(dataMoveTask, sourceDataMoveInfo, targetDataMoveInfo);

        stopSingleEngine(dataMoveTask);
    }

    private void MQMessageConsumer(DataMoveTaskEntity dataMoveTask, String MQMessage) throws DataStreamException {
        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            Long targetDataSourceId = dataMoveTask.getTargetObjectId();
            DataBaseEntity targetDataBase = dataMoveTask.getTargetDataBase();
            List<ColumnTypeDefineEntity> targetTableColumnTypeDefineList = dataMoveTask.getTargetTableColumnTypeDefineList();

            MQMessageEntity MQMessageObject = new MQMessageEntity();
            MQMessageParser(MQMessage, MQMessageObject);
            if (MQMessageObject.getDataType().equals(MQ_MESSAGE_DATA_TYPE_CDC_DML)) {
                CDCMessageEntity CDCMessage = parseJsonToCDCMessageEntity(MQMessageObject.getDataValue());

                DebeziumTableEntity targetDebeziumTable = null;
                if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
                    String targetTableName = CDCMessage.getTable().toLowerCase();

                    targetDebeziumTable = MQTableMap.computeIfAbsent(dataMoveTask.getTaskId(), k -> new ConcurrentHashMap<>()).computeIfAbsent(targetTableName, k -> {
                        DebeziumTableEntity debeziumTable = new DebeziumTableEntity();
                        try {
                            List<TableColumnEntity> targetColumns = moveTargetService.getTableColumns(targetDataSourceId, targetDataBase, targetTableName);
                            super.copyTableColumnsTypeDefine(targetColumns, targetTableColumnTypeDefineList);
                            debeziumTable.setTableColumnsList(targetColumns);
                            debeziumTable.setSqlInsertColumns(makeSqlInsertColumns(targetTableName, "", targetColumns));
                            debeziumTable.setKeyColumnsList(targetColumns.stream().filter(x -> x.isKeyFlag()).map(a -> a.getColumnName()).collect(Collectors.toList()));
                        } catch (DataStreamException e) {
                            throw new RuntimeException(e);
                        }
                        return debeziumTable;
                    });

                    if (targetDebeziumTable == null) {
                        throw new DataStreamException(DataStreamErrorCode.OPER_TASK_TARGET_TABLE_NULL_ERROR);
                    }
                }

                String dmlSql = generateDmlSQL(dataMoveTask, CDCMessage, targetDebeziumTable);
                if (moveTargetService.executeMetaDbSql(targetDataSourceId, dmlSql) != 1) {
                    throw new DataStreamException("executeMetaDbSql", " executeMetaDbSql fail !");
                }
            } else if (MQMessageObject.getDataType().equals(MQ_MESSAGE_DATA_TYPE_MAP)) {
                //todo 解析MAP数据对象
            }
        } else if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            MQMessageEntity MQMessageObject = new MQMessageEntity();
            MQMessageParser(MQMessage, MQMessageObject);
            //实时同步写文件没有意义，只能同步不带字段定义的文件，如果同步多个表字段格式不一样
            //写入文件对象
            Map<String, String> dataMap = new HashMap<String, String>();
            List<Map> dataListTarget = new ArrayList<>();
            dataListTarget.add(dataMap);
            dataMap.put(MQMessageObject.getDataValue(), MQMessageObject.getDataValue());
            writeMapToTargetNoFieldFile(dataListTarget, dataMoveTask);
        } else if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            //写入消息队列
            writeStringObjectToTargetMQ(dataMoveTask.getTaskId(), null, MQMessage, dataMoveTask);
        }
    }

    public void operateMQTaskStop(Long taskId) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = metaService.queryTaskByTaskId(taskId);
        if (!CollectionUtils.isEmpty(dataMoveTaskList)) {
            log.info("operateCdcTaskStop-----linkTaskId=" + taskId);
            stopSingleEngine(dataMoveTaskList.get(0));
        }
    }

    private void stopSingleEngine(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        //todo
        if (MQTableMap.containsKey(dataMoveTask.getTaskId())) {
            MQTableMap.remove(dataMoveTask.getTaskId());
        }

        if (MQSourceRunTimeRecordCountMap.containsKey(dataMoveTask.getTaskId())) {
            MQSourceRunTimeRecordCountMap.remove(dataMoveTask.getTaskId());
        }

        if (MQTargetRunTimeRecordCountMap.containsKey(dataMoveTask.getTaskId())) {
            MQTargetRunTimeRecordCountMap.remove(dataMoveTask.getTaskId());
        }

        if (MQErrorResultMap.containsKey(dataMoveTask.getTaskId())) {
            MQErrorResultMap.remove(dataMoveTask.getTaskId());
        }

        matchMQ(dataMoveTask.getSourceObjectType()).unbindConsumerDestination(dataMoveTask.getTaskId());
    }

    private void startMQMonitorThread(DataMoveTaskEntity dataMoveTask, DataMoveInfoEntity sourceDataMoveInfo, DataMoveInfoEntity targetDataMoveInfo) throws DataStreamException {
        Long sourceInfoId = sourceDataMoveInfo.getInfoId();
        Long targetInfoId = targetDataMoveInfo.getInfoId();
        Long taskId = dataMoveTask.getTaskId();

        Integer lastSourceDataCount = 0;
        Integer lastTargetDataCount = 0;

        while (true) {
            try {
                log.info("startMQMonitorThread--sleep 3000");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("startMQMonitorThread--InterruptedException", e);
                break;
            }

            AtomicLong currentSourceDataCount = MQSourceRunTimeRecordCountMap.get(taskId);
            if (currentSourceDataCount != null) {
                Integer dataCount = currentSourceDataCount.intValue() - lastSourceDataCount;
                if (dataCount > 0) {
                    refreshDataMoveInfoPageRowEnd(sourceInfoId, null, dataCount, dataCount, 0L, 0L, 0L);
                    lastSourceDataCount = currentSourceDataCount.intValue();
                }
            }

            AtomicLong currentTargetDataCount = MQTargetRunTimeRecordCountMap.get(taskId);
            if (currentTargetDataCount != null) {
                Integer dataCount = currentTargetDataCount.intValue() - lastTargetDataCount;
                if (dataCount > 0) {
                    refreshDataMoveInfoPageRowEnd(targetInfoId, null, dataCount, dataCount, 0L, 0L, 0L);
                    lastTargetDataCount = currentTargetDataCount.intValue();
                }
            }

            if (MQErrorResultMap.containsKey(taskId)) {
                log.info("startMQMonitorThread---MQErrorResultMap is error ---break");
                String errorCode = null;
                String errorMessage = null;
                Exception debeiziumException = MQErrorResultMap.get(dataMoveTask.getTaskId());
                if (debeiziumException instanceof DataStreamException) {
                    errorCode = ((DataStreamException) debeiziumException).getErrCode();
                    errorMessage = ((DataStreamException) debeiziumException).getErrMsg();
                } else if (debeiziumException instanceof Exception) {
                    errorCode = UNKNOWN_ERROR.getCode();
                    errorMessage = debeiziumException.getMessage();
                }

                metaService.updateDataMoveInfoErrorInfo(sourceInfoId, DATA_STREAM_TASK_STATE_RUNNING, DATA_STREAM_TASK_STATE_ERROR, errorCode, errorMessage);
                metaService.updateDataMoveInfoErrorInfo(targetInfoId, DATA_STREAM_TASK_STATE_RUNNING, DATA_STREAM_TASK_STATE_ERROR, errorCode, errorMessage);

                throw new DataStreamException(errorCode, errorMessage);
            }
        }
    }
}
