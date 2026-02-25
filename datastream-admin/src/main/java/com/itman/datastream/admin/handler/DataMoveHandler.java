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


import cn.hutool.extra.spring.SpringUtil;
import com.itman.datastream.admin.controller.domain.request.QueryDataMoveInfoRequest;
import com.itman.datastream.admin.controller.domain.request.QueryDataMoveTaskRequest;
import com.itman.datastream.common.api.IMQAdapterApi;
import com.itman.datastream.common.extend.TaskParamExtend;
import com.itman.datastream.common.api.IFileApi;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.engine.holder.CurrentValueObject;
import com.itman.datastream.engine.holder.DataStreamHolder;
import com.itman.datastream.engine.route.DataBaseSource;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.systemlog.ISystemLogEvent;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.admin.service.IMoveSourceService;
import com.itman.datastream.admin.service.IMoveTargetService;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.engine.context.ThreadContext.removeThreadLocalJob;
import static com.itman.datastream.engine.context.ThreadContext.setThreadLocalJob;
import static com.itman.datastream.common.utils.CommUtils.*;


@Slf4j
@Component
public class DataMoveHandler extends AbstractHandler {
    private final IMetaService metaService;
    private final IMoveSourceService moveSourceService;
    private final DataBaseSource dataBaseSource;
    private final ISystemLogEvent systemLogEvent;
    private final DataStreamHolder dataStreamHolder;
    private final TaskParamExtend taskParamExtend;

    public DataMoveHandler(DataSourceFactory dataSourceFactory, IMetaService metaService, IMoveSourceService moveSourceService, IMoveTargetService moveTargetService, DataStreamConfig dataStreamConfig, DataBaseSource dataBaseSource, ISystemLogEvent systemLogEvent, DataStreamHolder dataStreamHolder, TaskParamExtend taskParamExtend) {
        super(dataSourceFactory, dataStreamConfig, metaService, dataStreamHolder, moveTargetService);
        this.metaService = metaService;
        this.moveSourceService = moveSourceService;
        this.dataBaseSource = dataBaseSource;
        this.systemLogEvent = systemLogEvent;
        this.dataStreamHolder = dataStreamHolder;
        this.taskParamExtend = taskParamExtend;
    }

    private static final ConcurrentHashMap<Long, Object> taskLocks = new ConcurrentHashMap<>();

    public Integer getDataStreamQueueRunningSize(Long taskId) {
        return dataStreamHolder == null ? 0 : dataStreamHolder.getQueueRunningSize(taskId);
    }

    public Integer getDataStreamQueueMaxSize(Integer SourcePropertiesSelectCount, Integer dataStreamQueueChannel) {
        return SourcePropertiesSelectCount * dataStreamConfig.getDataStreamQueueSize() * dataStreamQueueChannel;
    }

    private boolean isSourceFinished(Long taskId) {
        if (dataStreamHolder.getTaskAllSourceThreadFinished(taskId).equals(1)) {
            return true;
        }
        return false;
    }

    public void sourceSelectFinishLock(Long taskId) {
        dataStreamHolder.setTaskAllSourceThreadFinished(taskId);
    }

