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
package com.itman.datastream.common.extend;

import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.DataMoveTaskEntity;
import com.itman.datastream.common.entity.TaskExtendEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskParamExtend {
    public final DataStreamConfig dataStreamConfig;

    public TaskParamExtend(DataStreamConfig dataStreamConfig) {
        this.dataStreamConfig = dataStreamConfig;
    }


    private void addParam(Long taskId, String paramName, Object paramValue, List<TaskExtendEntity> list) throws DataStreamException {
        if (paramValue == null) {
            return;
        }

        TaskExtendEntity taskExtend = new TaskExtendEntity();
        taskExtend.setTaskId(taskId);
        taskExtend.setParameterName(paramName);
        taskExtend.setParameterValue(paramValue.toString());
        list.add(taskExtend);
    }

    public void buildTaskExtendParameters(List<DataMoveTaskEntity> dataMoveTaskList, List<TaskExtendEntity> dataTaskExtendList) throws DataStreamException {

        for (DataMoveTaskEntity task : dataMoveTaskList) {

            Long taskId = task.getTaskId();

            // ===== 通用参数 =====
            addParam(taskId, TaskParamEnum.DATA_STREAM_QUEUE_CHANNEL.getKey(), dataStreamConfig.getDataStreamQueueChannel(), dataTaskExtendList);

            // ===== CheckMode =====
            addParam(taskId, TaskParamEnum.DATA_STREAM_CHECK_MODE.getKey(), task.getCheckMode(), dataTaskExtendList);

            // ===== Source 参数 =====
            addParam(taskId, TaskParamEnum.SOURCE_POOL_COUNT.getKey(), dataStreamConfig.getSource().getDataPoolCount(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.SOURCE_THREAD_COUNT.getKey(), dataStreamConfig.getSource().getThreadCount(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.SOURCE_SELECT_COUNT.getKey(), dataStreamConfig.getSource().getSelectCount(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.SOURCE_SEND_MODE.getKey(), dataStreamConfig.getSource().getSourceSendMode(), dataTaskExtendList);

            // ===== Offset & Debezium =====
            addParam(taskId, TaskParamEnum.SOURCE_OFFSET_KAFKA.getKey(), task.getSourceOffsetKafka(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.SOURCE_OFFSET_STORAGE.getKey(), task.getSourceOffsetStorage(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.SOURCE_OFFSET_START_POS.getKey(), task.getSourceOffsetStartPos(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.SOURCE_DEBEZIUM_SNAPSHOT.getKey(), task.getSourceDebeziumSnapshot(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.SOURCE_DEBEZIUM_OBJECT.getKey(), task.getSourceDebeziumObject(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.SOURCE_DATABASE_OBJECT_TYPE.getKey(), task.getSourceDataBaseObjectType(), dataTaskExtendList);

            // ===== Target 参数 =====
            addParam(taskId, TaskParamEnum.TARGET_POOL_COUNT.getKey(), dataStreamConfig.getTarget().getDataPoolCount(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.TARGET_THREAD_COUNT.getKey(), dataStreamConfig.getTarget().getThreadCount(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.TARGET_INSERT_COUNT.getKey(), dataStreamConfig.getTarget().getInsertCount(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.TARGET_MOVE_INFO_FLAG.getKey(), dataStreamConfig.getTarget().getMoveInfoFlag(), dataTaskExtendList);

            addParam(taskId, TaskParamEnum.TARGET_CHECK_FLAG.getKey(), task.getTargetCheckFlag(), dataTaskExtendList);
        }
    }

    public void loadTaskExtendParameters(DataMoveTaskEntity task) throws DataStreamException {
        for (TaskExtendEntity ext : task.getTaskExtendList()) {
            TaskParamEnum param = TaskParamEnum.of(ext.getParameterName());
            if (param != null) {
                param.getApplier().accept(task, ext.getParameterValue());
            }
        }
    }
}
