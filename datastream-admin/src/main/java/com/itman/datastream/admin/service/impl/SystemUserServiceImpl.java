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
package com.itman.datastream.admin.service.impl;

import com.itman.datastream.admin.service.ISystemUserService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.constant.RbacConstant;
import com.itman.datastream.common.entity.RoleEntity;
import com.itman.datastream.common.entity.SystemUserEntity;
import com.itman.datastream.common.entity.UserRoleEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.engine.dao.SystemPermissionDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.SEQ_SYSTEM_USER_ID;
import static com.itman.datastream.common.constant.DataStreamConstant.SEQ_USER_ROLE_ID;
import static com.itman.datastream.common.utils.CommUtils.genPageRow;

/**
 * 系统用户管理服务实现。
 * 包含新增/编辑/禁用/启用/删除/重置密码/分页查询，以及下列保护约束：
 * <ul>
 *     <li>账号唯一校验；</li>
 *     <li>内置 admin 禁止删除、禁用与降权（必须保留系统管理员角色）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemUserServiceImpl implements ISystemUserService {

    private final SystemPermissionDao systemPermissionDao;
    private final DataStreamDao dataStreamDao;
    private final DataSourceFactory dataSourceFactory;
    private final DataStreamConfig dataStreamConfig;
    private final PasswordEncoder passwordEncoder;

    private IDatabaseAdapter getDataBaseObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    @Override
    public Integer getUserCount(Integer queryFlag, String queryValue) throws DataStreamException {
        return systemPermissionDao.selectSystemUserListCount(queryFlag, queryValue);
    }

    @Override
    public List<SystemUserEntity> queryUserByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException {
        return systemPermissionDao.selectSystemUserList(queryFlag, queryValue, getDataBaseObject().makeSqlLimit(genPageRow(page, count), count));
    }

    @Override
    public SystemUserEntity getUserById(Long systemUserId) throws DataStreamException {
        if (systemUserId == null) {
            throw new DataStreamException("RBAC_USER_001", "用户ID不能为空");
        }
        return systemPermissionDao.selectSystemUserById(systemUserId);
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long systemUserId) throws DataStreamException {
        List<Long> roleIds = new ArrayList<>();
        List<RoleEntity> roles = systemPermissionDao.selectRolesByUserId(systemUserId);
        if (roles != null) {
            for (RoleEntity role : roles) {
                if (role != null && role.getRoleId() != null) {
                    roleIds.add(role.getRoleId());
                }
            }
        }
        return roleIds;
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public Long addUser(SystemUserEntity user, List<Long> roleIds) throws DataStreamException {
        validateUserRequired(user, true);
        checkUserCodeUnique(user.getSystemUserCode(), null);
        user.setSystemUserId(dataStreamDao.querySequence(SEQ_SYSTEM_USER_ID));
        if (StringUtils.isEmpty(user.getUsername())) {
            user.setUsername(user.getSystemUserCode());
        }
        if (user.getState() == null) {
            user.setState(RbacConstant.USER_STATE_ENABLED);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        systemPermissionDao.insertSystemUser(user);
        saveUserRoles(user.getSystemUserId(), roleIds);
        return user.getSystemUserId();
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void modifyUser(SystemUserEntity user, List<Long> roleIds) throws DataStreamException {
        if (user == null || user.getSystemUserId() == null) {
            throw new DataStreamException("RBAC_USER_003", "用户ID不能为空");
        }
        validateUserRequired(user, false);
        checkUserCodeUnique(user.getSystemUserCode(), user.getSystemUserId());
        SystemUserEntity existing = systemPermissionDao.selectSystemUserById(user.getSystemUserId());
        if (existing == null) {
            throw new DataStreamException("RBAC_USER_004", "用户不存在");
        }
        if (isAdmin(existing)) {
            if (Integer.valueOf(RbacConstant.USER_STATE_DISABLED).equals(user.getState())) {
                throw new DataStreamException("RBAC_USER_008", "内置管理员不可被禁用");
            }
            ensureAdminKeepsSystemAdminRole(roleIds);
        }
        if (StringUtils.isEmpty(user.getUsername())) {
            user.setUsername(user.getSystemUserCode());
        }
        if (user.getState() == null) {
            user.setState(existing.getState());
        }
        systemPermissionDao.updateSystemUser(user);
        saveUserRoles(user.getSystemUserId(), roleIds);
    }

    @Override
    public void updateUserState(Long systemUserId, Integer state) throws DataStreamException {
        if (systemUserId == null) {
            throw new DataStreamException("RBAC_USER_001", "用户ID不能为空");
        }
        if (state == null || (state != RbacConstant.USER_STATE_DISABLED && state != RbacConstant.USER_STATE_ENABLED)) {
            throw new DataStreamException("RBAC_USER_009", "用户状态不合法");
        }
        SystemUserEntity existing = systemPermissionDao.selectSystemUserById(systemUserId);
        if (existing == null) {
            throw new DataStreamException("RBAC_USER_004", "用户不存在");
        }
        if (isAdmin(existing) && Integer.valueOf(RbacConstant.USER_STATE_DISABLED).equals(state)) {
            throw new DataStreamException("RBAC_USER_008", "内置管理员不可被禁用");
        }
        systemPermissionDao.updateSystemUserState(systemUserId, state);
    }

    @Override
    public void resetPassword(Long systemUserId, String password) throws DataStreamException {
        if (systemUserId == null) {
            throw new DataStreamException("RBAC_USER_001", "用户ID不能为空");
        }
        if (StringUtils.isEmpty(password)) {
            throw new DataStreamException("RBAC_USER_010", "新密码不能为空");
        }
        systemPermissionDao.updateSystemUserPassword(systemUserId, passwordEncoder.encode(password));
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void delUser(Long systemUserId) throws DataStreamException {
        if (systemUserId == null) {
            throw new DataStreamException("RBAC_USER_001", "用户ID不能为空");
        }
        SystemUserEntity existing = systemPermissionDao.selectSystemUserById(systemUserId);
        if (existing == null) {
            throw new DataStreamException("RBAC_USER_004", "用户不存在");
        }
        if (isAdmin(existing)) {
            throw new DataStreamException("RBAC_USER_011", "内置管理员不可被删除");
        }
        systemPermissionDao.deleteUserRoleByUserId(systemUserId);
        systemPermissionDao.deleteSystemUser(systemUserId);
    }

    // ==================== 校验与工具 ====================

    private void validateUserRequired(SystemUserEntity user, boolean requirePassword) throws DataStreamException {
        if (user == null) {
            throw new DataStreamException("RBAC_USER_002", "用户不能为空");
        }
        if (StringUtils.isEmpty(user.getSystemUserCode())) {
            throw new DataStreamException("RBAC_USER_005", "登录账号不能为空");
        }
        if (StringUtils.isEmpty(user.getSystemUserName())) {
            throw new DataStreamException("RBAC_USER_012", "显示名不能为空");
        }
        if (requirePassword && StringUtils.isEmpty(user.getPassword())) {
            throw new DataStreamException("RBAC_USER_010", "登录密码不能为空");
        }
    }

    private void checkUserCodeUnique(String systemUserCode, Long excludeId) throws DataStreamException {
        Integer count = systemPermissionDao.selectSystemUserCodeCount(systemUserCode, excludeId);
        if (count != null && count > 0) {
            throw new DataStreamException("RBAC_USER_006", "账号【" + systemUserCode + "】已存在");
        }
    }

    private boolean isAdmin(SystemUserEntity user) {
        return user != null
                && (RbacConstant.ADMIN_USER_CODE.equals(user.getSystemUserCode())
                || Long.valueOf(1L).equals(user.getSystemUserId()));
    }

    private void ensureAdminKeepsSystemAdminRole(List<Long> roleIds) throws DataStreamException {
        RoleEntity adminRole = systemPermissionDao.selectRoleByCode(RbacConstant.SYSTEM_ADMIN_ROLE_CODE);
        if (adminRole == null || roleIds == null || !roleIds.contains(adminRole.getRoleId())) {
            throw new DataStreamException("RBAC_USER_007", "内置管理员不可被降权，必须保留系统管理员角色");
        }
    }

    private void saveUserRoles(Long systemUserId, List<Long> roleIds) throws DataStreamException {
        systemPermissionDao.deleteUserRoleByUserId(systemUserId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserRoleId(dataStreamDao.querySequence(SEQ_USER_ROLE_ID));
            userRole.setSystemUserId(systemUserId);
            userRole.setRoleId(roleId);
            systemPermissionDao.insertUserRole(userRole);
        }
    }
}