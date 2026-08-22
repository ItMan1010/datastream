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

/**
 * 权限管理（RBAC）通用常量。
 * 与 {@code doc/sql/datastream-rbac-seed.sql} 内置数据保持一致。
 */
public final class RbacConstant {

    private RbacConstant() {
    }

    /**
     * 内置系统管理员账号。
     */
    public static final String ADMIN_USER_CODE = "admin";

    /**
     * 内置系统管理员角色编码。
     */
    public static final String SYSTEM_ADMIN_ROLE_CODE = "SYSTEM_ADMIN";

    /**
     * 是否内置：0否、1是。
     */
    public static final int BUILT_IN_NO = 0;
    public static final int BUILT_IN_YES = 1;

    /**
     * 用户状态：0禁用、1启用。
     */
    public static final int USER_STATE_DISABLED = 0;
    public static final int USER_STATE_ENABLED = 1;

    /**
     * 权限类型：1菜单、2数据操作。
     */
    public static final int PERMISSION_TYPE_MENU = 1;
    public static final int PERMISSION_TYPE_DATA = 2;

    /**
     * 查询标志：1全部、2按编码、3按名称。
     */
    public static final int QUERY_FLAG_ALL = 1;
    public static final int QUERY_FLAG_CODE = 2;
    public static final int QUERY_FLAG_NAME = 3;
}