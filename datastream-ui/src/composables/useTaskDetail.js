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
 * 任务详情相关业务逻辑Hook
 */
import { ref, reactive } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { useMessage } from './useMessage'
import {
  getTaskTypeName,
  DATASOURCE_TYPE_DESC,
  SOURCE_LOAD_STRATEGY_DESC,
  SOURCE_DEBEZIUM_OBJECT_DESC,
  SOURCE_OFFSET_STORAGE_DESC,
  PROGRESS_COLORS, SOURCE_DATABASE_OBJECT_TYPE_DESC, TARGET_CHECK_FLAG_DESC, SOURCE_DEBEZIUM_SNAPSHOT_DESC
} from '@/constants/taskConstants'

export function useTaskDetail() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showError } = useMessage()

  // 任务详情抽屉状态
  const moveTaskProgressDrawer = ref(false)

  // 任务详情数据
  const taskDetail = reactive({
    taskId: '',
    taskType: '',
    taskTypeName: '',
    systemUserCode: '',
    createDate: '',
    state: '',
    stateName: '',
    stateDate: '',
    sourceObjectName: '',
    targetObjectName: '',
    hostName: '',
    sourceThreadCount: '',
    targetThreadCount: '',
    sourceObjectKeys: '',
    dataCount: '',
    dataActualCount: '',
    percentage: 0,
    dataMoveTotal: 0,
    dataCheckCount: 0,
    batchTaskId: undefined,
    taskDisc: '',
    sourceDataSourceTypeDesc: '',
    sourceDataSourceDesc: '',
    targetDataSourceTypeDesc: '',
    targetDataSourceDesc: '',
    sourceLoadStrategyDesc: '',
    sourceDebeziumObjectDesc: '',
    sourceOffsetStorageDesc: '',
    sourceDebeziumSnapshotDesc: '',
    sourceOffsetStartPos: '',
    sourceDataBaseObjectTypeDesc: '',
    targetCheckFlagDesc: '',
    errorCode: '',
    errorMsg: ''
  })

  // 任务进度数据
  const taskProgressTableData = ref([])

  // 任务执行列表
  const taskExecuteList = ref([])

  // 进度条颜色
  const customColors = PROGRESS_COLORS

  // 状态名称映射
  const getStateName = (state) => {
    const stateMap = {
      0: '等待运行',
      1: '运行中',
      2: '运行结束',
      3: '运行失败',
      4: '运行暂停'
    }
    return stateMap[state] || '未知状态'
  }

  // 显示任务进度详情
  const showTaskProgress = async (taskRowInfo) => {
    const taskId = taskRowInfo.taskId
    if (!taskId && taskId !== 0) {
      return
    }

    // 初始化任务详情
    Object.assign(taskDetail, {
      taskTypeName: getTaskTypeName(taskRowInfo.taskType),
      sourceDataSourceTypeDesc: DATASOURCE_TYPE_DESC[taskRowInfo.sourceObjectType] || '',
      targetDataSourceTypeDesc: DATASOURCE_TYPE_DESC[taskRowInfo.targetObjectType] || '',
      taskType: taskRowInfo.taskType,
      taskId: taskRowInfo.taskId,
      sourceObjectName: taskRowInfo.sourceObjectName,
      targetObjectName: taskRowInfo.targetObjectName,
      systemUserCode: taskRowInfo.systemUserCode,
      createDate: taskRowInfo.createDate,
      state: taskRowInfo.state,
      stateName: getStateName(taskRowInfo.state),
      stateDate: taskRowInfo.stateDate,
      hostName: taskRowInfo.hostName,
      sourceLoadStrategyDesc: SOURCE_LOAD_STRATEGY_DESC[taskRowInfo.sourceLoadStrategy] || '',
      sourceDebeziumObjectDesc: SOURCE_DEBEZIUM_OBJECT_DESC[taskRowInfo.sourceDebeziumObject] || '',
      sourceOffsetStorageDesc: taskRowInfo.sourceOffsetStorage === 3
        ? SOURCE_OFFSET_STORAGE_DESC[taskRowInfo.sourceOffsetStorage] + "(" + taskRowInfo.sourceOffsetKafka + ")"
        : SOURCE_OFFSET_STORAGE_DESC[taskRowInfo.sourceOffsetStorage] || '',
      sourceDebeziumSnapshotDesc: SOURCE_DEBEZIUM_SNAPSHOT_DESC[taskRowInfo.sourceDebeziumSnapshot] || '',
      sourceOffsetStartPos: taskRowInfo.sourceOffsetStartPos,
      sourceDataBaseObjectTypeDesc: SOURCE_DATABASE_OBJECT_TYPE_DESC[taskRowInfo.sourceDataBaseObjectType] || '',
      targetCheckFlagDesc: TARGET_CHECK_FLAG_DESC[taskRowInfo.targetCheckFlag] || '',
      batchTaskId: taskRowInfo.batchTaskId,
      taskDisc: taskRowInfo.taskDisc,
      sourceObjectKeys: taskRowInfo.sourceObjectKeys,
      errorCode: taskRowInfo.errorCode,
      errorMsg: taskRowInfo.errorMsg
    })

    // 处理数据源描述
    if (taskRowInfo.sourceFileFormat) {
      taskDetail.sourceDataSourceDesc = taskRowInfo.sourceFileFormat?.fileNameFormat || ''
    } else if (taskRowInfo.sourceDataBase) {
      taskDetail.sourceDataSourceDesc = taskRowInfo.sourceDataBase?.dataBaseName || ''
    } else if (taskRowInfo.sourceMQConfig) {
      taskDetail.sourceDataSourceDesc = taskRowInfo.sourceMQConfig?.mqConfigName || ''
    }

    if (taskRowInfo.targetDataBase) {
      taskDetail.targetDataSourceDesc = taskRowInfo.targetDataBase?.dataBaseName || ''
    } else if (taskRowInfo.targetFileFormat) {
      taskDetail.targetDataSourceDesc = taskRowInfo.targetFileFormat?.fileNameFormat || ''
    } else if (taskRowInfo.targetMQConfig) {
      taskDetail.targetDataSourceDesc = taskRowInfo.targetMQConfig?.mqConfigName || ''
    }

    try {
      const res = await http(constant.QUERY_TASK_PROGRESS, 'post', { taskId })

      if (res.errorCode !== '0') {
        showError(`查询任务进度失败：${res.errorMsg}`)
        return
      }

      taskProgressTableData.value = []
      taskExecuteList.value = []
      taskDetail.percentage = 0

      if (taskDetail.taskType === 4) {
        // 结构迁移
        taskDetail.dataMoveTotal = res.tableMoveCount
        taskDetail.dataCount = res.tableMoveDoneCount
        taskDetail.dataActualCount = res.tableMoveActualCount
        const percentage = (Number(taskDetail.dataCount / taskDetail.dataMoveTotal) * 100).toFixed(1)
        taskDetail.percentage = parseFloat(percentage) || 0
      } else {
        // 数据迁移
        taskDetail.dataCheckCount = res.dataCheckCount
        taskDetail.dataMoveTotal = res.sourceObjectCount || 0
        const moveTaskProgressData = res.dataMoveProgressList || []

        if (moveTaskProgressData.length === 1) {
          taskDetail.sourceThreadCount = moveTaskProgressData[0].threadCount
          taskDetail.targetThreadCount = 0
          taskDetail.dataCount = 0
          taskDetail.dataActualCount = 0
        } else if (moveTaskProgressData.length === 2) {
          taskDetail.sourceThreadCount = moveTaskProgressData[0].threadCount
          taskDetail.targetThreadCount = moveTaskProgressData[1].threadCount
          taskDetail.dataCount = moveTaskProgressData[1].dataCount
          taskDetail.dataActualCount = moveTaskProgressData[1].dataActualCount
        }

        if (taskDetail.dataMoveTotal !== 0) {
          const percentage = (Number(taskDetail.dataCount / taskDetail.dataMoveTotal) * 100).toFixed(1)
          taskDetail.percentage = parseFloat(percentage) || 0
        }
      }

      taskProgressTableData.value.push({
        tableName: taskRowInfo.sourceObjectName,
        percentage: taskDetail.percentage,
        movedAmount: taskDetail.dataCount
      })

      taskExecuteList.value = res.taskExecuteList || []
      moveTaskProgressDrawer.value = true
    } catch (err) {
      showError(`查询任务进度失败：${err}`)
    }
  }

  // 是否显示源端加载策略
  const showSourceLoadStrategy = () => {
    return [1, 2, 3].includes(taskDetail.taskType)
  }

  // 是否显示增量同步对象
  const showSourceDebeziumObject = () => {
    return [6].includes(taskDetail.taskType)
  }

  // 判断是否非空
  const isNotEmpty = (value) => {
    return value != null && value !== ''
  }

  return {
    loading,
    moveTaskProgressDrawer,
    taskDetail,
    taskProgressTableData,
    taskExecuteList,
    customColors,
    showTaskProgress,
    showSourceLoadStrategy,
    showSourceDebeziumObject,
    isNotEmpty
  }
}