    public void initTaskResource(final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        Long taskId = dataMoveTask.getTaskId();
        Integer SourcePropertiesSelectCount = dataMoveTask.getSourcePropertiesSelectCount();
        Integer dataStreamQueueChannel = dataMoveTask.getDataStreamQueueChannel();
        dataStreamHolder.initTaskQueue(taskId, dataStreamQueueChannel, SourcePropertiesSelectCount, dataStreamConfig.getDataStreamQueueSize());

        //todo 后续优化下沉到各个模块中
        if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            IMQAdapterApi mqAdapterApi = matchMQ(dataMoveTask.getTargetObjectType());
            Map<String, Object> additionalProps = new HashMap<>();
            String destination = dataMoveTask.getTargetObjectName();
            String bootstrapServers = dataMoveTask.getTargetMQConfig().getBootstrapServers();
            mqAdapterApi.bindProducerDestination(taskId, destination, bootstrapServers, additionalProps);
        } else if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            IFileApi fileApi = matchFileFormat(dataMoveTask.getTargetObjectType());
            fileApi.specialHeadDataWriteIntoFile(dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), dataMoveTask.getTargetFileFormat());
        }
    }

    public void releaseTaskResource(final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        dataStreamHolder.clearTaskQueue(dataMoveTask.getTaskId());
        taskLocks.remove(dataMoveTask.getTaskId());
        dataStreamHolder.clearTaskState(dataMoveTask.getTaskId());

        //todo 后续下沉到各个模块中
        if (isFileDataSource(dataMoveTask.getSourceObjectType())) {
            IFileApi fileApi = matchFileFormat(dataMoveTask.getSourceObjectType());
            fileApi.releaseFileResource(dataMoveTask.getTaskId(), dataMoveTask.getSourceFileFormat().getFileFormatId());
        }

        if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            IFileApi fileApi = matchFileFormat(dataMoveTask.getTargetObjectType());
            fileApi.releaseFileResource(dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), dataMoveTask.getTargetFileFormat());
        } else if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            IMQAdapterApi mqAdapterApi = matchMQ(dataMoveTask.getTargetObjectType());
            mqAdapterApi.unbindProducerDestination(dataMoveTask.getTaskId());
        }
    }

    public CurrentValueObject fetchAndUpdateNextPageRowNumLock(DataMoveTaskEntity dataMoveTask) {
        Object taskLock = taskLocks.computeIfAbsent(dataMoveTask.getTaskId(), k -> new Object());
        synchronized (taskLock) {
            String taskInitValue = dataMoveTask.getTaskId() + "_init";
            String taskEndValue = dataMoveTask.getTaskId() + "_end";
            CurrentValueObject currentValueObject = dataStreamHolder.getCurrentValueHashMap(dataMoveTask.getTaskId());
            //下面取值一定要用returnValueObject，不能用currentValueObject，这个一直应用缓存变量，会变化
            CurrentValueObject returnValueObject = new CurrentValueObject();
            BeanUtils.copyProperties(currentValueObject, returnValueObject);
            if (returnValueObject.getCurrentValue().equals(taskInitValue)) {
                returnValueObject.setCurrentValue("0");
                returnValueObject.setPageLoopCount(1);
            } else if (returnValueObject.getCurrentValue().equals(taskEndValue)) {
                //结束
                return null;
            }

            Integer beginPageRowNum = Integer.parseInt(returnValueObject.getCurrentValue());
            Integer nextBeginValue = beginPageRowNum + dataMoveTask.getSourcePropertiesSelectCount();

            if (nextBeginValue > dataMoveTask.getSourceObjectCount()) {
                //通知结束
                dataStreamHolder.setCurrentValueHashMap(dataMoveTask.getTaskId(), taskEndValue, -1);
            } else {
                dataStreamHolder.setCurrentValueHashMap(dataMoveTask.getTaskId(), nextBeginValue.toString(), (returnValueObject.getPageLoopCount() + 1));
            }
            return returnValueObject;
        }
    }

    public void initCurrentValueLock(Long taskId, String currentValue, Integer pageLoopCount) {
        dataStreamHolder.setCurrentValueHashMap(taskId, currentValue, pageLoopCount);
    }

    /**
     * 如果是生产和消费1:1并行优点：在应用异常退出情况能精准找到断点数据，但是多个线程可能存在长尾效应，目前线程可能消费很慢
     *
     * @param dataMoveTask
     * @throws DataStreamException
     */
    public void splitDataRangeForParallelProcessing(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        if (!(dataMoveTask.getSourceLoadStrategy().equals(LOAD_STRATEGY_BY_DATA_PART) && dataMoveTask.getDataStreamQueueChannel().equals(dataMoveTask.getSourcePropertiesThreadCount()) && dataMoveTask.getDataStreamQueueChannel().equals(dataMoveTask.getTargetPropertiesThreadCount()))) {
            return;
        }

        int sourceCount = dataMoveTask.getSourceObjectCount().intValue();
        int channelCount = dataMoveTask.getDataStreamQueueChannel();
        //计算每个通道分配到数据记录个数
        int dataSplitSize = (sourceCount + channelCount - 1) / channelCount;
        dataMoveTask.setDataRangSplitSize(dataSplitSize);
        dataMoveTask.setDataRangSplitMap(new HashMap<>(channelCount));
        String beginKeysValue = null;
        for (int i = 1; i <= channelCount; i++) {
            if (i == channelCount) {
                dataMoveTask.setDataRangSplitSize(sourceCount - (dataSplitSize) * (i - 1));
            }

            List<Map> endRowValueList = moveSourceService.executeSelectMapListSql(dataMoveTask.getSourceObjectId(), makeSqlCurrentPageMaxKeyValueWrap(beginKeysValue, dataMoveTask));
            String endKeysValue = null;
            if (!CollectionUtils.isEmpty(endRowValueList)) {
                endKeysValue = generateKeyValuesString(endRowValueList.get(0), dataMoveTask.getSourceTableKeysList());
            }

            if (i == channelCount) {
                if (!endKeysValue.equals(dataMoveTask.getSourceKeysEnd())) {
                    throw new DataStreamException(OPER_DATA_RANG_SPLIT_KEYS_NOT_EQUAL_ERROR);
                }
            }

            Map<String, String> dataSplitMapTemp = new HashMap<>(1);
            dataSplitMapTemp.put(beginKeysValue, endKeysValue);
            dataMoveTask.getDataRangSplitMap().put(i, dataSplitMapTemp);
            beginKeysValue = endKeysValue;
        }

        if (dataMoveTask.getDataRangSplitMap().size() != channelCount) {
            throw new DataStreamException(OPER_DATA_RANG_SPLIT_NOT_EQUAL_CHANNEL_ERROR);
        }
    }

    /**
     * 获取缓存存储的当前数据段取数可用值，同时更新下一个可用值
     *
     * @param dataMoveTask
     * @return
     * @throws DataStreamException
     */
    public CurrentValueObject fetchAndUpdateNextSegmentValueForPart(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        Object taskLock = taskLocks.computeIfAbsent(dataMoveTask.getTaskId(), k -> new Object());
        synchronized (taskLock) {
            String taskInitValue = dataMoveTask.getTaskId() + "_init";
            String taskEndValue = dataMoveTask.getTaskId() + "_end";
            //如果为空就算结束
            CurrentValueObject currentValueObject = dataStreamHolder.getCurrentValueHashMap(dataMoveTask.getTaskId());
            //下面取值一定要用currentValueObjectNew，不能用currentValueObject，这个一直应用缓存变量，会变化
            CurrentValueObject returnValueObject = new CurrentValueObject();
            BeanUtils.copyProperties(currentValueObject, returnValueObject);
            String startKeyValue = currentValueObject.getCurrentValue();
            if (returnValueObject.getCurrentValue().equals(taskInitValue)) {
                //如果是null认为还没开始，当前线程抢占首次,当前返回数据里最小值
                returnValueObject.setCurrentValue(dataMoveTask.getSourceKeysBegin());
                returnValueObject.setPageLoopCount(1);
                //加载数据值第一次必须是null,拼装sql用的是大于，没有大于等于
                startKeyValue = null;
            } else if (returnValueObject.getCurrentValue().equals(taskEndValue)) {
                //结束
                return null;
            }

            //获取下个数据段开始值
            List<Map> nextBeginValueList = moveSourceService.executeSelectMapListSql(dataMoveTask.getSourceObjectId(), makeSqlCurrentPageMaxKeyValueWrap(startKeyValue, dataMoveTask));
            if (CollectionUtils.isEmpty(nextBeginValueList)) {
                //通知结束
                dataStreamHolder.setCurrentValueHashMap(dataMoveTask.getTaskId(), taskEndValue, -1);
            } else {
                //缓存下一个加载开始数据供其他线程抢占处理
                String nextBeginValue = generateKeyValuesString(nextBeginValueList.get(0), dataMoveTask.getSourceTableKeysList());
                dataStreamHolder.setCurrentValueHashMap(dataMoveTask.getTaskId(), nextBeginValue, (returnValueObject.getPageLoopCount() + 1));
            }

            return returnValueObject;
        }
    }

    /**
     * 获取记录包括数据分页和分段
     * 分页：获取分页ID
     * 分段：获取下个数据开始主键值
     *
     * @param infoId
     * @param dataMoveTask
     * @return
     * @throws DataStreamException
     */
    private String fetchAndUpdateNextSegmentValue(Long infoId, DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        DataMoveInfoEntity dataMoveInfoFromMem = dataStreamHolder.getDataMoveInfo(infoId);

        CurrentValueObject currentValueObject = null;
        if (dataMoveInfoFromMem.getPageRowStart().equals(dataMoveInfoFromMem.getPageRowEnd())) {
            if (dataMoveTask.getSourceLoadStrategy().equals(LOAD_STRATEGY_BY_DATA_PART)) {
                //分段,可能联合主键
                currentValueObject = fetchAndUpdateNextSegmentValueForPart(dataMoveTask);
            } else {
                //分页处理
                currentValueObject = fetchAndUpdateNextPageRowNumLock(dataMoveTask);
            }
        } else {
            //不一致说明之前应用异常停止后重启处理
            currentValueObject = new CurrentValueObject();
            currentValueObject.setCurrentValue(dataMoveInfoFromMem.getPageRowStart());
            currentValueObject.setPageLoopCount(dataMoveInfoFromMem.getPageLoopCount());
        }

        if (currentValueObject == null || currentValueObject.getCurrentValue() == null || currentValueObject.getCurrentValue().equals("null")) {
            return null;
        }

        //记录当前值
        if (currentValueObject.getCurrentValue() != null && currentValueObject.getCurrentValue() != "null") {
            metaService.updateDataMoveInfoPageRowStart(infoId, currentValueObject.getCurrentValue(), currentValueObject.getPageLoopCount());
            dataStreamHolder.updateDataMoveInfoPageRowStart(infoId, currentValueObject.getCurrentValue(), currentValueObject.getPageLoopCount());
        }
        return currentValueObject.getCurrentValue();
    }

    /**
     * 断点续传的使用
     *
     * @param dataMoveInfoBreak
     * @param dataMoveTask
     * @return
     * @throws DataStreamException
     */
    public void fetchAndUpdateNextSegmentValueForBreak(DataMoveInfoEntity dataMoveInfoBreak, DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        dataStreamHolder.setCurrentValueHashMap(dataMoveTask.getTaskId(), dataMoveInfoBreak.getPageRowStart(), dataMoveInfoBreak.getPageLoopCount());
        if (dataMoveInfoBreak.getPageRowStart().equals(dataMoveInfoBreak.getPageRowEnd())) {
            if (dataMoveTask.getSourceLoadStrategy().equals(LOAD_STRATEGY_BY_DATA_PART)) {
                //分段,可能联合主键
                fetchAndUpdateNextSegmentValueForPart(dataMoveTask);
            } else {
                //分页处理
                fetchAndUpdateNextPageRowNumLock(dataMoveTask);
            }
        }
    }

    private String makeSelectStrategy(String selectColumnSql, String selectCondition, String beginRowValue, String sourceTableRowMinValue, Integer loadStrategy, String tableKeys, Integer dataSourceType, Integer SourcePropertiesSelectCount) throws DataStreamException {
        return super.matchDataBase(dataSourceType).makeSqlSelectByPage(loadStrategy, selectColumnSql, selectCondition, beginRowValue, sourceTableRowMinValue, tableKeys, SourcePropertiesSelectCount);
    }

    public void dataProducer(final Integer virtualId, final DataMoveTaskEntity dataMoveTask) {
        if (!CollectionUtils.isEmpty(dataMoveTask.getDataRangSplitMap())) {
            dataProducer2(virtualId, dataMoveTask);
        } else {
            dataMoveTask.setDataRangSplitSize(null);
            dataProducer1(virtualId, dataMoveTask);
        }
    }

    @Data
    public class TimeCostStore {
        private Instant startInstant;
        private Long currentCost = 0L;
        private Long minCost = 0L;
        private Long maxCost = 0L;
    }


    private void calTimeCost(TimeCostStore timeCostStore) {
        Instant endInstant = Instant.now();

        Long currentCost = Duration.between(timeCostStore.getStartInstant(), endInstant).toMillis();
        Long maxCost = currentCost > timeCostStore.getMaxCost() ? currentCost : timeCostStore.getMinCost();
        Long minCost = (timeCostStore.getMinCost().equals(0L)) ? currentCost : (currentCost < timeCostStore.getMinCost() ? currentCost : timeCostStore.getMinCost());
        timeCostStore.setMaxCost(maxCost);
        timeCostStore.setMinCost(minCost);
    }

    @Setter
    @Getter
    private class ProducerDataResult {
        private String beginRowValue;
        private List<Map> dataRowList;
    }

    private Map<String, Object> convertKeysToLowerCase(Map<String, Object> originalMap) {
        Map<String, Object> resultMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
            // 将key转换为小写，value保持不变
            resultMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }

        return resultMap;
    }

    private ProducerDataResult fetchSourceData(final Long infoId, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        String beginRowValue = null;
        List<Map> dataRowList = null;
        //每次加载数据数量
        int sourcePropertiesSelectCount = dataMoveTask.getSourcePropertiesSelectCount();
        if (isDataBaseDataSource(dataMoveTask.getSourceObjectType())) {
            //beginRowValue当前加载数据开始值，分段方式是主键、分页方式是页数
            //同时获取下一个加载值缓存到变量里
            beginRowValue = fetchAndUpdateNextSegmentValue(infoId, dataMoveTask);
            if (beginRowValue == null) {
                //说明没有数据结束
                return new ProducerDataResult();
            }

            //组装加载数据sql进行数据加载
            Integer loadStrategy = dataMoveTask.getSourceLoadStrategy();
            String selectColumnSql = dataMoveTask.getSourceSelectSql();
            String selectCondition = dataMoveTask.getSourceObjectCondition();
            String sourceTableKeys = dataMoveTask.getSourceObjectKeys();
            String sourceTableRowMinValue = dataMoveTask.getSourceKeysBegin();
            Integer dataSourceType = dataMoveTask.getSourceDataBase().getDataBaseType();
            String selectSql = makeSelectStrategy(selectColumnSql, selectCondition, beginRowValue, sourceTableRowMinValue, loadStrategy, sourceTableKeys, dataSourceType, sourcePropertiesSelectCount);
            dataRowList = moveSourceService.executeSelectMapListSql(dataMoveTask.getSourceObjectId(), selectSql);
            //如果是oralce把map里key字段名都是大写,需要都改成小写
            if (!CollectionUtils.isEmpty(dataRowList)) {
                String firstStringKey = dataRowList.get(0).keySet().iterator().next().toString();
                if (Character.isUpperCase(firstStringKey.charAt(0))) {
                    List<Map> resultList = new ArrayList<>();
                    for (Map<String, Object> map : dataRowList) {
                        resultList.add(convertKeysToLowerCase(map));
                    }
                    // 重新赋值
                    dataRowList = resultList;
                }
            }
        } else if (isFileDataSource(dataMoveTask.getSourceObjectType())) {
            //从文件里获取数据
            IFileApi fileApi = matchFileFormat(dataMoveTask.getSourceObjectType());
            dataRowList = fileApi.parseFileLineData(dataMoveTask.getTaskId(), dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceFileFormat(), sourcePropertiesSelectCount);
        }

        ProducerDataResult producerDataResult = new ProducerDataResult();
        producerDataResult.setDataRowList(dataRowList);
        producerDataResult.setBeginRowValue(beginRowValue);
        return producerDataResult;
    }

    /**
     * 非单通道模式：生产线程精确关心线程运行记录begin、end字段,如果值都一样说明加载加载发送完成
     *
     * @param virtualId
     * @param dataMoveTask
     */
    public void dataProducer1(final Integer virtualId, final DataMoveTaskEntity dataMoveTask) {
        String errorCode = null;
        String errorMessage = null;
        DataMoveInfoEntity dataMoveInfo = null;
        TimeCostStore timeCostStore = new TimeCostStore();

        try {
            dataMoveInfo = obtainSourceDataMoveInfo(virtualId, dataMoveTask);
            dataMoveInfo.setOldState(dataMoveInfo.getState());
            timeCostStore.setMinCost(dataMoveInfo.getMinCost());
            timeCostStore.setMaxCost(dataMoveInfo.getMaxCost());

            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), "dataProducer start");
            setThreadLocalJob(dataMoveInfo.getInfoId(), JOB_TYPE_MOVE_INFO);


            while (true) {
                timeCostStore.setStartInstant(Instant.now());

                verifyTaskShouldContinue(dataMoveTask.getTaskId(), virtualId);

                //获取源对象数据
                ProducerDataResult producerDataResult = fetchSourceData(dataMoveInfo.getInfoId(), dataMoveTask);
                List<Map> dataRowList = producerDataResult.getDataRowList();
                String beginRowValue = producerDataResult.getBeginRowValue();
                if (CollectionUtils.isEmpty(dataRowList)) {
                    break;
                }

                int dataCount = dataRowList.size();
                Integer dataActualCount = 0;
                if (dataMoveTask.getSendMode().equals(SOURCE_SEND_MODE_ASYNC)) {
                    dataStreamHolder.addQueue(dataMoveTask.getTaskId(), selectChannelId(virtualId, dataMoveTask.getDataStreamQueueChannel()), dataRowList);
                    dataActualCount = dataRowList.size();
                } else if (dataMoveTask.getSendMode().equals(SOURCE_SEND_MODE_SYNC)) {
                    if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_DEL)) {
                        dataActualCount = deleteDataFromSourceTable(dataRowList, dataMoveTask);
                    } else {
                        dataActualCount = handleWriteByDataSource(dataRowList, dataMoveTask);
                    }

                    if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_MOVE_DEL)) {
                        dataActualCount = deleteDataFromSourceTable(dataRowList, dataMoveTask);
                    }
                }

                String endRowValue = null;
                if (isDataBaseDataSource(dataMoveTask.getSourceObjectType())) {
                    endRowValue = generateKeyValuesString(dataRowList.get(dataCount - 1), dataMoveTask.getSourceTableKeysList());
                }

                //计算耗时
                calTimeCost(timeCostStore);

                log.info("linkTaskId={},infoId={},virtualId={},sourceObjectName={},nodeName={}, beginRowValue={},sourceSelectCount={},dataRowList.size={},dataActualCount={},loadCostTime={},loadStrategy={}", dataMoveTask.getTaskId(), dataMoveInfo.getInfoId(), virtualId, dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceDataNode(), beginRowValue, dataMoveTask.getSourcePropertiesSelectCount(), dataCount, dataActualCount, timeCostStore.getCurrentCost(), dataMoveTask.getSourceLoadStrategy());
                saveDataMoveInfoPageRowEnd(dataMoveInfo.getInfoId(), beginRowValue, endRowValue, dataMoveTask.getTaskId(), dataCount, dataActualCount, timeCostStore.getMaxCost(), timeCostStore.getMinCost(), timeCostStore.getCurrentCost());

                if (dataRowList.size() != dataMoveTask.getSourcePropertiesSelectCount()) {
                    break;
                }
            }
        } catch (DataStreamException aie) {
            log.error("linkTaskId={},sourceObjectName={},nodeName={} DataStreamException={}", dataMoveTask.getTaskId(), dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceDataNode(), aie);
            errorCode = aie.getErrCode();
            errorMessage = aie.getMessage();
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), getStackTraceAsString(aie));
        } catch (Exception e) {
            log.error("linkTaskId={},sourceObjectName={},nodeName={} Exception={}", dataMoveTask.getTaskId(), dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceDataNode(), e);
            errorCode = DataStreamErrorCode.UNKNOWN_ERROR.getCode();
            errorMessage = DataStreamErrorCode.UNKNOWN_ERROR.getMessage();
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), getStackTraceAsString(e));
        }

        if (!StringUtils.isEmpty(errorCode)) {
            dataStreamHolder.setErrorSourceVirtualId(dataMoveTask.getTaskId(), virtualId);
        }

        updateDataMoveInfoResult(dataMoveTask.getTaskId(), dataMoveInfo, errorCode, errorMessage);

        jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), "dataProducer end");
        removeThreadLocalJob();
    }

    private void updateDataMoveInfoResult(Long taskId, DataMoveInfoEntity dataMoveInfo, String errorCode, String errorMessage) {
        dataMoveInfo.setState((StringUtils.isEmpty(errorCode) ? DATA_STREAM_TASK_STATE_FINISH : DATA_STREAM_TASK_STATE_ERROR));
        dataMoveInfo.setErrorCode(errorCode);
        dataMoveInfo.setErrorMsg(errorMessage);
        DataMoveInfoEntity dataMoveInfoFromMem = dataStreamHolder.getDataMoveInfo(dataMoveInfo.getInfoId());
        if (dataMoveInfoFromMem != null) {
            dataMoveInfo.setDataCount(dataMoveInfoFromMem.getDataCount());
            dataMoveInfo.setDataActualCount(dataMoveInfoFromMem.getDataActualCount());
            dataMoveInfo.setMinCost(dataMoveInfoFromMem.getMinCost());
            dataMoveInfo.setMaxCost(dataMoveInfoFromMem.getMaxCost());
            dataMoveInfo.setLatelyCost(dataMoveInfoFromMem.getLatelyCost());
            dataMoveInfo.setSumCost(dataMoveInfoFromMem.getSumCost());
        }

        //clear放在更新之前，防止主线程里异步同时刷数据
        if (dataMoveInfo.getInfoFlag().equals(MOVE_INFO_FLAG_SOURCE)) {
            dataStreamHolder.clearSourceDataMoveInfo(taskId, dataMoveInfo.getInfoId());
        } else if (dataMoveInfo.getInfoFlag().equals(MOVE_INFO_FLAG_TARGET)) {
            dataStreamHolder.clearTargetDataMoveInfo(taskId, dataMoveInfo.getInfoId());
        }

        dataMoveInfo.setPageRowStart(dataMoveInfo.getPageRowStart() == null ? "null" : dataMoveInfo.getPageRowStart());
        dataMoveInfo.setPageRowEnd(dataMoveInfo.getPageRowEnd() == null ? "null" : dataMoveInfo.getPageRowEnd());

        metaService.updateDataMoveInfoById(dataMoveInfo);
    }

    public void syncDataMoveInfoRunning(Long taskId) {
        while (true) {
            log.info("syncDataMoveInfoRunning sleep linkTaskId={}", taskId);

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            List<Long> sourceMoveInfoIdList = dataStreamHolder.getSourceMoveInfoId(taskId);
            List<Long> targetMoveInfoIdList = dataStreamHolder.getTargetMoveInfoId(taskId);
            if (CollectionUtils.isEmpty(sourceMoveInfoIdList) && CollectionUtils.isEmpty(targetMoveInfoIdList)) {
                log.info("syncDataMoveInfoRunning break linkTaskId={}", taskId);

                break;
            }

            if (!CollectionUtils.isEmpty(sourceMoveInfoIdList)) {
                for (Long moveInfoId : sourceMoveInfoIdList) {
                    DataMoveInfoEntity dataMoveInfoFromMem = dataStreamHolder.getDataMoveInfo(moveInfoId);
                    if (dataMoveInfoFromMem != null) {
                        metaService.updateDataMoveInfoRunningById(dataMoveInfoFromMem);
                    }
                }
            } else {
                log.info("syncDataMoveInfoRunning sourceMoveInfoIdList is null linkTaskId={}", taskId);
                sourceSelectFinishLock(taskId);
            }

            if (!CollectionUtils.isEmpty(targetMoveInfoIdList)) {
                for (Long moveInfoId : targetMoveInfoIdList) {
                    DataMoveInfoEntity dataMoveInfoFromMem = dataStreamHolder.getDataMoveInfo(moveInfoId);
                    if (dataMoveInfoFromMem != null) {
                        metaService.updateDataMoveInfoRunningById(dataMoveInfoFromMem);
                    }
                }
            } else {
                log.info("syncDataMoveInfoRunning targetMoveInfoIdList is null linkTaskId={}", taskId);
            }
        }
    }

    /**
     * 无序消费：消费线程无需关心线程运行记录begin、end字段
     *
     * @param virtualId
     * @param dataMoveTask
     */

    public void dataConsumer1(final Integer virtualId, final DataMoveTaskEntity dataMoveTask) {
        String errorCode = null;
        String errorMessage = null;
        DataMoveInfoEntity dataMoveInfo = null;
        TimeCostStore timeCostStore = new TimeCostStore();
        int dataCountFromQueue = 0;

        try {
            dataMoveInfo = obtainTargetDataMoveInfo(virtualId, dataMoveTask);
            if (dataMoveInfo != null) {
                dataMoveInfo.setOldState(dataMoveInfo.getState());
                timeCostStore.setMinCost(dataMoveInfo.getMinCost());
                timeCostStore.setMaxCost(dataMoveInfo.getMaxCost());

                jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), "dataConsumer start");
                setThreadLocalJob(dataMoveInfo.getInfoId(), JOB_TYPE_MOVE_INFO);
            }

            while (true) {
                List<Map> dataListTarget = dataStreamHolder.pollQueue(dataMoveTask.getTaskId(), selectChannelId(virtualId, dataMoveTask.getDataStreamQueueChannel()), dataMoveTask.getTargetPropertiesInsertCount());
                int dataCount = dataListTarget.size();
                if (log.isDebugEnabled()) {
                    dataCountFromQueue += dataCount;
                    log.debug("dataConsumer1-linkTaskId={}, virtualId={},dataCountFromQueue={}", dataMoveTask.getTaskId(), virtualId, dataCountFromQueue);
                }

                if (dataCount == 0 && isSourceFinished(dataMoveTask.getTaskId())) {
                    if (dataMoveInfo != null) {
                        log.info("dataConsumer1-linkTaskId={}, virtualId={},infoId={},break1", dataMoveTask.getTaskId(), virtualId, dataMoveInfo.getInfoId());
                    } else {
                        log.info("dataConsumer1-linkTaskId={}, virtualId={},break2", dataMoveTask.getTaskId(), virtualId);
                    }
                    break;
                }

                if (dataCount != 0) {
                    timeCostStore.setStartInstant(Instant.now());

                    Integer dataActualCount = 0;
                    if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_DEL)) {
                        dataActualCount = deleteDataFromSourceTable(dataListTarget, dataMoveTask);
                    } else if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_CHECK)) {
                        dataActualCount = checkDataToTargetTable(dataListTarget, dataMoveTask);
                    } else {
                        dataActualCount = handleWriteByDataSource(dataListTarget, dataMoveTask);
                    }
                    if (!dataActualCount.equals(dataCount)) {
                        //todo 可能主键重复总数据量不一致
                        log.info("dataConsumer2-linkTaskId={}, virtualId={},dataListSize={},dataActualCount={}", dataMoveTask.getTaskId(), virtualId, dataCount, dataActualCount);
                    }

                    if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_MOVE_DEL)) {
                        dataActualCount = deleteDataFromSourceTable(dataListTarget, dataMoveTask);
                    }

                    if (dataMoveInfo != null) {
                        //计算耗时
                        calTimeCost(timeCostStore);

                        saveDataMoveInfoPageRowEnd(dataMoveInfo.getInfoId(), null, null, dataMoveTask.getTaskId(), dataCount, dataActualCount, timeCostStore.getMaxCost(), timeCostStore.getMinCost(), timeCostStore.getCurrentCost());
                    }
                } else {
                    sleepWait(1000);
                }
            }
        } catch (DataStreamException aie) {
            log.error("linkTaskId={},targetObjectName={},DataStreamException={}", dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), aie);
            errorCode = aie.getErrCode();
            errorMessage = aie.getMessage();
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), getStackTraceAsString(aie));
        } catch (Exception e) {
            log.error("linkTaskId={},targetObjectName={},Exception={}", dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), e);
            errorCode = DataStreamErrorCode.UNKNOWN_ERROR.getCode();
            errorMessage = DataStreamErrorCode.UNKNOWN_ERROR.getMessage();
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), getStackTraceAsString(e));
        }

        if (errorCode != null) {
            dataStreamHolder.setErrorTargetVirtualId(dataMoveTask.getTaskId(), virtualId);
        }

        if (dataMoveInfo != null) {
            updateDataMoveInfoResult(dataMoveTask.getTaskId(), dataMoveInfo, errorCode, errorMessage);

            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), "dataConsumer end");
        }

        removeThreadLocalJob();
    }

    private String getBreakKeyValues(final Long taskId, final Integer virtualId, String dataNode, Integer sourceMoveInfoState, Long targetDataSourceId) throws DataStreamException {
        //寻找断点
        String breakValue = null;
        DataMoveInfoEntity dataTargetMoveInfo = getRunDataMoveInfo(taskId, virtualId, dataNode, MOVE_INFO_FLAG_TARGET);
        if (!Objects.isNull(dataTargetMoveInfo) && !sourceMoveInfoState.equals(DATA_STREAM_TASK_STATE_FINISH) && dataTargetMoveInfo.getPageRowEnd() != null) {
            breakValue = dataTargetMoveInfo.getPageRowEnd();
        } else if (Objects.isNull(dataTargetMoveInfo)) {
            return breakValue;
        }

        //从目标库中(可能不存在)获取精准断点数据，
        List<DataMoveInfoEntity> dataMoveInfoTargetList = queryTargetDataMoveInfoByInfoId(dataTargetMoveInfo.getTaskId(), targetDataSourceId, dataTargetMoveInfo.getInfoId());
        if (!CollectionUtils.isEmpty(dataMoveInfoTargetList)) {
            breakValue = dataMoveInfoTargetList.get(0).getPageRowEnd();
            Integer loopCountDiff = dataMoveInfoTargetList.get(0).getLoopCount() - dataTargetMoveInfo.getLoopCount();
            if (loopCountDiff >= 2) {
                throw new DataStreamException(OPER_DATA_MOVE_INFO_TARGET_LOOP_COUNT_DIFF_ERROR);
            }

            Long dataCountDiff = dataMoveInfoTargetList.get(0).getDataCount() - dataTargetMoveInfo.getDataCount();
            if (dataCountDiff > 0) {
                Long dataActualCountDiff = dataMoveInfoTargetList.get(0).getDataActualCount() - dataTargetMoveInfo.getDataActualCount();
                refreshDataMoveInfoPageRowEnd(dataTargetMoveInfo.getInfoId(), dataMoveInfoTargetList.get(0).getPageRowEnd(), dataCountDiff.intValue(), dataActualCountDiff.intValue(), 0L, 0L, 0L);
            }
        }

        return breakValue;
    }

    private List<DataMoveInfoEntity> queryTargetDataMoveInfoByInfoId(Long taskId, Long dataSourceId, Long infoId) throws DataStreamException {
        if (!dataStreamHolder.getTargetMoveRunInfoExistsMap(taskId)) {
            return new ArrayList<>();
        }
        return super.moveTargetService.queryDataMoveInfoByInfoId(dataSourceId, infoId);
    }

    public void judgeTargetDataMoveInfoByInfoId(Long taskId, Long dataSourceId) {
        try {
            moveTargetService.queryDataMoveInfoByInfoId(dataSourceId, 1L);
            dataStreamHolder.setTargetMoveRunInfoExistsMap(taskId, true);
        } catch (DataStreamException e) {
            log.error("e=", e);
            dataStreamHolder.setTargetMoveRunInfoExistsMap(taskId, false);
        }
    }

    /**
     * 断点续传主要逻辑
     *
     * @param virtualId
     * @param sourceMoveInfoState
     * @param dataMoveTask
     * @return
     * @throws DataStreamException
     */
    private Map<String, String> generateMigrationParameters(final Integer virtualId, final Integer sourceMoveInfoState, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        Map<Integer, Map<String, String>> dataSplitMap = dataMoveTask.getDataRangSplitMap();
        String sourceTableKeys = dataMoveTask.getSourceObjectKeys();
        String selectCondition = dataMoveTask.getSourceObjectCondition();
        Long taskId = dataMoveTask.getTaskId();
        String dataNode = dataMoveTask.getSourceDataNode();
        Map<String, String> result = new HashMap<>();

        String dataSplitRangMinValue = null;
        if (dataSplitMap != null && dataSplitMap.containsKey(virtualId.intValue())) {
            dataSplitRangMinValue = dataSplitMap.get(virtualId.intValue()).keySet().iterator().next();
            String dataSplitRangMaxValue = dataSplitMap.get(virtualId.intValue()).get(dataSplitRangMinValue);
            String sourceTableRangSql = null;
            if (dataSplitRangMinValue != null) {
                sourceTableRangSql = "((" + sourceTableKeys + ") > (" + dataSplitRangMinValue + ") AND (" + sourceTableKeys + ") <= (" + dataSplitRangMaxValue + ")) ";
            } else {
                sourceTableRangSql = "(" + sourceTableKeys + ") <= (" + dataSplitRangMaxValue + ") ";
            }
            String sourceTableCondition = !ObjectUtils.isEmpty(selectCondition) ? ("AND " + selectCondition) : "";
            selectCondition = sourceTableRangSql + sourceTableCondition;
        }
        result.put("selectCondition", selectCondition);

        //断点续作，获取断点key数据
        String breakKeyValues = null;
        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            breakKeyValues = getBreakKeyValues(taskId, virtualId, dataNode, sourceMoveInfoState, dataMoveTask.getTargetObjectId());
        }
        //线程开始值
        String startKeyValues = breakKeyValues != null ? breakKeyValues : dataSplitRangMinValue;

        if (startKeyValues != null) {
            result.put("startKeyValues", startKeyValues);
        }
        return result;
    }

    public String generateKeyValuesString(Map dataRowMap, List<TableColumnEntity> tableKeysList) throws DataStreamException {
        int keysSize = tableKeysList.size();
        StringBuilder resultBuilder = new StringBuilder();
        for (int j = 0; j < keysSize; j++) {
            TableColumnEntity keyColumn = tableKeysList.get(j);
            String keyColumnValue = dataRowMap.get(keyColumn.getColumnName()) + "";
            resultBuilder.append(formatColumnValueForSql(keyColumn.getColumnTypeClassify(), keyColumnValue));
            if (j < (keysSize - 1)) {
                resultBuilder.append(",");
            }
        }
        return resultBuilder.toString();
    }

    void saveDataMoveInfoPageRowEnd(Long infoId, String pageRowStart, String pageRowEnd, Long taskId, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException {
        //写入数据库 todo以后指定类型写入
        if (dataStreamConfig.getDataStreamMoveTraceEnable()) {
            if (!metaService.updateDataMoveInfoPageRowEndByTrace(infoId, pageRowStart, pageRowEnd, taskId, dataCount, dataActualCount, maxCost, minCost, currentCost).equals(1)) {
                throw new DataStreamException(DataStreamErrorCode.OPER_UPDATE_TASK_INFO_FAIL_ERROR);
            }
        } else if (!dataStreamConfig.getDataStreamMoveInfoAsyncEnable()) {
            //生产者才会这样处理：使用pageRowStart替换moveinfo的end表示结束
            //消费者：pageRowStart、pageRowEnd都是null
            //todo 实现异步更新数据或者写入文件
            if (!metaService.updateDataMoveInfoPageRowEnd(infoId, pageRowStart, taskId, dataCount, dataActualCount, maxCost, minCost, currentCost).equals(1)) {
                throw new DataStreamException(DataStreamErrorCode.OPER_UPDATE_TASK_INFO_FAIL_ERROR);
            }
        }
        //生产者才会这样处理：使用pageRowStart替换moveinfo的end表示结束
        //消费者：pageRowStart、pageRowEnd都是null
        dataStreamHolder.updateDataMoveInfoPageRowEnd(infoId, pageRowStart, dataCount, dataActualCount, maxCost, minCost, currentCost);
    }


    public void dataConsumer(final Integer virtualId, final DataMoveTaskEntity dataMoveTask) {
        if (!CollectionUtils.isEmpty(dataMoveTask.getDataRangSplitMap())) {
            dataConsumer2(virtualId, dataMoveTask);
        } else {
            dataConsumer1(virtualId, dataMoveTask);
        }
    }

    /**
     * 单通道对等模式：生产线程无需关心线程运行记录begin、end字段
     *
     * @param virtualId
     * @param dataMoveTask
     */
    public void dataProducer2(final Integer virtualId, final DataMoveTaskEntity dataMoveTask) {
        String errorCode = null;
        String errorMessage = null;
        DataMoveInfoEntity dataMoveInfo = null;
        TimeCostStore timeCostStore = new TimeCostStore();

        Long taskId = dataMoveTask.getTaskId();
        String selectColumnSql = dataMoveTask.getSourceSelectSql();
        Integer loadStrategy = dataMoveTask.getSourceLoadStrategy();
        String sourceTableKeys = dataMoveTask.getSourceObjectKeys();
        String sourceTableRowMinValue = dataMoveTask.getSourceKeysBegin();

        Integer dataSourceType = dataMoveTask.getSourceDataBase().getDataBaseType();
        Long dataSourceId = dataMoveTask.getSourceDataBase().getDataBaseId();
        String sourceTableName = dataMoveTask.getSourceObjectName();
        String dataNode = dataMoveTask.getSourceDataNode();
        Integer sendMode = dataMoveTask.getSendMode();

        try {
            dataMoveInfo = obtainSourceDataMoveInfo(virtualId, dataMoveTask);
            dataMoveInfo.setOldState(dataMoveInfo.getState());
            timeCostStore.setMinCost(dataMoveInfo.getMinCost());
            timeCostStore.setMaxCost(dataMoveInfo.getMaxCost());
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), "dataProducer start");
            setThreadLocalJob(dataMoveInfo.getInfoId(), JOB_TYPE_MOVE_INFO);

            Map<String, String> result = generateMigrationParameters(virtualId, dataMoveInfo.getState(), dataMoveTask);
            String selectCondition = result.get("selectCondition");
            String startKeyValues = result.get("startKeyValues");

            while (true) {
                timeCostStore.setStartInstant(Instant.now());

                verifyTaskShouldContinue(taskId, virtualId);

                List<Map> dataRowList = null;
                if (isDataBaseDataSource(dataMoveTask.getSourceObjectType())) {
                    //从数据库获取数据
                    String selectSql = makeSelectStrategy(selectColumnSql, selectCondition, startKeyValues, sourceTableRowMinValue, loadStrategy, sourceTableKeys, dataSourceType, dataMoveTask.getSourcePropertiesSelectCount());
                    dataRowList = moveSourceService.executeSelectMapListSql(dataSourceId, selectSql);
                } else if (isFileDataSource(dataMoveTask.getSourceObjectType())) {
                    //从文件里获取数据
                    IFileApi fileApi = matchFileFormat(dataMoveTask.getSourceObjectType());
                    dataRowList = fileApi.parseFileLineData(dataMoveTask.getTaskId(), dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceFileFormat(), dataMoveTask.getSourcePropertiesSelectCount());
                }

                if (CollectionUtils.isEmpty(dataRowList)) {
                    break;
                }

                int dataCount = dataRowList.size();
                int dataActualCount = 0;
                if (sendMode.equals(SOURCE_SEND_MODE_ASYNC)) {
                    dataStreamHolder.addQueue(taskId, selectChannelId(virtualId, dataMoveTask.getDataStreamQueueChannel()), dataRowList);
                    dataActualCount = dataCount;
                } else if (sendMode.equals(SOURCE_SEND_MODE_SYNC)) {
/*                    dataActualCount = taskType.equals(DATA_STREAM_TASK_TYPE_DEL) ? deleteDataFromSourceTable(dataRowList, dataMoveTaskThread) : insertDataToTargetTable(dataRowList, dataMoveTaskThread);

                    if (taskType.equals(DATA_STREAM_TASK_TYPE_MOVE_DEL)) {
                        dataActualCount = deleteDataFromSourceTable(dataRowList, dataMoveTaskThread);
                    }*/
                }

                //计算耗时
                calTimeCost(timeCostStore);

                log.info("linkTaskId={},infoId={},virtualId={},sourceObjectName={},nodeName={}, beginValue={},sourceSelectCount={},dataRowList.size={},actualRecordCount={},loadCostTime={},loadStrategy={}", taskId, dataMoveInfo.getInfoId(), virtualId, sourceTableName, dataNode, startKeyValues, dataMoveTask.getSourcePropertiesSelectCount(), dataRowList.size(), dataActualCount, timeCostStore.getCurrentCost(), loadStrategy);
                //单通道对等模式：生产线程无需关心运行begin、end记录
                refreshDataMoveInfoPageRowEnd(dataMoveInfo.getInfoId(), null, dataCount, dataActualCount, timeCostStore.getMaxCost(), timeCostStore.getMinCost(), timeCostStore.getCurrentCost());

                if (dataCount != dataMoveTask.getSourcePropertiesSelectCount()) {
                    break;
                }

                startKeyValues = generateKeyValuesString(dataRowList.get(dataCount - 1), dataMoveTask.getSourceTableKeysList());
            }
        } catch (DataStreamException aie) {
            log.error("linkTaskId={},sourceObjectName={},nodeName={} DataStreamException={}", taskId, sourceTableName, dataNode, aie);
            errorCode = aie.getErrCode();
            errorMessage = aie.getMessage();
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), getStackTraceAsString(aie));
        } catch (Exception e) {
            log.error("linkTaskId={},sourceObjectName={},nodeName={} Exception={}", taskId, sourceTableName, dataNode, e);
            errorCode = DataStreamErrorCode.UNKNOWN_ERROR.getCode();
            errorMessage = DataStreamErrorCode.UNKNOWN_ERROR.getMessage();
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), getStackTraceAsString(e));
        }

        if (!StringUtils.isEmpty(errorCode)) {
            dataStreamHolder.setErrorSourceVirtualId(taskId, virtualId);
        }

        updateDataMoveInfoResult(dataMoveTask.getTaskId(), dataMoveInfo, errorCode, errorMessage);

        jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), "dataProducer end");
        removeThreadLocalJob();
    }

    public void dataConsumer2(final Integer virtualId, final DataMoveTaskEntity dataMoveTask) {
        String errorCode = null;
        String errorMessage = null;
        DataMoveInfoEntity dataMoveInfo = null;
        TimeCostStore timeCostStore = new TimeCostStore();

        Long taskId = dataMoveTask.getTaskId();
        String targetTableName = dataMoveTask.getTargetObjectName();
        Integer taskType = dataMoveTask.getTaskType();

        try {
            dataMoveInfo = obtainTargetDataMoveInfo(virtualId, dataMoveTask);
            if (dataMoveInfo != null) {
                dataMoveInfo.setOldState(dataMoveInfo.getState());
                timeCostStore.setMinCost(dataMoveInfo.getMinCost());
                timeCostStore.setMaxCost(dataMoveInfo.getMaxCost());

                jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), "dataConsumer start");
                setThreadLocalJob(dataMoveInfo.getInfoId(), JOB_TYPE_MOVE_INFO);

                if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
                    //注入在目标库记录运行表数据，实现断点续传强一致性
                    List<DataMoveInfoEntity> dataMoveInfoTargetList = queryTargetDataMoveInfoByInfoId(dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectId(), dataMoveInfo.getInfoId());
                    if (CollectionUtils.isEmpty(dataMoveInfoTargetList)) {
                        dataMoveInfoTargetList.add(dataMoveInfo);
                        insertTargetDataMoveInfo(dataMoveTask, dataMoveInfoTargetList);
                    }
                }
            }

            while (true) {
                List<Map> dataRowList = dataStreamHolder.pollQueue(taskId, selectChannelId(virtualId, dataMoveTask.getDataStreamQueueChannel()), dataMoveTask.getTargetPropertiesInsertCount());
                int dataCount = dataRowList.size();

                if (dataCount == 0 && isSourceFinished(taskId)) {
                    break;
                }

                if (dataCount != 0) {
                    timeCostStore.setStartInstant(Instant.now());

                    String pageRowKeyValues = null;
                    if (dataMoveInfo != null && isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
                        pageRowKeyValues = generateKeyValuesString(dataRowList.get(dataCount - 1), dataMoveTask.getTargetTableKeysList());
                    }

                    Integer dataActualCount = 0;
                    if (taskType.equals(DATA_STREAM_TASK_TYPE_DATA_DEL)) {
                        dataActualCount = deleteDataFromSourceTable(dataRowList, dataMoveTask);
                    } else if (taskType.equals(DATA_STREAM_TASK_TYPE_DATA_CHECK)) {
                        dataActualCount = checkDataToTargetTable(dataRowList, dataMoveTask);
                    } else {
                        dataActualCount = handleWriteByDataSource(dataRowList, dataMoveTask);
                    }
                    if (!dataActualCount.equals(dataCount)) {
                        log.info("linkTaskId={}, virtualId={},dataCount={},dataActualCount={}", taskId, virtualId, dataCount, dataActualCount);
                    }

                    if (taskType.equals(DATA_STREAM_TASK_TYPE_DATA_MOVE_DEL)) {
                        dataActualCount = deleteDataFromSourceTable(dataRowList, dataMoveTask);
                    }

                    if (dataMoveInfo != null) {
                        //计算耗时
                        calTimeCost(timeCostStore);

                        //单通道对等模式：消费线程需要精确关心线程运行记录end字段
                        refreshDataMoveInfoPageRowEnd(dataMoveInfo.getInfoId(), pageRowKeyValues, dataCount, dataActualCount, timeCostStore.getMaxCost(), timeCostStore.getMinCost(), timeCostStore.getCurrentCost());
                    }
                } else {
                    sleepWait(1000);
                }
            }
        } catch (DataStreamException aie) {
            log.error("linkTaskId={},targetObjectName={},DataStreamException={}", taskId, targetTableName, aie);
            errorCode = aie.getErrCode();
            errorMessage = aie.getMessage();
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), getStackTraceAsString(aie));
        } catch (Exception e) {
            log.error("linkTaskId={},targetObjectName={},Exception={}", taskId, targetTableName, e);
            errorCode = DataStreamErrorCode.UNKNOWN_ERROR.getCode();
            errorMessage = DataStreamErrorCode.UNKNOWN_ERROR.getMessage();
            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), getStackTraceAsString(e));
        }

        if (errorCode != null) {
            dataStreamHolder.setErrorTargetVirtualId(taskId, virtualId);
        }

        if (dataMoveInfo != null) {
            updateDataMoveInfoResult(dataMoveTask.getTaskId(), dataMoveInfo, errorCode, errorMessage);

            jobLogbackEventMoveInfo(dataMoveInfo.getInfoId(), "dataConsumer end");
        }
        removeThreadLocalJob();
    }

    private void insertTargetDataMoveInfo(final DataMoveTaskEntity dataMoveTask, List<DataMoveInfoEntity> dataMoveInfoTargetList) throws DataStreamException {
        if (!dataStreamHolder.getTargetMoveRunInfoExistsMap(dataMoveTask.getTaskId())) {
            return;
        }

        moveTargetService.insertDataMoveInfo(dataMoveTask.getTargetObjectId(), dataMoveTask.getTargetDataBase().getDataBaseType(), dataMoveInfoTargetList);
    }

    private void verifyTaskShouldContinue(final Long taskId, final Integer virtualId) throws DataStreamException {
        if (dataStreamHolder.isNoticeTaskStop(taskId)) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_NOTICE_STOP_ERROR);
        }

        //其他线程异常通知一起退出
        Integer errorSourceVirtualId = dataStreamHolder.getErrorSourceVirtualId(taskId);
        if (errorSourceVirtualId > 0 && !virtualId.equals(errorSourceVirtualId)) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_NOTICE_EXCEPTION_ERROR);
        }
    }

    public String makeSourceSelectSqlColumns(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        Boolean isOrderBy = dataMoveTask.getSourceLoadStrategy().equals(LOAD_STRATEGY_BY_DATA_PART) ? false : true;
        Integer dataSourceType = dataMoveTask.getSourceDataBase().getDataBaseType();
        return super.matchDataBase(dataSourceType).makeSqlSelectColumns(dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceObjectCondition(), dataMoveTask.getSourceTableColumns(), isOrderBy);
    }

    public String makeTargetInsertSqlColumns(DataMoveTaskEntity dataMoveTask) {
        return super.makeSqlInsertColumns(dataMoveTask.getTargetObjectName(), "", dataMoveTask.getTargetTableColumns());
    }

    public List<List<Object>> makeDataInsertObject(Long taskId, List<TableColumnEntity> tableColumns, List<Map> dataRecordList) {
        List<List<Object>> dataRowList = (dataRecordList.size() > dataStreamConfig.getDataStreamParallelStreamSize()) ? dataRecordList.parallelStream().map(x -> makeInsertRowObject(taskId, tableColumns, x)).collect(Collectors.toList()) : dataRecordList.stream().map(x -> makeInsertRowObject(taskId, tableColumns, x)).collect(Collectors.toList());
        return dataRowList;
    }

    public String makeSourceDataSelectCountSql(DataMoveTaskEntity dataMoveTask) {
        return makeDataSelectCountSql(dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceObjectCondition(), dataMoveTask.getSourceDataNode(), dataMoveTask.getSourceDataSet());
    }


    public String makeTargetDataSelectCountSql(DataMoveTaskEntity dataMoveTask) {
        String tableCondition = dataMoveTask.getTargetTableColumns().stream().filter(column -> column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID)).findFirst().map(column -> TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID + "=" + dataMoveTask.getTaskId()).orElse(null);

        return makeDataSelectCountSql(dataMoveTask.getTargetObjectName(), tableCondition, null, null);
    }

    public String makeDataSelectCountSql(String tableName, String tableCondition, String dataNode, Integer dataSet) {
        String tableCountSql = "select count(1) from " + tableName;
        if (dataNode != null) {
            tableCountSql = (dataSet != null) ? (String.format(SQL_FORMAT_HINT_BALANCE_DATANODE, dataSet, dataNode) + tableCountSql) : (String.format(SQL_FORMAT_HINT_DATANODE, dataNode) + tableCountSql);
        }

        if (!StringUtils.isEmpty(tableCondition)) {
            tableCountSql = tableCountSql + " where " + tableCondition;
        }
        return tableCountSql;
    }

    public List<Object> makeInsertRowObject(Long taskId, List<TableColumnEntity> tableColumns, Map dataRow) {
        List<Object> dataColumnList = new ArrayList<>();
        for (TableColumnEntity iterator : tableColumns) {
            Object columnValue = dataRow.get(iterator.getColumnName());
            if (Objects.isNull(columnValue)) {
                columnValue = dataRow.get(iterator.getColumnName().toLowerCase());
                if (Objects.isNull(columnValue) && TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID.equalsIgnoreCase(iterator.getColumnName())) {
                    columnValue = taskId;
                }
            }

            dataColumnList.add(columnValue);
        }
        return dataColumnList;
    }

    private void jobLogbackEventMoveInfo(Long infoId, String content) {
        systemLogEvent.jobLogbackEvent(JOB_TYPE_MOVE_INFO, infoId, content);
    }

    private Integer selectChannelId(Integer virtualId, Integer dataStreamQueueChannel) {
        return virtualId % dataStreamQueueChannel + 1;
    }

    private void sleepWait(long time) throws DataStreamException {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("canalMoveData -------------------insertDataFromQueue InterruptedException=", e);
            Thread.currentThread().interrupt();
            throw new DataStreamException(DataStreamErrorCode.OPER_INSERT_DATA_SLEEP_ERROR);
        }
    }

    Integer handleWriteByDataSource(final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        //todo 后续是否可以下沉到各个模块去
        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            //写入数据库对象
            return insertDataToTargetTable(-1L, null, dataListTarget, dataMoveTask);
        } else if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            //写入文件对象
            return super.writeDataToTargetFile(-1L, null, dataListTarget, dataMoveTask);
        } else if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            //写入消息队列
            return super.writeDataToTargetMQ(dataMoveTask.getTaskId(), null, dataListTarget, dataMoveTask);
        }
        return 0;
    }


    Integer insertDataToTargetTable(final Long infoId, String pageRowStart, final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        String errorCode = null;
        String errorMessage = null;
        String stackTrace = null;

        try {
            return insertDataToTargetTableByMode(infoId, pageRowStart, dataListTarget, dataMoveTask);
        } catch (DataStreamException aie) {
            errorCode = aie.getErrCode();
            errorMessage = aie.getMessage();
            log.error("linkTaskId={},DataStreamException={}", dataMoveTask.getTaskId(), aie);
            stackTrace = getStackTraceAsString(aie);
        } catch (Exception e) {
            errorCode = "-1";
            errorMessage = "data insert exception";
            log.error("linkTaskId={},Exception={}", dataMoveTask.getTaskId(), e);
            stackTrace = getStackTraceAsString(e);
        }

        List<Map> dataListTargetFilter = filterDuplicateData(dataMoveTask.getTargetDataBase().getDataBaseId(), dataMoveTask.getTargetDataBase().getDataBaseType(), dataListTarget, dataMoveTask.getTargetKeyColumns(), dataMoveTask.getTargetTableColumns(), dataMoveTask.getTargetObjectName(), moveTargetService);
        if (CollectionUtils.isEmpty(dataListTargetFilter)) {
            if (dataListTargetFilter == null) {
                systemLogEvent.jobLogbackEvent("filterDuplicateData:dataListTarget.size=" + dataListTarget.size() + ",dataListTargetFilter is null");
            } else {
                systemLogEvent.jobLogbackEvent("filterDuplicateData:dataListTarget.size=" + dataListTarget.size() + ",dataListTargetFilter.size=" + dataListTargetFilter.size());
            }
            return 0;
        } else if (dataListTargetFilter.size() != dataListTarget.size()) {
            systemLogEvent.jobLogbackEvent("filterDuplicateData:dataListTarget.size=" + dataListTarget.size() + ",dataListTargetFilter.size=" + dataListTargetFilter.size());
            return insertDataToTargetTableByMode(infoId, pageRowStart, dataListTargetFilter, dataMoveTask);
        } else {
            systemLogEvent.jobLogbackEvent(stackTrace);
            throw new DataStreamException(errorCode, errorMessage);
        }
    }

    Integer insertDataToTargetTableByMode(final Long infoId, final String pageRowEnd, final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        int dataListSize = dataListTarget.size();
        Instant start = Instant.now();
        int recordCount = 0;

        if (dataMoveTask.getTargetInsertMode() == null || dataMoveTask.getTargetInsertMode().equals(INSERT_MODE_BY_SPLICING)) {
            String insertSql = makeSqlBatchInsert(dataMoveTask.getTargetInsertSqlColumns(), dataMoveTask.getTaskId(), dataMoveTask.getTargetDataBase().getDataBaseType(), dataMoveTask.getTargetTableColumns(), dataListTarget);
            recordCount = moveTargetService.insertDataList(dataMoveTask.getTargetDataBase().getDataBaseId(), infoId, dataListTarget.size(), pageRowEnd, insertSql);
        } else if (dataMoveTask.getTargetInsertMode().equals(INSERT_MODE_BY_BIND)) {
            List<List<Object>> dataListTargetObject = makeDataInsertObject(dataMoveTask.getTaskId(), dataMoveTask.getTargetTableColumns(), dataListTarget);
            recordCount = moveTargetService.insertDataListBindVar(dataMoveTask.getTargetDataBase().getDataBaseId(), dataMoveTask.getTargetInsertSqlColumns(), dataListTargetObject);
        } else if (dataMoveTask.getTargetInsertMode().equals(INSERT_MODE_BY_AUTO)) {
            recordCount = insertDataListByAuto(infoId, pageRowEnd, dataListTarget, dataMoveTask);
        }

        if (recordCount != dataListSize) {
            log.error("linkTaskId={},targetObjectName={} insertDataList fail recordCount={},dataListSize={}", dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), recordCount, dataListSize);
            throw new DataStreamException(DataStreamErrorCode.OPER_INSERT_TARGET_DATA_ERROR);
        }

        Instant end = Instant.now();
        log.info("targetObjectName={},dataListTarget.size={},insertCostTime={}", dataMoveTask.getTargetObjectName(), dataListSize, Duration.between(start, end).toMillis());
        return recordCount;
    }

    private Integer insertDataListByAuto(final Long infoId, final String pageRowEnd, final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        try {
            String insertSql = makeSqlBatchInsert(dataMoveTask.getTargetInsertSqlColumns(), dataMoveTask.getTaskId(), dataMoveTask.getTargetDataBase().getDataBaseType(), dataMoveTask.getTargetTableColumns(), dataListTarget);
            return moveTargetService.insertDataList(dataMoveTask.getTargetDataBase().getDataBaseId(), infoId, dataListTarget.size(), pageRowEnd, insertSql);
        } catch (DataStreamException aie) {
            log.error("linkTaskId={},DataStreamException={}", dataMoveTask.getTaskId(), aie);
        } catch (Exception e) {
            log.error("linkTaskId={},Exception={}", dataMoveTask.getTaskId(), e);
        }

        List<List<Object>> dataListTargetObject = makeDataInsertObject(dataMoveTask.getTaskId(), dataMoveTask.getTargetTableColumns(), dataListTarget);
        return moveTargetService.insertDataListBindVar(dataMoveTask.getTargetDataBase().getDataBaseId(), dataMoveTask.getTargetInsertSqlColumns(), dataListTargetObject);
    }

    /**
     * 获取当前数据段数据段的最大key值的sql语句
     *
     * @param startKeyValue 开始值：一般从null开始，下一页查询startKeyValue就是上一页的最大值
     * @param dataMoveTask
     * @return
     * @throws DataStreamException
     */
    private String makeSqlCurrentPageMaxKeyValueWrap(String startKeyValue, DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        Integer dataSourceType = dataMoveTask.getSourceDataBase().getDataBaseType();
        String tableKeyColumns = dataMoveTask.getSourceObjectKeys();
        String tableName = dataMoveTask.getSourceObjectName();
        String selectCondition = dataMoveTask.getSourceObjectCondition();
        //表记录最小值
        String tableMinKeyValue = dataMoveTask.getSourceKeysBegin();
        //下个数据段的记录数
        Integer pageRowSize = dataMoveTask.getDataRangSplitSize() == null ? dataMoveTask.getSourcePropertiesSelectCount() : dataMoveTask.getDataRangSplitSize();

        return super.matchDataBase(dataSourceType).makeSqlCurrentPageMaxKeyValue(startKeyValue, tableKeyColumns, tableName, selectCondition, tableMinKeyValue, pageRowSize);
    }

    public void createTaskInstance(CreateTaskInstanceEntity createTaskInstance) throws DataStreamException {
        if (createTaskInstance.getTaskType().equals(DATA_STREAM_TASK_TYPE_TABLE_MOVE)) {
            createTableMoveTask(createTaskInstance);
        } else {
            createDataMoveTask(createTaskInstance);
        }
    }

    public void createTableMoveTask(CreateTaskInstanceEntity createTaskInstance) throws DataStreamException {
        log.info("---------------createDateMoveTaskRequest={}", createTaskInstance);
        if (createTaskInstance.getSourceObjectId().equals(createTaskInstance.getTargetObjectId()) && createTaskInstance.getSourceObjectName().equals(createTaskInstance.getTargetObjectName())) {
            throw new DataStreamException(OPER_CANT_NOT_SAME_DB_SAME_TABLE_ERROR);
        }

        List<DataMoveTaskEntity> dataMoveTaskList = new ArrayList<>();
        dataMoveTaskList.add(changeDataMoveTaskObject(null, createTaskInstance));
        dataMoveTaskList.forEach(x -> x.setTargetObjectName(x.getSourceObjectName()));

        List<MoveTableEntity> moveTableList = new ArrayList<>();
        if (createTaskInstance.getSourceDataBaseObjectType() != null && createTaskInstance.getSourceDataBaseObjectType().equals(MOVE_TABLE_OBJECT_TYPE_TABLE)) {
            String[] sourceTableNames = createTaskInstance.getSourceObjectName().split(",");
            if (sourceTableNames.length == 0) {
                throw new DataStreamException(OPER_SOURCE_TABLE_IS_NULL_ERROR);
            }

            for (String iterator : sourceTableNames) {
                MoveTableEntity moveTable = new MoveTableEntity();
                moveTable.setMoveTableId(metaService.querySequence(SEQ_MOVE_TABLE_ID));
                moveTable.setTaskId(dataMoveTaskList.get(0).getTaskId());
                moveTable.setSourceTableName(iterator);
                moveTable.setState(DATA_STREAM_TASK_STATE_INIT);
                moveTableList.add(moveTable);
            }
        } else if (createTaskInstance.getSourceDataBaseObjectType() != null && createTaskInstance.getSourceDataBaseObjectType().equals(MOVE_TABLE_OBJECT_TYPE_SCHEMA)) {
            List<TableInfoEntity> tableInfoList = SpringUtil.getBean(TableInfoHandler.class).getTableList(createTaskInstance.getSourceObjectId());
            if (!CollectionUtils.isEmpty(tableInfoList)) {
                for (TableInfoEntity iterator : tableInfoList) {
                    MoveTableEntity moveTable = new MoveTableEntity();
                    moveTable.setMoveTableId(metaService.querySequence(SEQ_MOVE_TABLE_ID));
                    moveTable.setTaskId(dataMoveTaskList.get(0).getTaskId());
                    moveTable.setSourceTableName(iterator.getTableName());
                    moveTable.setState(DATA_STREAM_TASK_STATE_INIT);
                    moveTableList.add(moveTable);
                }
            }
        }

        List<TaskExtendEntity> taskExtendList = buildTaskExtendParameters(dataMoveTaskList);

        metaService.createTableMoveTask(dataMoveTaskList, moveTableList, taskExtendList);
    }

    public void createDataMoveTask(CreateTaskInstanceEntity createTaskInstance) throws DataStreamException {
        if (createTaskInstance.getSourceObjectId().equals(createTaskInstance.getTargetObjectId()) && createTaskInstance.getSourceObjectType().equals(createTaskInstance.getTargetObjectType()) && createTaskInstance.getSourceObjectName().equals(createTaskInstance.getTargetObjectName())) {
            throw new DataStreamException(OPER_CANT_NOT_SAME_DB_SAME_TABLE_ERROR);
        }

        List<DataMoveTaskEntity> dataMoveTaskList = new ArrayList<>();
        if (createTaskInstance.getDataNodeFlag() != null && createTaskInstance.getDataNodeFlag().equals(1)) {
//            addDataBase(createTaskInstance.getSourceObjectId());
//
//            List<String> nodeNameList = moveSourceService.queryDataNodesByTableName(createTaskInstance.getSourceObjectId(), createTaskInstance.getSourceObjectName());
//            if (CollectionUtils.isEmpty(nodeNameList)) {
//                throw new DataStreamException(DataStreamErrorCode.OPER_TASK_CREATE_DATANODE_NULL_ERROR);
//            }
//
//            if (!Objects.isNull(createTaskInstance.getTableType()) && createTaskInstance.getTableType().equals(2)) {
//                createTaskInstance.setTaskDisc(createTaskInstance.getTaskDisc() + "全局表指定数据片加载数据:" + nodeNameList.get(0));
//                changeDataMoveTaskObject(nodeNameList.get(0), createTaskInstance);
//            } else {
//                for (String nodeName : nodeNameList) {
//                    dataMoveTaskList.add(changeDataMoveTaskObject(nodeName, createTaskInstance));
//                }
//            }
        } else {
            if (!Objects.isNull(createTaskInstance.getTableType()) && createTaskInstance.getTableType().equals(2)) {
                createTaskInstance.setTaskDisc(createTaskInstance.getTaskDisc() + "全局表指定数据片加载数据");
            }
            dataMoveTaskList.add(changeDataMoveTaskObject(null, createTaskInstance));
        }

        List<TaskExtendEntity> taskExtendList = buildTaskExtendParameters(dataMoveTaskList);

        metaService.createDataMoveTask(dataMoveTaskList, taskExtendList);
    }

    private List<TaskExtendEntity> buildTaskExtendParameters(List<DataMoveTaskEntity> dataMoveTaskList) throws DataStreamException {
        List<TaskExtendEntity> dataTaskExtendList = new ArrayList<>();
        taskParamExtend.buildTaskExtendParameters(dataMoveTaskList, dataTaskExtendList);
        for (TaskExtendEntity iterator : dataTaskExtendList) {
            iterator.setTaskExtendId(metaService.querySequence(SEQ_TASK_EXTEND_ID));
        }
        return dataTaskExtendList;
    }

    public void addDataBase(Long dataBaseId, Long taskId) throws DataStreamException {
        List<DataBaseEntity> dataBaseList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ID, dataBaseId, null, 1, 10);
        if (CollectionUtils.isEmpty(dataBaseList)) {
            throw new DataStreamException(OPER_QUERY_DATA_SOURCE_INFO_ERROR);
        }

        DataBaseEntity dataBase = new DataBaseEntity();
        dataBase.setDataBaseType(dataBaseList.get(0).getDataBaseType());
        dataBase.setDataPoolCount(1);
        dataBase.setKeyName(SOURCE_DATA_MOVE_SOURCE_KEY_NAME);
        dataBase.setDataBaseName(dataBaseList.get(0).getDataBaseName());
        dataBase.setUrl(dataBaseList.get(0).getUrl());
        dataBase.setUserName(dataBaseList.get(0).getUserName());
        dataBase.setPassWord(dataBaseList.get(0).getPassWord());

        if (!dataBase.getUrl().contains("?")) {
            dataBase.setUrl(dataBase.getUrl() + "?");
        }

        if (!dataBase.getUrl().contains("socketTimeout")) {
            dataBase.setUrl(dataBase.getUrl() + "&socketTimeout=5000");
        }

        dataBase.setSqlValidationQuery(matchDataBase(dataBase.getDataBaseType()).makeSqlValidationQuery());
        dataBase.setDriverClass(matchDataBase(dataBase.getDataBaseType()).getDriverClass());
        dataBaseSource.addDataBase(Collections.singletonList(dataBase), taskId);
    }

    private DataMoveTaskEntity changeDataMoveTaskObject(String dataNode, CreateTaskInstanceEntity createTaskInstance) throws DataStreamException {
        DataMoveTaskEntity dataMoveTask = new DataMoveTaskEntity();
        BeanUtils.copyProperties(createTaskInstance, dataMoveTask);
        dataMoveTask.setTaskId(metaService.querySequence(SEQ_MOVE_TASK_ID));
        dataMoveTask.setSourceDataNode(dataNode);
        dataMoveTask.setState(0);
        dataMoveTask.setSourceLoadStrategy(LOAD_STRATEGY_BY_LIMIT_PAG);
        dataMoveTask.setSendMode(dataStreamConfig.getSource().getSourceSendMode());
        return dataMoveTask;
    }

    public List<DataMoveProgressEntity> queryTaskProgress(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        if (dataMoveTask.getState().equals(DATA_STREAM_TASK_STATE_RUNNING)) {
            log.info("-------------------queryTaskProgress from mem!!");
            List<DataMoveInfoEntity> dataMoveInfoList = dataStreamHolder.getMoveInfoListFromMem(dataMoveTask.getTaskId());
            if (!CollectionUtils.isEmpty(dataMoveInfoList)) {
                return dataMoveInfoList.stream().collect(Collectors.groupingBy(DataMoveInfoEntity::getInfoFlag, Collectors.collectingAndThen(Collectors.toList(), list -> {
                    DataMoveProgressEntity entity = new DataMoveProgressEntity();
                    entity.setInfoFlag(list.get(0).getInfoFlag());
                    entity.setThreadCount((long) list.size());

                    // 使用汇总方式
                    entity.setDataCount(String.valueOf(list.stream().mapToLong(DataMoveInfoEntity::getDataCount).sum()));
                    entity.setDataActualCount(String.valueOf(list.stream().mapToLong(DataMoveInfoEntity::getDataActualCount).sum()));

                    return entity;
                }))).values().stream().collect(Collectors.toList());
            } else {
                return metaService.queryDataMoveInfoProgress(dataMoveTask.getTaskId());
            }
        } else {
            return metaService.queryDataMoveInfoProgress(dataMoveTask.getTaskId());
        }
    }

    public Integer querySourceCount(Long taskId) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = metaService.queryTaskByTaskId(taskId);
        if (CollectionUtils.isEmpty(dataMoveTaskList)) {
            return 0;
        }

        if (dataMoveTaskList.get(0).getSourceObjectCount() == null) {
            return 0;
        }
        return dataMoveTaskList.get(0).getSourceObjectCount().intValue();
    }

    public Integer queryDataMoveTaskCount(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        int total = 0;
        switch (queryDateMoveTaskRequest.getQueryFlag()) {
            case QUERY_DATE_MOVE_TASK_FLAG_BY_TASK_ID:
                total = metaService.queryTaskByTaskId(queryDateMoveTaskRequest.getTaskId()).size();
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_TABLE_NAME:
                total = metaService.getMoveTaskCountByTableName(queryDateMoveTaskRequest.getTableName());
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_STATE:
                total = metaService.getMoveTaskCount(null, null, queryDateMoveTaskRequest.getState());
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_TIME:
                total = metaService.getMoveTaskCount(queryDateMoveTaskRequest.getBeginDate(), queryDateMoveTaskRequest.getEndDate(), null);
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_BATCH_TASK_ID:
                total = metaService.getMoveTaskCountByBatchTaskId(queryDateMoveTaskRequest.getBatchTaskId());
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_COPY_TASK_ID:
                total = metaService.getMoveTaskCountByCopyTaskId(queryDateMoveTaskRequest.getCopyTaskId());
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_TASK_TYPE:
                total = metaService.getMoveTaskCountByTaskType(queryDateMoveTaskRequest.getTaskType());
                break;
            default:
                throw new DataStreamException(DataStreamErrorCode.OPER_TASK_QUERY_FLAG_NULL_ERROR);
        }
        return total;
    }

    public List<DataMoveTaskEntity> queryDataMoveTaskList(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = new ArrayList<>();

        switch (queryDateMoveTaskRequest.getQueryFlag()) {
            case QUERY_DATE_MOVE_TASK_FLAG_BY_TASK_ID:
                dataMoveTaskList.addAll(queryDateMoveTaskByTaskId(queryDateMoveTaskRequest));
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_TABLE_NAME:
                dataMoveTaskList.addAll(queryDateMoveTaskByTableName(queryDateMoveTaskRequest));
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_STATE:
                dataMoveTaskList.addAll(queryDateMoveTaskByState(queryDateMoveTaskRequest));
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_TIME:
                dataMoveTaskList.addAll(queryDateMoveTaskByDate(queryDateMoveTaskRequest));
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_BATCH_TASK_ID:
                dataMoveTaskList.addAll(queryDataMoveTaskByBatchTaskId(queryDateMoveTaskRequest));
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_COPY_TASK_ID:
                dataMoveTaskList.addAll(queryDataMoveTaskByCopyTaskId(queryDateMoveTaskRequest));
                break;
            case QUERY_DATE_MOVE_TASK_FLAG_BY_TASK_TYPE:
                dataMoveTaskList.addAll(queryDataMoveTaskByTaskType(queryDateMoveTaskRequest));
                break;
            default:
                throw new DataStreamException(DataStreamErrorCode.OPER_TASK_QUERY_FLAG_NULL_ERROR);
        }
        return dataMoveTaskList;
    }


    private List<DataMoveTaskEntity> queryDateMoveTaskByTaskId(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        Optional.ofNullable(queryDateMoveTaskRequest.getTaskId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_TASK_QUERY_TASK_ID_NULL_ERROR));
        return metaService.queryTaskByTaskId(queryDateMoveTaskRequest.getTaskId());
    }

    private List<DataMoveTaskEntity> queryDateMoveTaskByTableName(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        Optional.ofNullable(queryDateMoveTaskRequest.getTableName()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_TASK_QUERY_TABLE_NAME_NULL_ERROR));
        return metaService.queryDataMoveTaskByTableName(queryDateMoveTaskRequest.getTableName(), queryDateMoveTaskRequest.getPage(), queryDateMoveTaskRequest.getCount());
    }

    private List<DataMoveTaskEntity> queryDateMoveTaskByState(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        Optional.ofNullable(queryDateMoveTaskRequest.getState()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_TASK_QUERY_STATE_NULL_ERROR));
        return metaService.queryDataMoveTaskByState(queryDateMoveTaskRequest.getState(), queryDateMoveTaskRequest.getPage(), queryDateMoveTaskRequest.getCount());
    }

    private List<DataMoveTaskEntity> queryDataMoveTaskByBatchTaskId(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        Optional.ofNullable(queryDateMoveTaskRequest.getBatchTaskId()).orElseThrow(() -> new DataStreamException(OPER_BATCH_TASK_ID_NULL_ERROR));
        return metaService.queryDataMoveTaskByBatchTaskId(queryDateMoveTaskRequest.getBatchTaskId(), queryDateMoveTaskRequest.getPage(), queryDateMoveTaskRequest.getCount());
    }

    private List<DataMoveTaskEntity> queryDataMoveTaskByCopyTaskId(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        Optional.ofNullable(queryDateMoveTaskRequest.getCopyTaskId()).orElseThrow(() -> new DataStreamException(OPER_COPY_TASK_ID_NULL_ERROR));
        return metaService.queryDataMoveTaskByCopyTaskId(queryDateMoveTaskRequest.getCopyTaskId(), queryDateMoveTaskRequest.getPage(), queryDateMoveTaskRequest.getCount());
    }

    private List<DataMoveTaskEntity> queryDataMoveTaskByTaskType(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        Optional.ofNullable(queryDateMoveTaskRequest.getTaskType()).orElseThrow(() -> new DataStreamException(OPER_TASK_TYPE_NULL_ERROR));
        return metaService.queryDataMoveTaskByTaskType(queryDateMoveTaskRequest.getTaskType(), queryDateMoveTaskRequest.getPage(), queryDateMoveTaskRequest.getCount());
    }


    public List<DataMoveTaskEntity> queryDateMoveTaskByDate(QueryDataMoveTaskRequest queryDateMoveTaskRequest) throws DataStreamException {
        Optional.ofNullable(queryDateMoveTaskRequest.getBeginDate()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_TASK_QUERY_BEGIN_DATE_NULL_ERROR));
        Optional.ofNullable(queryDateMoveTaskRequest.getEndDate()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_TASK_QUERY_END_DATE_NULL_ERROR));
        return metaService.queryDataMoveTaskByDate(queryDateMoveTaskRequest.getBeginDate(), queryDateMoveTaskRequest.getEndDate(), queryDateMoveTaskRequest.getPage(), queryDateMoveTaskRequest.getCount());
    }

    public List<DataMoveInfoEntity> queryDataMoveInfoList(QueryDataMoveInfoRequest queryDateMoveInfoRequest) throws DataStreamException {
        List<DataMoveInfoEntity> dataMoveInfoList = new ArrayList<>();
        switch (queryDateMoveInfoRequest.getQueryFlag()) {
            case QUERY_DATE_MOVE_INFO_FLAG_BY_TASK_ID:
                Optional.ofNullable(queryDateMoveInfoRequest.getTaskId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_QUERY_INFO_TASK_ID_NULL_ERROR));

                dataMoveInfoList.addAll(metaService.queryDataMoveInfo(queryDateMoveInfoRequest.getTaskId(), null));
                break;
            case QUERY_DATE_MOVE_INFO_FLAG_BY_INFO_ID:
                Optional.ofNullable(queryDateMoveInfoRequest.getInfoId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_QUERY_INFO_INFO_ID_NULL_ERROR));

                dataMoveInfoList.addAll(metaService.queryDataMoveInfoByInfoId(queryDateMoveInfoRequest.getInfoId()));
                break;
            default:
                throw new DataStreamException(DataStreamErrorCode.OPER_QUERY_INFO_FLAG_NOT_EQUAL_ERROR);
        }
        return dataMoveInfoList;
    }

    public List<DataMoveInfoEntity> queryDataMoveInfoListFromMem(QueryDataMoveInfoRequest queryDateMoveInfoRequest) throws DataStreamException {
        List<DataMoveInfoEntity> dataMoveInfoList = new ArrayList<>();
        switch (queryDateMoveInfoRequest.getQueryFlag()) {
            case QUERY_DATE_MOVE_INFO_FLAG_BY_TASK_ID:
                Optional.ofNullable(queryDateMoveInfoRequest.getTaskId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_QUERY_INFO_TASK_ID_NULL_ERROR));
                dataMoveInfoList.addAll(dataStreamHolder.getMoveInfoListFromMem(queryDateMoveInfoRequest.getTaskId()));
                break;
            case QUERY_DATE_MOVE_INFO_FLAG_BY_INFO_ID:
                Optional.ofNullable(queryDateMoveInfoRequest.getInfoId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_QUERY_INFO_INFO_ID_NULL_ERROR));

                dataMoveInfoList.addAll(metaService.queryDataMoveInfoByInfoId(queryDateMoveInfoRequest.getInfoId()));
                break;
            default:
                throw new DataStreamException(DataStreamErrorCode.OPER_QUERY_INFO_FLAG_NOT_EQUAL_ERROR);
        }
        return dataMoveInfoList;
    }

    public void operateDataMoveTask(Long taskId, Integer operate) throws DataStreamException {
        switch (operate) {
            case OPERATE_DATE_MOVE_INFO_FLAG_BY_STOP:
                operateMoveTaskStop(taskId);
                break;
            case OPERATE_DATE_MOVE_INFO_FLAG_BY_REDO:
                operateMoveTaskRedo(taskId);
                break;
            case OPERATE_DATE_MOVE_INFO_FLAG_BY_COPY:
                operateMoveTaskCopy(taskId);
                break;
            default:
                throw new DataStreamException(DataStreamErrorCode.OPER_OPERATE_INFO_TYPE_ID_ERROR);
        }
    }


    public void operateMoveTaskStop(Long taskId) throws DataStreamException {
        List<DataMoveInfoEntity> dataMoveInfos = metaService.queryDataMoveInfo(taskId, MOVE_INFO_FLAG_SOURCE);
        if (!CollectionUtils.isEmpty(dataMoveInfos) && !dataMoveInfos.stream().anyMatch(x -> x.getState().equals(DATA_STREAM_TASK_STATE_RUNNING))) {
            throw new DataStreamException(DataStreamErrorCode.OPER_OPERATE_INFO_TYPE_BY_MOVE_ERROR);
        }

        metaService.operateMoveTaskStop(taskId);

        dataStreamHolder.noticeTaskStop(taskId);

        List<DataMoveTaskEntity> dataMoveTaskList = metaService.queryTaskByTaskId(taskId);
        if (CollectionUtils.isEmpty(dataMoveTaskList)) {
            throw new DataStreamException(OPER_QUERY_TASK_BY_TASKID_ERROR);
        }

        if (dataMoveTaskList.get(0).getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_CDC)) {
            if (isMQDataSource(dataMoveTaskList.get(0).getSourceObjectType())) {
                SpringUtil.getBean(DataMQHandler.class).operateMQTaskStop(taskId);
            } else {
                SpringUtil.getBean(DataCdcHandler.class).operateCdcTaskStop(taskId);
            }
        }
    }

    public void operateMoveTaskRedo(Long taskId) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = metaService.queryTaskByTaskId(taskId);
        if (CollectionUtils.isEmpty(dataMoveTaskList)) {
            throw new DataStreamException(OPER_QUERY_TASK_BY_TASKID_ERROR);
        }

        if (!Arrays.asList(DATA_STREAM_TASK_STATE_ERROR, DATA_STREAM_TASK_STATE_STOP).contains(dataMoveTaskList.get(0).getState())) {
            throw new DataStreamException("xxxx", "当前任务状态不能操作重处理!");
        }
