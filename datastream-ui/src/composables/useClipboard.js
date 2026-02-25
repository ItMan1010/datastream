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
import { ref } from 'vue'
import { useMessage } from './useMessage'

/**
 * 使用剪贴板
 * @returns {Object} 剪贴板相关状态和方法
 */
export function useClipboard() {
  const { showSuccess, showError } = useMessage()

  const copied = ref(false)
  const text = ref('')

  /**
   * 复制文本到剪贴板
   * @param {string} value - 要复制的文本
   * @param {boolean} showMessage - 是否显示消息提示
   * @returns {Promise<boolean>} 是否复制成功
   */
  const copy = async (value, showMessage = true) => {
    try {
      await navigator.clipboard.writeText(value)
      text.value = value
      copied.value = true

      if (showMessage) {
        showSuccess('复制成功')
      }

      // 3秒后重置状态
      setTimeout(() => {
        copied.value = false
      }, 3000)

      return true
    } catch (err) {
      // 降级方案
      try {
        const textarea = document.createElement('textarea')
        textarea.value = value
        textarea.style.position = 'fixed'
        textarea.style.opacity = '0'
        document.body.appendChild(textarea)
        textarea.select()
        const result = document.execCommand('copy')
        document.body.removeChild(textarea)

        if (result) {
          text.value = value
          copied.value = true

          if (showMessage) {
            showSuccess('复制成功')
          }

          setTimeout(() => {
            copied.value = false
          }, 3000)

          return true
        }
      } catch (e) {
        // 忽略
      }

      if (showMessage) {
        showError('复制失败')
      }

      return false
    }
  }

  /**
   * 从剪贴板读取文本
   * @returns {Promise<string>} 剪贴板内容
   */
  const paste = async () => {
    try {
      const value = await navigator.clipboard.readText()
      text.value = value
      return value
    } catch (err) {
      showError('读取剪贴板失败')
      return ''
    }
  }

  return {
    copied,
    text,
    copy,
    paste
  }
}

