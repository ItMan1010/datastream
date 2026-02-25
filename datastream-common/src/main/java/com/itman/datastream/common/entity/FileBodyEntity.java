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


@Data
public class FileBodyEntity extends BaseEntity {
    /**
     * 文件体主键ID
     */
    private Long fileBodyId;
    /**
     * 文件唯一标识
     */
    private Long fileFormatId;
    /**
     * 间隔标记符：(1)固定长度、(2)竖线|、(3)逗号，、(4)与符号&
     */
    private Integer splitFlag;
    /**
     * 行记录字段定义
     */
    private List<FileFieldEntity> fileFieldList;
    /**
     * 固定开始行: null不存在
     */
    private Integer fixBeginLine;
    /**
     * 固定结束行: null不存在
     */
    private Integer fixEndLine;

    //-------------界面扩展使用-------------------------------
    private String splitFlagName;
    //-------------界面扩展使用-------------------------------
}
