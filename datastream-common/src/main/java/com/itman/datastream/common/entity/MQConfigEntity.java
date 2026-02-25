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
public class MQConfigEntity {
    private Long mqConfigId;

    /**
     * 实例名称
     */
    private String mqConfigName;
    private Integer mqType;

    /**
     * Kafka服务地址（多个地址用逗号分隔）
     * 例如: localhost:9092,localhost:9093
     */
    private String bootstrapServers;

    /**
     * 报文格式
     * 1: JSON格式
     * 2: 分隔符格式
     */
    private Integer messageFormat;

    /**
     * 分隔符（当messageFormat为2时使用）
     */
    private String delimiter;
    /**
     * 备注
     */
    private String remark;

    private Integer state;

    /**
     * 创建时间
     */
    private String createDate;

    /**
     * 更新时间
     */
    private String stateDate;
    /**
     * 发布标志：0下线、1在线
     */
    private Integer onLineFlag;

    private String topicPrefix;
}

