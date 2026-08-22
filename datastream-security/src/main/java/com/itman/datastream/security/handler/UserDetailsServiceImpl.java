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
package com.itman.datastream.security.handler;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itman.datastream.common.entity.SystemUserEntity;
import com.itman.datastream.engine.dao.SystemPermissionDao;
import com.itman.datastream.security.annotation.LogOperate;
import com.itman.datastream.security.constant.SecurityConstant;
import com.itman.datastream.security.domain.SystemUser;
import com.itman.datastream.security.jwt.DsJwtUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    @Value("${sso.auth.service.url:}")
    private String ssoAuthServiceUrl;
    @Value("${auth.sso.mode.enabled:false}")
    private boolean ssoMode;
    @Value("${auth.local.systemUserAuth.test.mode: false}")
    private boolean localTestMode;
    @Value("${auth.local.systemUserAuth.password.mode: 1}")
    private int passwordMode;
    @Value("${auth.local.systemUserAuth.test.systemUserCode:admin}")
    private String testUser;
    @Value("${auth.local.systemUserAuth.test.password:admin}")
    private String testPassword;

    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private SystemPermissionDao systemPermissionDao;

    @LogOperate(operateType = 1, moduleName = "用户登录", username = "#username", description = "'帐号登录：'+#username")
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("isSsoModeEnabled: {}, isLocalTestModeEnabled: {}", isSsoModeEnabled(), isLocalTestModeEnabled());
        if (isSsoModeEnabled()) {
            if (StringUtils.isEmpty(this.ssoToken)) {
                throw new UsernameNotFoundException("登录失败,token不合法!");
            }
            return this.loadUserBySsoToken(username);
        }
        if (isLocalTestModeEnabled()) {
            return this.loadUserLocalTestMode(username);
        }
        return this.loadUserByDatabase(username);
    }


    private boolean isSsoModeEnabled() {
        return ssoMode;
    }

    private boolean isLocalTestModeEnabled() {
        return localTestMode;
    }

    private UserDetails loadUserLocalTestMode(String username) throws UsernameNotFoundException {
        String password = loginUser == null ? null : loginUser.getPassword();
        if (!testUser.equals(username) || !testPassword.equals(password)) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        SystemUser systemUser = new SystemUser();
        systemUser.setSystemUserId(1L);
        systemUser.setSystemUserName(username);
        systemUser.setSystemUserCode(username);
        systemUser.setUsername(username);
        // 过滤器已解密为明文，这里需与 DaoAuthenticationProvider 的 BCrypt 校验保持一致
        systemUser.setPassword(passwordEncoder.encode(password));
        // 本地测试模式映射内置系统管理员角色
        List<String> roles = new ArrayList<>();
        roles.add(SecurityConstant.SYSTEM_ADMIN_ROLE_CODE);
        systemUser.setRoles(roles);
        return new DsJwtUser(systemUser, buildAuthorities(roles, null));
    }

    private UserDetails loadUserByDatabase(String username) throws UsernameNotFoundException {
        if (loginUser == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        SystemUserEntity userEntity;
        try {
            userEntity = systemPermissionDao.selectSystemUserByCode(username);
        } catch (Exception e) {
            log.error("loadUserByDatabase.selectSystemUserByCode.error: ", e);
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        if (userEntity == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        if (userEntity.getState() != null && userEntity.getState() != 1) {
            throw new UsernameNotFoundException("账号已被禁用");
        }
        String rawPassword = loginUser.getPassword();
        if (StringUtils.isEmpty(rawPassword) || !passwordEncoder.matches(rawPassword, userEntity.getPassword())) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        SystemUser systemUser = new SystemUser();
        systemUser.setSystemUserId(userEntity.getSystemUserId());
        systemUser.setSystemUserCode(userEntity.getSystemUserCode());
        systemUser.setSystemUserName(userEntity.getSystemUserName());
        systemUser.setOrgId(userEntity.getOrgId());
        systemUser.setOrgName(userEntity.getOrgName());
        systemUser.setUsername(userEntity.getUsername() != null ? userEntity.getUsername() : userEntity.getSystemUserCode());
        systemUser.setPassword(userEntity.getPassword());

        List<String> roles = null;
        List<String> permissions = null;
        List<String> menus = null;
        try {
            Long systemUserId = userEntity.getSystemUserId();
            roles = systemPermissionDao.selectRoleCodesByUserId(systemUserId);
            permissions = systemPermissionDao.selectPermissionCodesByUserId(systemUserId);
            menus = systemPermissionDao.selectMenuRoutesByUserId(systemUserId);
        } catch (Exception e) {
            log.error("loadUserByDatabase.loadPermissions.error: ", e);
        }
        systemUser.setRoles(roles);
        systemUser.setPermissions(permissions);
        systemUser.setMenus(menus);

        return new DsJwtUser(systemUser, buildAuthorities(roles, permissions));
    }

    private List<GrantedAuthority> buildAuthorities(List<String> roles, List<String> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            for (String role : roles) {
                if (!StringUtils.isEmpty(role)) {
                    authorities.add(new SimpleGrantedAuthority(SecurityConstant.ROLE_PREFIX + role));
                }
            }
        }
        if (permissions != null) {
            for (String permission : permissions) {
                if (!StringUtils.isEmpty(permission)) {
                    authorities.add(new SimpleGrantedAuthority(SecurityConstant.PERM_PREFIX + permission));
                }
            }
        }
        return authorities;
    }

    private UserDetails loadUserBySsoToken(String ssoToken) throws UsernameNotFoundException {
        SystemUser systemUser = null;
        try {
            systemUser = this.getSystemUserInfoByToken(ssoToken);
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_TASK_ALL"));
        } catch (AuthenticationServiceException ase) {
            log.error("MyUserDetailsService.loadUserBySsoToken.error: ", ase);
            throw new UsernameNotFoundException(ase.getMessage());
        } catch (Exception e) {
            log.error("MyUserDetailsService.loadUserBySsoToken.error: ", e);
            throw new UsernameNotFoundException("登录出错");
        }
        return new DsJwtUser(systemUser);
    }

    private SystemUser getSystemUserInfoByToken(String ssoToken) throws AuthenticationServiceException {
        if (SecurityConstant.NULL_VALUE.equals(ssoAuthServiceUrl)) {
            throw new AuthenticationServiceException("登录失败，鉴权接口未配置");
        }

        JSONObject reqJo = new JSONObject();
        reqJo.put("ssoToken", ssoToken);
        String resp = HttpRequest.post(ssoAuthServiceUrl).body(reqJo.toJSONString()).execute().body();

        if (StringUtils.isEmpty(resp)) {
            throw new AuthenticationServiceException("鉴权接口调用出错");
        }
        JSONObject respJo = JSON.parseObject(resp);
        String resultCode = respJo.getString("resultCode");
        if (!SecurityConstant.SUCCESS_CODE.equals(resultCode)) {
            String resultMsg = respJo.getString("resultMsg");
            throw new AuthenticationServiceException(resultCode + ":" + resultMsg);
        }

        SystemUser systemUser = new SystemUser();
        JSONObject systemUserInfoJo = respJo.getJSONObject("systemUserInfo");
        if (null == systemUserInfoJo) {
            throw new AuthenticationServiceException("鉴权接口未返回工号信息");
        }
        systemUser.setSystemUserId(systemUserInfoJo.getLong("systemUserId"));
        systemUser.setSystemUserCode(systemUserInfoJo.getString("systemUserCode"));
        systemUser.setSystemUserName(systemUserInfoJo.getString("systemUserName"));
        systemUser.setOrgId(systemUserInfoJo.getLong("orgId"));
        systemUser.setOrgName(systemUserInfoJo.getString("orgName"));
        systemUser.setUsername(ssoToken);
        systemUser.setPassword(SecurityConstant.ANONYMOUS_USERNAME);
        return systemUser;
    }

    private SystemUser loginUser;
    private String ssoToken;

    public void setLoginUser(SystemUser loginUser) {
        this.loginUser = loginUser;
    }

    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }
}
