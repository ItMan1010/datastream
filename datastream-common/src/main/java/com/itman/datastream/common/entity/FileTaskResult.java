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

import java.util.Date;


@Data
public class FileTaskResult extends BaseEntity {
    /**
     * 任务运行明细主键标识ID
     */
    private Long taskResultId;
    /**
     * 任务实例标识ID
     */
    private Long taskInstanceId;
    /**
     * 任务运行信息
     */
    private String taskResult;
    /**
     * 比较结果标志 1:A比B多;2:A比B少；3:AB指定字段值不一致
     */
    private Integer compareFlag;
    /**
     * 比较结果信息
     */
    private String compareInfo;
    /**
     * 比较对象数据信息
     */
    private String compareData;
    /**
     * 平帐状态:0待平帐、1平帐成功、2平帐失败
     */
    private Integer repairState;
    /**
     * 平帐处理信息
     */
    private String repairInfo;
    /**
     * 平帐时间
     */
    private Date repairStateDate;


    //-------------界面扩展使用-------------------------------
    private String compareFlagName;
    private String repairStateName;
    //-------------界面扩展使用-------------------------------
}
