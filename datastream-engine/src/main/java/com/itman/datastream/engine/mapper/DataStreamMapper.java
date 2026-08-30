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
package com.itman.datastream.engine.mapper;

import com.itman.datastream.common.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;


@Mapper
public interface DataStreamMapper {
    Integer getMoveTaskCount(@Param("dbType") Integer dbType, @Param("beginDate") String beginDate, @Param("endDate") String endDate, @Param("state") Integer state, @Param("systemUserCode") String systemUserCode);

    Integer getMoveTaskCountByBatchTaskId(@Param("dbType") Integer dbType, @Param("batchTaskId") Long batchTaskId, @Param("systemUserCode") String systemUserCode);

    Integer getMoveTaskCountByCopyTaskId(@Param("dbType") Integer dbType, @Param("copyTaskId") Long copyTaskId, @Param("systemUserCode") String systemUserCode);

    Integer getMoveTaskCountByTaskType(@Param("dbType") Integer dbType, @Param("taskType") Integer taskType, @Param("systemUserCode") String systemUserCode);

    Integer getMoveTaskCountByTableName(@Param("dbType") Integer dbType, @Param("tableName") String tableName, @Param("systemUserCode") String systemUserCode);

    List<DataMoveTaskEntity> queryDataMoveTaskByState(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("state") Integer state, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByStateLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("state") Integer state, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByBatchTaskId(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("batchTaskId") Long batchTaskId, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByBatchTaskIdLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("batchTaskId") Long batchTaskId, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByCopyTaskId(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("copyTaskId") Long copyTaskId, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByCopyTaskIdLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("copyTaskId") Long copyTaskId, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByTaskType(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("taskType") Integer taskType, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByTaskTypeLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("taskType") Integer taskType, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByDate(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("beginDate") String beginDate, @Param("endDate") String endDate, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByDateLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("beginDate") String beginDate, @Param("endDate") String endDate, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByTableName(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("tableName") String tableName, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<DataMoveTaskEntity> queryDataMoveTaskByTableNameLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("tableName") String tableName, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    Integer updateDataMoveTaskState(@Param("sysdate") String sysdate, @Param("taskId") Long taskId, @Param("oldState") Integer oldState, @Param("state") Integer state, @Param("sendMode") Integer sendMode) throws DataAccessException;

    Integer updateDataMoveTaskLoadStrategy(@Param("taskId") Long taskId, @Param("loadStrategy") Integer loadStrategy) throws DataAccessException;

    Integer updateDataMoveInfoState(@Param("taskId") Long taskId, @Param("oldState") Integer oldState, @Param("state") Integer state) throws DataAccessException;

    Integer updateDataMoveTaskErrorInfo(@Param("sysdate") String sysdate, @Param("taskId") Long taskId, @Param("oldState") Integer oldState, @Param("state") Integer state, @Param("errorCode") String errorCode, @Param("errorMsg") String errorMsg) throws DataAccessException;

    Integer updateTaskExecuteErrorInfo(@Param("sysdate") String sysdate, @Param("taskExecuteId") Long taskExecuteId, @Param("oldState") Integer oldState, @Param("state") Integer state, @Param("errorCode") String errorCode, @Param("errorMsg") String errorMsg) throws DataAccessException;

    Integer insertDataMoveInfo(@Param("sysdate") String sysdate, @Param("dataMoveInfoList") List<DataMoveInfoEntity> dataMoveInfoList) throws DataAccessException;

    Integer insertOneDataMoveInfo(@Param("sysdate") String sysdate, @Param("dataMoveInfo") DataMoveInfoEntity dataMoveInfo) throws DataAccessException;

    Long querySequence(@Param("sqlSequence") String sqlSequence);

    Integer updateDataMoveTaskTableCount(@Param("taskId") Long taskId, @Param("tableCountFlag") Integer tableCountFlag, @Param("tableCount") Long tableCount) throws DataAccessException;

    Integer updateDataMoveTaskTableCountMinMaxValue(@Param("taskId") Long taskId, @Param("sourceObjectCount") Long sourceObjectCount, @Param("sourceTableKeys") String sourceTableKeys, @Param("sourceKeysBegin") String sourceKeysBegin, @Param("sourceKeysEnd") String sourceKeysEnd) throws DataAccessException;

    Integer updateDataMoveInfoPageRowEnd(@Param("infoId") Long infoId, @Param("pageRowEnd") String pageRowEnd, @Param("dataCount") Integer dataCount, @Param("dataActualCount") Integer dataActualCount, @Param("maxCost") Long maxCost, @Param("minCost") Long minCost, @Param("currentCost") Long currentCost) throws DataAccessException;

    Integer updateDataMoveInfoPageRowStart(@Param("infoId") Long infoId, @Param("pageRowStart") String pageRowStart, @Param("pageLoopCount") Integer pageLoopCount) throws DataAccessException;

    List<DataMoveTaskEntity> queryTaskByTaskId(@Param("dbType") Integer dbType, @Param("taskId") Long taskId) throws DataAccessException;

    Integer insertDataMoveTask(@Param("sysdate") String sysdate, @Param("dataMoveTaskList") List<DataMoveTaskEntity> dataMoveTaskList) throws DataAccessException;

    Integer insertTaskExtend(@Param("sysdate") String sysdate, @Param("dataTaskExtendList") List<TaskExtendEntity> dataTaskExtendList) throws DataAccessException;

    List<DataMoveProgressEntity> queryDataMoveInfoProgress(@Param("dbType") Integer dbType, @Param("taskId") Long taskId) throws DataAccessException;

    List<DataMoveInfoEntity> queryDataMoveInfo(@Param("dbType") Integer dbType, @Param("taskId") Long taskId, @Param("infoFlag") Integer infoFlag) throws DataAccessException;

    List<DataMoveInfoEntity> queryDataMoveInfoByInfoId(@Param("dbType") Integer dbType, @Param("infoId") Long infoId) throws DataAccessException;

    List<DataMoveInfoEntity> queryDataMoveInfoByTaskIdAndVirtualId(@Param("dbType") Integer dbType, @Param("taskId") Long taskId, @Param("virtualId") Integer virtualId, @Param("infoFlag") Integer infoFlag, @Param("dataNode") String dataNode) throws DataAccessException;

    Integer updateDataMoveInfoErrorInfo(@Param("sysdate") String sysdate, @Param("infoId") Long infoId, @Param("oldState") Integer oldState, @Param("state") Integer state, @Param("errorCode") String errorCode, @Param("errorMsg") String errorMsg) throws DataAccessException;
    Integer updateDataMoveInfoById(@Param("sysdate") String sysdate, @Param("dataMoveInfo") DataMoveInfoEntity dataMoveInfo) throws DataAccessException;
    Integer updateDataMoveInfoRunningById(@Param("sysdate") String sysdate, @Param("dataMoveInfo") DataMoveInfoEntity dataMoveInfo) throws DataAccessException;

    List<TableLinkTaskEntity> queryTableLinkTaskByState(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("state") Integer state) throws DataAccessException;
    Integer queryTableLinkTaskByStateCount(@Param("dbType") Integer dbType, @Param("state") Integer state) throws DataAccessException;

    List<TableLinkTaskEntity> queryTableLinkTaskByStateLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("state") Integer state) throws DataAccessException;

    List<TableLinkTaskEntity> queryTableLinkTaskByTaskId(@Param("dbType") Integer dbType, @Param("linkTaskId") Long linkTaskId) throws DataAccessException;

    List<TableLinkTaskEntity> queryTableLinkTaskByTableLinkId(@Param("dbType") Integer dbType, @Param("tableLinkId") Long tableLinkId) throws DataAccessException;

    List<TableLinkTaskEntity> queryTableLinkTaskByDate(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("beginDate") String beginDate, @Param("endDate") String endDate) throws DataAccessException;
    Integer queryTableLinkTaskByDateCount(@Param("dbType") Integer dbType, @Param("beginDate") String beginDate, @Param("endDate") String endDate) throws DataAccessException;

    List<TableLinkTaskEntity> queryDataBackTaskByDateLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("beginDate") String beginDate, @Param("endDate") String endDate) throws DataAccessException;

    Integer updateTableLinkTask(@Param("sysdate") String sysdate, @Param("linkTaskId") Long linkTaskId, @Param("oldState") Integer oldState, @Param("state") Integer state, @Param("hostName") String hostName, @Param("hostIp") String hostIp) throws DataAccessException;

    Integer updateTableLinkErrorTask(@Param("sysdate") String sysdate, @Param("linkTaskId") Long linkTaskId, @Param("oldState") Integer oldState, @Param("state") Integer state, @Param("errorCode") String errorCode, @Param("errorMsg") String errorMsg) throws DataAccessException;

    Integer insertLinkTaskTable(@Param("sysdate") String sysdate, @Param("linkTable") LinkTaskTableEntity linkTable) throws DataAccessException;

    Integer insertTableLinkTask(@Param("sysdate") String sysdate, @Param("tableLinkTask") TableLinkTaskEntity tableLinkTask) throws DataAccessException;

    DataBaseEntity getDataBaseById(@Param("dbType") Integer dbType, @Param("dataBaseId") Long dataBaseId);

    Integer testDataBase(@Param("testSql") String testSql);

    Integer getDataBaseCount(@Param("dbType") Integer dbType, @Param("queryFlag") Integer queryFlag, @Param("queryValue") Long queryValue, @Param("state") Integer state, @Param("systemUserCode") String systemUserCode);

    List<DataBaseEntity> queryDataBase(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("queryFlag") Integer queryFlag, @Param("queryValue") Long queryValue, @Param("state") Integer state, @Param("systemUserCode") String systemUserCode);

    List<DataBaseEntity> queryDataBaseLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("queryFlag") Integer queryFlag, @Param("queryValue") Long queryValue, @Param("state") Integer state, @Param("systemUserCode") String systemUserCode);

    Integer insertDataBase(@Param("sysdate") String sysdate, @Param("dataBase") DataBaseEntity dataBase) throws DataAccessException;

    Integer updateDataBase(@Param("sysdate") String sysdate, @Param("dataBase") DataBaseEntity dataBase) throws DataAccessException;

    Integer updateDataBaseState(@Param("sysdate") String sysdate, @Param("dataBaseId") Long dataBaseId, @Param("state") Integer state) throws DataAccessException;

    Integer statMoveTaskCount(@Param("dbType") Integer dbType, @Param("state") String state, @Param("systemUserCode") String systemUserCode);

    Integer statLinkTaskCount(@Param("dbType") Integer dbType, @Param("state") String state, @Param("systemUserCode") String systemUserCode);

    List<StatDayCountEntity> statMoveTaskCountGroupByDay(@Param("dbType") Integer dbType, @Param("intervalDay") String intervalDay, @Param("systemUserCode") String systemUserCode);

    List<StatDayCountEntity> statMoveTaskCountGroupByDayLikeOracle(@Param("intervalDay") String intervalDay, @Param("systemUserCode") String systemUserCode);

    List<StatDayCountEntity> statLinkTaskCountGroupByDay(@Param("dbType") Integer dbType, @Param("intervalDay") String intervalDay, @Param("systemUserCode") String systemUserCode);

    List<StatDayCountEntity> statLinkTaskCountGroupByDayLikeOracle(@Param("intervalDay") String intervalDay, @Param("systemUserCode") String systemUserCode);

    List<StatTaskTypeCountEntity> statMoveTaskCountGroupByType(@Param("dbType") Integer dbType, @Param("systemUserCode") String systemUserCode);

    List<StatTaskStateCountEntity> statMoveTaskCountGroupByState(@Param("dbType") Integer dbType, @Param("systemUserCode") String systemUserCode);

    Integer getBatchTaskCountByDate(@Param("dbType") Integer dbType, @Param("beginDate") String beginDate, @Param("endDate") String endDate) throws DataAccessException;

    Integer queryTableMap(@Param("tableName") String tableName);

    List<TableMapEntity> queryTableMapALL(@Param("dbType") Integer dbType);

    Integer executeMetaDbSql(@Param("metaDbSql") String metaDbSql);

    Integer insertMoveTrace(@Param("sysdate") String sysdate, @Param("dataMoveTrace") DataMoveTraceEntity dataMoveTrace) throws DataAccessException;

    Integer insertMetrics(@Param("metricsList") List<MetricsEntity> metricsList) throws DataAccessException;

    List<MetricsEntity> queryMetrics(@Param("taskId") Long taskId);

    Integer insertTaskExecute(@Param("sysdate") String sysdate, @Param("taskExecute") TaskExecuteEntity taskExecute) throws DataAccessException;

    List<TaskExecuteEntity> queryTaskExecute(@Param("taskId") Long taskId);

    Integer updateTaskExecuteState(@Param("sysdate") String sysdate, @Param("taskExecuteId") Long taskExecuteId, @Param("oldState") Integer oldState, @Param("state") Integer state) throws DataAccessException;

    List<String> queryDataNodesByTableName(@Param("tableName") String tableName) throws DataAccessException;

    List<Map> executeSelectMapListSql(@Param("selectSql") String selectSql) throws DataAccessException;

    Integer insertDataList(@Param("insertSql") String insertSql) throws DataAccessException;

    Integer insertDataListBindVar(@Param("insertSqlColumns") String insertSqlColumns, @Param("dataListTarget") List<List<Object>> dataListTarget) throws DataAccessException;

    Integer updateDataList(@Param("updateSql") String updateSql) throws DataAccessException;

    Integer executeDeleteRecordSql(@Param("deleteSql") String deleteSql) throws DataAccessException;

    Long executeSelectRecordCountSql(@Param("selectSql") String selectSql) throws DataAccessException;

    String executeSelectStringValueSql(@Param("selectSql") String selectSql) throws DataAccessException;

    Map<String, String> executeSelectMapMaxMinSql(@Param("selectSql") String selectSql);

    List<TaskExtendEntity> queryTaskExtend(@Param("taskId") Long taskId);

    Integer insertMoveTableRecord(@Param("sysdate") String sysdate, @Param("moveTable") MoveTableEntity moveTable) throws DataAccessException;

    List<MoveTableEntity> queryMoveTable(@Param("taskId") Long taskId);

    Integer updateMoveTableErrorInfo(@Param("sysdate") String sysdate, @Param("moveTableId") Long moveTableId, @Param("oldState") Integer oldState, @Param("state") Integer state, @Param("errorCode") String errorCode, @Param("errorMsg") String errorMsg) throws DataAccessException;
    Integer updateMoveTableSQL(@Param("moveTableId") Long moveTableId, @Param("tableSql") String tableSql) throws DataAccessException;

    Integer insertDataCheck(@Param("sysdate") String sysdate, @Param("dataCheckList") List<DataCheckEntity> dataCheckList) throws DataAccessException;

    List<DataCheckEntity> queryDataCheck(@Param("taskId") Long taskId);

    List<DataCheckEntity> queryDataCheckById(@Param("dataCheckId") Long dataCheckId);

    Integer updateDataCheck(@Param("sysdate") String sysdate, @Param("dataCheckId") Long dataCheckId, @Param("oldState") Integer oldState, @Param("state") Integer state, @Param("errorCode") String errorCode, @Param("errorMsg") String errorMsg) throws DataAccessException;

    List<ColumnTypeDefineEntity> queryColumnTypeDefine(@Param("databaseType") String databaseType);

    List<ColumnTypeMapEntity> queryColumnTypeMap(@Param("databaseTypeA") String databaseTypeA, @Param("databaseTypeB") String databaseTypeB);

    Integer insertColumnTypeTest(@Param("columnTypeTestList") List<ColumnTypeTestEntity> columnTypeTestList) throws DataAccessException;

    DebeziumOffsetEntity findOffsets(@Param("offsetKey") String offsetKey) throws DataAccessException;

    Integer insertOffsets(@Param("offsetkey") String offsetkey, @Param("offsetValue") String offsetValue, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateOffsets(@Param("offsetkey") String offsetkey, @Param("offsetValue") String offsetValue, @Param("sysdate") String sysdate) throws DataAccessException;
    Integer insertDebeziumHistory(@Param("debeziumHistoryId") Long debeziumHistoryId, @Param("server") String server, @Param("historyData") String historyData, @Param("sysdate") String sysdate) throws DataAccessException;

    List<DebeziumHistoryEntity> selectDebeziumHistory(String server) throws DataAccessException;

}
