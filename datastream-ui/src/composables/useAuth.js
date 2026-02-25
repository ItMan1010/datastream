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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMainStore } from '@/store'
import http from '@/utils/request'
import constant from '@/comm/constants'
import { useMessage } from './useMessage'

/**
 * 认证Hook
 * @returns {Object} 认证相关状态和方法
 */
export function useAuth() {
  const router = useRouter()
  const mainStore = useMainStore()
  const { showSuccess, showError } = useMessage()

  const loading = ref(false)

  // 计算属性：用户信息描述
  const systemUserDesc = computed(() => {
    const systemUser = mainStore.getLoginSystemUser
    return `${systemUser.systemUserName || ''}（${systemUser.systemUserCode || ''}）`
  })

  // 计算属性：是否已登录
  const isLoggedIn = computed(() => {
    return !!sessionStorage.getItem('token')
  })

  /**
   * 检查Token是否有效
   * @returns {boolean} Token是否有效
   */
  const checkToken = () => {
    return !!sessionStorage.getItem('token')
  }

  /**
   * 跳转到登录页
   */
  const redirectToLogin = () => {
    router.push({ name: 'login' })
  }

  /**
   * 登出
   */
  const logout = async () => {
    loading.value = true
    try {
      await http(constant.URL_AUTH_LOGOUT, 'post', {})
      // 清空登陆信息
      sessionStorage.removeItem('token')
      mainStore.setLoginSystemUser({})
      redirectToLogin()
      showSuccess('退出登录成功！')
    } catch (err) {
      let errorMsg = '退出失败'
      if (err && typeof err === 'string') {
        errorMsg = err
      } else if (err && err.resultMsg) {
        errorMsg = err.resultMsg
      } else if (err && err.message) {
        errorMsg = err.message
      }
      showError(errorMsg)
    } finally {
      loading.value = false
    }
  }

  /**
   * 验证并跳转
   * 如果未登录则跳转到登录页
   * @returns {boolean} 是否已登录
   */
  const validateAndRedirect = () => {
    if (!checkToken()) {
      redirectToLogin()
      return false
    }
    return true
  }

  return {
    loading,
    systemUserDesc: systemUserDesc,
    isLoggedIn,
    checkToken,
    redirectToLogin,
    logout,
    validateAndRedirect
  }
}

