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
package com.itman.datastream.admin.support;

import com.itman.datastream.common.entity.TableColumnEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.itman.datastream.common.constant.DataStreamConstant.SQL_FORMAT_HINT_BALANCE_DATANODE;
import static com.itman.datastream.common.constant.DataStreamConstant.SQL_FORMAT_HINT_DATANODE;
import static com.itman.datastream.common.constant.DataStreamConstant.TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID;

/**
 * 数据迁移 SQL 生成的纯逻辑辅助类（非行为性提取自 DataMoveHandler）。
 * 抽取目的：让 count SQL 拼接与插入行值提取可脱离 DataMoveHandler 的重依赖被独立单测。
 */
public final class DataMoveSqlSupport {

    private DataMoveSqlSupport() {
    }

    public static String makeDataSelectCountSql(String tableName, String tableCondition, String dataNode, Integer dataSet) {
        String tableCountSql = "select count(1) from " + tableName;
        if (dataNode != null) {
            tableCountSql = (dataSet != null)
                    ? (String.format(SQL_FORMAT_HINT_BALANCE_DATANODE, dataSet, dataNode) + tableCountSql)
                    : (String.format(SQL_FORMAT_HINT_DATANODE, dataNode) + tableCountSql);
        }

        if (!StringUtils.isEmpty(tableCondition)) {
            tableCountSql = tableCountSql + " where " + tableCondition;
        }
        return tableCountSql;
    }

    public static List<Object> makeInsertRowObject(Long taskId, List<TableColumnEntity> tableColumns, Map dataRow) {
        List<Object> dataColumnList = new ArrayList<>();
        for (TableColumnEntity iterator : tableColumns) {
            Object columnValue = dataRow.get(iterator.getColumnName());
            if (Objects.isNull(columnValue)) {
                columnValue = dataRow.get(iterator.getColumnName().toLowerCase());
                if (Objects.isNull(columnValue) && TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID.equalsIgnoreCase(iterator.getColumnName())) {
                    columnValue = taskId;
                }
            }

            dataColumnList.add(columnValue);
        }
        return dataColumnList;
    }
}
