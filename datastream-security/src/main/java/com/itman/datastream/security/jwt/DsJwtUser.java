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
package com.itman.datastream.security.jwt;

import com.itman.datastream.security.domain.SystemUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DsJwtUser implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private SystemUser systemUser;
    private Collection<? extends GrantedAuthority> authorities;
    private List<String> roles;
    private List<String> permissions;

    public DsJwtUser(SystemUser systemUser) {
        this(systemUser, Collections.singletonList(new SimpleGrantedAuthority("ROLE_TASK_ALL")));
    }

    public DsJwtUser(SystemUser systemUser, Collection<? extends GrantedAuthority> authorities) {
        this.id = systemUser.getSystemUserId();
        this.username = systemUser.getUsername() != null ? systemUser.getUsername() : systemUser.getSystemUserCode();
        this.password = systemUser.getPassword();
        this.systemUser = systemUser;
        this.authorities = authorities;
        this.roles = systemUser.getRoles();
        this.permissions = systemUser.getPermissions();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public SystemUser getSystemUserInfo() {
        return systemUser;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
