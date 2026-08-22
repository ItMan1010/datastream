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

import com.itman.datastream.admin.service.IPermissionService;
import com.itman.datastream.common.constant.RbacConstant;
import com.itman.datastream.common.entity.PermissionEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.engine.dao.SystemPermissionDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.SEQ_PERMISSION_ID;

/**
 * 权限资源管理服务实现。
 * 包含菜单权限树与数据权限列表查询、权限资源维护，以及内置资源保护约束。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements IPermissionService {

    private final SystemPermissionDao systemPermissionDao;
    private final DataStreamDao dataStreamDao;

    @Override
    public List<PermissionEntity> queryMenuTree() throws DataStreamException {
        return systemPermissionDao.selectPermissionList(RbacConstant.PERMISSION_TYPE_MENU, null);
    }

    @Override
    public List<PermissionEntity> queryDataPermissionList() throws DataStreamException {
        return systemPermissionDao.selectPermissionList(RbacConstant.PERMISSION_TYPE_DATA, null);
    }

    @Override
    public Long addPermission(PermissionEntity permission) throws DataStreamException {
        validatePermissionRequired(permission);
        checkPermissionCodeUnique(permission.getPermissionCode(), null);
        permission.setPermissionId(dataStreamDao.querySequence(SEQ_PERMISSION_ID));
        permission.setBuiltIn(RbacConstant.BUILT_IN_NO);
        systemPermissionDao.insertPermission(permission);
        return permission.getPermissionId();
    }

    @Override
    public void modifyPermission(PermissionEntity permission) throws DataStreamException {
        if (permission == null || permission.getPermissionId() == null) {
            throw new DataStreamException("RBAC_PERM_001", "权限ID不能为空");
        }
        validatePermissionRequired(permission);
        checkPermissionCodeUnique(permission.getPermissionCode(), permission.getPermissionId());
        PermissionEntity existing = systemPermissionDao.selectPermissionById(permission.getPermissionId());
        if (existing == null) {
            throw new DataStreamException("RBAC_PERM_002", "权限不存在");
        }
        if (isBuiltIn(existing)) {
            throw new DataStreamException("RBAC_PERM_003", "内置权限不可修改");
        }
        permission.setBuiltIn(existing.getBuiltIn());
        systemPermissionDao.updatePermission(permission);
    }

    @Override
    public void delPermission(Long permissionId) throws DataStreamException {
        if (permissionId == null) {
            throw new DataStreamException("RBAC_PERM_001", "权限ID不能为空");
        }
        PermissionEntity existing = systemPermissionDao.selectPermissionById(permissionId);
        if (existing == null) {
            throw new DataStreamException("RBAC_PERM_002", "权限不存在");
        }
        if (isBuiltIn(existing)) {
            throw new DataStreamException("RBAC_PERM_004", "内置权限不可删除");
        }
        Integer refCount = systemPermissionDao.countPermissionRoleReferenced(permissionId);
        if (refCount != null && refCount > 0) {
            throw new DataStreamException("RBAC_PERM_005", "该权限仍被 " + refCount + " 个角色引用，不能删除");
        }
        systemPermissionDao.deletePermission(permissionId);
    }

    // ==================== 校验与工具 ====================

    private void validatePermissionRequired(PermissionEntity permission) throws DataStreamException {
        if (permission == null) {
            throw new DataStreamException("RBAC_PERM_006", "权限不能为空");
        }
        if (StringUtils.isEmpty(permission.getPermissionCode())) {
            throw new DataStreamException("RBAC_PERM_007", "权限编码不能为空");
        }
        if (StringUtils.isEmpty(permission.getPermissionName())) {
            throw new DataStreamException("RBAC_PERM_008", "权限名称不能为空");
        }
        if (permission.getPermissionType() == null) {
            throw new DataStreamException("RBAC_PERM_009", "权限类型不能为空");
        }
    }

    private void checkPermissionCodeUnique(String permissionCode, Long excludeId) throws DataStreamException {
        Integer count = systemPermissionDao.selectPermissionCodeCount(permissionCode, excludeId);
        if (count != null && count > 0) {
            throw new DataStreamException("RBAC_PERM_010", "权限编码【" + permissionCode + "】已存在");
        }
    }

    private boolean isBuiltIn(PermissionEntity permission) {
        return permission != null && Integer.valueOf(RbacConstant.BUILT_IN_YES).equals(permission.getBuiltIn());
    }
}