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
package com.itman.datastream.engine.holder;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueueObject {
    //批量迁移行数据:key字段名-value字段值
    private Map dataMap;

    //数据增量迁移
    private Map<String, Object> beforeData;
    private Map<String, Object> afterData;
    private String handleType;
    private String table;
    private List<String> timestampFields;
}
