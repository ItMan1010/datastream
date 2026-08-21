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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 库内置字段类型定义与映射标识集合。
 *
 * <p>来源说明：本集合与 {@code doc/sql/datastream-column-type-map-v2.sql}
 * 初始化的全部 INSERT 语句保持一致，作为「库内置数据；只允许修改、不允许删除」的判定依据。</p>
 *
 * <p>注意：当初始化脚本新增内置类型定义或映射时，必须同步维护下方两个集合，否则删除保护会失效。</p>
 */
public final class ColumnTypeBuiltInConstant {

    private ColumnTypeBuiltInConstant() {
    }

    /**
     * 脚本预置的「字段类型定义」主键集合（对应 data_stream_column_type_define.column_type_define_id）。
     * 包括 MySQL、PostgreSQL、Oracle 三套内置类型定义。
     */
    public static final Set<Long> BUILTIN_TYPE_DEFINE_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // ---- MySQL ----
            // 整数 + 位
            1L, 2L, 3L, 4L, 5L, 100L,
            // 无符号整数
            111L, 112L, 113L, 114L, 115L,
            // 小数/浮点
            6L, 7L, 8L,
            // 日期时间
            9L, 10L, 11L, 12L, 13L, 101L,
            // 字符串及文本
            14L, 15L, 16L, 17L, 18L, 19L, 20L, 102L, 103L,
            // 二进制
            104L, 105L, 106L, 107L, 108L, 109L,
            // 空间类型
            110L,
            // ---- PostgreSQL ----
            // 整数
            21L, 22L, 23L, 24L, 25L,
            // 小数/浮点
            26L, 27L, 28L, 29L, 30L,
            // 日期时间
            31L, 32L, 33L, 34L, 35L, 36L,
            // 字符串及文本 / uuid
            37L, 38L, 39L, 40L, 41L, 42L,
            // 二进制 / 布尔
            200L, 201L, 202L,
            // ---- Oracle ----
            // 数值
            60L, 61L, 62L, 79L,
            // 日期时间
            63L, 64L, 75L, 76L,
            // 字符串
            65L, 66L, 67L, 68L, 69L, 70L,
            // 二进制
            71L, 72L, 73L, 74L,
            // 其他
            77L, 78L
    )));

    /**
     * 脚本预置的「类型映射」主键集合（对应 data_stream_column_type_map.column_type_map_id）。
     * 包括 MySQL &lt;-&gt; PostgreSQL / Oracle 的所有内置映射。
     */
    public static final Set<Long> BUILTIN_TYPE_MAP_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // MySQL -> PostgreSQL 数值
            1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 300L,
            // MySQL -> PostgreSQL 无符号
            311L, 312L, 313L, 314L, 315L,
            // MySQL -> PostgreSQL 日期时间
            9L, 10L, 11L, 12L, 13L, 301L,
            // MySQL -> PostgreSQL 字符串
            14L, 15L, 16L, 17L, 18L, 19L, 20L, 302L, 303L,
            // MySQL -> PostgreSQL 二进制
            304L, 305L, 306L, 307L, 308L, 309L, 310L,
            // Oracle -> MySQL 数值
            21L, 22L, 23L, 24L, 25L, 26L,
            // Oracle -> MySQL 日期时间
            27L, 28L, 30L, 31L,
            // Oracle -> MySQL 字符串
            32L, 33L, 34L, 35L, 36L, 37L,
            // Oracle -> MySQL 二进制
            38L, 39L,
            // PostgreSQL -> MySQL 数值
            51L, 52L, 53L, 54L, 55L, 56L, 57L, 58L,
            // PostgreSQL -> MySQL 日期时间
            61L, 62L, 63L, 64L,
            // PostgreSQL -> MySQL 字符串
            71L, 72L, 73L,
            // MySQL -> Oracle 数值
            81L, 82L, 83L, 84L, 85L, 86L, 87L,
            // MySQL -> Oracle 日期时间
            91L, 92L, 93L, 94L,
            // MySQL -> Oracle 字符串
            101L, 102L, 103L, 104L, 105L, 106L,
            // MySQL -> Oracle 二进制
            111L
    )));
}