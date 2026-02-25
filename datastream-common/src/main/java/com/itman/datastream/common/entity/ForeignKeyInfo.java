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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外键信息实体类
 * 用于存储表的外键约束信息
 */
@Data
public class ForeignKeyInfo {
    /**
     * 外键名称
     */
    private String fkName;

    /**
     * 主键表名
     */
    private String pkTableName;

    /**
     * 列映射关系：key=主键列名, value=外键列名
     */
    private Map<String, String> columnMapping = new HashMap<>();

    /**
     * 更新规则（CASCADE、RESTRICT、SET NULL、NO ACTION、SET DEFAULT）
     */
    private String updateRule;

    /**
     * 删除规则（CASCADE、RESTRICT、SET NULL、NO ACTION、SET DEFAULT）
     */
    private String deleteRule;

    /**
     * 添加列映射关系
     *
     * @param pkColumn  主键列名
     * @param fkColumn  外键列名
     */
    public void addColumnMapping(String pkColumn, String fkColumn) {
        this.columnMapping.put(pkColumn, fkColumn);
    }

    /**
     * 获取主键列列表
     */
    public List<String> getPkColumns() {
        return new ArrayList<>(this.columnMapping.keySet());
    }

    /**
     * 获取外键列列表
     */
    public List<String> getFkColumns() {
        return new ArrayList<>(this.columnMapping.values());
    }
}
