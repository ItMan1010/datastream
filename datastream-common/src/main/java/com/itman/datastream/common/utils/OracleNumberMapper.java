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
package com.itman.datastream.common.utils;

import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.entity.TableColumnEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * Oracle NUMBER 类型智能映射工具
 *
 * 映射规则：
 * 1. 根据精度 p 和小数位 s 动态选择目标类型
 * 2. 整数类型：p≤2→tinyint, p≤4→smallint, p≤9→int, p≤18→bigint, p>18→decimal
 * 3. 小数类型：统一使用 decimal/numeric
 * 4. 无精度信息时使用默认类型 bigint
 */
@Slf4j
public class OracleNumberMapper {

    /** MySQL decimal 最大精度 */
    private static final int MYSQL_DECIMAL_MAX_PRECISION = 65;
    /** PostgreSQL numeric 最大精度 */
    private static final int PG_NUMERIC_MAX_PRECISION = 1000;
    /** 小数位最大值 */
    private static final int MAX_SCALE = 30;

    /**
     * 映射 Oracle NUMBER 到目标数据库类型
     *
     * @param column 源字段信息（columnSize=精度p, decimalDigits=小数位s）
     * @param targetDb 目标数据库类型
     * @return 目标类型字符串（如 "int", "decimal(10,2)"）
     */
    public static String mapNumberType(TableColumnEntity column, DataBaseEnum targetDb) {
        Integer precision = column.getColumnSize();      // Oracle 精度 p
        Integer scale = column.getDecimalDigits();        // Oracle 小数位 s

        // 无精度信息时的默认处理
        if (precision == null) {
            log.warn("Oracle NUMBER 无精度信息，使用默认映射 bigint, column={}", column.getColumnName());
            return "bigint";
        }

        // 有小数位 → decimal/numeric
        if (scale != null && scale > 0) {
            return mapDecimalType(precision, scale, targetDb);
        }

        // 整数类型判断
        return mapIntegerType(precision, targetDb);
    }

    /**
     * 映射整数类型
     */
    private static String mapIntegerType(int precision, DataBaseEnum targetDb) {
        if (targetDb == DataBaseEnum.MYSQL || targetDb == DataBaseEnum.DORIS
                || targetDb == DataBaseEnum.TIDB || targetDb == DataBaseEnum.OCEANBASE
                || targetDb == DataBaseEnum.STARROCKS) {
            // MySQL 兼容数据库有 tinyint
            if (precision <= 2) {
                return "tinyint";
            }
            if (precision <= 4) {
                return "smallint";
            }
            if (precision <= 9) {
                return "int";
            }
            if (precision <= 18) {
                return "bigint";
            }
            return "decimal(" + precision + ",0)";  // 超大整数
        } else if (targetDb == DataBaseEnum.POSTGRESQL || targetDb == DataBaseEnum.GAUSSDB) {
            // PostgreSQL 兼容数据库没有 tinyint，最小是 smallint
            if (precision <= 4) {
                return "smallint";
            }
            if (precision <= 9) {
                return "integer";
            }
            if (precision <= 18) {
                return "bigint";
            }
            return "numeric(" + precision + ",0)";  // 超大整数
        } else if (targetDb == DataBaseEnum.ORACLE || targetDb == DataBaseEnum.DAMENG) {
            // Oracle 兼容数据库继续使用 NUMBER
            if (precision <= 38) {
                return "number(" + precision + ",0)";
            }
            return "number(38,0)";
        } else {
            // 其他数据库默认使用 bigint
            if (precision <= 18) {
                return "bigint";
            }
            return "decimal(" + precision + ",0)";
        }
    }

    /**
     * 映射小数类型
     */
    private static String mapDecimalType(int precision, int scale, DataBaseEnum targetDb) {
        int maxPrecision;
        String typePrefix;

        if (targetDb == DataBaseEnum.MYSQL || targetDb == DataBaseEnum.DORIS
                || targetDb == DataBaseEnum.TIDB || targetDb == DataBaseEnum.OCEANBASE
                || targetDb == DataBaseEnum.STARROCKS) {
            maxPrecision = MYSQL_DECIMAL_MAX_PRECISION;
            typePrefix = "decimal";
        } else if (targetDb == DataBaseEnum.POSTGRESQL || targetDb == DataBaseEnum.GAUSSDB) {
            maxPrecision = PG_NUMERIC_MAX_PRECISION;
            typePrefix = "numeric";
        } else if (targetDb == DataBaseEnum.ORACLE || targetDb == DataBaseEnum.DAMENG) {
            maxPrecision = 38;
            typePrefix = "number";
        } else {
            // 默认使用 MySQL 标准
            maxPrecision = MYSQL_DECIMAL_MAX_PRECISION;
            typePrefix = "decimal";
        }

        int adjustedPrecision = Math.min(precision, maxPrecision);
        int adjustedScale = Math.min(scale, MAX_SCALE);

        if (adjustedPrecision < precision) {
            log.warn("Oracle NUMBER 精度 {} 超过目标数据库最大值 {}，截断处理",
                    precision, maxPrecision);
        }

        return typePrefix + "(" + adjustedPrecision + "," + adjustedScale + ")";
    }

    /**
     * 判断是否需要特殊处理 Oracle NUMBER
     */
    public static boolean isOracleNumber(DataBaseEnum sourceDb, String columnTypeName) {
        return (DataBaseEnum.ORACLE.equals(sourceDb) || DataBaseEnum.DAMENG.equals(sourceDb))
                && "number".equalsIgnoreCase(columnTypeName);
    }
}
