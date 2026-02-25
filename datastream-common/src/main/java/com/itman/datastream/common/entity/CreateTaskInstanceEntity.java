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

@Data
public class CreateTaskInstanceEntity {
    private Integer taskType;
    private String sourceObjectName;
    private Long sourceObjectId;
    private Integer sourceObjectType;
    private String sourceTableCondition;
    private Integer sourceOffsetStorage;
    private String sourceOffsetKafka;
    private String sourceOffsetStartPos;
    private Integer sourceDebeziumObject;
    /**
     * 是否执行全量快照:0不执行、1执行
     */
    private Integer sourceDebeziumSnapshot;

    /**
     * 数据库处理对象类型：表对象1、schema对象2
     */
    private Integer sourceDataBaseObjectType;

    private Integer dataNodeFlag;
    private Integer dataSet;
    private Integer tableType;
    private Integer priority;
    private String targetObjectName;
    private Long targetObjectId;
    private Integer targetObjectType;
    private String systemUserCode;
    private String taskDisc;
    private Integer targetInsertMode;
    /**
     * 源数据清理是否校验目标数据:1校验、2不校验，如果为空校验
     */
    private Integer targetCheckFlag;
    private Integer checkMode;
}
