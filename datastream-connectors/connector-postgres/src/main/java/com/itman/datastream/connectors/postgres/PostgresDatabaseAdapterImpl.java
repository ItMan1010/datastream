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
package com.itman.datastream.connectors.postgres;

import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.constant.DataStreamConstant;
import com.itman.datastream.connectors.common.NonOracleSqlBuilder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgresDatabaseAdapterImpl extends NonOracleSqlBuilder {
    @Override
    public DataBaseEnum getDataBaseType() {
        return DataBaseEnum.POSTGRESQL;
    }

    @Override
    public Boolean chooseDS(Integer dataSourceType) {
        return dataSourceType.equals(DataStreamConstant.DATA_SOURCE_TYPE_PG);
    }

    @Override
    public String stringToDate(String columnValue) {
        String format = columnValue.length() <= 17 ? "'yyyymmddhh24missms'" : "'yyyymmddhh24missus'";
        return "to_timestamp('" + columnValue + "'," + format + ")";
    }

    @Override
    public String dateToString(String columnName) {
        return "to_char(" + columnName + ",'yyyymmddhh24missms') " + columnName;
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
    public String dateToString2(String columnName) {
        return "to_char(" + columnName + ",'yyyy-mm-dd hh24:mi:ss:ms') " + columnName;
    }

    @Override
    public String dateToString3(String columnName) {
        return "to_char(" + columnName + ",'yyyymmddhh24miss') " + columnName;
    }

    @Override
    public String makeSqlLimit(Integer pageRow, Integer pageCount) {
        if (pageRow == null && pageCount == null) {
            return " ";
        } else {
            return String.format("limit %d offset %d", pageCount, pageRow);
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
        return "now()";
    }

    @Override
    public String makeSqlSequence(String sequenceName) {
        return "select nextval('" + sequenceName + "')";
    }

    @Override
    public String makeSqlValidationQuery() {
        return "select 1";
    }

    @Override
    public String makeSqlIfNull() {
        return "coalesce";
    }

    @Override
    public String makeSqlIgnore() {
        return " ";
    }

    @Override
    public String makeSqlMinMax(String columnName, String tableName) {
        return "select " + "cast(max(" + columnName + ") as bigint) as \"ma_value\", cast(min(" + columnName + ") as bigint) as \"min_value\" from " + tableName;
    }

    @Override
    public String getDriverClass() {
        return "org.postgresql.Driver";
    }

    @Override
    public String makeSqlIntervalDay(Integer days) {
        return "interval '" + days + " day'";
    }

    @Override
    public List<String> makeInitDataStreamMetaDbSql() {
        List<String> sqlList = new ArrayList<>();
        return sqlList;
    }

    @Override
    public String makeSqlComment(String tableName, String columnName, String comment) {
        return "comment on column " + tableName + "." + columnName + " is '" + comment + "';\n";
    }

    @Override
    public String makeSqlComment(String comment) {
        return " ";
    }
}
