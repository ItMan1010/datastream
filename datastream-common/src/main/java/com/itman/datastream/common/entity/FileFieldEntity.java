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
public class FileFieldEntity extends BaseEntity {
    /**
     * 文件字段唯一标识
     */
    private Long fileFieldId;
    /**
     * 文件标识
     */
    private Long fileFormatId;
    /**
     * 字段所属标志：1 文件特殊行、2 文件数据正文
     */
    private Integer belongFlag;
    /**
     * 字段所属值，根据belongFlag取不同值
     */
    private Long belongId;
    /**
     * 可以约定一个名称
     * 如果不填，自动按顺序编排：fieldName01、fieldName02、fieldName03...
     */
    private String fieldName;
    /**
     * 如果数据体定义splitFlag=1，每个字段固定长度
     */
    private Integer fixWidth;
    /**
     * 行记录字段占位，从1开始，1、2、3...
     */
    private Integer position;
    /**
     * 用于特殊行(belongFlag=1)定义：1表示该字段记录总行数
     * 用于特殊行(belongFlag=2)定义：2表示数据行体用于累加字段名称
     */
    private Integer sumLineFlag;
    /**
     * 当belongFlag=1:记录数据行体用于累加字段名称
     * 当belongFlag=2:记录当前字段累加属于特殊行指定字段名称上
     */
    private String sumFieldName;
}
