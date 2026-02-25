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

/**
 * 使用异步操作
 * @param {Function} asyncFn - 异步函数
 * @param {Object} options - 配置选项
 * @returns {Object} 异步操作相关状态和方法
 */
export function useAsync(asyncFn, options = {}) {
  const {
    immediate = false,
    initialData = null,
    onSuccess,
    onError
  } = options

  const data = ref(initialData)
  const loading = ref(false)
  const error = ref(null)

  const isReady = computed(() => !loading.value && !error.value)
  const hasError = computed(() => !!error.value)

  /**
   * 执行异步操作
   * @param  {...any} args - 传递给异步函数的参数
   * @returns {Promise} 操作结果
   */
  const execute = async (...args) => {
    loading.value = true
    error.value = null

    try {
      const result = await asyncFn(...args)
      data.value = result
      onSuccess?.(result)
      return result
    } catch (err) {
      error.value = err
      onError?.(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 重置状态
   */
  const reset = () => {
    data.value = initialData
    loading.value = false
    error.value = null
  }

  // 立即执行
  if (immediate) {
    execute()
  }

  return {
    data,
    loading,
    error,
    isReady,
    hasError,
    execute,
    reset
  }
}

/**
 * 使用可重试的异步操作
 * @param {Function} asyncFn - 异步函数
 * @param {Object} options - 配置选项
 * @returns {Object} 异步操作相关状态和方法
 */
export function useRetryAsync(asyncFn, options = {}) {
  const {
    maxRetries = 3,
    retryDelay = 1000,
    ...asyncOptions
  } = options

  const retryCount = ref(0)

  const wrappedFn = async (...args) => {
    let lastError

    for (let i = 0; i <= maxRetries; i++) {
      try {
        retryCount.value = i
        return await asyncFn(...args)
      } catch (err) {
        lastError = err
        if (i < maxRetries) {
          await new Promise(resolve => setTimeout(resolve, retryDelay * (i + 1)))
        }
      }
    }

    throw lastError
  }

  const asyncState = useAsync(wrappedFn, asyncOptions)

  return {
    ...asyncState,
    retryCount
  }
}

