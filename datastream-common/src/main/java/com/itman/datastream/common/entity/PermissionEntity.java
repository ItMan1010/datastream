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
 * 权限资源实体类。
 * 对应表 {@code data_stream_permission}，用于菜单权限（permission_type=1）与
 * 数据权限（permission_type=2）的维护。
 */
@Data
public class PermissionEntity {
    /**
     * 主键标识，序列名称：SEQ_PERMISSION_ID
     */
    private Long permissionId;

    /**
     * 权限编码（唯一，如 task:create）
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限类型：1菜单、2数据操作
     */
    private Integer permissionType;

    /**
     * 父权限标识（菜单树）
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 菜单路由标识
     */
    private String route;

    /**
     * 是否内置：0否、1是
     */
    private Integer builtIn;
}