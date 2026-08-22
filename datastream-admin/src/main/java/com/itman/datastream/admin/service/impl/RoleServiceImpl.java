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

import com.itman.datastream.admin.service.IRoleService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.constant.RbacConstant;
import com.itman.datastream.common.entity.PermissionEntity;
import com.itman.datastream.common.entity.RoleEntity;
import com.itman.datastream.common.entity.RolePermissionEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.engine.dao.SystemPermissionDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.SEQ_ROLE_ID;
import static com.itman.datastream.common.constant.DataStreamConstant.SEQ_ROLE_PERMISSION_ID;
import static com.itman.datastream.common.utils.CommUtils.genPageRow;

/**
 * 角色管理服务实现。
 * 包含角色新增/编辑/删除/分页查询/用户派发与角色-权限授权，以及下列保护约束：
 * <ul>
 *     <li>角色编码、名称唯一校验；</li>
 *     <li>被使用角色禁止删除；</li>
 *     <li>内置系统管理员角色禁止删除、修改及权限调整。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {

    private final SystemPermissionDao systemPermissionDao;
    private final DataStreamDao dataStreamDao;
    private final DataSourceFactory dataSourceFactory;
    private final DataStreamConfig dataStreamConfig;

    private IDatabaseAdapter getDataBaseObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    @Override
    public Integer getRoleCount(Integer queryFlag, String queryValue) throws DataStreamException {
        return systemPermissionDao.selectRoleListCount(queryFlag, queryValue);
    }

    @Override
    public List<RoleEntity> queryRoleByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException {
        return systemPermissionDao.selectRoleList(queryFlag, queryValue, getDataBaseObject().makeSqlLimit(genPageRow(page, count), count));
    }

    @Override
    public List<RoleEntity> queryAllRole() throws DataStreamException {
        return systemPermissionDao.selectRoleList(RbacConstant.QUERY_FLAG_ALL, null, null);
    }

    @Override
    public RoleEntity getRoleById(Long roleId) throws DataStreamException {
        if (roleId == null) {
            throw new DataStreamException("RBAC_ROLE_001", "角色ID不能为空");
        }
        return systemPermissionDao.selectRoleById(roleId);
    }

    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) throws DataStreamException {
        List<Long> permissionIds = new ArrayList<>();
        List<PermissionEntity> permissions = systemPermissionDao.selectPermissionsByRoleId(roleId);
        if (permissions != null) {
            for (PermissionEntity permission : permissions) {
                if (permission != null && permission.getPermissionId() != null) {
                    permissionIds.add(permission.getPermissionId());
                }
            }
        }
        return permissionIds;
    }

    @Override
    public Long addRole(RoleEntity role) throws DataStreamException {
        validateRoleRequired(role);
        checkRoleCodeUnique(role.getRoleCode(), null);
        checkRoleNameUnique(role.getRoleName(), null);
        role.setRoleId(dataStreamDao.querySequence(SEQ_ROLE_ID));
        role.setBuiltIn(RbacConstant.BUILT_IN_NO);
        systemPermissionDao.insertRole(role);
        return role.getRoleId();
    }

    @Override
    public void modifyRole(RoleEntity role) throws DataStreamException {
        if (role == null || role.getRoleId() == null) {
            throw new DataStreamException("RBAC_ROLE_001", "角色ID不能为空");
        }
        validateRoleRequired(role);
        checkRoleCodeUnique(role.getRoleCode(), role.getRoleId());
        checkRoleNameUnique(role.getRoleName(), role.getRoleId());
        RoleEntity existing = systemPermissionDao.selectRoleById(role.getRoleId());
        if (existing == null) {
            throw new DataStreamException("RBAC_ROLE_002", "角色不存在");
        }
        if (isBuiltIn(existing)) {
            throw new DataStreamException("RBAC_ROLE_003", "内置角色不可修改");
        }
        role.setBuiltIn(existing.getBuiltIn());
        systemPermissionDao.updateRole(role);
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void delRole(Long roleId) throws DataStreamException {
        if (roleId == null) {
            throw new DataStreamException("RBAC_ROLE_001", "角色ID不能为空");
        }
        RoleEntity existing = systemPermissionDao.selectRoleById(roleId);
        if (existing == null) {
            throw new DataStreamException("RBAC_ROLE_002", "角色不存在");
        }
        if (isBuiltIn(existing)) {
            throw new DataStreamException("RBAC_ROLE_004", "内置角色不可删除");
        }
        Integer refCount = systemPermissionDao.countRoleUserReferenced(roleId);
        if (refCount != null && refCount > 0) {
            throw new DataStreamException("RBAC_ROLE_005", "该角色仍被 " + refCount + " 个用户使用，不能删除");
        }
        systemPermissionDao.deleteRolePermissionByRoleId(roleId);
        systemPermissionDao.deleteUserRoleByRoleId(roleId);
        systemPermissionDao.deleteRole(roleId);
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void assignRolePermissions(Long roleId, List<Long> permissionIds) throws DataStreamException {
        if (roleId == null) {
            throw new DataStreamException("RBAC_ROLE_001", "角色ID不能为空");
        }
        RoleEntity existing = systemPermissionDao.selectRoleById(roleId);
        if (existing == null) {
            throw new DataStreamException("RBAC_ROLE_002", "角色不存在");
        }
        if (isBuiltIn(existing)) {
            throw new DataStreamException("RBAC_ROLE_006", "内置角色不可调整权限");
        }
        systemPermissionDao.deleteRolePermissionByRoleId(roleId);
        if (permissionIds == null) {
            return;
        }
        for (Long permissionId : permissionIds) {
            if (permissionId == null) {
                continue;
            }
            RolePermissionEntity rolePermission = new RolePermissionEntity();
            rolePermission.setRolePermissionId(dataStreamDao.querySequence(SEQ_ROLE_PERMISSION_ID));
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            systemPermissionDao.insertRolePermission(rolePermission);
        }
    }

    // ==================== 校验与工具 ====================

    private void validateRoleRequired(RoleEntity role) throws DataStreamException {
        if (role == null) {
            throw new DataStreamException("RBAC_ROLE_007", "角色不能为空");
        }
        if (StringUtils.isEmpty(role.getRoleCode())) {
            throw new DataStreamException("RBAC_ROLE_008", "角色编码不能为空");
        }
        if (StringUtils.isEmpty(role.getRoleName())) {
            throw new DataStreamException("RBAC_ROLE_009", "角色名称不能为空");
        }
    }

    private void checkRoleCodeUnique(String roleCode, Long excludeId) throws DataStreamException {
        Integer count = systemPermissionDao.selectRoleCodeCount(roleCode, excludeId);
        if (count != null && count > 0) {
            throw new DataStreamException("RBAC_ROLE_010", "角色编码【" + roleCode + "】已存在");
        }
    }

    private void checkRoleNameUnique(String roleName, Long excludeId) throws DataStreamException {
        Integer count = systemPermissionDao.selectRoleNameCount(roleName, excludeId);
        if (count != null && count > 0) {
            throw new DataStreamException("RBAC_ROLE_011", "角色名称【" + roleName + "】已存在");
        }
    }

    private boolean isBuiltIn(RoleEntity role) {
        return role != null && Integer.valueOf(RbacConstant.BUILT_IN_YES).equals(role.getBuiltIn());
    }
}