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
import java.util.List;
import java.util.Map;

@Data
public class TableInfoEntity {
    private String tableName;        // 表名
    private String tableType;        // 表类型 (TABLE, VIEW)
    private String tableComment;     // 表注释
    private String schemaName;       // 模式名
    private Long rowCount;          // 行数
    private String tableSize;        // 表大小
    private Date createTime;         // 创建时间
    private Date updateTime;         // 更新时间
    private List<TableColumnEntity> columns; // 列信息
    private Map<String, List<String>> keyColumnMap;
    private Map<String, List<String>> indexColumnMap;
    private Map<String, ForeignKeyInfo> foreignKeyMap; // 外键信息
}
