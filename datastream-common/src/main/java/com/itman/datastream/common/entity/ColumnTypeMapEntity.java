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
 * 字段类型映射实体类
 * 用于定义不同数据库之间的字段类型映射关系
 */
@Data
public class ColumnTypeMapEntity {
    /**
     * 映射ID
     */
    private Long columnTypeMapId;

    /**
     * 源字段类型定义ID
     */
    private Long columnTypeDefineIdA;

    /**
     * 源数据库类型
     */
    private String databaseTypeA;

    /**
     * 源字段类型名称
     */
    private String columnTypeNameA;

    /**
     * 目标字段类型定义ID
     */
    private Long columnTypeDefineIdB;

    /**
     * 目标数据库类型
     */
    private String databaseTypeB;

    /**
     * 目标字段类型名称
     */
    private String columnTypeNameB;

    // ========== 新增字段 (2026-01-08) ==========

    /**
     * 匹配级别
     * <p>表示类型映射的匹配精确程度：</p>
     * <ul>
     *   <li>1 - 精确匹配：类型完全等价，可无损转换（如 int -> int4）</li>
     *   <li>2 - 兼容匹配：类型兼容，但可能有精度损失或范围差异（如 float -> float4）</li>
     *   <li>3 - 降级匹配：需要特殊处理或应用程序介入（如 INTERVAL -> varchar）</li>
     * </ul>
     */
    private Integer matchLevel;

    /**
     * 精度转换规则表达式
     * <p>用于定义精度参数的转换规则，支持表达式：</p>
     * <ul>
     *   <li>null - 无需转换</li>
     *   <li>min(p, 65) - 取源精度与目标最大精度的较小值</li>
     *   <li>ORACLE_NUMBER_MAPPER - 由 OracleNumberMapper 动态处理</li>
     * </ul>
     */
    private String precisionConversionRule;

    /**
     * 长度转换规则表达式
     * <p>用于定义长度参数的转换规则，支持表达式：</p>
     * <ul>
     *   <li>null - 无需转换</li>
     *   <li>min(len, 65535) - 取源长度与目标最大长度的较小值</li>
     *   <li>min(len, 4000) - Oracle VARCHAR2 长度限制</li>
     * </ul>
     */
    private String lengthConversionRule;

    /**
     * 转换警告信息
     * <p>当类型转换可能导致数据损失或其他问题时，提供警告信息</p>
     * <p>例如："精度可能损失"、"范围不同"等</p>
     */
    private String conversionWarning;

    /**
     * 是否可逆转换（双向无损）
     * <p>1-可逆（双向无损），0-不可逆（有损失）</p>
     * <p>例如：int -> int4 是可逆的，但 float -> float4 可能有精度损失</p>
     */
    private Integer isReversible;

    /**
     * 判断是否为精确匹配
     */
    public boolean isExactMatch() {
        return matchLevel != null && matchLevel == 1;
    }

    /**
     * 判断是否为兼容匹配
     */
    public boolean isCompatibleMatch() {
        return matchLevel != null && matchLevel == 2;
    }

    /**
     * 判断是否为降级匹配
     */
    public boolean isDowngradeMatch() {
        return matchLevel != null && matchLevel == 3;
    }

    /**
     * 判断是否可逆转换
     */
    public boolean isReversible() {
        return isReversible != null && isReversible == 1;
    }

    /**
     * 判断是否有转换警告
     */
    public boolean hasWarning() {
        return conversionWarning != null && !conversionWarning.isEmpty();
    }

    /**
     * 判断是否需要特殊处理（如 Oracle NUMBER）
     */
    public boolean requiresSpecialHandling() {
        return "ORACLE_NUMBER_MAPPER".equals(precisionConversionRule);
    }
}
