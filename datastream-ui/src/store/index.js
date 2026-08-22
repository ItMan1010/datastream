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
import { defineStore } from 'pinia'

export const useMainStore = defineStore('main', {
  state: () => ({
    // 登陆信息
    loginSystemUser: {},
    // 角色编码集合
    loginRoles: [],
    // 权限编码集合
    loginPermissions: [],
    // 允许访问的菜单路由列表
    loginMenus: [],
    // 通用查询参数
    commQueryParams: null,
    // 暗黑模式状态
    isDarkMode: false
  }),

  getters: {
    getLoginSystemUser: (state) => {
      if (state.loginSystemUser.systemUserCode) {
        return state.loginSystemUser
      }
      return localStorage.getItem('SYSTEM_USER_INFO') ? JSON.parse(localStorage.getItem('SYSTEM_USER_INFO')) : {}
    },
    getLoginRoles: (state) => {
      if (state.loginRoles && state.loginRoles.length) {
        return state.loginRoles
      }
      const stored = localStorage.getItem('SYSTEM_USER_ROLES')
      return stored ? JSON.parse(stored) : []
    },
    getLoginPermissions: (state) => {
      if (state.loginPermissions && state.loginPermissions.length) {
        return state.loginPermissions
      }
      const stored = localStorage.getItem('SYSTEM_USER_PERMISSIONS')
      return stored ? JSON.parse(stored) : []
    },
    getLoginMenus: (state) => {
      if (state.loginMenus && state.loginMenus.length) {
        return state.loginMenus
      }
      const stored = localStorage.getItem('SYSTEM_USER_MENUS')
      return stored ? JSON.parse(stored) : []
    },
    getIsAdmin: (state) => {
      const roles = state.loginRoles && state.loginRoles.length
        ? state.loginRoles
        : (localStorage.getItem('SYSTEM_USER_ROLES') ? JSON.parse(localStorage.getItem('SYSTEM_USER_ROLES')) : [])
      return roles.includes('SYSTEM_ADMIN')
    }
  },

  actions: {
    setLoginSystemUser(systemUser) {
      const user = systemUser || {}
      this.loginSystemUser = user
      this.loginRoles = user.roles || []
      this.loginPermissions = user.permissions || []
      this.loginMenus = user.menus || []
      localStorage.setItem('SYSTEM_USER_INFO', JSON.stringify(user))
      localStorage.setItem('SYSTEM_USER_ROLES', JSON.stringify(this.loginRoles))
      localStorage.setItem('SYSTEM_USER_PERMISSIONS', JSON.stringify(this.loginPermissions))
      localStorage.setItem('SYSTEM_USER_MENUS', JSON.stringify(this.loginMenus))
    },

    setCommQueryParams(params) {
      this.commQueryParams = params
    },

    setDarkMode(isDark) {
      this.isDarkMode = isDark
      localStorage.setItem('datastream-dark-mode', isDark.toString())
    },

    toggleDarkMode() {
      this.setDarkMode(!this.isDarkMode)
    },

    initDarkMode() {
      const savedMode = localStorage.getItem('datastream-dark-mode')
      if (savedMode !== null) {
        this.isDarkMode = savedMode === 'true'
      }
    }
  }
})
