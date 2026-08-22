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
package com.itman.datastream.security.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SystemUser implements Serializable {
    private static final long serialVersionUID = 5589664484427030464L;
    private Long systemUserId;
    private String systemUserCode;
    private String systemUserName;
    private String password;
    private Long orgId;
    private String orgName;
    private String username;
    /**
     * 角色编码列表（登录响应返回，供前端菜单/权限过滤使用）
     */
    private List<String> roles;
    /**
     * 权限编码列表（登录响应返回，供前端操作鉴权使用）
     */
    private List<String> permissions;
    /**
     * 允许访问的菜单路由列表（登录响应返回，供前端菜单过滤使用）
     */
    private List<String> menus;
}
