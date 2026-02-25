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
package com.itman.datastream.common.api;

import com.itman.datastream.common.entity.TableColumnEntity;

import java.util.List;
import java.util.Map;

public interface IDatabaseAdapter {

    Boolean chooseDS(Integer dataSourceType);

    String stringToDate(String columnValue);

    String dateToString(String columnName);

    String timestampToString(String columnName);

    String timeToString(String columnType, String columnName);

    String dateToString2(String columnName);

    String dateToString3(String columnName);

    String makeSqlLimit(Integer pageRow, Integer pageCount);

    String makeSqlLimit(Integer pageCount);

    String makeSqlSystemDate();

    String makeSqlSequence(String sequenceName);

    String makeSqlValidationQuery();

    String makeSqlIfNull();

    String makeSqlMinMax(String columnName, String tableName);

    String makeSqlIgnore();

    String getDriverClass();

    String makeSqlIntervalDay(Integer days);

    List<String> makeInitDataStreamMetaDbSql();

    List<String> makeSqlKeyColumnSelect(List<String> targetKeyColumns, List<TableColumnEntity> tableColumns, Map dataRecord);

    String makeSqlBatchInsert(String insertSqlColumns, Long taskId, List<TableColumnEntity> tableColumns, List<Map> dataRecordList, Integer dataStreamParallelStreamSize);

    String makeSqlSelectCountByKey(final Map dataRecord, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final String tableName);

    String makeSqlDeleteRow(final String tableName, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final Map dataRecord);

    String makeSqlSelectColumns(String tableName, String tableCondition, List<TableColumnEntity> tableColumns, Boolean isOrderBy);

    String makeSqlSelectByPage(final Integer loadStrategy, String selectColumns, String selectCondition, String selectBeginKeyValue, String tableMinKeyValue, String tableKeyColumns, Integer selectCount);

    String makeSqlCurrentPageMaxKeyValue(String currentKeysValue, String tableKeyColumns, String tableName, String selectCondition, String tableMinKeyValue, Integer pageRowSize);

    String makeSqlComment(String tableName, String columnName, String comment);
    String makeSqlComment(String comment);
    String makeSqlKeyColumn(String tableName, String pkName, String pkColumns);
    String makeSqlIndexColumn(String tableName, String indexName, String indexColumns);

    /**
     * 生成外键约束 SQL
     *
     * @param tableName 表名
     * @param fkName 外键名称
     * @param fkColumns 外键列（多个列用逗号分隔）
     * @param pkTableName 主键表名
     * @param pkColumns 主键列（多个列用逗号分隔）
     * @param onDelete 删除规则（CASCADE、SET NULL、RESTRICT、NO ACTION）
     * @param onUpdate 更新规则（CASCADE、SET NULL、RESTRICT、NO ACTION）
     * @return 外键 SQL 语句
     */
    String makeSqlForeignKey(String tableName, String fkName, String fkColumns,
                             String pkTableName, String pkColumns,
                             String onDelete, String onUpdate);

    String makeSqlUpdateRow(final String tableName, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final Map dataRecordBefore, final Map dataRecordAfter);

}
