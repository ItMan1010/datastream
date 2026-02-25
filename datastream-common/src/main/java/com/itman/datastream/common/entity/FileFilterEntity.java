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
public class FileFilterEntity extends BaseEntity {
    /**
     * 文件过滤标识
     */
    private Long fileFilterId;
    /**
     * 文件定义标识
     */
    private Long fileFormatId;
    /**
     * 文件字段标识
     */
    private Long fileFieldId;
    /**
     * 符号标识：1(等于=)、2(大于号>)、3(小于号<)、4(大于等于号>=)、5(小于等于号<=)
     */
    private Integer symbolId;
    /**
     * 同一组条件是与关系，不同组条件是或关系
     */
    private Integer symbolGroup;
    /**
     * 字段值
     */
    private String fileFieldValue;
}
