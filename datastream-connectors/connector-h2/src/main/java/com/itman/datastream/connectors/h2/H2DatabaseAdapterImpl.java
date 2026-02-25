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
package com.itman.datastream.connectors.h2;

import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.constant.DataStreamConstant;
import com.itman.datastream.connectors.common.NonOracleSqlBuilder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class H2DatabaseAdapterImpl extends NonOracleSqlBuilder {
    public static final String FUNCTION_DATE_FORMAT = "formatdatetime";

    @Override
    public DataBaseEnum getDataBaseType() {
        return DataBaseEnum.H2;
    }

    @Override
    public Boolean chooseDS(Integer dataSourceType) {
        return dataSourceType.equals(DataStreamConstant.DATA_SOURCE_TYPE_H2);
    }

    @Override
    public String stringToDate(String columnValue) {
        return "parsedatetime('" + columnValue + "','yyyyMMddHHmmss')";
    }

    @Override
    public String dateToString(String columnName) {
        return FUNCTION_DATE_FORMAT + "(" + columnName + ",'yyyyMMddHHmmss') " + columnName;
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
        return FUNCTION_DATE_FORMAT + "(" + columnName + ",'yyyyMMddHHmmss') " + columnName;
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
        return "select " + sequenceName + ".nextval";
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
        return "DATEADD('DAY', -" + days + ", CURRENT_DATE)";
    }

    @Override
    public List<String> makeInitDataStreamMetaDbSql() {
        List<String> sqlList = new ArrayList<>();
        sqlList.add("CREATE TABLE IF NOT EXISTS data_stream_session (\n" +
                "    token_key VARCHAR(256) NOT NULL,\n" +
                "    username VARCHAR(30) NOT NULL,\n" +
                "    create_date TIMESTAMP NOT NULL,\n" +
                "    expire_date TIMESTAMP NOT NULL,\n" +
                "    state INT NOT NULL,\n" +
                "    PRIMARY KEY (token_key)\n" +
                "    );\n" +
                "COMMENT ON TABLE data_stream_session IS '系统session日志';\n" +
                "COMMENT ON COLUMN data_stream_session.token_key IS 'token';\n" +
                "COMMENT ON COLUMN data_stream_session.username IS '用户名';\n" +
                "COMMENT ON COLUMN data_stream_session.create_date IS '生成时间';\n" +
                "COMMENT ON COLUMN data_stream_session.expire_date IS '失效时间';\n" +
                "COMMENT ON COLUMN data_stream_session.state IS '状态：1(生成)、2(删除)';\n" +
                "CREATE INDEX IF NOT EXISTS idx_data_stream_session_id_01\n" +
                "    ON data_stream_session (token_key);");

        sqlList.add("CREATE TABLE IF NOT EXISTS data_stream_data_source (\n" +
                "    data_source_id BIGINT NOT NULL,\n" +
                "    data_source_type INT NOT NULL,\n" +
                "    data_source_name VARCHAR(128) NOT NULL,\n" +
                "    url VARCHAR(128) NOT NULL,\n" +
                "    user_name VARCHAR(128) NOT NULL,\n" +
                "    pass_word VARCHAR(128) NOT NULL,\n" +
                "    table_key_not_supported INT DEFAULT NULL,\n" +
                "    create_date TIMESTAMP NOT NULL,\n" +
                "    state INT NOT NULL,\n" +
                "    state_date TIMESTAMP NOT NULL,\n" +
                "    PRIMARY KEY (data_source_id)\n" +
                "    );\n" +
                "-- 添加表和列注释\n" +
                "COMMENT ON TABLE data_stream_data_source IS '数据源配置表';\n" +
                "COMMENT ON COLUMN data_stream_data_source.data_source_id IS '主键标识，序列名称：SEQ_DATA_SOURCE_ID';\n" +
                "COMMENT ON COLUMN data_stream_data_source.data_source_type IS '数据源类型: 1(teledb)、2(mysql)、3(oracle)、4(postgresql)、5(doris)';\n" +
                "COMMENT ON COLUMN data_stream_data_source.data_source_name IS '数据源名称';\n" +
                "COMMENT ON COLUMN data_stream_data_source.url IS '数据库链接';\n" +
                "COMMENT ON COLUMN data_stream_data_source.user_name IS '数据库用户名';\n" +
                "COMMENT ON COLUMN data_stream_data_source.pass_word IS '数据库密码';\n" +
                "COMMENT ON COLUMN data_stream_data_source.table_key_not_supported IS '数据不支持表主键：1:不支持，其他默认支持';\n" +
                "COMMENT ON COLUMN data_stream_data_source.create_date IS '记录生成时间';\n" +
                "COMMENT ON COLUMN data_stream_data_source.state IS '状态：0(删除)、1(下线)、2(上线)';\n" +
                "COMMENT ON COLUMN data_stream_data_source.state_date IS '状态时间';\n" +
                "-- 创建索引\n" +
                "CREATE INDEX IF NOT EXISTS idx_data_stream_data_source_01 ON data_stream_data_source (data_source_id);");

        return sqlList;
    }

    @Override
    public String makeSqlComment(String tableName, String columnName, String comment) {
        return " comment '" + comment + "'";
    }

    @Override
    public String makeSqlComment( String comment) {
        return " ";
    }
}
