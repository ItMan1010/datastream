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
 * 任务观测相关业务逻辑Hook
 */
import { ref } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { useMessage } from './useMessage'

export function useTaskObserve() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showError } = useMessage()

  // 观测抽屉状态
  const moveTaskInfoDrawer = ref(false)

  // 当前任务类型
  const currentTaskType = ref(null)

  // 当前观测的任务ID
  const currentObservedTaskId = ref(null)

  // 数据迁移观测数据
  const moveTaskInfoListData = ref([])
  const dataSendMode = ref('')
  const queueMaxSize = ref('')
  const queueNumber = ref('')
  const queueRunningSize = ref('')

  // 表结构迁移观测数据
  const tableMoveListData = ref([])

  // 筛选相关
  const dataNodeFilters = ref([])
  const infoFlagFilters = ref([])
  const moveListFilters = ref({})
  const allMoveList = ref([])
  const allMoveListCopy = ref([])

  // 查询数据迁移执行明细
  const queryDataMoveInfoList = async (taskId) => {
    moveListFilters.value = {}
    moveTaskInfoListData.value = []
    dataSendMode.value = ''
    queueMaxSize.value = ''
    queueNumber.value = ''
    queueRunningSize.value = ''
    currentObservedTaskId.value = taskId
    moveTaskInfoDrawer.value = true
    dataNodeFilters.value = []

    try {
      startLoading()
      const res = await http(constant.QUERY_DATA_MOVE_INFO_LIST, 'post', {
        taskId,
        queryFlag: 1
      })

      if (res.errorCode !== '0') {
        showError(`查询任务执行明细失败：${res.errorMsg}`)
        return
      }

      dataSendMode.value = res.dataSendMode === 1 ? '异步' : '同步'
      queueMaxSize.value = res.queueMaxSize
      queueNumber.value = res.queueNumber
      queueRunningSize.value = res.queueRunningSize

      const moveTaskInfoList = res.dataMoveInfoList || []
      const dataNodeList = []
      infoFlagFilters.value = [
        { text: '源端线程', value: '源端线程' },
        { text: '目标端线程', value: '目标端线程' }
      ]

      let i = 1
      moveTaskInfoList.forEach(item => {
        item.index = i++

        if (item.dataNode && !dataNodeList.includes(item.dataNode)) {
          dataNodeList.push(item.dataNode)
        }

        if (item.state === 0 || item.state === 1) {
          item.operIcon = 'el-icon-video-pause'
        } else if (item.state === 3 || item.state === 4) {
          item.operIcon = 'el-icon-video-play'
        }

        item.infoFlagDesc = item.infoFlag === 1 ? '源端线程' : '目标端线程'
      })

      allMoveList.value = moveTaskInfoList
      allMoveListCopy.value = moveTaskInfoList.slice()
      moveTaskInfoListData.value = moveTaskInfoList

      dataNodeList.forEach(item => {
        dataNodeFilters.value.push({
          text: item,
          value: item
        })
      })
    } catch (err) {
      showError(`查询任务执行明细失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 查询表结构迁移明细
  const queryTableMoveList = async (taskId) => {
    tableMoveListData.value = []
    moveTaskInfoDrawer.value = true

    try {
      startLoading()
      const res = await http(constant.QUERY_TABLE_MOVE_LIST, 'post', { taskId })

      if (res.errorCode !== '0') {
        showError(`查询表结构迁移明细失败：${res.errorMsg}`)
        return
      }

      const tableMoveList = res.moveTableList || []
      let i = 1

      tableMoveList.forEach(item => {
        item.index = i++
        // 格式化状态显示
        const stateDescMap = {
          0: '等待迁移',
          1: '迁移中',
          2: '迁移结束',
          3: '迁移失败',
          4: '迁移暂停'
        }
        item.stateDesc = stateDescMap[item.state] || '未知状态'
      })

      tableMoveListData.value = tableMoveList
    } catch (err) {
      showError(`查询表结构迁移明细失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 处理观测
  const handleObserve = (row) => {
    currentTaskType.value = row.taskType
    if (row.taskType === 4) {
      queryTableMoveList(row.taskId)
    } else {
      queryDataMoveInfoList(row.taskId)
    }
  }

  return {
    loading,
    moveTaskInfoDrawer,
    currentTaskType,
    currentObservedTaskId,
    moveTaskInfoListData,
    dataSendMode,
    queueMaxSize,
    queueNumber,
    queueRunningSize,
    tableMoveListData,
    dataNodeFilters,
    infoFlagFilters,
    queryDataMoveInfoList,
    queryTableMoveList,
    handleObserve
  }
}

