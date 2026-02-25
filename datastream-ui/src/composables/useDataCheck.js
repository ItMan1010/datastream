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
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { useMessage } from './useMessage'

export function useDataCheck() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showSuccess, showError } = useMessage()

  // 稽核抽屉状态
  const dataCheckDrawer = ref(false)

  // 稽核数据列表
  const dataCheckListData = ref([])

  // 当前稽核任务ID
  const currentDataCheckTaskId = ref(null)

  // 查询数据稽核
  const handleDataCheck = async (taskId) => {
    dataCheckListData.value = []
    currentDataCheckTaskId.value = taskId

    try {
      startLoading()
      const res = await http(constant.QUERY_DATA_CHECK, 'post', { taskId })

      if (res.errorCode !== '0') {
        showError(`查询数据稽核出错：${res.errorMsg}`)
        return
      }

      const dataCheckList = res.dataCheckList || []

      dataCheckList.forEach(item => {
        // 状态描述
        const stateDescMap = {
          1: '稽核生成',
          2: '修订成功',
          3: '修订失败'
        }
        item.stateDesc = stateDescMap[item.state] || '未知状态'

        // 差异结果描述
        const checkResultDescMap = {
          1: '源数据多',
          2: '数据不一致',
          3: '目标数据多'
        }
        item.checkResultDesc = checkResultDescMap[item.checkResult] || '未知结果'
      })

      dataCheckListData.value = dataCheckList
      dataCheckDrawer.value = true
    } catch (err) {
      showError(`查询数据稽核出错：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 修订数据稽核
  const repairDataCheck = async (checkType, taskId, dataCheckId) => {
    try {
      startLoading()
      const res = await http(constant.REPAIR_DATA_CHECK, 'post', {
        dataCheckId,
        checkType,
        taskId
      })

      if (res.errorCode !== '0') {
        showError(`修订差异数据出错：${res.errorMsg}`)
        return
      }

      showSuccess('触发修订完成')
      // 刷新稽核数据列表
      await handleDataCheck(taskId)
    } catch (err) {
      showError(`修订差异数据出错：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 关闭稽核抽屉
  const closeDataCheckDrawer = () => {
    dataCheckDrawer.value = false
    dataCheckListData.value = []
  }

  return {
    loading,
    dataCheckDrawer,
    dataCheckListData,
    currentDataCheckTaskId,
    handleDataCheck,
    repairDataCheck,
    closeDataCheckDrawer
  }
}

