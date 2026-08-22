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
import { useMainStore } from '@/store'

/**
 * 权限校验 Composable
 * 基于登录响应中的角色、权限编码、允许菜单路由列表进行前端鉴权。
 */
export function usePermission() {
  const mainStore = useMainStore()

  /**
   * 是否拥有指定角色（系统管理员直接放行）
   * @param {string} roleCode - 角色编码
   */
  const hasRole = (roleCode) => {
    if (isAdmin()) return true
    return mainStore.getLoginRoles.includes(roleCode)
  }

  /**
   * 是否内置系统管理员
   */
  const isAdmin = () => {
    return mainStore.getIsAdmin
  }

  /**
   * 是否拥有指定数据/操作权限编码
   * @param {string} permissionCode - 权限编码，如 task:create
   */
  const hasPermission = (permissionCode) => {
    if (isAdmin()) return true
    return mainStore.getLoginPermissions.includes(permissionCode)
  }

  /**
   * 是否允许访问指定菜单路由
   * @param {string} routeName - 路由名称，如 taskManage
   */
  const hasMenu = (routeName) => {
    if (isAdmin()) return true
    if (!routeName) return true
    return mainStore.getLoginMenus.includes(routeName)
  }

  return {
    hasRole,
    isAdmin,
    hasPermission,
    hasMenu
  }
}