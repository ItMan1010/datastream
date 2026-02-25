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
 * 通用消息提示hook
 * 统一管理消息提示
 */
import { ElMessage, ElMessageBox } from 'element-plus'

export function useMessage() {
  // 成功消息
  const showSuccess = (message, options = {}) => {
    ElMessage.success({
      message,
      duration: 2000,
      ...options
    })
  }

  // 错误消息
  const showError = (message, options = {}) => {
    ElMessage.error({
      message,
      duration: 3000,
      ...options
    })
  }

  // 警告消息
  const showWarning = (message, options = {}) => {
    ElMessage.warning({
      message,
      duration: 2500,
      ...options
    })
  }

  // 信息消息
  const showInfo = (message, options = {}) => {
    ElMessage.info({
      message,
      duration: 2000,
      ...options
    })
  }

  // 确认对话框
  const confirm = (message, title = '提示', options = {}) => {
    return ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      ...options
    })
  }

  // 成功后带回调的消息
  const showSuccessWithCallback = (message, callback, duration = 1000) => {
    ElMessage.success({
      message,
      duration,
      onClose: callback
    })
  }

  // API错误处理
  const handleApiError = (error, prefix = '操作') => {
    let errorMsg = `${prefix}失败`
    if (typeof error === 'string') {
      errorMsg = error
    } else if (error?.errorMsg) {
      errorMsg = `${prefix}失败：${error.errorMsg}`
    } else if (error?.message) {
      errorMsg = `${prefix}失败：${error.message}`
    }
    showError(errorMsg)
  }

  return {
    showSuccess,
    showError,
    showWarning,
    showInfo,
    confirm,
    showSuccessWithCallback,
    handleApiError
  }
}

