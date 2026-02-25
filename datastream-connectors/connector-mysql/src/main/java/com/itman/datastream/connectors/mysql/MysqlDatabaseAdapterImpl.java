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
package com.itman.datastream.connectors.mysql;

import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.connectors.common.NonOracleSqlBuilder;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.itman.datastream.common.constant.DataStreamConstant.*;

@Component
public class MysqlDatabaseAdapterImpl extends NonOracleSqlBuilder {
    public static final String FUNCTION_DATE_FORMAT = "date_format";

    @Override
    public DataBaseEnum getDataBaseType() {
        return DataBaseEnum.MYSQL;
    }

    @Override
    public Boolean chooseDS(Integer dataSourceType) {
        return Arrays.asList(DATA_SOURCE_TYPE_MYSQL, DATA_SOURCE_TYPE_SHARDING, DATA_SOURCE_TYPE_DORIS, DATA_SOURCE_TYPE_MEM).contains(dataSourceType);
    }

    @Override
    public String stringToDate(String columnValue) {
        return "str_to_date('" + columnValue + "','%Y%m%d%H%i%s%f')";
    }

    @Override
    public String dateToString(String columnName) {
        return FUNCTION_DATE_FORMAT + "(" + columnName + ",'%Y%m%d%H%i%s%f') " + columnName;
    }

    @Override
    public String timestampToString(String columnName) {
        return dateToString(columnName);
    }

    @Override
    public String timeToString(String columnType, String columnName) {
        return dateToString(columnName);
    }

    @Override
    public String dateToString3(String columnName) {
        return FUNCTION_DATE_FORMAT + "(" + columnName + ",'%Y%m%d%H%i%s') " + columnName;
    }

    @Override
    public String dateToString2(String columnName) {
        return FUNCTION_DATE_FORMAT + "(" + columnName + ",'%Y-%m-%d %H:%i:%s') " + columnName;
    }

    @Override
    public String makeSqlLimit(Integer pageRow, Integer pageCount) {
        if (pageRow == null && pageCount == null) {
            return " ";
        } else {
            return String.format("limit %d,%d", pageRow, pageCount);
        }
    }

    @Override
    public String makeSqlLimit(Integer pageCount) {
        if (pageCount == null) {
            return " ";
        } else {
            return String.format("limit %d", pageCount);
        }
    }

    @Override
    public String makeSqlSystemDate() {
        return "sysdate()";
    }

    @Override
    public String makeSqlSequence(String sequenceName) {
        return "select nextseq('" + sequenceName + "')";
    }

    @Override
    public String makeSqlValidationQuery() {
        return "select 1";
    }

    @Override
    public String makeSqlIfNull() {
        return "ifnull";
    }

    @Override
    public String makeSqlIgnore() {
        return " ";
    }

    @Override
    public String makeSqlMinMax(String columnName, String tableName) {
        return "select " + "max(" + columnName + ") as max_value, min(" + columnName + ") as min_value from " + tableName;
    }

    @Override
    public String getDriverClass() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String makeSqlIntervalDay(Integer days) {
        return "interval " + days + " day";
    }

    @Override
    public List<String> makeInitDataStreamMetaDbSql() {
        List<String> sqlList = new ArrayList<>();
        return sqlList;
    }

    @Override
    public String makeSqlComment(String tableName, String columnName, String comment) {
        return "alter table " + tableName + " modify column " + columnName + " comment '" + comment + "';\n";
    }
    @Override
    public String makeSqlComment(String comment) {
        return " comment '" + comment + "'";
    }
}
