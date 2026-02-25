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
 * 任务管理业务逻辑Hook
 */
import { ref, reactive, computed } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'
import dayjs from 'dayjs'
import {
  TASK_TYPE_MAP,
  DATASOURCE_TYPE_DESC,
  SOURCE_LOAD_STRATEGY_DESC,
  SOURCE_DEBEZIUM_OBJECT_DESC,
  SOURCE_OFFSET_STORAGE_DESC,
  DATE_PICKER_SHORTCUTS,
  PROGRESS_COLORS,
  getTaskTypeName,
  getTaskStateInfo
} from '@/constants/taskConstants'

export function useTaskManage() {
  const { loading, startLoading, stopLoading } = useLoading()
  const pagination = usePagination({ defaultPageSize: 50 })
  const { showSuccess, showError, showWarning, confirm, showSuccessWithCallback } = useMessage()

  // 任务列表数据
  const dataMoveTaskListData = ref([])

  // 查询表单
  const queryForm = reactive({
    taskId: '',
    queryFlag: '4',
    tableName: '',
    state: '0',
    moveDate: [],
    batchTaskId: '',
    copyTaskId: null,
    taskType: '1'
  })

  // 日期选择器配置
  const pickerOptions = {
    shortcuts: DATE_PICKER_SHORTCUTS
  }

  // 初始化日期范围（最近一周）
  const initDateRange = () => {
    const start = new Date()
    start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
    start.setHours(0, 0, 0, 0)
    const end = new Date()
    end.setHours(23, 59, 59, 999)
    queryForm.moveDate = [start, end]
  }

  // 查询任务列表
  const queryDataMoveTaskList = async () => {
    let request = {
      queryFlag: queryForm.queryFlag
    }

    // 根据查询标志构建请求参数
    switch (queryForm.queryFlag) {
      case '1':
        if (!queryForm.taskId) {
          showWarning('任务标识不能为空')
          return
        }
        request.taskId = queryForm.taskId
        break
      case '2':
        if (!queryForm.tableName?.trim()) {
          showWarning('迁移对象名不能为空')
          return
        }
        request.tableName = queryForm.tableName
        request.page = pagination.pageNum.value
        request.count = pagination.pageSize.value
        break
      case '3':
        if (!queryForm.state) {
          showWarning('请选择数据迁移状态')
          return
        }
        request.state = queryForm.state
        request.page = pagination.pageNum.value
        request.count = pagination.pageSize.value
        break
      case '4':
        const beginDate = queryForm.moveDate[0]
        const endDate = queryForm.moveDate[1]
        if (!beginDate || !endDate) {
          showWarning('请选择时间范围')
          return
        }
        if (beginDate.getTime() > endDate.getTime()) {
          showWarning('开始时间不能大于结束时间')
          return
        }
        request.beginDate = dayjs(beginDate).format('YYYYMMDDHHmmss')
        request.endDate = dayjs(endDate).format('YYYYMMDDHHmmss')
        request.page = pagination.pageNum.value
        request.count = pagination.pageSize.value
        break
      case '5':
        if (!queryForm.batchTaskId) {
          showWarning('批次ID不能为空')
          return
        }
        request.batchTaskId = queryForm.batchTaskId
        request.page = pagination.pageNum.value
        request.count = pagination.pageSize.value
        break
      case '6':
        if (!queryForm.copyTaskId) {
          showWarning('复制任务标识不能为空')
          return
        }
        request.copyTaskId = queryForm.copyTaskId
        request.page = pagination.pageNum.value
        request.count = pagination.pageSize.value
        break
      case '7':
        request.taskType = queryForm.taskType
        request.page = pagination.pageNum.value
        request.count = pagination.pageSize.value
        break
    }

    try {
      startLoading()
      const res = await http(constant.QUERY_DATA_MOVE_TASK_LIST, 'post', request)

      if (res.errorCode !== '0') {
        showError(`查询任务失败：${res.errorMsg}`)
        return
      }

      const dataMoveTaskList = res.dataMoveTaskList || []
      dataMoveTaskList.forEach(item => {
        if (item.state === 0 || item.state === 1) {
          item.operIcon = 'el-icon-video-pause'
        } else if (item.state === 3 || item.state === 4) {
          item.operIcon = 'el-icon-video-play'
        }
      })

      dataMoveTaskListData.value = dataMoveTaskList
      pagination.setTotal(res.count)
    } catch (err) {
      showError(`查询任务失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 操作任务（暂停/重启/复制）
  const operateDataMoveTask = async (taskInfo, operate) => {
    const taskId = taskInfo.taskId
    if (!taskId || taskId < 1) {
      showWarning('任务暂停、重启、复制时，任务标识不能为空')
      return
    }

    if (![1, 2, 3].includes(operate)) {
      showWarning('处理类型输入有误')
      return
    }

    const operateDescArr = ['暂停', '重启', '复制']
    const operMsg = operateDescArr[operate - 1]

    try {
      const res = await http(constant.OPERATE_DATA_MOVE_TASK, 'post', { taskId, operate })

      if (res.errorCode !== '0') {
        showError(`${operMsg}任务失败：${res.errorMsg}`)
        return
      }

      queryDataMoveTaskList()

      if (operate === 3) {
        showSuccess(`${operMsg}任务（${taskId}）成功`)
      }
    } catch (err) {
      showError(`${operMsg}任务失败：${err}`)
    }
  }

  // 确认复制任务
  const operateDataMoveTaskCopy = async (taskInfo) => {
    try {
      await confirm('确认需要复制该任务?')
      await operateDataMoveTask(taskInfo, 3)
    } catch {
      // 用户取消
    }
  }

  // 分页处理
  const handleSizeChange = (val) => {
    pagination.pageSize.value = val
    queryDataMoveTaskList()
  }

  const handleCurrentChange = (val) => {
    pagination.pageNum.value = val
    queryDataMoveTaskList()
  }

  // 是否显示分页
  const showPagination = computed(() => {
    return ['2', '3', '4'].includes(queryForm.queryFlag)
  })

  return {
    // 状态
    loading,
    dataMoveTaskListData,
    queryForm,
    pickerOptions,
    pagination,
    showPagination,

    // 常量
    TASK_TYPE_MAP,
    DATASOURCE_TYPE_DESC,
    SOURCE_LOAD_STRATEGY_DESC,
    SOURCE_DEBEZIUM_OBJECT_DESC,
    SOURCE_OFFSET_STORAGE_DESC,
    PROGRESS_COLORS,

    // 工具函数
    getTaskTypeName,
    getTaskStateInfo,

    // 方法
    initDateRange,
    queryDataMoveTaskList,
    operateDataMoveTask,
    operateDataMoveTaskCopy,
    handleSizeChange,
    handleCurrentChange
  }
}

