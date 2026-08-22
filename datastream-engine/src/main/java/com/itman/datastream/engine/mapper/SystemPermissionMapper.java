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
package com.itman.datastream.engine.mapper;

import com.itman.datastream.common.entity.PermissionEntity;
import com.itman.datastream.common.entity.RoleEntity;
import com.itman.datastream.common.entity.RolePermissionEntity;
import com.itman.datastream.common.entity.SystemUserEntity;
import com.itman.datastream.common.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * 权限管理系统 Mapper。
 * 覆盖用户、角色、权限三张主表与用户-角色、角色-权限两张关联表的 CRUD 与授权查询。
 */
@Mapper
public interface SystemPermissionMapper {

    // ---- 系统用户 data_stream_system_user ----

    List<SystemUserEntity> selectSystemUserList(@Param("queryFlag") Integer queryFlag,
                                                @Param("queryValue") String queryValue,
                                                @Param("sqlLimit") String sqlLimit) throws DataAccessException;

    Integer selectSystemUserListCount(@Param("queryFlag") Integer queryFlag,
                                      @Param("queryValue") String queryValue) throws DataAccessException;

    SystemUserEntity selectSystemUserById(@Param("systemUserId") Long systemUserId) throws DataAccessException;

    SystemUserEntity selectSystemUserByCode(@Param("systemUserCode") String systemUserCode) throws DataAccessException;

    Integer selectSystemUserCodeCount(@Param("systemUserCode") String systemUserCode,
                                      @Param("excludeId") Long excludeId) throws DataAccessException;

    Integer insertSystemUser(@Param("user") SystemUserEntity user) throws DataAccessException;

    Integer updateSystemUser(@Param("user") SystemUserEntity user) throws DataAccessException;

    Integer updateSystemUserPassword(@Param("systemUserId") Long systemUserId,
                                     @Param("password") String password) throws DataAccessException;

    Integer updateSystemUserState(@Param("systemUserId") Long systemUserId,
                                  @Param("state") Integer state) throws DataAccessException;

    Integer deleteSystemUser(@Param("systemUserId") Long systemUserId) throws DataAccessException;

    // ---- 角色 data_stream_role ----

    List<RoleEntity> selectRoleList(@Param("queryFlag") Integer queryFlag,
                                    @Param("queryValue") String queryValue,
                                    @Param("sqlLimit") String sqlLimit) throws DataAccessException;

    Integer selectRoleListCount(@Param("queryFlag") Integer queryFlag,
                                @Param("queryValue") String queryValue) throws DataAccessException;

    RoleEntity selectRoleById(@Param("roleId") Long roleId) throws DataAccessException;

    RoleEntity selectRoleByCode(@Param("roleCode") String roleCode) throws DataAccessException;

    Integer selectRoleCodeCount(@Param("roleCode") String roleCode,
                                @Param("excludeId") Long excludeId) throws DataAccessException;

    Integer selectRoleNameCount(@Param("roleName") String roleName,
                                @Param("excludeId") Long excludeId) throws DataAccessException;

    Integer insertRole(@Param("role") RoleEntity role) throws DataAccessException;

    Integer updateRole(@Param("role") RoleEntity role) throws DataAccessException;

    Integer deleteRole(@Param("roleId") Long roleId) throws DataAccessException;

    Integer countRoleUserReferenced(@Param("roleId") Long roleId) throws DataAccessException;

    // ---- 用户-角色 data_stream_user_role ----

    Integer insertUserRole(@Param("userRole") UserRoleEntity userRole) throws DataAccessException;

    Integer deleteUserRoleByUserId(@Param("systemUserId") Long systemUserId) throws DataAccessException;

    Integer deleteUserRoleByRoleId(@Param("roleId") Long roleId) throws DataAccessException;

    List<RoleEntity> selectRolesByUserId(@Param("systemUserId") Long systemUserId) throws DataAccessException;

    List<String> selectRoleCodesByUserId(@Param("systemUserId") Long systemUserId) throws DataAccessException;

    // ---- 权限资源 data_stream_permission ----

    List<PermissionEntity> selectPermissionList(@Param("permissionType") Integer permissionType,
                                                @Param("sqlLimit") String sqlLimit) throws DataAccessException;

    Integer selectPermissionListCount(@Param("permissionType") Integer permissionType) throws DataAccessException;

    List<PermissionEntity> selectAllPermission() throws DataAccessException;

    PermissionEntity selectPermissionById(@Param("permissionId") Long permissionId) throws DataAccessException;

    Integer selectPermissionCodeCount(@Param("permissionCode") String permissionCode,
                                      @Param("excludeId") Long excludeId) throws DataAccessException;

    Integer insertPermission(@Param("permission") PermissionEntity permission) throws DataAccessException;

    Integer updatePermission(@Param("permission") PermissionEntity permission) throws DataAccessException;

    Integer deletePermission(@Param("permissionId") Long permissionId) throws DataAccessException;

    Integer countPermissionRoleReferenced(@Param("permissionId") Long permissionId) throws DataAccessException;

    // ---- 角色-权限 data_stream_role_permission ----

    Integer insertRolePermission(@Param("rolePermission") RolePermissionEntity rolePermission) throws DataAccessException;

    Integer deleteRolePermissionByRoleId(@Param("roleId") Long roleId) throws DataAccessException;

    List<PermissionEntity> selectPermissionsByRoleId(@Param("roleId") Long roleId) throws DataAccessException;

    List<String> selectPermissionCodesByUserId(@Param("systemUserId") Long systemUserId) throws DataAccessException;

    List<String> selectMenuRoutesByUserId(@Param("systemUserId") Long systemUserId) throws DataAccessException;
}