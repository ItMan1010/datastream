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

import com.itman.datastream.engine.dao.SystemPermissionDao;
import com.itman.datastream.security.constant.SecurityConstant;
import com.itman.datastream.security.domain.SystemUser;
import com.itman.datastream.security.exception.PermissionDeniedException;
import com.itman.datastream.security.jwt.DsJwtUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限校验服务。
 * 供 {@code @PreAuthorize} 调用，系统管理员与兼容的 SSO 全量角色直接放行，
 * 否则校验当前认证是否含 {@code PERM_<code>} 权限。无权限时抛出
 * {@link PermissionDeniedException}，携带缺失权限的中文名称与编码。
 */
@Slf4j
@Component("permissionService")
public class PermissionService {

    private static final String SYSTEM_ADMIN_AUTHORITY = SecurityConstant.ROLE_PREFIX + SecurityConstant.SYSTEM_ADMIN_ROLE_CODE;
    private static final String LEGACY_TASK_ALL_AUTHORITY = "ROLE_TASK_ALL";
    private static final String FALLBACK_PERMISSION_NAME = "该操作";

    @Resource
    private SystemPermissionDao systemPermissionDao;

    private final Map<String, String> permissionNameCache = new ConcurrentHashMap<>();

    public boolean hasPermission(String permissionCode) {
        if (!checkPermission(permissionCode)) {
            throw new PermissionDeniedException(buildSingleDeniedMessage(permissionCode));
        }
        return true;
    }

    public boolean hasAnyPermission(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            throw new PermissionDeniedException("无权限执行该操作，请联系管理员");
        }
        for (String code : permissionCodes) {
            if (checkPermission(code)) {
                return true;
            }
        }
        throw new PermissionDeniedException(buildAnyDeniedMessage(permissionCodes));
    }

    /**
     * 判断当前登录用户是否为系统管理员（或兼容的 SSO 全量角色）。
     * 用于行级数据范围控制：管理员返回 {@code true}，可见全部任务数据。
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if (SYSTEM_ADMIN_AUTHORITY.equals(auth) || LEGACY_TASK_ALL_AUTHORITY.equals(auth)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前登录用户的用户编码。
     * 兼容 JWT 认证（principal 为 String，即 systemUserCode）与登录态（principal 为 {@link DsJwtUser}）两种情形。
     */
    public String getCurrentUserCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof DsJwtUser) {
            SystemUser systemUser = ((DsJwtUser) principal).getSystemUserInfo();
            return systemUser != null ? systemUser.getSystemUserCode() : null;
        }
        return authentication.getName();
    }

    private boolean checkPermission(String permissionCode) {
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

    private String buildSingleDeniedMessage(String permissionCode) {
        return "无【" + resolvePermissionName(permissionCode) + "（" + permissionCode + "）】权限，请联系管理员";
    }

    private String buildAnyDeniedMessage(String[] permissionCodes) {
        List<String> parts = new ArrayList<>();
        for (String code : permissionCodes) {
            parts.add("【" + resolvePermissionName(code) + "（" + code + "）】");
        }
        return "无" + String.join("或", parts) + "权限，请联系管理员";
    }

    private String resolvePermissionName(String permissionCode) {
        if (StringUtils.isEmpty(permissionCode)) {
            return FALLBACK_PERMISSION_NAME;
        }
        String cached = permissionNameCache.get(permissionCode);
        if (cached != null) {
            return cached;
        }
        String name = null;
        try {
            name = systemPermissionDao.selectPermissionNameByCode(permissionCode);
        } catch (Exception e) {
            log.warn("查询权限名称失败，permissionCode={}", permissionCode, e);
        }
        if (StringUtils.isEmpty(name)) {
            name = FALLBACK_PERMISSION_NAME;
        }
        permissionNameCache.put(permissionCode, name);
        return name;
    }
}
