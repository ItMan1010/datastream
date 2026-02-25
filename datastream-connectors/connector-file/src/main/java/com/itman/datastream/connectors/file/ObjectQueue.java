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
package com.itman.datastream.connectors.file;

import com.itman.datastream.common.entity.FileTaskResult;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.itman.datastream.common.constant.DataStreamConstant.COMPARED_FLAG_NO;


@Slf4j
public class ObjectQueue {

    private ObjectQueue() {
        // 私有构造函数，防止外部实例化
    }

    //通过静态内部类方式保证了线程安全
    public static class HoldObjectQueue {
        private static ObjectQueue instance = new ObjectQueue();
    }

    public static LinkedBlockingQueue<QueueData> getDataQueue() {
        return dataQueue;
    }

    private static LinkedBlockingQueue<QueueData> dataQueue = null;

    public static List<FileTaskResult> getTaskResultList() {
        return taskResultList;
    }

    private static List<FileTaskResult> taskResultList = null;

    public static Map<String, QueueData> getDataObjectMap() {
        return dataObjectMap;
    }

    private static Map<String, QueueData> dataObjectMap = null;
    private static final Object lock = new Object();
    private static boolean aFinished = false;
    //A、B只要有一个注册失败状态，其他线程收到状态都退出处理
    private static AtomicBoolean workThreadFailState = new AtomicBoolean(false);
    private static Long currentRow = 0L;
    private static ConcurrentHashMap<Long, AtomicInteger> dataSumCountMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Long, AtomicLong> dataSumAmountMap = new ConcurrentHashMap<>();


    public static Long getBeginRowNum(Integer tableSelectCount) {
        synchronized (lock) {
            Long beginRow = currentRow;
            currentRow += tableSelectCount;
            return beginRow;
        }
    }

    public static boolean isAFinished() {
        synchronized (lock) {
            return aFinished;
        }
    }

    public static boolean isWorkThreadFail() {
        return workThreadFailState.get();
    }

    public static void setWorkThreadFail() {
        workThreadFailState.set(true);
    }

    public static void aSetFinish() {
        synchronized (lock) {
            aFinished = true;
        }
    }

    public static void setDataSumCount(Long taskInstanceId, Integer dataCount) {
        dataSumCountMap.computeIfAbsent(taskInstanceId, k -> new AtomicInteger(0)).addAndGet(dataCount);
    }

    public static void setDataSumAmount(Long sumFieldRelaId, Long dataAmount) {
        dataSumAmountMap.computeIfAbsent(sumFieldRelaId, k -> new AtomicLong(0L)).addAndGet(dataAmount);
    }

    public static Integer getDataSumCount(Long taskInstanceId) {
        if (CollectionUtils.isEmpty(dataSumCountMap)) {
            return 0;
        } else {
            return dataSumCountMap.get(taskInstanceId).get();
        }
    }

    public static ConcurrentHashMap<Long, AtomicInteger> getDataSumCountMap() {
        return dataSumCountMap;
    }

    public static Long getDataSumAmount(Long sumFieldRelaId) {
        if (dataSumAmountMap.containsKey(sumFieldRelaId)) {
            return dataSumAmountMap.get(sumFieldRelaId).get();
        }
        return 0L;
    }


    public static void initProcess(Integer onceCrawNum, Integer filterFlag) {
        synchronized (lock) {
            aFinished = false;
            workThreadFailState.set(false);
            currentRow = 0L;
            if (dataQueue != null) {
                dataQueue.clear();
            }
            dataQueue = new LinkedBlockingQueue<>(onceCrawNum * 3);

            if (!(filterFlag != null && filterFlag.equals(1))) {
                if (dataObjectMap != null) {
                    dataObjectMap.clear();
                }
                dataObjectMap = new HashMap<>(1000000);
            }

            if (taskResultList != null) {
                taskResultList.clear();
            }
            taskResultList = new ArrayList<>(1000);
            dataSumCountMap.clear();
            dataSumAmountMap.clear();
        }
    }

    public static void finishProcess(final Long taskInstanceId) {
        dataSumCountMap.remove(taskInstanceId);
    }

    public static List<QueueData> queuePoll(Integer onceCrawNum) {
        List<QueueData> records = new ArrayList<>(onceCrawNum);
        for (int i = 0; i < onceCrawNum; i++) {
            QueueData r = dataQueue.poll();
            if (r != null) {
                records.add(r);
            } else {
                break;
            }
        }
        return records;
    }

    public static void putDataIntoQueue(final List<QueueData> dataQueueList) throws DataStreamException {
        if (CollectionUtils.isEmpty(dataQueueList)) {
            return;
        }

        try {
            for (QueueData iterator : dataQueueList) {
                while (true) {
                    if (workThreadFailState.get()) {
                        log.error("-------putDataIntoQueue break !");
                        throw new DataStreamException("putDataIntoQueue", "被通知中止退出处理");
                    }

                    if (dataQueue.offer(iterator, 1000, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                    //如果消费端处理慢的，则需要一直在这里循环处理
                    log.info("------putDataIntoQueue continue !");
                }
            }
        } catch (InterruptedException e) {
            log.error("InterruptedException=", e);
            throw new DataStreamException("putDataIntoQueue", "插入数据到消息内存队列失败InterruptedException !");
        }
    }

    public static void putDataIntoMap(final String mapKey, final Map<Long, String> dataObjetMap) {
        synchronized (lock) {
            QueueData queueData = new QueueData();
            queueData.setDataMap(dataObjetMap);
            queueData.setComparedFlag(COMPARED_FLAG_NO);
            dataObjectMap.computeIfAbsent(mapKey, key -> queueData);
        }
    }

    public static void putTaskResult(final FileTaskResult taskResult) {
        synchronized (lock) {
            taskResultList.add(taskResult);
        }
    }
}
