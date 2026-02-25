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
    }
  },

  actions: {
    setLoginSystemUser(systemUser) {
      this.loginSystemUser = systemUser
      localStorage.setItem('SYSTEM_USER_INFO', JSON.stringify(systemUser))
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
