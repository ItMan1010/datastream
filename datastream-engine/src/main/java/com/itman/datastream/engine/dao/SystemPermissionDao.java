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
package com.itman.datastream.engine.dao;

import com.itman.datastream.common.entity.PermissionEntity;
import com.itman.datastream.common.entity.RoleEntity;
import com.itman.datastream.common.entity.RolePermissionEntity;
import com.itman.datastream.common.entity.SystemUserEntity;
import com.itman.datastream.common.entity.UserRoleEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.mapper.SystemPermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限管理系统 DAO。
 * 覆盖用户、角色、权限与两张关联表的数据访问，统一异常为 {@link DataStreamException}。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SystemPermissionDao {

    public final SystemPermissionMapper systemPermissionMapper;

    // ---- 系统用户 ----

    public List<SystemUserEntity> selectSystemUserList(Integer queryFlag, String queryValue, String sqlLimit) throws DataStreamException {
        try {
            return systemPermissionMapper.selectSystemUserList(queryFlag, queryValue, sqlLimit);
        } catch (Exception e) {
            log.error("查询系统用户列表失败", e);
            throw new DataStreamException("RBAC_USER_DB_001", "查询系统用户列表失败：" + e.getMessage());
        }
    }

    public Integer selectSystemUserListCount(Integer queryFlag, String queryValue) throws DataStreamException {
        try {
            return systemPermissionMapper.selectSystemUserListCount(queryFlag, queryValue);
        } catch (Exception e) {
            log.error("查询系统用户总数失败", e);
            throw new DataStreamException("RBAC_USER_DB_002", "查询系统用户总数失败：" + e.getMessage());
        }
    }

    public SystemUserEntity selectSystemUserById(Long systemUserId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectSystemUserById(systemUserId);
        } catch (Exception e) {
            log.error("查询系统用户详情失败", e);
            throw new DataStreamException("RBAC_USER_DB_003", "查询系统用户详情失败：" + e.getMessage());
        }
    }

    public SystemUserEntity selectSystemUserByCode(String systemUserCode) throws DataStreamException {
        try {
            return systemPermissionMapper.selectSystemUserByCode(systemUserCode);
        } catch (Exception e) {
            log.error("按账号查询系统用户失败", e);
            throw new DataStreamException("RBAC_USER_DB_004", "按账号查询系统用户失败：" + e.getMessage());
        }
    }

    public Integer selectSystemUserCodeCount(String systemUserCode, Long excludeId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectSystemUserCodeCount(systemUserCode, excludeId);
        } catch (Exception e) {
            log.error("校验系统用户账号唯一性失败", e);
            throw new DataStreamException("RBAC_USER_DB_005", "校验系统用户账号唯一性失败：" + e.getMessage());
        }
    }

    public Integer insertSystemUser(SystemUserEntity user) throws DataStreamException {
        try {
            return systemPermissionMapper.insertSystemUser(user);
        } catch (Exception e) {
            log.error("新增系统用户失败", e);
            throw new DataStreamException("RBAC_USER_DB_006", "新增系统用户失败：" + e.getMessage());
        }
    }

    public Integer updateSystemUser(SystemUserEntity user) throws DataStreamException {
        try {
            return systemPermissionMapper.updateSystemUser(user);
        } catch (Exception e) {
            log.error("修改系统用户失败", e);
            throw new DataStreamException("RBAC_USER_DB_007", "修改系统用户失败：" + e.getMessage());
        }
    }

    public Integer updateSystemUserPassword(Long systemUserId, String password) throws DataStreamException {
        try {
            return systemPermissionMapper.updateSystemUserPassword(systemUserId, password);
        } catch (Exception e) {
            log.error("重置系统用户密码失败", e);
            throw new DataStreamException("RBAC_USER_DB_008", "重置系统用户密码失败：" + e.getMessage());
        }
    }

    public Integer updateSystemUserState(Long systemUserId, Integer state) throws DataStreamException {
        try {
            return systemPermissionMapper.updateSystemUserState(systemUserId, state);
        } catch (Exception e) {
            log.error("更新系统用户状态失败", e);
            throw new DataStreamException("RBAC_USER_DB_009", "更新系统用户状态失败：" + e.getMessage());
        }
    }

    public Integer deleteSystemUser(Long systemUserId) throws DataStreamException {
        try {
            return systemPermissionMapper.deleteSystemUser(systemUserId);
        } catch (Exception e) {
            log.error("删除系统用户失败", e);
            throw new DataStreamException("RBAC_USER_DB_010", "删除系统用户失败：" + e.getMessage());
        }
    }

    // ---- 角色 ----

    public List<RoleEntity> selectRoleList(Integer queryFlag, String queryValue, String sqlLimit) throws DataStreamException {
        try {
            return systemPermissionMapper.selectRoleList(queryFlag, queryValue, sqlLimit);
        } catch (Exception e) {
            log.error("查询角色列表失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_001", "查询角色列表失败：" + e.getMessage());
        }
    }

    public Integer selectRoleListCount(Integer queryFlag, String queryValue) throws DataStreamException {
        try {
            return systemPermissionMapper.selectRoleListCount(queryFlag, queryValue);
        } catch (Exception e) {
            log.error("查询角色总数失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_002", "查询角色总数失败：" + e.getMessage());
        }
    }

    public RoleEntity selectRoleById(Long roleId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectRoleById(roleId);
        } catch (Exception e) {
            log.error("查询角色详情失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_003", "查询角色详情失败：" + e.getMessage());
        }
    }

    public RoleEntity selectRoleByCode(String roleCode) throws DataStreamException {
        try {
            return systemPermissionMapper.selectRoleByCode(roleCode);
        } catch (Exception e) {
            log.error("按编码查询角色失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_004", "按编码查询角色失败：" + e.getMessage());
        }
    }

    public Integer selectRoleCodeCount(String roleCode, Long excludeId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectRoleCodeCount(roleCode, excludeId);
        } catch (Exception e) {
            log.error("校验角色编码唯一性失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_005", "校验角色编码唯一性失败：" + e.getMessage());
        }
    }

    public Integer selectRoleNameCount(String roleName, Long excludeId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectRoleNameCount(roleName, excludeId);
        } catch (Exception e) {
            log.error("校验角色名称唯一性失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_006", "校验角色名称唯一性失败：" + e.getMessage());
        }
    }

    public Integer insertRole(RoleEntity role) throws DataStreamException {
        try {
            return systemPermissionMapper.insertRole(role);
        } catch (Exception e) {
            log.error("新增角色失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_007", "新增角色失败：" + e.getMessage());
        }
    }

    public Integer updateRole(RoleEntity role) throws DataStreamException {
        try {
            return systemPermissionMapper.updateRole(role);
        } catch (Exception e) {
            log.error("修改角色失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_008", "修改角色失败：" + e.getMessage());
        }
    }

    public Integer deleteRole(Long roleId) throws DataStreamException {
        try {
            return systemPermissionMapper.deleteRole(roleId);
        } catch (Exception e) {
            log.error("删除角色失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_009", "删除角色失败：" + e.getMessage());
        }
    }

    public Integer countRoleUserReferenced(Long roleId) throws DataStreamException {
        try {
            return systemPermissionMapper.countRoleUserReferenced(roleId);
        } catch (Exception e) {
            log.error("统计角色被用户引用次数失败", e);
            throw new DataStreamException("RBAC_ROLE_DB_010", "统计角色被用户引用次数失败：" + e.getMessage());
        }
    }

    // ---- 用户-角色 ----

    public Integer insertUserRole(UserRoleEntity userRole) throws DataStreamException {
        try {
            return systemPermissionMapper.insertUserRole(userRole);
        } catch (Exception e) {
            log.error("新增用户角色关联失败", e);
            throw new DataStreamException("RBAC_UR_DB_001", "新增用户角色关联失败：" + e.getMessage());
        }
    }

    public Integer deleteUserRoleByUserId(Long systemUserId) throws DataStreamException {
        try {
            return systemPermissionMapper.deleteUserRoleByUserId(systemUserId);
        } catch (Exception e) {
            log.error("清理用户角色关联失败", e);
            throw new DataStreamException("RBAC_UR_DB_002", "清理用户角色关联失败：" + e.getMessage());
        }
    }

    public Integer deleteUserRoleByRoleId(Long roleId) throws DataStreamException {
        try {
            return systemPermissionMapper.deleteUserRoleByRoleId(roleId);
        } catch (Exception e) {
            log.error("清理角色用户关联失败", e);
            throw new DataStreamException("RBAC_UR_DB_005", "清理角色用户关联失败：" + e.getMessage());
        }
    }

    public List<RoleEntity> selectRolesByUserId(Long systemUserId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectRolesByUserId(systemUserId);
        } catch (Exception e) {
            log.error("查询用户角色失败", e);
            throw new DataStreamException("RBAC_UR_DB_003", "查询用户角色失败：" + e.getMessage());
        }
    }

    public List<String> selectRoleCodesByUserId(Long systemUserId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectRoleCodesByUserId(systemUserId);
        } catch (Exception e) {
            log.error("查询用户角色编码失败", e);
            throw new DataStreamException("RBAC_UR_DB_004", "查询用户角色编码失败：" + e.getMessage());
        }
    }

    // ---- 权限资源 ----

    public List<PermissionEntity> selectPermissionList(Integer permissionType, String sqlLimit) throws DataStreamException {
        try {
            return systemPermissionMapper.selectPermissionList(permissionType, sqlLimit);
        } catch (Exception e) {
            log.error("查询权限列表失败", e);
            throw new DataStreamException("RBAC_PERM_DB_001", "查询权限列表失败：" + e.getMessage());
        }
    }

    public Integer selectPermissionListCount(Integer permissionType) throws DataStreamException {
        try {
            return systemPermissionMapper.selectPermissionListCount(permissionType);
        } catch (Exception e) {
            log.error("查询权限总数失败", e);
            throw new DataStreamException("RBAC_PERM_DB_002", "查询权限总数失败：" + e.getMessage());
        }
    }

    public List<PermissionEntity> selectAllPermission() throws DataStreamException {
        try {
            return systemPermissionMapper.selectAllPermission();
        } catch (Exception e) {
            log.error("查询全部权限失败", e);
            throw new DataStreamException("RBAC_PERM_DB_003", "查询全部权限失败：" + e.getMessage());
        }
    }

    public PermissionEntity selectPermissionById(Long permissionId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectPermissionById(permissionId);
        } catch (Exception e) {
            log.error("查询权限详情失败", e);
            throw new DataStreamException("RBAC_PERM_DB_004", "查询权限详情失败：" + e.getMessage());
        }
    }

    public Integer selectPermissionCodeCount(String permissionCode, Long excludeId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectPermissionCodeCount(permissionCode, excludeId);
        } catch (Exception e) {
            log.error("校验权限编码唯一性失败", e);
            throw new DataStreamException("RBAC_PERM_DB_005", "校验权限编码唯一性失败：" + e.getMessage());
        }
    }

    public String selectPermissionNameByCode(String permissionCode) throws DataStreamException {
        try {
            return systemPermissionMapper.selectPermissionNameByCode(permissionCode);
        } catch (Exception e) {
            log.error("按编码查询权限名称失败", e);
            throw new DataStreamException("RBAC_PERM_DB_010", "按编码查询权限名称失败：" + e.getMessage());
        }
    }

    public Integer insertPermission(PermissionEntity permission) throws DataStreamException {
        try {
            return systemPermissionMapper.insertPermission(permission);
        } catch (Exception e) {
            log.error("新增权限失败", e);
            throw new DataStreamException("RBAC_PERM_DB_006", "新增权限失败：" + e.getMessage());
        }
    }

    public Integer updatePermission(PermissionEntity permission) throws DataStreamException {
        try {
            return systemPermissionMapper.updatePermission(permission);
        } catch (Exception e) {
            log.error("修改权限失败", e);
            throw new DataStreamException("RBAC_PERM_DB_007", "修改权限失败：" + e.getMessage());
        }
    }

    public Integer deletePermission(Long permissionId) throws DataStreamException {
        try {
            return systemPermissionMapper.deletePermission(permissionId);
        } catch (Exception e) {
            log.error("删除权限失败", e);
            throw new DataStreamException("RBAC_PERM_DB_008", "删除权限失败：" + e.getMessage());
        }
    }

    public Integer countPermissionRoleReferenced(Long permissionId) throws DataStreamException {
        try {
            return systemPermissionMapper.countPermissionRoleReferenced(permissionId);
        } catch (Exception e) {
            log.error("统计权限被角色引用次数失败", e);
            throw new DataStreamException("RBAC_PERM_DB_009", "统计权限被角色引用次数失败：" + e.getMessage());
        }
    }

    // ---- 角色-权限 ----

    public Integer insertRolePermission(RolePermissionEntity rolePermission) throws DataStreamException {
        try {
            return systemPermissionMapper.insertRolePermission(rolePermission);
        } catch (Exception e) {
            log.error("新增角色权限关联失败", e);
            throw new DataStreamException("RBAC_RP_DB_001", "新增角色权限关联失败：" + e.getMessage());
        }
    }

    public Integer deleteRolePermissionByRoleId(Long roleId) throws DataStreamException {
        try {
            return systemPermissionMapper.deleteRolePermissionByRoleId(roleId);
        } catch (Exception e) {
            log.error("清理角色权限关联失败", e);
            throw new DataStreamException("RBAC_RP_DB_002", "清理角色权限关联失败：" + e.getMessage());
        }
    }

    public List<PermissionEntity> selectPermissionsByRoleId(Long roleId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectPermissionsByRoleId(roleId);
        } catch (Exception e) {
            log.error("查询角色权限失败", e);
            throw new DataStreamException("RBAC_RP_DB_003", "查询角色权限失败：" + e.getMessage());
        }
    }

    public List<String> selectPermissionCodesByUserId(Long systemUserId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectPermissionCodesByUserId(systemUserId);
        } catch (Exception e) {
            log.error("查询用户权限编码失败", e);
            throw new DataStreamException("RBAC_RP_DB_004", "查询用户权限编码失败：" + e.getMessage());
        }
    }

    public List<String> selectMenuRoutesByUserId(Long systemUserId) throws DataStreamException {
        try {
            return systemPermissionMapper.selectMenuRoutesByUserId(systemUserId);
        } catch (Exception e) {
            log.error("查询用户菜单路由失败", e);
            throw new DataStreamException("RBAC_RP_DB_005", "查询用户菜单路由失败：" + e.getMessage());
        }
    }
}