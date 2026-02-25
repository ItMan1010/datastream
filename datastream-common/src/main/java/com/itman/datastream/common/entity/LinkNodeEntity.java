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
public class LinkNodeEntity {
    /**
     * 节点标识
     */
    private Long linkNodeId;
    /**
     * 链接标识
     */
    private Long tableLinkId;
    /**
     * 数据源标识
     */
    private Long dataSourceId;
    /**
     * 节点表名
     */
    private String tableName;
    /**
     * 节点字段名
     */
    private String fieldName;
    /**
     * 父节点标识
     */
    private Long parentLinkNodeId;
    /**
     * 父节点链接字段
     */
    private String parentFieldName;

    private Integer posX;
    private Integer posY;
    private List<LinkNodeEntity> linkNodeList;
}
