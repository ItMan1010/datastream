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
package com.itman.datastream.common.entity;

import lombok.Data;

/**
 * 字段类型定义实体类
 * 用于定义各种数据库的字段类型及其属性
 */
@Data
public class ColumnTypeDefineEntity {
    /**
     * 类型定义ID
     */
    private Long columnTypeDefineId;

    /**
     * 数据库类型（mysql、postgresql、oracle等）
     */
    private String databaseType;

    /**
     * 字段类型分类：1数值型、2字符串型、3时间类型、4二进制等
     * @deprecated 使用 typeCategory 替代，提供更详细的分类
     */
    @Deprecated
    private Integer columnTypeClassify;

    /**
     * 字段类型名称（tinyint、varchar、date等）
     */
    private String columnTypeName;

    /**
     * 标准字段大小（用于显示）
     */
    private Long columnStandardSize;

    /**
     * 备注说明
     */
    private String remark;

    // ========== 新增字段 (2026-01-08) ==========

    /**
     * 类型分类：更详细的类型分类
     * <p>可能的值：</p>
     * <ul>
     *   <li>NUMERIC_INTEGER - 整数类型</li>
     *   <li>NUMERIC_FIXED_POINT - 定点数类型（精确小数）</li>
     *   <li>NUMERIC_FLOATING_POINT - 浮点数类型（近似值）</li>
     *   <li>STRING_SHORT - 短字符串（有长度限制）</li>
     *   <li>STRING_LONG - 长字符串（无长度限制或很大）</li>
     *   <li>DATETIME_DATE - 日期类型</li>
     *   <li>DATETIME_TIME - 时间类型</li>
     *   <li>DATETIME_TIMESTAMP - 时间戳类型</li>
     *   <li>BINARY - 二进制类型</li>
     *   <li>BOOLEAN - 布尔类型</li>
     *   <li>JSON - JSON类型</li>
     *   <li>ARRAY - 数组类型</li>
     *   <li>UUID - UUID类型</li>
     *   <li>OTHER - 其他类型</li>
     * </ul>
     */
    private String typeCategory;

    /**
     * 最大精度（整数位数）
     * <p>对于整数类型，表示最大位数；对于小数类型，表示总精度（整数位+小数位）</p>
     * <p>例如：MySQL decimal 最大精度为 65，PostgreSQL numeric 最大精度为 1000</p>
     */
    private Integer maxPrecision;

    /**
     * 最大小数位数
     * <p>仅对定点数类型有效，表示小数点后最多位数</p>
     * <p>例如：MySQL decimal 最大小数位为 30</p>
     */
    private Integer maxScale;

    /**
     * 字符串最大长度
     * <p>对于字符串类型，表示最大字符数或字节数</p>
     * <p>例如：MySQL varchar 最大 65535，Oracle VARCHAR2 最大 4000</p>
     */
    private Long characterMaxLength;

    /**
     * 最小值
     * <p>仅对有符号整数类型有效，表示可存储的最小值</p>
     * <p>例如：tinyint 最小值为 -128</p>
     */
    private Long minValue;

    /**
     * 最大值
     * <p>仅对有符号整数类型有效，表示可存储的最大值</p>
     * <p>例如：tinyint 最大值为 127（有符号）或 255（无符号）</p>
     */
    private Long maxValue;

    /**
     * 是否支持字符集（如 nvarchar、nchar）
     * <p>1-支持，0-不支持</p>
     * <p>例如：PostgreSQL 的 varchar 不区分，Oracle 的 NVARCHAR2 支持 Unicode</p>
     */
    private Integer isNationalFlag;

    /**
     * 是否必须指定长度参数
     * <p>1-必须指定，0-可选</p>
     * <p>例如：MySQL varchar 必须指定长度，text 不需要</p>
     */
    private Integer requireLengthParam;

    /**
     * 创建人工号（登录账号）
     */
    private String systemUserCode;

    /**
     * 判断是否为整数类型
     */
    public boolean isNumericInteger() {
        return "NUMERIC_INTEGER".equals(typeCategory);
    }

    /**
     * 判断是否为定点数类型（精确小数）
     */
    public boolean isNumericFixedPoint() {
        return "NUMERIC_FIXED_POINT".equals(typeCategory);
    }

    /**
     * 判断是否为浮点数类型（近似值）
     */
    public boolean isNumericFloatingPoint() {
        return "NUMERIC_FLOATING_POINT".equals(typeCategory);
    }

    /**
     * 判断是否为字符串类型
     */
    public boolean isStringType() {
        return typeCategory != null && typeCategory.startsWith("STRING");
    }

    /**
     * 判断是否为长字符串类型（无长度限制或很大）
     */
    public boolean isLongString() {
        return "STRING_LONG".equals(typeCategory);
    }

    /**
     * 判断是否为日期时间类型
     */
    public boolean isDateTimeType() {
        return typeCategory != null && typeCategory.startsWith("DATETIME");
    }

    /**
     * 判断是否为二进制类型
     */
    public boolean isBinaryType() {
        return "BINARY".equals(typeCategory);
    }

    /**
     * 判断是否支持国家字符集（Unicode）
     */
    public boolean supportsNationalCharset() {
        return isNationalFlag != null && isNationalFlag == 1;
    }

    /**
     * 判断是否必须指定长度参数
     */
    public boolean requiresLengthParam() {
        return requireLengthParam != null && requireLengthParam == 1;
    }
}
