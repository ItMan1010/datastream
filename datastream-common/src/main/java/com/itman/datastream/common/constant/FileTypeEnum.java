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

import java.util.Arrays;
import java.util.Objects;

/**
 * 文件类型的枚举定义
 */
@Getter
@AllArgsConstructor
public enum FileTypeEnum {

    /**
     * 文本文件
     */
    TEXT(8, "Text"),

    /**
     * Excel
     */
    EXCEL(9, "Excel");

    private Integer id;
    private String name;

    public static boolean exists(String name) {
        return Arrays.stream(values()).anyMatch(item -> item.name().equalsIgnoreCase(name));
    }

    public static FileTypeEnum of(Integer id) {
        if (!Objects.isNull(id)) {
            for (FileTypeEnum type : FileTypeEnum.values()) {
                if (type.getId().equals(id)) {
                    return type;
                }
            }
        }

        throw new IllegalArgumentException("cannot find enum id: " + id);
    }
}
