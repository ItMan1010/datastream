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
package com.itman.datastream.security.service;

import com.itman.datastream.security.constant.SecurityConstant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * 数据权限校验服务。
 * 供 {@code @PreAuthorize} 调用，系统管理员与兼容的 SSO 全量角色直接放行，
 * 否则校验当前认证是否含 {@code PERM_<code>} 权限。
 */
@Component("permissionService")
public class PermissionService {

    private static final String SYSTEM_ADMIN_AUTHORITY = SecurityConstant.ROLE_PREFIX + SecurityConstant.SYSTEM_ADMIN_ROLE_CODE;
    private static final String LEGACY_TASK_ALL_AUTHORITY = "ROLE_TASK_ALL";

    public boolean hasPermission(String permissionCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        if (StringUtils.isEmpty(permissionCode)) {
            return false;
        }
        String required = SecurityConstant.PERM_PREFIX + permissionCode;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if (SYSTEM_ADMIN_AUTHORITY.equals(auth) || LEGACY_TASK_ALL_AUTHORITY.equals(auth)) {
                return true;
            }
            if (required.equals(auth)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyPermission(String... permissionCodes) {
        if (permissionCodes == null) {
            return false;
        }
        for (String code : permissionCodes) {
            if (hasPermission(code)) {
                return true;
            }
        }
        return false;
    }
}