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
package com.itman.datastream.common.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataMoveTaskEntity {
    private Long taskId;
    private Long taskExecuteId;
    private Integer taskType;

    private Long sourceObjectId;
    private Integer sourceObjectType;
    private String sourceObjectName;
    private String sourceObjectCondition;
    private Long sourceObjectCount;
    private String sourceKeysBegin;
    private String sourceKeysEnd;
    //todo 后续DataBaseEntity、FileFormatEntity使用object对象进行合并
    private DataBaseEntity sourceDataBase;
    private FileFormatEntity sourceFileFormat;
    private MQConfigEntity sourceMQConfig;
    private List<TableColumnEntity> sourceTableColumns;
    private List<TableColumnEntity> sourceTableKeysList;
    private List<ColumnTypeDefineEntity> sourceTableColumnTypeDefineList;
    private List<String> sourceKeyColumns;
    private String sourceSelectSqlColumns;
    private String sourceSelectSql;
    private String sourceObjectKeys;
    private Integer sourceLoadStrategy;
    private String sourceDataNode;
    private Integer sourceDataSet;
    private Integer splitTableFlag;
    private Integer sourceOffsetStorage;
    private String sourceOffsetKafka;
    private String sourceOffsetStartPos;
    /**
     * 是否执行全量快照:0不执行、1执行
     */
    private Integer sourceDebeziumSnapshot;
    private Integer sourceDebeziumObject;
    /**
     * 数据库处理对象类型：schema对象1、表对象2
     */
    private Integer sourceDataBaseObjectType;

    private Long targetObjectId;
    private Integer targetObjectType;
    private String targetObjectName;
    private Integer targetObjectBeginCount;
    private Integer targetObjectEndCount;
    //ToDo 后续targetDataBase、targetFileFormat、targetMQ使用Object对象进行抽象
    private DataBaseEntity targetDataBase;
    private FileFormatEntity targetFileFormat;
    private MQConfigEntity targetMQConfig;
    private List<TableColumnEntity> targetTableColumns;
    private List<TableColumnEntity> targetTableKeysList;
    private List<ColumnTypeDefineEntity> targetTableColumnTypeDefineList;
    private List<String> targetKeyColumns;
    private String targetInsertSqlColumns;
    private Integer targetInsertMode;
    /**
     * 源数据清理是否校验目标数据:1校验、2不校验，如果为空校验
     */
    private Integer targetCheckFlag;

    private String createDate;
    private Integer state;
    private String stateDate;
    private Integer priority;
    private String systemUserCode;
    private String errorCode;
    private String errorMsg;
    private String taskDisc;
    private Integer checkMode;
    private Long copyTaskId;
    private Integer sendMode;
    private Integer dataRangSplitSize = null;
    /**
     * 数据生产者消费者1:1单通道方式数据分割
     */
    private Map<Integer, Map<String, String>> dataRangSplitMap = null;
    private List<TaskExtendEntity> taskExtendList;
    private Integer dataStreamQueueChannel;
    private Integer sourcePropertiesDataPoolCount;
    private Integer sourcePropertiesThreadCount;
    private Integer sourcePropertiesSelectCount;
    private Integer sourcePropertiesSendMode;
    private Integer targetPropertiesDataPollCount;
    private Integer targetPropertiesThreadCount;
    private Integer targetPropertiesInsertCount;
    private Integer targetPropertiesMoveInfoFlag;
}
