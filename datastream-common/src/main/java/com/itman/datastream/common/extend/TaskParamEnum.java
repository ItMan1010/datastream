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

import com.itman.datastream.common.entity.DataMoveTaskEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum TaskParamEnum {

    DATA_STREAM_QUEUE_CHANNEL("DataStreamQueueChannel", (task, v) -> task.setDataStreamQueueChannel(Integer.parseInt(v))),

    DATA_STREAM_CHECK_MODE("DataStreamCheckMode", (task, v) -> task.setCheckMode(Integer.parseInt(v))),


    SOURCE_POOL_COUNT("SourcePropertiesDataPoolCount", (task, v) -> task.setSourcePropertiesDataPoolCount(Integer.parseInt(v))),

    SOURCE_THREAD_COUNT("SourcePropertiesThreadCount", (task, v) -> task.setSourcePropertiesThreadCount(Integer.parseInt(v))),

    SOURCE_SELECT_COUNT("SourcePropertiesSelectCount", (task, v) -> task.setSourcePropertiesSelectCount(Integer.parseInt(v))),

    SOURCE_SEND_MODE("SourcePropertiesSendMode", (task, v) -> task.setSourcePropertiesSendMode(Integer.parseInt(v))),

    SOURCE_OFFSET_STORAGE("SourceOffsetStorage", (task, v) -> task.setSourceOffsetStorage(Integer.parseInt(v))),

    SOURCE_OFFSET_KAFKA("SourceOffsetKafka", (task, v) -> task.setSourceOffsetKafka(v)),

    SOURCE_OFFSET_START_POS("SourceOffsetStartPos", (task, v) -> task.setSourceOffsetStartPos(v)),
    SOURCE_DEBEZIUM_SNAPSHOT("SourceDebeziumSnapshot", (task, v) -> task.setSourceDebeziumSnapshot(Integer.parseInt(v))),

    SOURCE_DEBEZIUM_OBJECT("SourceDebeziumObject", (task, v) -> task.setSourceDebeziumObject(Integer.parseInt(v))),

    SOURCE_DATABASE_OBJECT_TYPE("sourceDataBaseObjectType", (task, v) -> task.setSourceDataBaseObjectType(Integer.parseInt(v))),

    TARGET_POOL_COUNT("TargetPropertiesDataPollCount", (task, v) -> task.setTargetPropertiesDataPollCount(Integer.parseInt(v))),

    TARGET_THREAD_COUNT("TargetPropertiesThreadCount", (task, v) -> task.setTargetPropertiesThreadCount(Integer.parseInt(v))),

    TARGET_INSERT_COUNT("TargetPropertiesInsertCount", (task, v) -> task.setTargetPropertiesInsertCount(Integer.parseInt(v))),

    TARGET_MOVE_INFO_FLAG("TargetPropertiesMoveInfoFlag", (task, v) -> task.setTargetPropertiesMoveInfoFlag(Integer.parseInt(v))),

    TARGET_CHECK_FLAG("targetPropertiesCheckFlag", (task, v) -> task.setTargetCheckFlag(Integer.parseInt(v)));

    private final String key;
    private final BiConsumer<DataMoveTaskEntity, String> applier;

    private static final Map<String, TaskParamEnum> CACHE = Arrays.stream(values()).collect(Collectors.toMap(TaskParamEnum::getKey, Function.identity()));

    public static TaskParamEnum of(String key) {
        return CACHE.get(key);
    }
}