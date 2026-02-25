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
package com.itman.datastream.admin.service;

import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;

public interface IMetaService {
    List<DataMoveTaskEntity> queryDataMoveTaskByState(Integer state, Integer page, Integer count) throws DataStreamException;

    List<DataMoveTaskEntity> queryDataMoveTaskByBatchTaskId(Long batchTaskId, Integer page, Integer count) throws DataStreamException;

    List<DataMoveTaskEntity> queryDataMoveTaskByCopyTaskId(Long copyTaskId, Integer page, Integer count) throws DataStreamException;

    List<DataMoveTaskEntity> queryDataMoveTaskByTaskType(Integer taskType, Integer page, Integer count) throws DataStreamException;

    List<DataMoveTaskEntity> queryDataMoveTaskByDate(String beginDate, String endDate, Integer page, Integer count) throws DataStreamException;

    List<DataMoveTaskEntity> queryDataMoveTaskByTableName(String tableName, Integer page, Integer count) throws DataStreamException;

    Integer updateDataMoveTaskState(Long taskId, Long taskExecuteId, Integer oldState, Integer state, Integer sendMode) throws DataStreamException;

    Integer updateDataMoveTaskLoadStrategy(Long taskId, Integer loadStrategy) throws DataStreamException;

    void operateMoveTaskStop(Long taskId) throws DataStreamException;

