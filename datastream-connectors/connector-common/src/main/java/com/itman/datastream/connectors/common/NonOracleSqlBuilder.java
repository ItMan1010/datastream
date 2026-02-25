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
package com.itman.datastream.connectors.common;

import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.entity.TableColumnEntity;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.LOAD_STRATEGY_BY_DATA_PART;

public abstract class NonOracleSqlBuilder extends AbstractSqlBuilder {
    /**
     * 获取数据库类型，由子类实现
     */
    public abstract DataBaseEnum getDataBaseType();

    abstract public String makeSqlLimit(Integer pageRow, Integer pageCount);

    abstract public String makeSqlLimit(Integer pageCount);

    @Override
    public String makeSqlCurrentPageMaxKeyValue(String currentKeysValue, String tableKeyColumns, String tableName, String selectCondition, String tableMinKeyValue, Integer pageRowSize) {
        StringBuffer makeSql = new StringBuffer();
        makeSql.append(" select ");
        makeSql.append(tableKeyColumns);
        makeSql.append(" from ");
        makeSql.append(tableName.toLowerCase());

        if (!StringUtils.isEmpty(selectCondition) || currentKeysValue != null) {
            makeSql.append(" where ");
        }

        if (!StringUtils.isEmpty(selectCondition)) {
            makeSql.append(selectCondition);
        }

        if (!StringUtils.isEmpty(selectCondition) && currentKeysValue != null) {
            makeSql.append(" and ");
        }

        if (currentKeysValue != null) {
            makeSql.append(" ( ");
            makeSql.append(tableKeyColumns);
            makeSql.append(" ) > (");
            makeSql.append(currentKeysValue);
            makeSql.append(") ");
        }
        makeSql.append(" order by ");
        makeSql.append(tableKeyColumns);
        makeSql.append(" ");

        String selectLimit = makeSqlLimit((pageRowSize - 1), 1);
        makeSql.append(selectLimit);

        return makeSql.toString();
    }

    @Override
    public String makeSqlSelectByPage(Integer loadStrategy, String selectColumns, String selectCondition, String selectBeginKeyValue, String tableMinKeyValue, String tableKeyColumns, Integer selectCount) {
        String selectLimit = loadStrategy.equals(LOAD_STRATEGY_BY_DATA_PART) ? makeSqlLimit(selectCount) : makeSqlLimit(Integer.parseInt(selectBeginKeyValue), selectCount);

        String selectSqlConditionTemp = " ";
        if (loadStrategy.equals(LOAD_STRATEGY_BY_DATA_PART)) {
            selectSqlConditionTemp = " where " + (StringUtils.isEmpty(selectCondition) ? " " : (selectCondition));
            if (!StringUtils.isEmpty(selectCondition) && selectBeginKeyValue != null) {
                selectSqlConditionTemp = selectSqlConditionTemp + " and ";
            }
            if (selectBeginKeyValue != null) {
                String sqlTemp = selectBeginKeyValue.equals(tableMinKeyValue) ? "=" : "";
                selectSqlConditionTemp = selectSqlConditionTemp + "(" + tableKeyColumns + ") >" + sqlTemp + " (" + selectBeginKeyValue + ") order by " + tableKeyColumns;
            } else {
                selectSqlConditionTemp = selectSqlConditionTemp + " order by " + tableKeyColumns;
            }
        }

        return selectColumns + selectSqlConditionTemp + " " + selectLimit;
    }

    public String makeSqlBatchInsert(String insertSqlColumns, Long taskId, List<TableColumnEntity> tableColumns, List<Map> dataRecordList, Integer dataStreamParallelStreamSize) {
        List<String> dataRowList = (dataRecordList.size() > dataStreamParallelStreamSize) ?
                dataRecordList.parallelStream().map(x -> makeSqlInsertRow(taskId, tableColumns, x)).collect(Collectors.toList()) :
                dataRecordList.stream().map(x -> makeSqlInsertRow(taskId, tableColumns, x)).collect(Collectors.toList());

        StringBuffer insertSql = new StringBuffer();
        insertSql.append(insertSqlColumns);
        insertSql.append(dataRowList.stream().collect(Collectors.joining(",")));
        return insertSql.toString();
    }

}
