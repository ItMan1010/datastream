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
public class FileTaskEntity extends BaseEntity {
    /**
     * 任务标识ID
     */
    private Long taskId;
    /**
     * 任务类型标识
     */
    private Integer taskType;
    /**
     * A对象类型
     */
    private Integer aObjectType;
    /**
     * A对象标识ID
     */
    private Long aObjectId;
    /**
     * A对象
     */
    private Object aObject;
    /**
     * B对象类型
     */
    private Integer bObjectType;
    /**
     * B对象标识ID
     */
    private Long bObjectId;
    /**
     * B对象
     */
    private Object bObject;
    /**
     * 备注
     */
    private String remark;
    /**
     * 字段关联明细
     */
    private List<FileTaskFieldRelaEntity> fileTaskFieldRelaList;
    /**
     * 顺序处理标识,1数据顺序处理
     */
    private Integer orderFlag;
    /**
     * 服务修复调用url地址
     */
    private String repairUrl;
    /**
     * 发布标志：0下线、1在线
     */
    private Integer onLineFlag;
    /**
     * 需要获取汇总字段关联标识
     */
    private Map<String, Long> sumFieldRelaIdMap;


    //-------------界面扩展使用-------------------------------
    private Map<Long, String> aFieldNameMap;
    private Map<Long, String> bFieldNameMap;
    private String onLineFlagName;
    /**
     * B对象字段名称
     * 用户界面选择显示
     */
    private List<String> bFieldNameList;
    /**
     * A对象字段名称
     * 用户界面选择显示
     */
    private List<String> aFieldNameList;
    /**
     * A对象类型名称
     */
    private String aObjectTypeName;
    /**
     * A对象名称
     */
    private String aObjectName;
    /**
     * B对象类型名称
     */
    private String bObjectTypeName;
    /**
     * B对象类型名称
     */
    private String bObjectName;
    /**
     * 任务类型名称
     */
    private String taskTypeName;
    /**
     * 任务对象配置页面操作标识：1新增、2删除
     */
    private Integer taskFieldMappedAction;
    /**
     * 任务对象配置操作行体字段索引值
     */
    private Integer taskFieldMappedIndex;

    /**
     * 任务配置操作：0无、1保存、2匹配
     */
    private Integer taskAction;
    //-------------界面扩展使用-------------------------------
}
