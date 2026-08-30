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

import lombok.Data;

/**
 * 达梦 all_tab_columns 列元数据实体，承载达梦 NUMBER 的精度/小数位、
 * 可空性、默认值与列注释等完整信息，供表结构迁移使用。
 */
@Data
public class DamengTableColumnsEntity {
    private String columnName;
    private String dataType;
    private Integer dataLength;
    private Integer dataPrecision;
    private Integer dataScale;
    private String nullable;
    private String dataDefault;
    private String comments;
}