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
package com.itman.datastream.connectors.oracle;

import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.connectors.common.AbstractSqlBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.LOAD_STRATEGY_BY_DATA_PART;

@Component
public abstract class OracleSqlBuilder extends AbstractSqlBuilder {
    /**
     * 获取数据库类型，由子类实现
     */
    public abstract DataBaseEnum getDataBaseType();

    @Override
    public String makeSqlCurrentPageMaxKeyValue(String currentKeysValue, String tableKeyColumns, String tableName, String selectCondition, String tableMinKeyValue, Integer pageRowSize) {
        StringBuffer sql = new StringBuffer();
        sql.append(" select ");
        sql.append(tableKeyColumns);
        sql.append(" from ( select ");
        sql.append(tableKeyColumns);
        sql.append(" , row_number() over (order by ");
        sql.append(tableKeyColumns);
        sql.append(" ) as rn from ");
        sql.append(tableName.toLowerCase());
        sql.append(" where ");
        if (!StringUtils.isEmpty(selectCondition)) {
            sql.append(selectCondition);
            sql.append(" and (");
        }
        sql.append(tableKeyColumns);
        if (currentKeysValue != null) {
            sql.append(") > (");
//            if (currentKeysValue.equals(tableMinKeyValue)) {
//                sql.append(") >= (");
//            } else {
//                sql.append(") > (");
//            }
            sql.append(currentKeysValue);
        }
        sql.append(") ) where rn =");
        sql.append(pageRowSize);
        return sql.toString();
    }

    @Override
    public String makeSqlSelectByPage(Integer loadStrategy, String selectColumns, String selectCondition, String selectBeginKeyValue, String tableMinKeyValue, String tableKeyColumns, Integer selectCount) {
        String selectSqlConditionTemp = " ";
        if (loadStrategy.equals(LOAD_STRATEGY_BY_DATA_PART)) {
            selectSqlConditionTemp = StringUtils.isEmpty(selectCondition) ? " where " : " and ";
            if (selectBeginKeyValue != null) {
                String sqlTemp = selectBeginKeyValue.equals(tableMinKeyValue) ? "=" : "";
                selectSqlConditionTemp = selectSqlConditionTemp + "(" + tableKeyColumns + ") >" + sqlTemp + " " + selectBeginKeyValue + " order by " + tableKeyColumns;
            } else {
                selectSqlConditionTemp = selectSqlConditionTemp + " order by " + tableKeyColumns;
            }
        }
        return String.format("select row_.*, rownum rn from ( %s %s )row_ where rownum <= %d", selectColumns, selectSqlConditionTemp, selectCount);
    }

    public String makeSqlBatchInsert(String insertSqlColumns, Long taskId, List<TableColumnEntity> tableColumns, List<Map> dataRecordList, Integer dataStreamParallelStreamSize) {
        // 使用正则表达式替换 "insert" 后跟任意数量空格再跟 "into" 为 "into"
        // 这样可以正确处理 "insert into" 和 "insert  into" (两个空格) 的情况
        String insertSqlColumnsTemp = insertSqlColumns.replaceFirst("insert\\s+into", "into");
        List<String> dataRowList = (dataRecordList.size() > dataStreamParallelStreamSize) ? dataRecordList.parallelStream().map(x -> (insertSqlColumnsTemp + makeSqlInsertRow(taskId, tableColumns, x))).collect(Collectors.toList()) :
                dataRecordList.stream().map(x -> (insertSqlColumnsTemp + makeSqlInsertRow(taskId, tableColumns, x))).collect(Collectors.toList());
        StringBuffer insertSql = new StringBuffer();
        insertSql.append("insert all ");
        insertSql.append(dataRowList.stream().collect(Collectors.joining("")));
        insertSql.append("select 1 from dual");
        return insertSql.toString();
    }
}