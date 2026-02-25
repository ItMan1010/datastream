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
package com.itman.datastream.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;


@Getter
@AllArgsConstructor
public enum DataBaseEnum {
    /**
     * MySQL数据库
     */
    MYSQL(2, "mysql",
            "jdbc:(?<type>[a-z]+)://(?<host>[a-zA-Z0-9-//.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)(\\?.*)?"),
    /**
     * Oracle数据库
     */
    ORACLE(3, "oracle",
            "jdbc:(?<type>[a-z]+):thin:@//(?<host>[a-zA-Z0-9-//.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)?"),
    /**
     * PostgreSQL数据库
     */
    POSTGRESQL(4, "postgresql",
            "jdbc:(?<type>[a-z]+)://(?<host>[a-zA-Z0-9-//.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)(\\?.*)?"),

    /**
     * H2数据库
     */
    H2(7, "h2", "jdbc:(?<type>[a-z0-9]+):.*:.*"),

    /**
     * Doris数据库
     */
    DORIS(5, "doris",
            "jdbc:(?<type>[a-z]+)://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)(\\?.*)?"),
    /**
     * SQL Server数据库
     */
    SQLSERVER(14, "sqlserver",
            "jdbc:sqlserver://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+);databaseName=(?<database>[a-zA-Z0-9_]+)"),
    /**
     * 达梦数据库
     */
    DAMENG(15, "dameng",
            "jdbc:dm://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)"),
    /**
     * 人大金仓数据库
     */
    KINGBASE(16, "kingbase",
            "jdbc:kingbase8://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)"),
    /**
     * TiDB数据库
     */
    TIDB(17, "tidb",
            "jdbc:(?<type>[a-z]+)://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)(\\?.*)?"),
    /**
     * OceanBase数据库
     */
    OCEANBASE(18, "oceanbase",
            "jdbc:(?<type>[a-z]+)://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)(\\?.*)?"),
    /**
     * GaussDB数据库
     */
    GAUSSDB(19, "gaussdb",
            "jdbc:postgresql://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)(\\?.*)?"),
    /**
     * ClickHouse数据库
     */
    CLICKHOUSE(20, "clickhouse",
            "jdbc:clickhouse://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)"),
    /**
     * StarRocks数据库
     */
    STARROCKS(21, "starrocks",
            "jdbc:mysql://(?<host>[a-zA-Z0-9-.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)(\\?.*)?");

    private int id;
    private String name;
    private String urlPattern;

    public static DataBaseEnum of(String name) {
        if (!StringUtils.isEmpty(name)) {
            for (DataBaseEnum type : DataBaseEnum.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return type;
                }
            }
        }

        throw new IllegalArgumentException("cannot find enum name: " + name);
    }

    public static DataBaseEnum of(Integer id) {
        if (id != null) {
            for (DataBaseEnum type : DataBaseEnum.values()) {
                if (type.getId() == id) {
                    return type;
                }
            }
        }

        throw new IllegalArgumentException("cannot find enum id: " + id);
    }
}
