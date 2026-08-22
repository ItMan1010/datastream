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
 * 角色实体类。
 * 对应表 {@code data_stream_role}，用于权限管理系统角色。
 */
@Data
public class RoleEntity {
    /**
     * 主键标识，序列名称：SEQ_ROLE_ID
     */
    private Long roleId;

    /**
     * 角色编码（唯一）
     */
    private String roleCode;

    /**
     * 角色名称（唯一）
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 是否内置：0否、1是
     */
    private Integer builtIn;

    /**
     * 创建时间
     */
    private String createDate;

    /**
     * 更新时间
     */
    private String updateDate;
}