    Integer updateDataMoveTaskErrorInfo(Long taskId, Long taskExecuteId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException;

    Integer insertDataMoveInfo(List<DataMoveInfoEntity> dataMoveInfoList) throws DataStreamException;

    Long querySequence(String sequenceName) throws DataStreamException;

    Integer updateDataMoveTaskTableCount(Long taskId, Integer tableCountFlag, Long tableCount) throws DataStreamException;

    Integer updateDataMoveTaskTableCountMinMaxValue(Long taskId, Long sourceObjectCount, String sourceTableKeys, String sourceKeysBegin, String sourceKeysEn) throws DataStreamException;

    Integer updateDataMoveInfoPageRowEnd(Long infoId, String pageRowEnd, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException;

    Integer updateDataMoveInfoPageRowEndByTrace(Long infoId, String pageRowStart, String pageRowEnd, Long taskId, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException;

    Integer updateDataMoveInfoPageRowEnd(Long infoId, String pageRowEnd, Long taskId, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException;

    Integer updateDataMoveInfoPageRowStart(Long infoId, String pageRowStart, Integer pageLoopCount) throws DataStreamException;

    List<DataMoveTaskEntity> queryTaskByTaskId(Long taskId) throws DataStreamException;

    Integer insertDataMoveTask(List<DataMoveTaskEntity> dataMoveTaskList) throws DataStreamException;

    Integer insertTaskExtend(List<TaskExtendEntity> dataTaskExtendList) throws DataStreamException;

    List<DataMoveProgressEntity> queryDataMoveInfoProgress(Long taskId) throws DataStreamException;

    List<DataMoveInfoEntity> queryDataMoveInfo(Long taskId, Integer infoFlag) throws DataStreamException;

    List<DataMoveInfoEntity> queryDataMoveInfoByInfoId(Long infoId) throws DataStreamException;

    List<DataMoveInfoEntity> queryDataMoveInfoByTaskIdAndVirtualId(Long taskId, Integer virtualId, Integer infoFlag, String dataNode) throws DataStreamException;

    void updateDataMoveInfoErrorInfo(Long infoId, Integer oldState, Integer state, String errorCode, String errorMsg);

    void updateDataMoveInfoById(DataMoveInfoEntity dataMoveInfo);
    void updateDataMoveInfoRunningById(DataMoveInfoEntity dataMoveInfo);

    Integer getMoveTaskCount(String beginDate, String endDate, Integer state) throws DataStreamException;

    Integer getMoveTaskCountByBatchTaskId(Long batchTaskId) throws DataStreamException;

    Integer getMoveTaskCountByCopyTaskId(Long copyTaskId) throws DataStreamException;

    Integer getMoveTaskCountByTaskType(Integer taskType) throws DataStreamException;

    Integer getMoveTaskCountByTableName(String tableName) throws DataStreamException;

    List<TableLinkTaskEntity> queryTableLinkTaskByState(Integer state, Integer page, Integer count) throws DataStreamException;
    Integer queryTableLinkTaskByStateCount(Integer state) throws DataStreamException;

    List<TableLinkTaskEntity> queryTableLinkTaskByLinkTaskId(Long linkTaskId) throws DataStreamException;

    List<TableLinkTaskEntity> queryTableLinkTaskByTableLinkId(Long tableLinkId) throws DataStreamException;

    List<TableLinkTaskEntity> queryTableLinkTaskByDate(String beginDate, String endDate, Integer page, Integer count) throws DataStreamException;
    Integer queryTableLinkTaskByDateCount(String beginDate, String endDate) throws DataStreamException;

    Integer updateTableLinkTask(Long linkTaskId, Integer oldState, Integer state, String hostName, String hostIp) throws DataStreamException;

    Integer updateTableLinkErrorTask(Long taskId, Integer oldState, Integer state, String erroCode, String erroMsg) throws DataStreamException;

    Integer insertLinkTaskTable(LinkTaskTableEntity linkTaskTableEntity) throws DataStreamException;

    Integer insertTableLinkTask(TableLinkTaskEntity tableLinkTask) throws DataStreamException;

    List<DataBaseEntity> queryDataBase(Integer queryFlag, Long queryValue, Integer state, Integer page, Integer count) throws DataStreamException;

    Integer getDataBaseCount(Integer queryFlag, Long queryValue, Integer state) throws DataStreamException;

    Integer insertDataBase(DataBaseEntity dataBase) throws DataStreamException;

    Integer updateDataBase(DataBaseEntity dataBase) throws DataStreamException;

    Integer updateDataBaseState(Long dataBaseId, Integer state) throws DataStreamException;

    Integer statMoveTaskCount(String state) throws DataStreamException;

    Integer statLinkTaskCount(String state) throws DataStreamException;

    List<StatDayCountEntity> statMoveTaskCountGroupByDay(Integer days) throws DataStreamException;

    List<StatDayCountEntity> statLinkTaskCountGroupByDay(Integer days) throws DataStreamException;

    void initDataStreamMetaDb() throws DataStreamException;

    Integer insertMetrics(List<MetricsEntity> metricsList) throws DataStreamException;

    List<MetricsEntity> queryMetrics(Long taskId) throws DataStreamException;

    void operateMoveTaskRedo(Long taskId, Integer oldState) throws DataStreamException;

    List<TaskExecuteEntity> queryTaskExecute(Long taskId) throws DataStreamException;

    List<TaskExtendEntity> queryTaskExtend(Long taskId) throws DataStreamException;

    void loadTaskExtendParameters(DataMoveTaskEntity dataMoveTask) throws DataStreamException;

    Integer insertMoveTable(List<MoveTableEntity> moveTableList) throws DataStreamException;

    List<MoveTableEntity> queryMoveTable(Long taskId) throws DataStreamException;

    Integer updateMoveTableErrorInfo(Long moveTableId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException;
    Integer updateMoveTableSQL(Long moveTableId, String tableSql) throws DataStreamException;
    Integer insertDataCheck(List<DataCheckEntity> dataCheckList) throws DataStreamException;

    List<DataCheckEntity> queryDataCheck(Long taskId) throws DataStreamException;

    List<DataCheckEntity> queryDataCheckById(Long dataCheckId) throws DataStreamException;

    Integer updateDataCheck(Long dataCheckId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException;

    List<ColumnTypeDefineEntity> queryColumnTypeDefine(String databaseType) throws DataStreamException;

    List<ColumnTypeMapEntity> queryColumnTypeMap(String databaseTypeA, String databaseTypeB) throws DataStreamException;

    Integer insertColumnTypeTest(List<ColumnTypeTestEntity> columnTypeTestList) throws DataStreamException;

    void createTableMoveTask(List<DataMoveTaskEntity> dataMoveTaskList, List<MoveTableEntity> moveTableList, List<TaskExtendEntity> dataTaskExtendList) throws DataStreamException;

    void createDataMoveTask(List<DataMoveTaskEntity> dataMoveTaskList, List<TaskExtendEntity> dataTaskExtendList) throws DataStreamException;
}