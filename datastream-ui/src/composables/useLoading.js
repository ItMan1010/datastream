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
/**
 * 通用加载状态管理hook
 * 用于管理API请求的loading状态
 */
import { ref } from 'vue'

export function useLoading(initialState = false) {
  const loading = ref(initialState)

  const setLoading = (state) => {
    loading.value = state
  }

  const startLoading = () => {
    loading.value = true
  }

  const stopLoading = () => {
    loading.value = false
  }

  /**
   * 包装异步函数，自动管理loading状态
   * @param {Function} fn - 异步函数
   * @returns {Function} - 包装后的函数
   */
  const withLoading = (fn) => {
    return async (...args) => {
      try {
        startLoading()
        return await fn(...args)
      } finally {
        stopLoading()
      }
    }
  }

  return {
    loading,
    setLoading,
    startLoading,
    stopLoading,
    withLoading
  }
}

