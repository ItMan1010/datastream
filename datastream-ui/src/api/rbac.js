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
import http from '@/utils/request'
import constant from '@/comm/constants'

// ============ 用户管理 ============

/**
 * 分页查询系统用户
 * @param {Object} params - queryFlag/queryValue/page/count
 */
export function queryUserRows(params) {
  return http(constant.QUERY_USER_ROWS, 'post', params)
}

/**
 * 查询用户详情（含已派发角色）
 * @param {number} systemUserId - 用户ID
 */
export function queryUserInfo(systemUserId) {
  return http(constant.QUERY_USER_INFO, 'post', { systemUserId })
}

/**
 * 新增用户
 * @param {Object} user - 用户实体（密码为明文）
 * @param {Array<number>} roleIds - 角色ID列表
 */
export function addUser(user, roleIds) {
  return http(constant.ADD_USER, 'post', { user, roleIds })
}

/**
 * 修改用户
 * @param {Object} user - 用户实体
 * @param {Array<number>} roleIds - 角色ID列表
 */
export function modifyUser(user, roleIds) {
  return http(constant.MODIFY_USER, 'post', { user, roleIds })
}

/**
 * 修改用户状态（禁用/启用）
 * @param {number} systemUserId - 用户ID
 * @param {number} state - 0禁用、1启用
 */
export function updateUserState(systemUserId, state) {
  return http(constant.UPDATE_USER_STATE, 'post', { systemUserId, state })
}

/**
 * 重置用户密码
 * @param {number} systemUserId - 用户ID
 * @param {string} password - 新密码（明文）
 */
export function resetUserPassword(systemUserId, password) {
  return http(constant.RESET_USER_PASSWORD, 'post', { systemUserId, password })
}

/**
 * 删除用户
 * @param {number} systemUserId - 用户ID
 */
export function delUser(systemUserId) {
  return http(constant.DEL_USER, 'post', { systemUserId })
}

// ============ 角色管理 ============

/**
 * 分页查询角色
 * @param {Object} params - queryFlag/queryValue/page/count
 */
export function queryRoleRows(params) {
  return http(constant.QUERY_ROLE_ROWS, 'post', params)
}

/**
 * 查询全部角色
 */
export function queryAllRole() {
  return http(constant.QUERY_ALL_ROLE, 'post', {})
}

/**
 * 查询角色详情（含已授权权限）
 * @param {number} roleId - 角色ID
 */
export function queryRoleInfo(roleId) {
  return http(constant.QUERY_ROLE_INFO, 'post', { roleId })
}

/**
 * 新增角色
 * @param {Object} role - 角色实体
 */
export function addRole(role) {
  return http(constant.ADD_ROLE, 'post', { role })
}

/**
 * 修改角色
 * @param {Object} role - 角色实体
 */
export function modifyRole(role) {
  return http(constant.MODIFY_ROLE, 'post', { role })
}

/**
 * 删除角色
 * @param {number} roleId - 角色ID
 */
export function delRole(roleId) {
  return http(constant.DEL_ROLE, 'post', { roleId })
}

/**
 * 角色授权
 * @param {number} roleId - 角色ID
 * @param {Array<number>} permissionIds - 权限ID列表
 */
export function assignRolePermissions(roleId, permissionIds) {
  return http(constant.ASSIGN_ROLE_PERMISSIONS, 'post', { roleId, permissionIds })
}

// ============ 权限资源管理 ============

/**
 * 查询菜单权限树
 */
export function queryMenuTree() {
  return http(constant.QUERY_MENU_TREE, 'post', {})
}

/**
 * 查询数据权限列表
 */
export function queryDataPermissionList() {
  return http(constant.QUERY_DATA_PERMISSION_LIST, 'post', {})
}

/**
 * 新增权限资源
 * @param {Object} permission - 权限实体
 */
export function addPermission(permission) {
  return http(constant.ADD_PERMISSION, 'post', { permission })
}

/**
 * 修改权限资源
 * @param {Object} permission - 权限实体
 */
export function modifyPermission(permission) {
  return http(constant.MODIFY_PERMISSION, 'post', { permission })
}

/**
 * 删除权限资源
 * @param {number} permissionId - 权限ID
 */
export function delPermission(permissionId) {
  return http(constant.DEL_PERMISSION, 'post', { permissionId })
}