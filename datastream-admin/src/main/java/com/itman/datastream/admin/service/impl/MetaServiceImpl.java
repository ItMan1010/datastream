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
package com.itman.datastream.admin.service.impl;

import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.extend.TaskParamExtend;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.engine.dao.FileDao;
import com.itman.datastream.engine.dao.MQConfigDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.utils.CommUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaServiceImpl implements IMetaService {
    private final DataStreamConfig dataStreamConfig;
    private final DataStreamDao dataStreamDao;
    private final FileDao fileDao;
    private final TaskParamExtend taskParamExtend;
    private final MQConfigDao mqConfigDao;

    private final DataSourceFactory dataSourceFactory;
    private static final Object lock = new Object();
    public static Map<String, SequenceEntity> canalSequenceMap = new HashMap<>();


    @Override
    public List<DataMoveTaskEntity> queryDataMoveTaskByState(Integer state, Integer page, Integer count, String systemUserCode) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.queryDataMoveTaskByState(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), state, systemUserCode) : dataStreamDao.queryDataMoveTaskByStateLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), state, systemUserCode);
        loadDataSourceByMoveTasks(dataMoveTaskList);
        return dataMoveTaskList;
    }

    @Override
    public List<DataMoveTaskEntity> queryDataMoveTaskByBatchTaskId(Long batchTaskId, Integer page, Integer count, String systemUserCode) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.queryDataMoveTaskByBatchTaskId(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), batchTaskId, systemUserCode) : dataStreamDao.queryDataMoveTaskByBatchTaskIdLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), batchTaskId, systemUserCode);
        loadDataSourceByMoveTasks(dataMoveTaskList);
        return dataMoveTaskList;
    }

    @Override
    public List<DataMoveTaskEntity> queryDataMoveTaskByCopyTaskId(Long copyTaskId, Integer page, Integer count, String systemUserCode) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.queryDataMoveTaskByCopyTaskId(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), copyTaskId, systemUserCode) : dataStreamDao.queryDataMoveTaskByCopyTaskIdLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), copyTaskId, systemUserCode);
        loadDataSourceByMoveTasks(dataMoveTaskList);
        return dataMoveTaskList;
    }

    @Override
    public List<DataMoveTaskEntity> queryDataMoveTaskByTaskType(Integer taskType, Integer page, Integer count, String systemUserCode) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.queryDataMoveTaskByTaskType(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), taskType, systemUserCode) : dataStreamDao.queryDataMoveTaskByTaskTypeLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), taskType, systemUserCode);
        loadDataSourceByMoveTasks(dataMoveTaskList);
        return dataMoveTaskList;
    }

    private void setSourceDataSource(DataMoveTaskEntity iterator) throws DataStreamException {
        iterator.getSourceDataBase().setKeyName(SOURCE_DATA_MOVE_SOURCE_KEY_NAME);
        iterator.getSourceDataBase().setDataPoolCount(iterator.getSourcePropertiesDataPoolCount());
        iterator.getSourceDataBase().setSqlValidationQuery(dataSourceFactory.matchDataBase(iterator.getSourceDataBase().getDataBaseType()).makeSqlValidationQuery());
        iterator.getSourceDataBase().setSchemaName(parseSchemaNameJdbcUrl(iterator.getSourceDataBase().getUrl()));
        iterator.getSourceDataBase().setDriverClass(dataSourceFactory.matchDataBase(iterator.getSourceDataBase().getDataBaseType()).getDriverClass());
        iterator.getSourceDataBase().setDataBaseMinIdle(dataStreamConfig.getDataStreamDataSourceMinIdle());
        iterator.getSourceDataBase().setDataBaseMaxWait(dataStreamConfig.getDataStreamDataSourceMaxWait());
        iterator.getSourceDataBase().setDataBaseMinEvictableIdleTimeMillis(dataStreamConfig.getDataStreamDataSourceMinEvictableIdleTimeMillis());
        iterator.getSourceDataBase().setDataBaseTimeBetweenEvictionRunsMillis(dataStreamConfig.getDataStreamDataSourceTimeBetweenEvictionRunsMillis());
    }

    private void setTargetDataSource(DataMoveTaskEntity iterator) throws DataStreamException {
        iterator.getTargetDataBase().setKeyName(SOURCE_DATA_MOVE_TARGET_KEY_NAME);
        iterator.getTargetDataBase().setDataPoolCount(iterator.getTargetPropertiesDataPollCount());
        iterator.getTargetDataBase().setSqlValidationQuery(dataSourceFactory.matchDataBase(iterator.getTargetDataBase().getDataBaseType()).makeSqlValidationQuery());
        iterator.getTargetDataBase().setSchemaName(parseSchemaNameJdbcUrl(iterator.getTargetDataBase().getUrl()));
        iterator.getTargetDataBase().setDriverClass(dataSourceFactory.matchDataBase(iterator.getTargetDataBase().getDataBaseType()).getDriverClass());
        iterator.getTargetDataBase().setDataBaseMinIdle(dataStreamConfig.getDataStreamDataSourceMinIdle());
        iterator.getTargetDataBase().setDataBaseMaxWait(dataStreamConfig.getDataStreamDataSourceMaxWait());
        iterator.getTargetDataBase().setDataBaseMinEvictableIdleTimeMillis(dataStreamConfig.getDataStreamDataSourceMinEvictableIdleTimeMillis());
        iterator.getTargetDataBase().setDataBaseTimeBetweenEvictionRunsMillis(dataStreamConfig.getDataStreamDataSourceTimeBetweenEvictionRunsMillis());
    }

    public void loadTaskExtendParameters(DataMoveTaskEntity task) throws DataStreamException {

        List<TaskExtendEntity> extendList = queryTaskExtend(task.getTaskId());
        if (CollectionUtils.isEmpty(extendList)) {
            return;
        }

        task.setTaskExtendList(extendList);

        taskParamExtend.loadTaskExtendParameters(task);
    }

    private void loadDataSourceByMoveTasks(List<DataMoveTaskEntity> dataMoveTaskList) throws DataStreamException {
        if (CollectionUtils.isEmpty(dataMoveTaskList)) {
            return;
        }

        Map<Long, DataBaseEntity> dataBaseMap = new HashMap<>(dataMoveTaskList.size());
        Map<Long, FileFormatEntity> fileFormatMap = new HashMap<>(dataMoveTaskList.size());
        Map<Long, MQConfigEntity> mqConfigMap = new HashMap<>(dataMoveTaskList.size());

        for (DataMoveTaskEntity iterator : dataMoveTaskList) {
            loadTaskExtendParameters(iterator);

            if (isDataBaseDataSource(iterator.getSourceObjectType()) && !Objects.isNull(iterator.getSourceObjectId())) {
                iterator.setSourceDataBase(getDataBase(iterator.getSourceObjectId(), dataBaseMap));
                if (!Objects.isNull(iterator.getSourceDataBase())) {
                    setSourceDataSource(iterator);
                }
            } else if (isFileDataSource(iterator.getSourceObjectType()) && !Objects.isNull(iterator.getSourceObjectId())) {
                iterator.setSourceFileFormat(getFileFormat(iterator.getSourceObjectId(), fileFormatMap));
            } else if (isMQDataSource(iterator.getSourceObjectType()) && !Objects.isNull(iterator.getSourceObjectId())) {
                iterator.setSourceMQConfig(getMQConfig(iterator.getSourceObjectId(), mqConfigMap));
            }

            if (isDataBaseDataSource(iterator.getTargetObjectType()) && !Objects.isNull(iterator.getTargetObjectId())) {
                iterator.setTargetDataBase(getDataBase(iterator.getTargetObjectId(), dataBaseMap));
                if (!Objects.isNull(iterator.getTargetDataBase())) {
                    setTargetDataSource(iterator);
                }
            } else if (isFileDataSource(iterator.getTargetObjectType()) && !Objects.isNull(iterator.getTargetObjectId())) {
                iterator.setTargetFileFormat(getFileFormat(iterator.getTargetObjectId(), fileFormatMap));
            } else if (isMQDataSource(iterator.getTargetObjectType()) && !Objects.isNull(iterator.getTargetObjectId())) {
                iterator.setTargetMQConfig(getMQConfig(iterator.getTargetObjectId(), mqConfigMap));
            }
        }
    }

    private DataBaseEntity getDataBase(Long dataSourceObjectId, Map<Long, DataBaseEntity> dataBaseMap) throws DataStreamException {
        DataBaseEntity dataBase = null;
        if (dataBaseMap.containsKey(dataSourceObjectId)) {
            dataBase = dataBaseMap.get(dataSourceObjectId);
        } else {
            dataBase = dataStreamDao.getDataBaseById(dataSourceObjectId);
            dataBaseMap.put(dataSourceObjectId, dataBase);
        }
        return dataBase;
    }

    private FileFormatEntity getFileFormat(Long dataSourceObjectId, Map<Long, FileFormatEntity> fileFormatMap) throws DataStreamException {
        FileFormatEntity FileFormat = null;
        if (fileFormatMap.containsKey(dataSourceObjectId)) {
            FileFormat = fileFormatMap.get(dataSourceObjectId);
        } else {
            FileFormat = fileDao.selectFileFormat(dataSourceObjectId);
            fileFormatMap.put(dataSourceObjectId, FileFormat);
        }
        return FileFormat;
    }

    private MQConfigEntity getMQConfig(Long dataSourceObjectId, Map<Long, MQConfigEntity> MQConfigMap) throws DataStreamException {
        MQConfigEntity mqConfig = null;
        if (MQConfigMap.containsKey(dataSourceObjectId)) {
            mqConfig = MQConfigMap.get(dataSourceObjectId);
        } else {
            List<MQConfigEntity> mqConfigList = mqConfigDao.queryMQConfigById(dataSourceObjectId);
            if(!CollectionUtils.isEmpty(mqConfigList)){
                mqConfig = mqConfigList.get(0);
                MQConfigMap.put(dataSourceObjectId, mqConfig);
            }
        }
        return mqConfig;
    }

    @Override
    public List<DataMoveTaskEntity> queryDataMoveTaskByDate(String beginDate, String endDate, Integer page, Integer count, String systemUserCode) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.queryDataMoveTaskByDate(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), geMetaDbObject().stringToDate(beginDate), geMetaDbObject().stringToDate(endDate), systemUserCode) : dataStreamDao.queryDataMoveTaskByDateLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), geMetaDbObject().stringToDate(beginDate), geMetaDbObject().stringToDate(endDate), systemUserCode);

        loadDataSourceByMoveTasks(dataMoveTaskList);
        return dataMoveTaskList;
    }

    @Override
    public List<DataMoveTaskEntity> queryDataMoveTaskByTableName(String tableName, Integer page, Integer count, String systemUserCode) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.queryDataMoveTaskByTableName(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), tableName, systemUserCode) : dataStreamDao.queryDataMoveTaskByTableNameLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), tableName, systemUserCode);
        loadDataSourceByMoveTasks(dataMoveTaskList);
        return dataMoveTaskList;
    }

    @Override
    public Integer updateDataMoveTaskState(Long taskId, Long taskExecuteId, Integer oldState, Integer state, Integer sendMode) throws DataStreamException {
        if (DATA_STREAM_TASK_STATE_RUNNING.equals(state)) {
            return dataStreamDao.updateDataMoveTaskStateByTransaction(geMetaDbObject().makeSqlSystemDate(), taskId, taskExecuteId, oldState, state, sendMode);
        } else {
            return dataStreamDao.updateDataMoveTaskState(geMetaDbObject().makeSqlSystemDate(), taskId, oldState, state, sendMode);
        }
    }

    @Override
    public Integer updateDataMoveTaskLoadStrategy(Long taskId, Integer loadStrategy) throws DataStreamException {
        return dataStreamDao.updateDataMoveTaskLoadStrategy(taskId, loadStrategy);
    }

    @Override
    public void operateMoveTaskStop(Long taskId) throws DataStreamException {
        dataStreamDao.operateMoveTaskStopByTransaction(taskId, geMetaDbObject().makeSqlSystemDate());
    }

    @Override
    public void operateMoveTaskRedo(Long taskId, Integer oldState) throws DataStreamException {
        dataStreamDao.operateMoveTaskRedoByTransaction(geMetaDbObject().makeSqlSystemDate(), taskId, oldState);
    }

    @Override
    public Integer updateDataMoveTaskErrorInfo(Long taskId, Long taskExecuteId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        return dataStreamDao.updateDataMoveTaskErrorInfoByTransaction(geMetaDbObject().makeSqlSystemDate(), taskId, taskExecuteId, oldState, state, errorCode, errorMsg);
    }

    @Override
    public Integer insertDataMoveInfo(List<DataMoveInfoEntity> dataMoveInfoList) throws DataStreamException {
        return (!Arrays.asList(DATA_SOURCE_TYPE_ORACLE, DATA_SOURCE_TYPE_H2).contains(dataStreamConfig.getMetaDbBaseType())) ? dataStreamDao.insertDataMoveInfo(geMetaDbObject().makeSqlSystemDate(), dataMoveInfoList) : dataStreamDao.insertDataMoveInfoByTransaction(geMetaDbObject().makeSqlSystemDate(), dataMoveInfoList);
    }

    @Override
    public Long querySequence(String sequenceName) throws DataStreamException {
        return dataStreamDao.querySequence(sequenceName);
    }

    @Override
    public Integer updateDataMoveTaskTableCount(Long taskId, Integer tableCountFlag, Long tableCount) throws DataStreamException {
        return dataStreamDao.updateDataMoveTaskTableCount(taskId, tableCountFlag, tableCount);
    }

    @Override
    public Integer updateDataMoveTaskTableCountMinMaxValue(Long taskId, Long sourceObjectCount, String sourceTableKeys, String sourceKeysBegin, String sourceKeysEnd) throws DataStreamException {
        return dataStreamDao.updateDataMoveTaskTableCountMinMaxValue(taskId, sourceObjectCount, sourceTableKeys, sourceKeysBegin, sourceKeysEnd);
    }

    @Override
    public Integer updateDataMoveInfoPageRowEnd(Long infoId, String pageRowEnd, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException {
        return dataStreamDao.updateDataMoveInfoPageRowEnd(infoId, pageRowEnd, dataCount, dataActualCount, maxCost, minCost, currentCost);
    }

    @Override
    public Integer updateDataMoveInfoPageRowEndByTrace(Long infoId, String pageRowStart, String pageRowEnd, Long taskId, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException {
        return dataStreamDao.updateDataMoveInfoPageRowEndByTrace(geMetaDbObject().makeSqlSystemDate(), infoId, pageRowStart, pageRowEnd, taskId, dataCount, dataActualCount, maxCost, minCost, currentCost);
    }

    @Override
    public Integer updateDataMoveInfoPageRowEnd(Long infoId, String pageRowEnd, Long taskId, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException {
        return dataStreamDao.updateDataMoveInfoPageRowEnd(infoId, pageRowEnd, dataCount, dataActualCount, maxCost, minCost, currentCost);
    }


    @Override
    public Integer updateDataMoveInfoPageRowStart(Long infoId, String pageRowStart, Integer pageLoopCount) throws DataStreamException {
        return dataStreamDao.updateDataMoveInfoPageRowStart(infoId, pageRowStart, pageLoopCount);
    }

    @Override
    public List<DataMoveTaskEntity> queryTaskByTaskId(Long taskId) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = dataStreamDao.queryTaskByTaskId(taskId);
        loadDataSourceByMoveTasks(dataMoveTaskList);
        return dataMoveTaskList;
    }

    @Override
    public Integer insertDataMoveTask(List<DataMoveTaskEntity> dataMoveTaskList) throws DataStreamException {
        return dataStreamDao.insertDataMoveTask(geMetaDbObject().makeSqlSystemDate(), dataMoveTaskList);
    }

    @Override
    public Integer insertTaskExtend(List<TaskExtendEntity> dataTaskExtendList) throws DataStreamException {
        return dataStreamDao.insertTaskExtend(geMetaDbObject().makeSqlSystemDate(), dataTaskExtendList);
    }

    @Override
    public List<DataMoveProgressEntity> queryDataMoveInfoProgress(Long taskId) throws DataStreamException {
        return dataStreamDao.queryDataMoveInfoProgress(taskId);
    }

    @Override
    public List<DataMoveInfoEntity> queryDataMoveInfo(Long taskId, Integer infoFlag) throws DataStreamException {
        return dataStreamDao.queryDataMoveInfo(taskId, infoFlag);
    }

    @Override
    public List<DataMoveInfoEntity> queryDataMoveInfoByInfoId(Long infoId) throws DataStreamException {
        return dataStreamDao.queryDataMoveInfoByInfoId(infoId);
    }

    @Override
    public List<DataMoveInfoEntity> queryDataMoveInfoByTaskIdAndVirtualId(Long taskId, Integer virtualId, Integer infoFlag, String dataNode) throws DataStreamException {
        return dataStreamDao.queryDataMoveInfoByTaskIdAndVirtualId(taskId, virtualId, infoFlag, dataNode);
    }

    @Override
    public void updateDataMoveInfoErrorInfo(Long infoId, Integer oldState, Integer state, String errorCode, String errorMsg) {
        try {
            Integer updateCount = dataStreamDao.updateDataMoveInfoErrorInfo(geMetaDbObject().makeSqlSystemDate(), infoId, oldState, state, errorCode, errorMsg);
            if (!updateCount.equals(1)) {
                log.error("infoId={}", infoId);
            }
        } catch (DataStreamException aie) {
            log.error("infoId={},DataStreamException={}", infoId, aie);
        } catch (Exception e) {
            log.error("infoId={},Exception=", infoId, e);
        }
    }

    @Override
    public void updateDataMoveInfoById(DataMoveInfoEntity dataMoveInfo) {
        try {
            Integer updateCount = dataStreamDao.updateDataMoveInfoById(geMetaDbObject().makeSqlSystemDate(), dataMoveInfo);
            if (!updateCount.equals(1)) {
                log.error("infoId={}", dataMoveInfo.getInfoId());
            }
        } catch (DataStreamException aie) {
            log.error("infoId={},DataStreamException={}", dataMoveInfo.getInfoId(), aie);
        } catch (Exception e) {
            log.error("infoId={},Exception=", dataMoveInfo.getInfoId(), e);
        }
    }

    @Override
    public void updateDataMoveInfoRunningById(DataMoveInfoEntity dataMoveInfo) {
        try {
            Integer updateCount = dataStreamDao.updateDataMoveInfoRunningById(geMetaDbObject().makeSqlSystemDate(), dataMoveInfo);
            if (!updateCount.equals(1)) {
                log.error("infoId={}", dataMoveInfo.getInfoId());
            }
        } catch (DataStreamException aie) {
            log.error("infoId={},DataStreamException={}", dataMoveInfo.getInfoId(), aie);
        } catch (Exception e) {
            log.error("infoId={},Exception=", dataMoveInfo.getInfoId(), e);
        }
    }

    @Override
    public Integer getMoveTaskCount(String beginDate, String endDate, Integer state, String systemUserCode) throws DataStreamException {
        return dataStreamDao.getMoveTaskCount(StringUtils.isEmpty(beginDate) ? null : geMetaDbObject().stringToDate(beginDate), StringUtils.isEmpty(endDate) ? null : geMetaDbObject().stringToDate(endDate), state, systemUserCode);
    }

    @Override
    public Integer getMoveTaskCountByBatchTaskId(Long batchTaskId, String systemUserCode) throws DataStreamException {
        return dataStreamDao.getMoveTaskCountByBatchTaskId(batchTaskId, systemUserCode);
    }

    @Override
    public Integer getMoveTaskCountByCopyTaskId(Long copyTaskId, String systemUserCode) throws DataStreamException {
        return dataStreamDao.getMoveTaskCountByCopyTaskId(copyTaskId, systemUserCode);
    }

    @Override
    public Integer getMoveTaskCountByTaskType(Integer taskType, String systemUserCode) throws DataStreamException {
        return dataStreamDao.getMoveTaskCountByTaskType(taskType, systemUserCode);
    }

    @Override
    public Integer getMoveTaskCountByTableName(String tableName, String systemUserCode) throws DataStreamException {
        return dataStreamDao.getMoveTaskCountByTableName(tableName, systemUserCode);
    }

    @Override
    public List<TableLinkTaskEntity> queryTableLinkTaskByState(Integer state, Integer page, Integer count) throws DataStreamException {
        List<TableLinkTaskEntity> tableLinkTaskEntityList = (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.queryTableLinkTaskByState(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), state) : dataStreamDao.queryTableLinkTaskByStateLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), state);
        getLinkTaskDataSource(tableLinkTaskEntityList);
        return tableLinkTaskEntityList;
    }

    @Override
    public Integer queryTableLinkTaskByStateCount(Integer state) throws DataStreamException {
        return dataStreamDao.queryTableLinkTaskByStateCount(state);
    }

    private void getLinkTaskDataSource(List<TableLinkTaskEntity> dataBackTaskList) throws DataStreamException {
        if (CollectionUtils.isEmpty(dataBackTaskList)) {
            return;
        }

        for (TableLinkTaskEntity iterator : dataBackTaskList) {
            if (!Objects.isNull(iterator.getSourceDataBaseId())) {
                iterator.setSourceDataSource(dataStreamDao.getDataBaseById(iterator.getSourceDataBaseId()));
                if (!Objects.isNull(iterator.getSourceDataSource())) {
                    iterator.getSourceDataSource().setKeyName(SOURCE_DATA_LINK_SOURCE_KEY_NAME);
                    iterator.getSourceDataSource().setDataPoolCount(dataStreamConfig.getSource().getDataPoolCount());
                    iterator.getSourceDataSource().setSchemaName(parseSchemaNameJdbcUrl(iterator.getSourceDataSource().getUrl()));
                    iterator.getSourceDataSource().setSqlValidationQuery(dataSourceFactory.matchDataBase(iterator.getSourceDataSource().getDataBaseType()).makeSqlValidationQuery());
                    iterator.getSourceDataSource().setDriverClass(dataSourceFactory.matchDataBase(iterator.getSourceDataSource().getDataBaseType()).getDriverClass());
                }
            }

            if (!Objects.isNull(iterator.getTargetDataBaseId())) {
                iterator.setTargetDataSource(dataStreamDao.getDataBaseById(iterator.getTargetDataBaseId()));
                if (!Objects.isNull(iterator.getTargetDataSource())) {
                    iterator.getTargetDataSource().setKeyName(SOURCE_DATA_LINK_TARGET_KEY_NAME);
                    iterator.getTargetDataSource().setDataPoolCount(dataStreamConfig.getTarget().getDataPoolCount());
                    iterator.getTargetDataSource().setSchemaName(parseSchemaNameJdbcUrl(iterator.getTargetDataSource().getUrl()));
                    iterator.getTargetDataSource().setSqlValidationQuery(dataSourceFactory.matchDataBase(iterator.getTargetDataSource().getDataBaseType()).makeSqlValidationQuery());
                    iterator.getTargetDataSource().setDriverClass(dataSourceFactory.matchDataBase(iterator.getTargetDataSource().getDataBaseType()).getDriverClass());
                }
            }
        }
    }

    @Override
    public List<TableLinkTaskEntity> queryTableLinkTaskByLinkTaskId(Long linkTaskId) throws DataStreamException {
        List<TableLinkTaskEntity> tableLinkTaskEntityList = dataStreamDao.queryTableLinkTaskByTaskId(linkTaskId);
        getLinkTaskDataSource(tableLinkTaskEntityList);
        return tableLinkTaskEntityList;
    }

    @Override
    public List<TableLinkTaskEntity> queryTableLinkTaskByTableLinkId(Long tableLinkId) throws DataStreamException {
        return dataStreamDao.queryTableLinkTaskByTableLinkId(tableLinkId);
    }

    @Override
    public List<TableLinkTaskEntity> queryTableLinkTaskByDate(String beginDate, String endDate, Integer page, Integer count) throws DataStreamException {
        List<TableLinkTaskEntity> tableLinkTaskEntityList = (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.queryTableLinkTaskByDate(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), geMetaDbObject().stringToDate(beginDate), geMetaDbObject().stringToDate(endDate)) : dataStreamDao.queryDataBackTaskByDateLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), geMetaDbObject().stringToDate(beginDate), geMetaDbObject().stringToDate(endDate));
        getLinkTaskDataSource(tableLinkTaskEntityList);
        return tableLinkTaskEntityList;
    }

    @Override
    public Integer queryTableLinkTaskByDateCount(String beginDate, String endDate) throws DataStreamException {
        return dataStreamDao.queryTableLinkTaskByDateCount(geMetaDbObject().stringToDate(beginDate), geMetaDbObject().stringToDate(endDate));
    }

    @Override
    public Integer updateTableLinkTask(Long linkTaskId, Integer oldState, Integer state, String hostName, String hostIp) throws DataStreamException {
        return dataStreamDao.updateTableLinkTask(geMetaDbObject().makeSqlSystemDate(), linkTaskId, oldState, state, hostName, hostIp);
    }

    @Override
    public Integer updateTableLinkErrorTask(Long linkTaskId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        return dataStreamDao.updateTableLinkErrorTask(geMetaDbObject().makeSqlSystemDate(), linkTaskId, oldState, state, errorCode, errorMsg);
    }

    @Override
    public Integer insertLinkTaskTable(LinkTaskTableEntity dataBackTable) throws DataStreamException {
        return dataStreamDao.insertLinkTaskTable(geMetaDbObject().makeSqlSystemDate(), dataBackTable);
    }

    @Override
    public Integer insertTableLinkTask(TableLinkTaskEntity tableLinkTask) throws DataStreamException {
        return dataStreamDao.insertTableLinkTask(geMetaDbObject().makeSqlSystemDate(), tableLinkTask);
    }

    @Override
    public List<DataBaseEntity> queryDataBase(Integer queryFlag, Long queryValue, Integer state, Integer page, Integer count) throws DataStreamException {
        return (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ?
                dataStreamDao.queryDataBase(geMetaDbObject().makeSqlLimit(genPageRow(page, count), count), queryFlag, queryValue, state) :
                dataStreamDao.queryDataBaseLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), queryFlag, queryValue, state);
    }

    @Override
    public Integer getDataBaseCount(Integer queryFlag, Long queryValue, Integer state) throws DataStreamException {
        return dataStreamDao.getDataBaseCount(queryFlag, queryValue, state);
    }


    @Override
    public Integer insertDataBase(DataBaseEntity dataBase) throws DataStreamException {
        return dataStreamDao.insertDataBase(geMetaDbObject().makeSqlSystemDate(), dataBase);
    }

    @Override
    public Integer updateDataBase(DataBaseEntity dataBase) throws DataStreamException {
        return dataStreamDao.updateDataBase(geMetaDbObject().makeSqlSystemDate(), dataBase);
    }

    @Override
    public Integer updateDataBaseState(Long dataBaseId, Integer state) throws DataStreamException {
        return dataStreamDao.updateDataBaseState(geMetaDbObject().makeSqlSystemDate(), dataBaseId, state);
    }

    private IDatabaseAdapter geMetaDbObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    @Override
    public Integer statMoveTaskCount(String state) throws DataStreamException {
        return dataStreamDao.statMoveTaskCount(state);
    }

    @Override
    public Integer statLinkTaskCount(String state) throws DataStreamException {
        return dataStreamDao.statLinkTaskCount(state);
    }

    @Override
    public List<StatDayCountEntity> statMoveTaskCountGroupByDay(Integer days) throws DataStreamException {
        return dataStreamDao.statMoveTaskCountGroupByDay(geMetaDbObject().makeSqlIntervalDay(days));
    }

    @Override
    public List<StatDayCountEntity> statLinkTaskCountGroupByDay(Integer days) throws DataStreamException {
        return dataStreamDao.statLinkTaskCountGroupByDay(geMetaDbObject().makeSqlIntervalDay(days));
    }

    @Override
    public void initDataStreamMetaDb() throws DataStreamException {
        List<String> sqlList = geMetaDbObject().makeInitDataStreamMetaDbSql();
        sqlList.forEach(x -> {
            try {
                dataStreamDao.executeMetaDbSql(x);
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Integer insertMetrics(List<MetricsEntity> metricsList) throws DataStreamException {
        return dataStreamDao.insertMetrics(metricsList);
    }

    @Override
    public List<MetricsEntity> queryMetrics(Long taskId) throws DataStreamException {
        return dataStreamDao.queryMetrics(taskId);
    }

    @Override
    public List<TaskExecuteEntity> queryTaskExecute(Long taskId) throws DataStreamException {
        return dataStreamDao.queryTaskExecute(taskId);
    }

    @Override
    public List<TaskExtendEntity> queryTaskExtend(Long taskId) throws DataStreamException {
        return dataStreamDao.queryTaskExtend(taskId);
    }

    @Override
    public Integer insertMoveTable(List<MoveTableEntity> moveTableList) throws DataStreamException {
        return dataStreamDao.insertMoveTable(geMetaDbObject().makeSqlSystemDate(), moveTableList);
    }

    @Override
    public List<MoveTableEntity> queryMoveTable(Long taskId) throws DataStreamException {
        return dataStreamDao.queryMoveTable(taskId);
    }

    @Override
    public Integer updateMoveTableErrorInfo(Long moveTableId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        return dataStreamDao.updateMoveTableErrorInfo(geMetaDbObject().makeSqlSystemDate(), moveTableId, oldState, state, errorCode, errorMsg);
    }

    @Override
    public Integer updateMoveTableSQL(Long moveTableId, String tableSql) throws DataStreamException {
        return dataStreamDao.updateMoveTableSQL(moveTableId, tableSql);
    }

    @Override
    public Integer insertDataCheck(List<DataCheckEntity> dataCheckList) throws DataStreamException {
        return dataStreamDao.insertDataCheck(geMetaDbObject().makeSqlSystemDate(), dataCheckList);
    }

    @Override
    public List<DataCheckEntity> queryDataCheck(Long taskId) throws DataStreamException {
        return dataStreamDao.queryDataCheck(taskId);
    }

    @Override
    public List<DataCheckEntity> queryDataCheckById(Long dataCheckId) throws DataStreamException {
        return dataStreamDao.queryDataCheckById(dataCheckId);
    }

    @Override
    public Integer updateDataCheck(Long dataCheckId, Integer oldState, Integer state, String errorCode, String errorMsg) throws DataStreamException {
        return dataStreamDao.updateDataCheck(geMetaDbObject().makeSqlSystemDate(), dataCheckId, oldState, state, errorCode, errorMsg);
    }

    @Override
    public List<ColumnTypeDefineEntity> queryColumnTypeDefine(String databaseType) throws DataStreamException {
        return dataStreamDao.queryColumnTypeDefine(databaseType);
    }

    @Override
    public List<ColumnTypeMapEntity> queryColumnTypeMap(String databaseTypeA, String databaseTypeB) throws DataStreamException {
        return dataStreamDao.queryColumnTypeMap(databaseTypeA, databaseTypeB);
    }

    @Override
    public Integer insertColumnTypeTest(List<ColumnTypeTestEntity> columnTypeTestList) throws DataStreamException {
        return dataStreamDao.insertColumnTypeTest(columnTypeTestList);
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public void createTableMoveTask(List<DataMoveTaskEntity> dataMoveTaskList, List<MoveTableEntity> moveTableList, List<TaskExtendEntity> dataTaskExtendList) throws DataStreamException {
        if (insertDataMoveTask(dataMoveTaskList) != dataMoveTaskList.size()) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_CREATE_INSERT_ERROR);
        }

        insertMoveTable(moveTableList);

        insertTaskExtend(dataTaskExtendList);
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public void createDataMoveTask(List<DataMoveTaskEntity> dataMoveTaskList, List<TaskExtendEntity> dataTaskExtendList) throws DataStreamException {
        if (insertDataMoveTask(dataMoveTaskList) != dataMoveTaskList.size()) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_CREATE_INSERT_ERROR);
        }
        insertTaskExtend(dataTaskExtendList);
    }
}

