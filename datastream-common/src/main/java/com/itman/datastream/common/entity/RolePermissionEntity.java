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
 * 角色-权限关联实体类。
 * 对应表 {@code data_stream_role_permission}，记录角色被授予的权限。
 */
@Data
public class RolePermissionEntity {
    /**
     * 主键标识，序列名称：SEQ_ROLE_PERMISSION_ID
     */
    private Long rolePermissionId;

    /**
     * 角色标识
     */
    private Long roleId;

    /**
     * 权限标识
     */
    private Long permissionId;
}