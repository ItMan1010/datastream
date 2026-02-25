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
package com.itman.datastream.engine.holder;

import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.entity.DataMoveInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class DataStreamHolder {
    private DataStreamHolder() {
        // 私有构造函数，防止外部实例化
    }

    //通过静态内部类方式保证了线程安全
    public static class HoldDataStreamQueue {
        private static DataStreamHolder instance = new DataStreamHolder();
    }

    private final static ConcurrentHashMap<Long, Map<Integer, LinkedBlockingQueue<QueueObject>>> dataStreamQueueHashMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Integer> errorSourceVirtualIdHashMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Integer> errorTargetVirtualIdHashMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Integer> taskAllSourceThreadFinishedMap = new ConcurrentHashMap<>();
    //缓存分页处理当前页数值，用于多线程并发控制
    //缓存分段处理当前主键key值，用于多线程并发控制
    private final static ConcurrentHashMap<Long, CurrentValueObject> taskCurrentValueHashMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Boolean> targetMoveRunInfoExistsMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, DataMoveInfoEntity> taskDataMoveInfoHashMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, List<Long>> taskAllSourceMoveInfoIdMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, List<Long>> taskAllTargetMoveInfoIdMap = new ConcurrentHashMap<>();


    public static void setTargetMoveRunInfoExistsMap(Long taskId, Boolean flag) {
        //false表示在目标不存在运行记录表
        targetMoveRunInfoExistsMap.put(taskId, flag);
    }

    public static Boolean getTargetMoveRunInfoExistsMap(Long taskId) {
        return targetMoveRunInfoExistsMap.get(taskId);
    }

    public static void setCurrentValueHashMap(Long taskId, String currentValue, Integer pageLoopCount) {
        // 仅在确实需要新对象时创建，避免不必要的对象创建开销
        taskCurrentValueHashMap.compute(taskId, (key, existingValue) -> {
            CurrentValueObject valueObject = (existingValue != null) ? existingValue : new CurrentValueObject();
            valueObject.setCurrentValue(currentValue);
            valueObject.setPageLoopCount(pageLoopCount);
            return valueObject;
        });
    }

    public static CurrentValueObject getCurrentValueHashMap(Long taskId) {
        return taskCurrentValueHashMap.get(taskId);
    }

    public static void setErrorSourceVirtualId(Long taskId, Integer virtualId) {
        errorSourceVirtualIdHashMap.put(taskId, virtualId);
    }

    public static Integer getErrorSourceVirtualId(Long taskId) {
        return errorSourceVirtualIdHashMap.get(taskId);
    }


    public static void setErrorTargetVirtualId(Long taskId, Integer virtualId) {
        errorTargetVirtualIdHashMap.put(taskId, virtualId);
    }

    public static Integer getErrorTargetVirtualId(Long taskId) {
        return errorTargetVirtualIdHashMap.get(taskId);
    }

    public static void setTaskAllSourceThreadFinished(Long taskId) {
        taskAllSourceThreadFinishedMap.put(taskId, 1);
    }

    public static Integer getTaskAllSourceThreadFinished(Long taskId) {
        return taskAllSourceThreadFinishedMap.get(taskId);
    }

    public static void initTaskQueue(Long taskId, Integer channelCount, Integer selectCount, Integer dataStreamQueueSize) {
        dataStreamQueueHashMap.remove(taskId);
        dataStreamQueueHashMap.computeIfAbsent(taskId, k -> {
            Map<Integer, LinkedBlockingQueue<QueueObject>> innerMap = new HashMap<>(channelCount);
            for (int channelId = 1; channelId <= channelCount; channelId++) {
                innerMap.put(channelId, new LinkedBlockingQueue<>(selectCount * dataStreamQueueSize));
            }
            return innerMap;
        });
        errorSourceVirtualIdHashMap.put(taskId, 0);
        errorTargetVirtualIdHashMap.put(taskId, 0);
        taskAllSourceThreadFinishedMap.put(taskId, 0);
    }

    public static void clearTaskQueue(Long taskId) {
        dataStreamQueueHashMap.remove(taskId);
        errorSourceVirtualIdHashMap.remove(taskId);
        errorTargetVirtualIdHashMap.remove(taskId);
        taskAllSourceThreadFinishedMap.remove(taskId);
        taskCurrentValueHashMap.remove(taskId);
        targetMoveRunInfoExistsMap.remove(taskId);
    }

    public static void addQueue(final Long taskId, final Integer channelId, final List<Map> dataList) throws DataStreamException {
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }

        for (Map iterator : dataList) {
            while (true) {
                try {
                    QueueObject queueObject = new QueueObject();
                    queueObject.setDataMap(iterator);
                    if (dataStreamQueueHashMap.get(taskId).get(channelId).offer(queueObject, 100, TimeUnit.MILLISECONDS)) {
                        break;
                    }

                    //只要有一个消费线程异常，就停止写入
                    if (getErrorTargetVirtualId(taskId) > 0) {
                        throw new DataStreamException(DataStreamErrorCode.OPER_DATA_TARGET_THREAD_FAIL_NOTICE_OUT_ERROR);
                    }
                } catch (InterruptedException e) {
                    log.error("linkTaskId={}, InterruptedException=", taskId, e);
                    Thread.currentThread().interrupt();
                    throw new DataStreamException(DataStreamErrorCode.OPER_PUT_QUEUE_ERROR);
                }
            }
        }
    }

    public static List<Map> pollQueue(final Long taskId, final Integer channelId, final Integer insertCount) {
        List<Map> records = new ArrayList<>(insertCount);
        for (int i = 0; i < insertCount; i++) {
            QueueObject r = dataStreamQueueHashMap.get(taskId).get(channelId).poll();
            if (r != null) {
                records.add(r.getDataMap());
            } else if (r == null) {
                break;
            }
        }
        return records;
    }

    public Integer getQueueRunningSize(final Long taskId) {
        AtomicReference<Integer> runningSize = new AtomicReference<>(0);
        if (dataStreamQueueHashMap.containsKey(taskId)) {
            dataStreamQueueHashMap.get(taskId).forEach((x, y) -> {
                runningSize.set(runningSize.get() + y.size());
            });
        }
        return runningSize.get();
    }


    private static ConcurrentHashMap<Long, Boolean> noticeTaskStopHashMap = new ConcurrentHashMap<>();
    //linkTaskId,taskType
    private static ConcurrentHashMap<Long, Integer> taskIsRunningMap = new ConcurrentHashMap<>();

    public static void setTaskIsRunningMap(Long taskId, Integer taskType) {
        taskIsRunningMap.put(taskId, taskType);
    }

    public static Integer getTaskRunningSize() {
        return taskIsRunningMap.size();
    }

    public static List<Long> getTaskRunningList() {
        if (taskIsRunningMap.isEmpty()) {
            return Collections.emptyList(); // 返回不可变空集合，避免空指针
        }
        return new ArrayList<>(taskIsRunningMap.keySet());
    }

    public static void clearTaskRunning(Long taskId) {
        taskIsRunningMap.remove(taskId);
    }

    public static void noticeTaskStop(Long taskId) {
        noticeTaskStopHashMap.put(taskId, true);
    }

    public static void noticeTaskRedo(Long taskId) {
        noticeTaskStopHashMap.put(taskId, false);
    }


    public static Boolean isNoticeTaskStop(Long taskId) {
        return noticeTaskStopHashMap.get(taskId) == null ? false : noticeTaskStopHashMap.get(taskId);
    }

    public static void setDataMoveInfo(Long infoId, DataMoveInfoEntity dataMoveInfo) {
        taskDataMoveInfoHashMap.put(infoId, dataMoveInfo);
    }

    public static DataMoveInfoEntity getDataMoveInfo(Long infoId) {
        return taskDataMoveInfoHashMap.get(infoId);
    }

    public static void updateDataMoveInfoPageRowStart(Long infoId, String pageRowStart, Integer pageLoopCount) {
        taskDataMoveInfoHashMap.compute(infoId, (key, entity) -> {
            if (entity == null) return null; // 或抛异常
            entity.setPageRowStart(pageRowStart);
            entity.setPageLoopCount(pageLoopCount);
            return entity;
        });
    }

    public static void updateDataMoveInfoPageRowEnd(Long infoId, String pageRowEnd, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) {
        taskDataMoveInfoHashMap.compute(infoId, (key, entity) -> {
            if (entity == null) return null; // 或抛异常
            entity.setPageRowEnd(pageRowEnd);
            entity.setDataCount(dataCount + entity.getDataCount());
            entity.setLoopCount(1 + entity.getLoopCount());
            entity.setDataActualCount(dataActualCount + entity.getDataActualCount());
            if (maxCost != 0) entity.setMaxCost(maxCost);
            if (minCost != 0) entity.setMinCost(minCost);
            if (currentCost != 0) entity.setLatelyCost(currentCost);
            if (currentCost != 0) entity.setSumCost(currentCost + entity.getSumCost());
            return entity;
        });
    }

    public static void clearTaskState(Long taskId) {
        noticeTaskStopHashMap.remove(taskId);
    }

    public static void addSourceMoveInfoId(Long taskId, Long infoId) {
        taskAllSourceMoveInfoIdMap.computeIfAbsent(taskId, k -> new ArrayList<>()).add(infoId);
    }

    public static List<Long> getSourceMoveInfoId(Long taskId) {
        return taskAllSourceMoveInfoIdMap.get(taskId);
    }

    public static void addTargetMoveInfoId(Long taskId, Long infoId) {
        taskAllTargetMoveInfoIdMap.computeIfAbsent(taskId, k -> new ArrayList<>()).add(infoId);
    }

    public static List<Long> getTargetMoveInfoId(Long taskId) {
        return taskAllTargetMoveInfoIdMap.get(taskId);
    }

    public static void clearSourceDataMoveInfo(Long taskId, Long infoId) {
        taskDataMoveInfoHashMap.remove(infoId);
        taskAllSourceMoveInfoIdMap.computeIfPresent(taskId, (k, idList) -> {
            idList.remove(infoId);
            return idList.isEmpty() ? null : idList; // 如果列表为空，则自动移除整个条目
        });
    }

    public static void clearTargetDataMoveInfo(Long taskId, Long infoId) {
        taskDataMoveInfoHashMap.remove(infoId);
        taskAllTargetMoveInfoIdMap.computeIfPresent(taskId, (k, idList) -> {
            idList.remove(infoId);
            return idList.isEmpty() ? null : idList; // 如果列表为空，则自动移除整个条目
        });
    }

    public static List<DataMoveInfoEntity> getMoveInfoListFromMem(Long taskId) {
        List<DataMoveInfoEntity> dataMoveInfoList = new ArrayList<>();
        List<Long> sourceMoveInfoIdList = getSourceMoveInfoId(taskId);
        if(!CollectionUtils.isEmpty(sourceMoveInfoIdList)){
            for (Long iterator : sourceMoveInfoIdList) {
                dataMoveInfoList.add(getDataMoveInfo(iterator));
            }
        }


        List<Long> targetMoveInfoIdList = getTargetMoveInfoId(taskId);
        if(!CollectionUtils.isEmpty(targetMoveInfoIdList)){
            for (Long iterator : targetMoveInfoIdList) {
                dataMoveInfoList.add(getDataMoveInfo(iterator));
            }
        }
        return dataMoveInfoList;
    }
}
