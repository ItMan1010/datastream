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
 * 系统用户实体类。
 * 对应表 {@code data_stream_system_user}，用于权限管理系统用户账号。
 */
@Data
public class SystemUserEntity {
    /**
     * 主键标识，序列名称：SEQ_SYSTEM_USER_ID
     */
    private Long systemUserId;

    /**
     * 登录账号（唯一）
     */
    private String systemUserCode;

    /**
     * 显示名
     */
    private String systemUserName;

    /**
     * 登录密码（BCrypt 密文）
     */
    private String password;

    /**
     * 机构标识
     */
    private Long orgId;

    /**
     * 机构名称
     */
    private String orgName;

    /**
     * 用户名（兼容旧字段）
     */
    private String username;

    /**
     * 状态：0禁用、1启用
     */
    private Integer state;

    /**
     * 创建时间
     */
    private String createDate;

    /**
     * 更新时间
     */
    private String updateDate;
}