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
package com.itman.datastream.connectors.file;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class QueueData {
    /**
     * 1:对象数据转换业务标准格式：key=taskFieldRelaId、value=对象数据字段值
     */
    private Map<Long, String> dataMap;
    /**
     * 比对标志，0未比较、1已比较
     */
    private Integer comparedFlag;
    /**
     * 参与比较key
     */
    private String comparedKey;
    /**
     * 对象字段名称和值映射
     */
    private Map<String, String> fieldNameValueMap;
}
