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
 * 任务日志相关业务逻辑Hook
 */
import { ref } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { useMessage } from './useMessage'

export function useTaskLog() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showError } = useMessage()

  // 日志抽屉状态
  const taskLogDrawer = ref(false)

  // 日志信息
  const taskLogInfo = ref([])

  // 显示任务日志
  const showTaskLog = async (jobType, jobId) => {
    taskLogInfo.value = []

    try {
      startLoading()
      const res = await http(constant.QUERY_JOB_BACK, 'post', { jobType, jobId })

      if (res.errorCode !== '0') {
        showError(`查询任务日志出错：${res.errorMsg}`)
        return
      }

      const logs = res.jobLogbackList || []
      logs.forEach(item => {
        const logDesc = `[${item.jobLogbackId}] [${item.createDate}] `
        if (item.content) {
          const list = item.content.replace(/\\\\t/g, '  ').split(/\\\\n/g)
          list.forEach((line, idx, arr) => {
            arr[idx] = logDesc + line
          })
          taskLogInfo.value = taskLogInfo.value.concat(list)
        }
      })

      taskLogDrawer.value = true
    } catch (err) {
      showError(`查询任务日志出错：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 关闭日志抽屉
  const closeTaskLogDrawer = () => {
    taskLogDrawer.value = false
    taskLogInfo.value = []
  }

  return {
    loading,
    taskLogDrawer,
    taskLogInfo,
    showTaskLog,
    closeTaskLogDrawer
  }
}