//        List<DataMoveInfoEntity> dataMoveInfos = metaService.queryDataMoveInfo(linkTaskId, MOVE_INFO_FLAG_SOURCE);
//        if (!CollectionUtils.isEmpty(dataMoveInfos) && !dataMoveInfos.stream().anyMatch(x -> x.getState().equals(dataMoveTaskList.get(0).getState()))) {
//            throw new DataStreamException(OPER_TASK_RUN_STATE_NOT_EQUAL_ERROR);
//        }

        metaService.operateMoveTaskRedo(taskId, dataMoveTaskList.get(0).getState());
        dataStreamHolder.noticeTaskRedo(taskId);
    }

    public void operateMoveTaskCopy(Long taskId) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = metaService.queryTaskByTaskId(taskId);
        if (CollectionUtils.isEmpty(dataMoveTaskList)) {
            throw new DataStreamException(OPER_QUERY_TASK_BY_ID_ERROR);
        }

        if (!Arrays.asList(DATA_STREAM_TASK_STATE_FINISH, DATA_STREAM_TASK_STATE_ERROR, DATA_STREAM_TASK_STATE_STOP).contains(dataMoveTaskList.get(0).getState())) {
            throw new DataStreamException(OPER_TASK_IS_RUN_NOT_COPY_ERROR);
        }


        Long copyTaskId = dataMoveTaskList.get(0).getCopyTaskId() != null ? dataMoveTaskList.get(0).getCopyTaskId() : dataMoveTaskList.get(0).getTaskId();
        dataMoveTaskList.get(0).setCopyTaskId(copyTaskId);
        dataMoveTaskList.get(0).setTaskId(metaService.querySequence(SEQ_MOVE_TASK_ID));
        dataMoveTaskList.get(0).setState(0);
        dataMoveTaskList.get(0).setTaskDisc(dataMoveTaskList.get(0).getTaskDisc() + "【任务复制生成:" + copyTaskId + "】");

        List<TaskExtendEntity> dataTaskExtendList = new ArrayList<>();
        for (TaskExtendEntity iterator : dataMoveTaskList.get(0).getTaskExtendList()) {
            TaskExtendEntity taskExtend = new TaskExtendEntity();
            taskExtend.setTaskId(dataMoveTaskList.get(0).getTaskId());
            taskExtend.setTaskExtendId(metaService.querySequence(SEQ_TASK_EXTEND_ID));
            taskExtend.setParameterName(iterator.getParameterName());
            taskExtend.setParameterValue(iterator.getParameterValue());
            dataTaskExtendList.add(taskExtend);
        }


        List<MoveTableEntity> moveTableList = new ArrayList<>();
        if (dataMoveTaskList.get(0).getTaskType().equals(DATA_STREAM_TASK_TYPE_TABLE_MOVE)) {
            List<MoveTableEntity> moveTablelist = metaService.queryMoveTable(copyTaskId);
            for (MoveTableEntity iterator : moveTablelist) {
                MoveTableEntity moveTable = new MoveTableEntity();
                moveTable.setMoveTableId(metaService.querySequence(SEQ_MOVE_TABLE_ID));
                moveTable.setTaskId(dataMoveTaskList.get(0).getTaskId());
                moveTable.setSourceTableName(iterator.getSourceTableName());
                moveTable.setState(DATA_STREAM_TASK_STATE_INIT);
                moveTableList.add(moveTable);
            }
        }


        metaService.createTableMoveTask(dataMoveTaskList, moveTableList, dataTaskExtendList);
    }

    public String makeSqlMinMax(Integer dataBaseType, String columnName, String tableName) throws DataStreamException {
        return matchDataBase(dataBaseType).makeSqlMinMax(columnName, tableName);
    }

    private Integer deleteDataFromSourceTable(final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        List<Map> dataListTargetFilter = filterTargetExistsData(dataListTarget, dataMoveTask);

        List<String> dataDeleteSqlList = (dataListTargetFilter.size() > dataStreamConfig.getDataStreamParallelStreamSize()) ? dataListTargetFilter.parallelStream().map(dataRecord -> {
            try {
                return makeSqlSourceDataKeyDelete(dataRecord, dataMoveTask);
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).filter(Objects::nonNull).collect(Collectors.toList()) : dataListTargetFilter.stream().map(dataRecord -> {
            try {
                return makeSqlSourceDataKeyDelete(dataRecord, dataMoveTask);
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return moveSourceService.deleteDataList(dataMoveTask.getSourceObjectId(), dataDeleteSqlList);
    }

    private Integer checkDataToTargetTable(final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        List<Map> dataListTargetNotExist = filterTargetNotExistsData(dataListTarget, dataMoveTask);
        //转换格式写入稽核差异表
        List<DataCheckEntity> dataCheckList = new ArrayList<>();
        for (Map iterator : dataListTargetNotExist) {
            List<String> dataKeyValueList = matchDataBase(dataMoveTask.getTargetDataBase().getDataBaseType()).makeSqlKeyColumnSelect(dataMoveTask.getTargetKeyColumns(), dataMoveTask.getTargetTableColumns(), iterator);
            if (!CollectionUtils.isEmpty(dataKeyValueList)) {
                String keysValue = dataKeyValueList.stream().collect(Collectors.joining(" | "));
                DataCheckEntity dataCheck = new DataCheckEntity();
                dataCheck.setDataCheckId(metaService.querySequence(SEQ_DATA_CHECK_ID));
                dataCheck.setState(DATA_CHECK_STATE_CHECK_GEN);
                dataCheck.setCheckResult(DATA_CHECK_RESULT_SOURCE_MORE);
                dataCheck.setTaskId(dataMoveTask.getTaskId());
                dataCheck.setCheckKeys(keysValue);
                dataCheckList.add(dataCheck);

                if (dataMoveTask.getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_CHECK) && dataMoveTask.getTaskDisc().contains("genFromDataCheckId")) {
                    Long dataCheckTaskId = parseDataCheckTaskId(dataMoveTask.getTaskDisc());
                    if (dataCheckTaskId > 0L) {
                        dataCheck = new DataCheckEntity();
                        dataCheck.setDataCheckId(metaService.querySequence(SEQ_DATA_CHECK_ID));
                        dataCheck.setState(DATA_CHECK_STATE_CHECK_GEN);
                        dataCheck.setCheckResult(DATA_CHECK_RESULT_SOURCE_LESS);
                        dataCheck.setTaskId(dataCheckTaskId);
                        dataCheck.setCheckKeys(keysValue);
                        dataCheckList.add(dataCheck);
                    }
                }
            }
        }
        if (!CollectionUtils.isEmpty(dataCheckList)) {
            metaService.insertDataCheck(dataCheckList);
        }

        return dataListTarget.size();
    }

    private Long parseDataCheckTaskId(String temp) {
        int startIndex = temp.indexOf('[') + 1;
        int endIndex = temp.indexOf(']');
        // 根据索引截取子字符串
        String taskIdStr = temp.substring(startIndex, endIndex);
        Long taskId = Long.parseLong(taskIdStr);
        return taskId;
    }

    List<Map> filterTargetExistsData(final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        //无需过滤直接去删除
        if (dataMoveTask.getTargetCheckFlag() != null && dataMoveTask.getTargetCheckFlag().equals(2)) {
            return dataListTarget;
        }

        if (CollectionUtils.isEmpty(dataMoveTask.getTargetKeyColumns())) {
            return new ArrayList<>(0);
        }

        List<Map> dataList = (dataListTarget.size() > dataStreamConfig.getDataStreamParallelStreamSize()) ? dataListTarget.parallelStream().filter(dataRecord -> {
            try {
                return moveTargetService.executeSelectRecordCountSql(dataMoveTask.getTargetDataBase().getDataBaseId(), makeSqlSelectCountByKey(dataMoveTask.getTargetDataBase().getDataBaseType(), dataRecord, dataMoveTask.getTargetKeyColumns(), dataMoveTask.getTargetTableColumns(), dataMoveTask.getTargetObjectName())) > 0L;
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()) : dataListTarget.stream().filter(dataRecord -> {
            try {
                return moveTargetService.executeSelectRecordCountSql(dataMoveTask.getTargetDataBase().getDataBaseId(), makeSqlSelectCountByKey(dataMoveTask.getTargetDataBase().getDataBaseType(), dataRecord, dataMoveTask.getTargetKeyColumns(), dataMoveTask.getTargetTableColumns(), dataMoveTask.getTargetObjectName())) > 0L;
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return dataList;
    }

    List<Map> filterTargetNotExistsData(final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        if (CollectionUtils.isEmpty(dataMoveTask.getTargetKeyColumns())) {
            return new ArrayList<>(0);
        }

        List<Map> dataList = (dataListTarget.size() > dataStreamConfig.getDataStreamParallelStreamSize()) ? dataListTarget.parallelStream().filter(dataRecord -> {
            try {
                return moveTargetService.executeSelectRecordCountSql(dataMoveTask.getTargetDataBase().getDataBaseId(), makeSqlSelectCountByKey(dataMoveTask.getTargetDataBase().getDataBaseType(), dataRecord, dataMoveTask.getTargetKeyColumns(), dataMoveTask.getTargetTableColumns(), dataMoveTask.getTargetObjectName())) == 0L;
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()) : dataListTarget.stream().filter(dataRecord -> {
            try {
                return moveTargetService.executeSelectRecordCountSql(dataMoveTask.getTargetDataBase().getDataBaseId(), makeSqlSelectCountByKey(dataMoveTask.getTargetDataBase().getDataBaseType(), dataRecord, dataMoveTask.getTargetKeyColumns(), dataMoveTask.getTargetTableColumns(), dataMoveTask.getTargetObjectName())) == 0L;
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return dataList;
    }

    public Integer getTaskRunningSize() {
        return dataStreamHolder.getTaskRunningSize();
    }

    public void clearTaskRunning(Long taskId) {
        dataStreamHolder.clearTaskRunning(taskId);
    }

    public void setTaskRunning(Long taskId, Integer taskType) {
        dataStreamHolder.setTaskIsRunningMap(taskId, taskType);
    }

    public List<TaskExecuteEntity> queryTaskExecute(Long taskId) throws DataStreamException {
        return metaService.queryTaskExecute(taskId);
    }

    public void loadTaskExtendParameters(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        metaService.loadTaskExtendParameters(dataMoveTask);
    }

    public List<MoveTableEntity> queryMoveTable(Long taskId) throws DataStreamException {
        return metaService.queryMoveTable(taskId);
    }

    public List<DataMoveTaskEntity> queryTaskByTaskId(Long taskId) throws DataStreamException {
        return metaService.queryTaskByTaskId(taskId);
    }

    public void jobLogbackEventTableInfo(Long moveTableId, String content) {
        systemLogEvent.jobLogbackEvent(JOB_TYPE_MOVE_TABLE, moveTableId, content);
    }

    public Long statFileLineCount(final Integer sourceObjectType, final String objectFileName, final String localPath) throws DataStreamException {
        IFileApi fileApi = matchFileFormat(sourceObjectType);
        return fileApi.statFileLineCount(objectFileName, localPath).longValue();
    }
}
