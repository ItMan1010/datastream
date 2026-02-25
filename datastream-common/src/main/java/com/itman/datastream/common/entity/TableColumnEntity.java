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

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableColumnEntity extends ColumnTypeDefineEntity{
    /**
     * 字段名称，统一转成小写
     */
    private String columnName;
    /**
     * 列大小/长度
     */
    private Integer columnSize;
    /**
     * 小数位数（标度）
     */
    private Integer decimalDigits;
    /**
     * JDBC数据类型
     */
    private Integer dataType;
    /**
     * 类型名称,如：int,，统一转成小写
     */
    private String typeName;
    /**
     * 是否可空
     * columnNoNulls = 0
     * columnNullable = 1
     * columnNullableUnknown = 2
     */
    private Integer nullAble;
    /**
     * 字段描述信息
     */
    private String remarks;
    /**
     * 字段默认值
     */
    private String columnDef;

    /**
     * 是否是主键字段
     */
    private boolean keyFlag;
    /**
     * 是否是索引字段
     */
    private boolean indexFlag;
    /**
     * 列在表中的序号（从1开始）
     */
    private Integer ordinalPosition;
    /**
     * 指示列是否为自增列("yes"/"no")
     */
    private String isAutoIncrement;
    /**
     * 指示列是否允许null值("yes"/"no")
     */
    private String isNullable;
}

