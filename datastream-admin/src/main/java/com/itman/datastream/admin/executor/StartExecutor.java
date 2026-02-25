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

import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.DataMoveTaskEntity;
import com.itman.datastream.common.entity.TableLinkTaskEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.itman.datastream.common.constant.DataStreamConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartExecutor {
    private final DataStreamConfig dataStreamConfig;
    private final IMetaService metaService;
    private final MoveExecutor moveExecutor;
    private final MonitorExecutor monitorExecutor;
    private final LinkTaskExecutor linkTaskExecutor;

    /**
     * 1: 迁移任务启动
     * 2: 指标监控启动
     * 3: 表链接启动
     */
    public static final String START_EXECUTOR_TYPE_MOVE = "1";
    public static final String START_EXECUTOR_TYPE_MONITOR = "2";
    public static final String START_EXECUTOR_TYPE_LINK = "3";


    AtomicInteger counter = new AtomicInteger(0); // 计数器确保唯一性

    private final ExecutorService executorService = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "Start-Executor-" + counter.getAndIncrement());
        return t;
    });

    @PostConstruct
    public void init() throws DataStreamException {

        if (dataStreamConfig.getDataStreamInitDataMetaDbEnable()) {
            log.info("getDataStreamInitDataMetaDbEnable is Enable");
            metaService.initDataStreamMetaDb();
        }

        if (dataStreamConfig.getDataStreamStartExecutorType() == null) {
            log.info("DataStreamStartExecutorType is not config!");
            return;
        }

        List<String> startExecutorTypeList = Arrays.asList(dataStreamConfig.getDataStreamStartExecutorType().split("\\|"));
        if (CollectionUtils.isEmpty(startExecutorTypeList)) {
            log.info("DataStreamStartExecutorType config  is null!");
            return;
        }

        if (startExecutorTypeList.contains(START_EXECUTOR_TYPE_MOVE)) {
            executorService.submit(() -> {
                scheduledMoveTask();
            });
        }

        if (startExecutorTypeList.contains(START_EXECUTOR_TYPE_MONITOR)) {
            executorService.submit(() -> {
                monitorExecutor.scheduledMonitor();
            });
        }

        if (startExecutorTypeList.contains(START_EXECUTOR_TYPE_LINK)) {
            executorService.submit(() -> {
                scheduledLinkTask();
            });
        }
    }

    public void scheduledMoveTask() {
        while (true) {
            try {
                if (moveExecutor.getTaskRunningSize() <= dataStreamConfig.getDataStreamParallelTaskSize()) {
                    dispatchMoveTask();
                } else {
                    log.info("now max task running size:" + moveExecutor.getTaskRunningSize());
                }

                Thread.sleep(3000);
            } catch (DataStreamException aie) {
                log.error("scheduledMoveTask ---------SystemException=", aie);
            } catch (InterruptedException ie) {
                log.error("scheduledMoveTask ---------Thread was interrupted", ie);
                Thread.currentThread().interrupt(); // 恢复中断状态
            } catch (ThreadDeath td) {
                log.error("scheduledMoveTask ---------Thread is dead", td);
            } catch (Exception e) {
                log.error("scheduledMoveTask ---------Exception=", e);
            }
        }
    }

    public void dispatchMoveTask() throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = metaService.queryDataMoveTaskByState(DATA_STREAM_TASK_STATE_INIT, 1, 10);
        if (!CollectionUtils.isEmpty(dataMoveTaskList) && dataMoveTaskList.get(0).getTaskId() != null) {
            if (dataMoveTaskList.get(0).getState().equals(DATA_STREAM_TASK_STATE_INIT)) {
                Integer sendMode = (dataMoveTaskList.get(0).getSendMode() == null) ? dataMoveTaskList.get(0).getSourcePropertiesSendMode() : dataMoveTaskList.get(0).getSendMode();
                Long taskExecuteId = metaService.querySequence(SEQ_TASK_EXECUTE_ID);
                dataMoveTaskList.get(0).setTaskExecuteId(taskExecuteId);
                Integer updateCount = metaService.updateDataMoveTaskState(dataMoveTaskList.get(0).getTaskId(), taskExecuteId, DATA_STREAM_TASK_STATE_INIT, DATA_STREAM_TASK_STATE_RUNNING, sendMode);
                if (!updateCount.equals(1)) {
                    log.info("dispatchMoveTask : have been done by other host thread, linkTaskId={}", dataMoveTaskList.get(0).getTaskId());
                    return;
                }

                dataMoveTaskList.get(0).setSendMode(sendMode);
                moveExecutor.dispatchTaskProcess(dataMoveTaskList.get(0));
            }
        } else {
            log.info("dispatchMoveTask : there is no data move task !");
        }
    }

    public void scheduledLinkTask() {
        while (true) {
            try {
                dispatchTableLinkTask();

                Thread.sleep(3000);
            } catch (DataStreamException aie) {
                log.error("scheduledLinkTask ---------Exception=", aie);
            } catch (InterruptedException ie) {
                log.error("scheduledLinkTask ---------Thread was interrupted", ie);
                Thread.currentThread().interrupt(); // 恢复中断状态
            } catch (ThreadDeath td) {
                log.error("scheduledLinkTask ---------Thread is dead", td);
            } catch (Exception e) {
                log.error("scheduledLinkTask ---------Exception=", e);
            }
        }
    }

    public void dispatchTableLinkTask() throws DataStreamException {
        List<TableLinkTaskEntity> tableLinkTaskList = metaService.queryTableLinkTaskByState(TABLE_LINK_TASK_STATE_INIT, 1, 1000);
        if (!tableLinkTaskList.isEmpty() && tableLinkTaskList.get(0).getLinkTaskId() != null) {
            if (tableLinkTaskList.get(0).getState().equals(TABLE_LINK_TASK_STATE_INIT)) {
                if (!metaService.updateTableLinkTask(tableLinkTaskList.get(0).getLinkTaskId(), TABLE_LINK_TASK_STATE_INIT, TABLE_LINK_TASK_STATE_ING, dataStreamConfig.getHostName(), dataStreamConfig.getHostIP()).equals(1)) {
                    log.info("dispatchTableLinkTask : have been done by other host thread, linkTaskId={}", tableLinkTaskList.get(0).getLinkTaskId());
                    return;
                }
                linkTaskExecutor.tableLinkTaskProcess(tableLinkTaskList.get(0));
            }
        } else {
            log.info("dispatchTableLinkTask : there is no data back task !");
        }
    }
}
