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
package com.itman.datastream.engine.dao;

import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.utils.CommUtils;
import com.itman.datastream.engine.mapper.DataStreamMapper;
import com.itman.datastream.engine.systemlog.ISystemLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.constant.DataStreamConstant.DATA_STREAM_TASK_STATE_STOP;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.getStackTraceAsString;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DataStreamDao {
    public final DataStreamMapper dataStreamMapper;
    private final DataStreamConfig dataStreamConfig;
    private final DataSourceFactory dataSourceFactory;
    public final ISystemLogEvent systemLogEvent;


    public Integer getMoveTaskCount(String beginDate, String endDate, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.getMoveTaskCount(dataStreamConfig.getMetaTeledbType(), beginDate, endDate, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TABLE_COUNT_ERROR);
        }
    }

    public Integer getMoveTaskCountByBatchTaskId(Long batchTaskId) throws DataStreamException {
        try {
            return dataStreamMapper.getMoveTaskCountByBatchTaskId(dataStreamConfig.getMetaTeledbType(), batchTaskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TABLE_COUNT_ERROR);
        }
    }

    public Integer getMoveTaskCountByCopyTaskId(Long copyTaskId) throws DataStreamException {
        try {
            return dataStreamMapper.getMoveTaskCountByCopyTaskId(dataStreamConfig.getMetaTeledbType(), copyTaskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TABLE_COUNT_ERROR);
        }
    }

    public Integer getMoveTaskCountByTaskType(Integer taskType) throws DataStreamException {
        try {
            return dataStreamMapper.getMoveTaskCountByTaskType(dataStreamConfig.getMetaTeledbType(), taskType);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TABLE_COUNT_ERROR);
        }
    }

    public Integer getMoveTaskCountByTableName(String tableName) throws DataStreamException {
        try {
            return dataStreamMapper.getMoveTaskCountByTableName(dataStreamConfig.getMetaTeledbType(), tableName);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TABLE_COUNT_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByState(String sqlLimit, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByState(dataStreamConfig.getMetaTeledbType(), sqlLimit, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByStateLikeOracle(Integer pageBeginRow, Integer pageEndRow, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByStateLikeOracle(pageBeginRow, pageEndRow, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByBatchTaskId(String sqlLimit, Long batchTaskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByBatchTaskId(dataStreamConfig.getMetaTeledbType(), sqlLimit, batchTaskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByBatchTaskIdLikeOracle(Integer pageBeginRow, Integer pageEndRow, Long batchTaskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByBatchTaskIdLikeOracle(pageBeginRow, pageEndRow, batchTaskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByCopyTaskId(String sqlLimit, Long copyTaskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByCopyTaskId(dataStreamConfig.getMetaTeledbType(), sqlLimit, copyTaskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByCopyTaskIdLikeOracle(Integer pageBeginRow, Integer pageEndRow, Long copyTaskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByCopyTaskIdLikeOracle(pageBeginRow, pageEndRow, copyTaskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByTaskType(String sqlLimit, Integer taskType) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByTaskType(dataStreamConfig.getMetaTeledbType(), sqlLimit, taskType);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByTaskTypeLikeOracle(Integer pageBeginRow, Integer pageEndRow, Integer taskType) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByTaskTypeLikeOracle(pageBeginRow, pageEndRow, taskType);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByDate(String sqlLimit, String beginDate, String endDate) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByDate(dataStreamConfig.getMetaTeledbType(), sqlLimit, beginDate, endDate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_DATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByDateLikeOracle(Integer pageBeginRow, Integer pageEndRow, String beginDate, String endDate) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByDateLikeOracle(pageBeginRow, pageEndRow, beginDate, endDate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_DATE_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByTableName(String sqlLimit, String tableName) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByTableName(dataStreamConfig.getMetaTeledbType(), sqlLimit, tableName);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_TABLE_NAME_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskByTableNameLikeOracle(Integer pageBeginRow, Integer pageEndRow, String tableName) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveTaskByTableNameLikeOracle(pageBeginRow, pageEndRow, tableName);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_TABLE_NAME_ERROR);
        }
    }

    public Integer updateDataMoveTaskState(String sysdate, Long taskId, Integer oldState, Integer state, Integer sendMode) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveTaskState(sysdate, taskId, oldState, state, sendMode);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_STATE_ERROR);
        }
    }

    public Integer updateDataMoveTaskLoadStrategy(Long taskId, Integer loadStrategy) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveTaskLoadStrategy(taskId, loadStrategy);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_STATE_ERROR);
        }
    }

    public Integer updateDataMoveInfoState(Long taskId, Integer oldState, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveInfoState(taskId, oldState, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_INFO_STATE_ERROR);
        }
    }

    public Integer updateDataMoveTaskErrorInfo(String sysdate, Long taskId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveTaskErrorInfo(sysdate, taskId, oldState, state, errorCode, errorMsg);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_ERROR_MSG_ERROR);
        }
    }

    public Integer updateTaskExecuteErrorInfo(String sysdate, Long taskExecuteId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        try {
            return dataStreamMapper.updateTaskExecuteErrorInfo(sysdate, taskExecuteId, oldState, state, errorCode, errorMsg);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_EXECUTOR_ERROR_MSG_ERROR);
        }
    }

    public Integer insertDataMoveInfo(String sysdate, List<DataMoveInfoEntity> dataMoveInfoList) throws DataStreamException {
        try {
            return dataStreamMapper.insertDataMoveInfo(sysdate, dataMoveInfoList);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_INSERT_DATA_MOVE_INFO_ERROR);
        }
    }

    public Integer insertOneDataMoveInfo(String sysdate, DataMoveInfoEntity dataMoveInfo) throws DataStreamException {
        try {
            return dataStreamMapper.insertOneDataMoveInfo(sysdate, dataMoveInfo);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_INSERT_DATA_MOVE_INFO_ERROR);
        }
    }

    public Long querySequence(String sequenceName) throws DataStreamException {
        if (dataStreamConfig.getSequenceMode() == null || dataStreamConfig.getSequenceMode().equals(1)) {
            return CommUtils.generateUniqueLong();
        } else {
            return dataStreamMapper.querySequence(getDataBaseObject().makeSqlSequence(sequenceName));
        }
    }

    private IDatabaseAdapter getDataBaseObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }


    public Integer updateDataMoveTaskTableCount(Long taskId, Integer tableCountFlag, Long tableCount) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveTaskTableCount(taskId, tableCountFlag, tableCount);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_TABLE_COUNT_ERROR);
        }
    }

    public Integer updateDataMoveTaskTableCountMinMaxValue(Long taskId, Long sourceObjectCount, String sourceTableKeys, String sourceKeysBegin, String sourceKeysEnd) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveTaskTableCountMinMaxValue(taskId, sourceObjectCount, sourceTableKeys, sourceKeysBegin, sourceKeysEnd);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_TABLE_COUNT_ERROR);
        }
    }

    public Integer updateDataMoveInfoPageRowEnd(Long infoId, String pageRowEnd, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveInfoPageRowEnd(infoId, (pageRowEnd==null?"null":pageRowEnd), dataCount, dataActualCount, maxCost, minCost, currentCost);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_MOVE_INFO_TABLE_COUNT_ERROR);
        }
    }

    public Integer updateDataMoveInfoPageRowEnd(Long infoId, String pageRowEnd, Integer dataCount, Integer dataActualCount) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveInfoPageRowEnd(infoId, pageRowEnd, dataCount, dataActualCount, 0L, 0L, 0L);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_MOVE_INFO_TABLE_COUNT_ERROR);
        }
    }

    public Integer updateDataMoveInfoPageRowStart(Long infoId, String pageRowStart, Integer pageLoopCount) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveInfoPageRowStart(infoId, pageRowStart, pageLoopCount);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_MOVE_INFO_PAGE_ROW_START_ERROR);
        }
    }

    public List<DataMoveTaskEntity> queryTaskByTaskId(Long taskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryTaskByTaskId(dataStreamConfig.getMetaTeledbType(), taskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_TASK_ID_ERROR);
        }
    }


    public Integer insertDataMoveTask(String sysdate, List<DataMoveTaskEntity> dataMoveTaskList) throws DataStreamException {
        try {
            return dataStreamMapper.insertDataMoveTask(sysdate, dataMoveTaskList);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_INSERT_DATA_MOVE_TASK_ERROR);
        }
    }

    public Integer insertTaskExtend(String sysdate, List<TaskExtendEntity> dataTaskExtendList) throws DataStreamException {
        try {
            return dataStreamMapper.insertTaskExtend(sysdate, dataTaskExtendList);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_INSERT_TASK_EXTEND_ERROR);
        }
    }

    public List<DataMoveProgressEntity> queryDataMoveInfoProgress(Long taskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveInfoProgress(dataStreamConfig.getMetaTeledbType(), taskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_MOVE_INFO_PROGRESS_ERROR);
        }
    }

    public List<DataMoveInfoEntity> queryDataMoveInfo(Long taskId, Integer infoFlag) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveInfo(dataStreamConfig.getMetaTeledbType(), taskId, infoFlag);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_MOVE_INFO_ERROR);
        }
    }

    public List<DataMoveInfoEntity> queryDataMoveInfoByInfoId(Long infoId) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveInfoByInfoId(dataStreamConfig.getMetaTeledbType(), infoId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_MOVE_INFO_BY_INFO_ID_ERROR);
        }
    }

    public List<DataMoveInfoEntity> queryDataMoveInfoByTaskIdAndVirtualId(Long taskId, Integer virtualId, Integer infoFlag, String dataNode) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataMoveInfoByTaskIdAndVirtualId(dataStreamConfig.getMetaTeledbType(), taskId, virtualId, infoFlag, dataNode);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_MOVE_INFO_BY_TASK_VIRTUAL_ERROR);
        }
    }

    public Integer updateDataMoveInfoById(String sysdate, DataMoveInfoEntity dataMoveInfo) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveInfoById(sysdate, dataMoveInfo);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_INFO_ERROR_MSG_ERROR);
        }
    }

    public Integer updateDataMoveInfoRunningById(String sysdate, DataMoveInfoEntity dataMoveInfo) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveInfoRunningById(sysdate, dataMoveInfo);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_INFO_ERROR_MSG_ERROR);
        }
    }

    public Integer updateDataMoveInfoErrorInfo(String sysdate, Long infoId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataMoveInfoErrorInfo(sysdate, infoId, oldState, state, errorCode, errorMsg);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_INFO_ERROR_MSG_ERROR);
        }
    }

    public List<TableLinkTaskEntity> queryTableLinkTaskByState(String sqlLimit, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.queryTableLinkTaskByState(dataStreamConfig.getMetaTeledbType(), sqlLimit, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public Integer queryTableLinkTaskByStateCount(Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.queryTableLinkTaskByStateCount(dataStreamConfig.getMetaTeledbType(), state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<TableLinkTaskEntity> queryTableLinkTaskByStateLikeOracle(Integer pageBeginRow, Integer pageEndRow, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.queryTableLinkTaskByStateLikeOracle(pageBeginRow, pageEndRow, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_STATE_ERROR);
        }
    }

    public List<TableLinkTaskEntity> queryTableLinkTaskByTaskId(Long linkTaskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryTableLinkTaskByTaskId(dataStreamConfig.getMetaTeledbType(), linkTaskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_TASK_ID_ERROR);
        }
    }

    public List<TableLinkTaskEntity> queryTableLinkTaskByTableLinkId(Long tableLinkId) throws DataStreamException {
        try {
            return dataStreamMapper.queryTableLinkTaskByTableLinkId(dataStreamConfig.getMetaTeledbType(), tableLinkId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_TASK_ID_ERROR);
        }
    }

    public List<TableLinkTaskEntity> queryTableLinkTaskByDate(String sqlLimit, String beginDate, String endDate) throws DataStreamException {
        try {
            return dataStreamMapper.queryTableLinkTaskByDate(dataStreamConfig.getMetaTeledbType(), sqlLimit, beginDate, endDate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_DATE_ERROR);
        }
    }

    public Integer queryTableLinkTaskByDateCount(String beginDate, String endDate) throws DataStreamException {
        try {
            return dataStreamMapper.queryTableLinkTaskByDateCount(dataStreamConfig.getMetaTeledbType(), beginDate, endDate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_DATE_ERROR);
        }
    }

    public List<TableLinkTaskEntity> queryDataBackTaskByDateLikeOracle(Integer pageBeginRow, Integer pageEndRow, String beginDate, String endDate) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataBackTaskByDateLikeOracle(pageBeginRow, pageEndRow, beginDate, endDate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TASK_BY_DATE_ERROR);
        }
    }

    public Integer updateTableLinkTask(String sysdate, Long linkTaskId, Integer oldState, Integer state, String hostName, String hostIp) throws DataStreamException {
        try {
            return dataStreamMapper.updateTableLinkTask(sysdate, linkTaskId, oldState, state, hostName, hostIp);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_STATE_ERROR);
        }
    }

    public Integer updateTableLinkErrorTask(String sysdate, Long linkTaskId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        try {
            return dataStreamMapper.updateTableLinkErrorTask(sysdate, linkTaskId, oldState, state, errorCode, errorMsg);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_STATE_ERROR);
        }
    }

    public Integer insertLinkTaskTable(String sysdate, LinkTaskTableEntity linkTable) throws DataStreamException {
        try {
            return dataStreamMapper.insertLinkTaskTable(sysdate, linkTable);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_INSERT_TASK_LINK_TABLE_ERROR);
        }
    }


    public Integer insertTableLinkTask(String sysdate, TableLinkTaskEntity tableLinkTask) throws DataStreamException {
        try {
            return dataStreamMapper.insertTableLinkTask(sysdate, tableLinkTask);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_INSERT_TABLE_LINK_TASK_ERROR);
        }
    }

    public DataBaseEntity getDataBaseById(Long dataSourceId) throws DataStreamException {
        try {
            return dataStreamMapper.getDataBaseById(dataStreamConfig.getMetaTeledbType(), dataSourceId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_FLOW_CONFIG_ERROR);
        }
    }

    public Integer testDataBase(String testSql) throws DataStreamException {
        try {
            return dataStreamMapper.testDataBase(testSql);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_TEST_DATA_SOURCE_ERROR);
        }
    }

    public Integer getDataBaseCount(Integer queryFlag, Long queryValue, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.getDataBaseCount(dataStreamConfig.getMetaTeledbType(), queryFlag, queryValue, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_DATA_SOURCE_ERROR);
        }
    }

    public List<DataBaseEntity> queryDataBase(String sqlLimit, Integer queryFlag, Long queryValue, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataBase(dataStreamConfig.getMetaTeledbType(), sqlLimit, queryFlag, queryValue, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_DATA_SOURCE_ERROR);
        }
    }

    public List<DataBaseEntity> queryDataBaseLikeOracle(Integer pageBeginRow, Integer pageEndRow, Integer queryFlag, Long queryValue, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataBaseLikeOracle(pageBeginRow, pageEndRow, queryFlag, queryValue, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_DATA_SOURCE_ERROR);
        }
    }

    public Integer insertDataBase(String sysdate, DataBaseEntity dataBase) throws DataStreamException {
        try {
            return dataStreamMapper.insertDataBase(sysdate, dataBase);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_DATA_SOURCE_ERROR);
        }
    }

    public Integer updateDataBase(String sysdate, DataBaseEntity dataBase) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataBase(sysdate, dataBase);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_DATA_SOURCE_ERROR);
        }
    }

    public Integer updateDataBaseState(String sysdate, Long dataBaseId, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataBaseState(sysdate, dataBaseId, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_DATA_SOURCE_STATE_ERROR);
        }
    }

    public Integer statMoveTaskCount(String state) throws DataStreamException {
        try {
            return dataStreamMapper.statMoveTaskCount(dataStreamConfig.getMetaTeledbType(), state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_STAT_MOVE_TASK_COUNT_ERROR);
        }
    }

    public Integer statLinkTaskCount(String state) throws DataStreamException {
        try {
            return dataStreamMapper.statLinkTaskCount(dataStreamConfig.getMetaTeledbType(), state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_STAT_LINK_TASK_COUNT_ERROR);
        }
    }

    public List<StatDayCountEntity> statMoveTaskCountGroupByDay(String intervalDay) throws DataStreamException {
        try {
            return (!Arrays.asList(DATA_SOURCE_TYPE_ORACLE, DATA_SOURCE_TYPE_H2).contains(dataStreamConfig.getMetaDbBaseType())) ? dataStreamMapper.statMoveTaskCountGroupByDay(dataStreamConfig.getMetaTeledbType(), intervalDay) : dataStreamMapper.statMoveTaskCountGroupByDayLikeOracle(intervalDay);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_STAT_MOVE_TASK_COUNT_GROUP_BY_DAY_ERROR);
        }
    }

    public List<StatDayCountEntity> statLinkTaskCountGroupByDay(String intervalDay) throws DataStreamException {
        try {
            return (!Arrays.asList(DATA_SOURCE_TYPE_ORACLE, DATA_SOURCE_TYPE_H2).contains(dataStreamConfig.getMetaDbBaseType())) ?
                    dataStreamMapper.statLinkTaskCountGroupByDay(dataStreamConfig.getMetaTeledbType(), intervalDay) :
                    dataStreamMapper.statLinkTaskCountGroupByDayLikeOracle(intervalDay);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_STAT_BACK_TASK_COUNT_GROUP_BY_DAY_ERROR);
        }
    }

    public Integer executeMetaDbSql(String metaDbSql) throws DataStreamException {
        try {
            return dataStreamMapper.executeMetaDbSql(metaDbSql);
        } catch (Exception e) {
            log.error("error", e);
            systemLogEvent.jobLogbackEvent(getStackTraceAsString(e));
            throw new DataStreamException(DAO_UPDATE_META_DB_SQL_ERROR);
        }
    }

    public Integer insertMoveTrace(String sysdate, DataMoveTraceEntity dataMoveTrace) throws DataStreamException {
        try {
            return dataStreamMapper.insertMoveTrace(sysdate, dataMoveTrace);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_MOVE_TRACE_ERROR);
        }
    }

    public Integer insertMetrics(List<MetricsEntity> metricsList) throws DataStreamException {
        try {
            return dataStreamMapper.insertMetrics(metricsList);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_METRICS_ERROR);
        }
    }

    public List<MetricsEntity> queryMetrics(Long taskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryMetrics(taskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_METRICS_ERROR);
        }
    }

    public Integer insertTaskExecute(String sysdate, TaskExecuteEntity taskExecute) throws DataStreamException {
        try {
            return dataStreamMapper.insertTaskExecute(sysdate, taskExecute);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_INSERT_TASK_EXECUTOR_ERROR);
        }
    }

    public List<TaskExecuteEntity> queryTaskExecute(Long taskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryTaskExecute(taskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_TASK_EXECUTOR_ERROR);
        }
    }

    public Integer updateTaskExecuteState(String sysdate, Long taskExecuteId, Integer oldState, Integer state) throws DataStreamException {
        try {
            return dataStreamMapper.updateTaskExecuteState(sysdate, taskExecuteId, oldState, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_TASK_EXECUTOR_ERROR);
        }
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public void operateMoveTaskStopByTransaction(Long taskId, String sysdate) throws DataStreamException {
        updateDataMoveInfoState(taskId, DATA_STREAM_TASK_STATE_RUNNING, DATA_STREAM_TASK_STATE_STOP);
        updateDataMoveTaskState(sysdate, taskId, DATA_STREAM_TASK_STATE_RUNNING, DATA_STREAM_TASK_STATE_STOP, null);
        List<TaskExecuteEntity> taskExecuteList = queryTaskExecute(taskId);
        if (!CollectionUtils.isEmpty(taskExecuteList)) {
            updateTaskExecuteState(sysdate, taskExecuteList.get(0).getTaskExecuteId(), DATA_STREAM_TASK_STATE_RUNNING, DATA_STREAM_TASK_STATE_STOP);
        }
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public Integer updateDataMoveTaskStateByTransaction(String sysdate, Long taskId, Long taskExecuteId, Integer oldState, Integer state, Integer sendMode) throws DataStreamException {
        TaskExecuteEntity taskExecute = new TaskExecuteEntity();
        taskExecute.setTaskId(taskId);
        taskExecute.setTaskExecuteId(taskExecuteId);
        taskExecute.setState(state);
        taskExecute.setHostName(dataStreamConfig.getHostName());
        taskExecute.setHostIp(dataStreamConfig.getHostIP());
        insertTaskExecute(sysdate, taskExecute);
        return updateDataMoveTaskState(sysdate, taskId, oldState, state, sendMode);
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public Integer updateDataMoveTaskErrorInfoByTransaction(String sysdate, Long taskId, Long taskExecuteId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        Integer updateRecord = updateTaskExecuteErrorInfo(sysdate, taskExecuteId, oldState, state, errorCode, errorMsg);
        if (updateRecord != 1) {
            throw new DataStreamException(DAO_UPDATE_TASK_EXECUTE_ERROR_INFO_ERROR);
        }

        updateRecord = updateDataMoveTaskErrorInfo(sysdate, taskId, oldState, state, errorCode, errorMsg);
        if (updateRecord != 1) {
            throw new DataStreamException(DAO_UPDATE_TASK_ERROR_INFO_ERROR);
        }

        return updateRecord;
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public Integer insertDataMoveInfoByTransaction(String sysdate, List<DataMoveInfoEntity> dataMoveInfoList) throws DataStreamException {
        dataMoveInfoList.forEach(x -> {
            try {
                insertOneDataMoveInfo(sysdate, x);
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        });
        return dataMoveInfoList.size();
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public Integer updateDataMoveInfoPageRowEndByTrace(String sysdate, Long infoId, String pageRowStart, String pageRowEnd, Long taskId, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException {
        DataMoveTraceEntity dataMoveTrace = new DataMoveTraceEntity();
        dataMoveTrace.setTraceId(querySequence(SEQ_MOVE_INFO_ID));
        dataMoveTrace.setInfoId(infoId);
        dataMoveTrace.setTaskId(taskId);
        dataMoveTrace.setPageRowStart(pageRowStart == null ? "null" : pageRowStart);
        dataMoveTrace.setPageRowEnd(pageRowEnd == null ? "null" : pageRowEnd);
        dataMoveTrace.setDataCount(dataCount);
        dataMoveTrace.setDataActualCount(dataActualCount);
        insertMoveTrace(sysdate, dataMoveTrace);
        return updateDataMoveInfoPageRowEnd(infoId, dataMoveTrace.getPageRowStart(), dataCount, dataActualCount, maxCost, minCost, currentCost);
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public void operateMoveTaskRedoByTransaction(String sysdate, Long taskId, Integer oldState) throws DataStreamException {
        //强制moveInfo的所有重处理，包括有线程已经完成的
        updateDataMoveInfoState(taskId, null, DATA_STREAM_TASK_STATE_RUNNING);
        updateDataMoveTaskState(sysdate, taskId, oldState, DATA_STREAM_TASK_STATE_INIT, null);
    }


    public List<String> queryDataNodesByTableName(String tableName) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataNodesByTableName(tableName);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_DATA_NODES_BY_TABLE_NAME_ERROR);
        }
    }

    public List<Map> executeSelectMapListSql(String selectSql) throws DataStreamException {
        try {
            return dataStreamMapper.executeSelectMapListSql(selectSql);
        } catch (Exception e) {
            log.error("error", e);
            systemLogEvent.jobLogbackEvent(getStackTraceAsString(e));
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_DATA_BY_ROW_ERROR);
        }
    }

    public Integer insertDataList(String insertSql) throws DataStreamException {
        return dataStreamMapper.insertDataList(insertSql);
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public void syncDataByTransaction(List<String> insertSqlList) throws DataStreamException {
        for (String insertSqlIterator : insertSqlList) {
            Integer insertSize = insertDataList(insertSqlIterator);
            if (insertSize.equals(0)) {
                throw new DataStreamException(DataStreamErrorCode.OPER_TARGET_TABLE_BY_SERVICE_REPEAT_ERROR);
            }
        }
    }

    public Integer insertDataListBindVar(String insertSqlColumns, List<List<Object>> dataListTarget) throws DataStreamException {
        try {
            return dataStreamMapper.insertDataListBindVar(insertSqlColumns, dataListTarget);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_INSERT_DATA_LIST_ERROR);
        }
    }

    public Integer updateDataList(String updateSql) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataList(updateSql);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_UPDATE_DATA_LIST_ERROR);
        }
    }

    public Integer executeDeleteRecordSql(String deleteSql) throws DataStreamException {
        try {
            return dataStreamMapper.executeDeleteRecordSql(deleteSql);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_DELETE_DATA_LIST_ERROR);
        }
    }

    public Long executeSelectRecordCountSql(String selectSql) throws DataStreamException {
        try {
            return dataStreamMapper.executeSelectRecordCountSql(selectSql);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_TABLE_COUNT_ERROR);
        }
    }

    public String executeSelectStringValueSql(String selectSql) throws DataStreamException {
        return dataStreamMapper.executeSelectStringValueSql(selectSql);
    }

    public Map<String, String> executeSelectMapMaxMinSql(String selectSql) throws DataStreamException {
        try {
            return dataStreamMapper.executeSelectMapMaxMinSql(selectSql);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DataStreamErrorCode.DAO_QUERY_MAXMINKEYNUM_ERROR);
        }
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public Integer insertDataListByTransaction(Long infoId, Integer dataCount, String pageRowEnd, String insertSql) throws DataStreamException {
        Integer dataActualCount = insertDataList(insertSql);

        if (infoId > 0) {
            updateDataMoveInfoPageRowEnd(infoId, pageRowEnd, dataCount, dataActualCount);
        }

        return dataActualCount;
    }

    public List<TaskExtendEntity> queryTaskExtend(Long taskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryTaskExtend(taskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_TASK_EXTEND_ERROR);
        }
    }

    public Integer insertMoveTable(String sysdate, List<MoveTableEntity> moveTableList) throws DataStreamException {
        try {
            Integer insertSize = 0;
            for (MoveTableEntity iterator : moveTableList) {
                insertSize += dataStreamMapper.insertMoveTableRecord(sysdate, iterator);
            }
            return insertSize;
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_MOVE_TABLE_ERROR);
        }
    }

    public List<MoveTableEntity> queryMoveTable(Long taskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryMoveTable(taskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_MOVE_TABLE_ERROR);
        }
    }

    public Integer updateMoveTableSQL(Long moveTableId, String tableSql) throws DataStreamException {
        try {
            return dataStreamMapper.updateMoveTableSQL(moveTableId, tableSql);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_MOVE_TABLE_SQL_ERROR);
        }
    }

    public Integer updateMoveTableErrorInfo(String sysdate, Long moveTableId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        try {
            return dataStreamMapper.updateMoveTableErrorInfo(sysdate, moveTableId, oldState, state, errorCode, errorMsg);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_MOVE_TABLE_ERROR);
        }
    }

    public Integer insertDataCheck(String sysdate, List<DataCheckEntity> dataCheckList) throws DataStreamException {
        try {
            return dataStreamMapper.insertDataCheck(sysdate, dataCheckList);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_MOVE_TABLE_ERROR);
        }
    }

    public List<DataCheckEntity> queryDataCheck(Long taskId) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataCheck(taskId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_DATA_CHECK_ERROR);
        }
    }

    public List<DataCheckEntity> queryDataCheckById(Long dataCheckId) throws DataStreamException {
        try {
            return dataStreamMapper.queryDataCheckById(dataCheckId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_DATA_CHECK_BY_ID_ERROR);
        }
    }

    public Integer updateDataCheck(String sysdate, Long dataCheckId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        try {
            return dataStreamMapper.updateDataCheck(sysdate, dataCheckId, oldState, state, errorCode, errorMsg);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_DATA_CHECK_ERROR);
        }
    }

    public List<ColumnTypeDefineEntity> queryColumnTypeDefine(String databaseType) throws DataStreamException {
        try {
            return dataStreamMapper.queryColumnTypeDefine(databaseType);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_COLUMN_TYPE_DEFINE_ERROR);
        }
    }

    public List<ColumnTypeMapEntity> queryColumnTypeMap(String databaseTypeA, String databaseTypeB) throws DataStreamException {
        try {
            return dataStreamMapper.queryColumnTypeMap(databaseTypeA, databaseTypeB);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_COLUMN_TYPE_MAP_ERROR);
        }
    }

    public Integer insertColumnTypeTest(List<ColumnTypeTestEntity> columnTypeTestList) throws DataStreamException {
        try {
            return dataStreamMapper.insertColumnTypeTest(columnTypeTestList);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_COLUMN_TYPE_TEST_ERROR);
        }
    }

    public DebeziumOffsetEntity findOffsets(String keys) throws DataStreamException {
        try {
            return dataStreamMapper.findOffsets(keys);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_FIND_OFFSETS_ERROR);
        }
    }

    public Integer insertOffsets(String offsetkey, String offsetValue, String sysdate) throws DataStreamException {
        try {
            return dataStreamMapper.insertOffsets(offsetkey, offsetValue, sysdate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_OFFSETS_ERROR);
        }
    }

    public Integer updateOffsets(String offsetkey, String offsetValue, String sysdate) throws DataStreamException {
        try {
            return dataStreamMapper.updateOffsets(offsetkey, offsetValue, sysdate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_OFFSETS_ERROR);
        }
    }

    public Integer insertDebeziumHistory(Long debeziumHistoryId, String sysdate, String server, String historyData) throws DataStreamException {
        try {
            return dataStreamMapper.insertDebeziumHistory(debeziumHistoryId, server, historyData, sysdate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_OFFSETS_ERROR);
        }
    }

    public List<DebeziumHistoryEntity> selectDebeziumHistory(String server) throws DataStreamException {
        try {
            return dataStreamMapper.selectDebeziumHistory(server);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(UNKNOWN_ERROR);
        }
    }
}
