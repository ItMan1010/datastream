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
package com.itman.datastream.connectors.dameng;

import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.constant.DataStreamConstant;
import com.itman.datastream.connectors.common.NonOracleSqlBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 达梦（DM8）数据库方言适配器。
 * <p>达梦 SQL 方言高度兼容 Oracle（to_char/to_date/sysdate/dual/rownum/nvl/comment on column），
 * 同时支持 MySQL 风格 limit m,n 分页与多行 values 插入，因此继承 {@link NonOracleSqlBuilder}，
 * 仅覆写日期/空值函数为 Oracle 兼容写法，分页保持 limit 语法。</p>
 */
@Component
public class DamengDatabaseAdapterImpl extends NonOracleSqlBuilder {

    @Override
    public DataBaseEnum getDataBaseType() {
        return DataBaseEnum.DAMENG;
    }

    @Override
    public Boolean chooseDS(Integer dataSourceType) {
        return dataSourceType.equals(DataStreamConstant.DATA_SOURCE_TYPE_DAMENG);
    }

    @Override
    public String stringToDate(String columnValue) {
        return "to_date('" + columnValue + "','yyyymmddhh24miss')";
    }

    @Override
    public String dateToString(String columnName) {
        return "to_char(" + columnName + ",'yyyymmddhh24miss') " + columnName;
    }

    @Override
    public String timestampToString(String columnName) {
        return "to_char(" + columnName + ",'yyyymmddhh24missff') " + columnName;
    }

    @Override
    public String timeToString(String columnType, String columnName) {
        if (columnType.equalsIgnoreCase("date")) {
            return dateToString(columnName);
        } else {
            return timestampToString(columnName);
        }
    }

    @Override
    public String dateToString2(String columnName) {
        return "to_char(" + columnName + ",'yyyy-mm-dd hh24:mi:ss') " + columnName;
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
        return "sysdate";
    }

    @Override
    public String makeSqlSequence(String sequenceName) {
        return "select " + sequenceName + ".nextval from dual";
    }

    @Override
    public String makeSqlValidationQuery() {
        return "select 1 from dual";
    }

    @Override
    public String makeSqlIfNull() {
        return "nvl";
    }

    @Override
    public String makeSqlIgnore() {
        return " ";
    }

    @Override
    public String makeSqlMinMax(String columnName, String tableName) {
        return "select " + "max(" + columnName + ") as \"max_value\", min(" + columnName + ") as \"min_value\" from " + tableName;
    }

    @Override
    public String getDriverClass() {
        return "dm.jdbc.driver.DmDriver";
    }

    @Override
    public String makeSqlIntervalDay(Integer days) {
        return " " + days + " ";
    }

    @Override
    public List<String> makeInitDataStreamMetaDbSql() {
        return new ArrayList<>();
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