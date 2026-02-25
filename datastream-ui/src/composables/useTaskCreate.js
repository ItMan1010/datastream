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
 * 创建任务相关业务逻辑Hook
 */
import { ref, reactive } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { useMessage } from './useMessage'
import { useMainStore } from '@/store/index.js'

export function useTaskCreate() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showSuccess, showError, showWarning, confirm } = useMessage()
  const mainStore = useMainStore()

  // 创建任务抽屉状态
  const createNewTaskDrawer = ref(false)
  const newTaskDrawerDir = ref('btt')

  // 数据源选择弹窗状态
  const dialogTableVisible = ref(false)
  const isSourceSelect = ref(true)
  const uniqueId = ref('_move')

  // ShardingDB标志
  const isShardingDBFlag = ref(false)

  // 表单数据
  const form = reactive({
    taskType: '1',
    sourceObjectName: '',
    dataNodeFlag: 0,
    dataSet: 0,
    priority: 0,
    sourceTableCondition: '',
    targetObjectName: '',
    sourceDataSourceName: '',
    sourceObjectId: null,
    sourceObjectType: null,
    sourceObjectCategory: '',
    sourceDebeziumObject: 3,
    sourceOffsetStorage: 2,
    sourceDebeziumSnapshot:0,
    sourceDataBaseObjectType: 1,
    sourceOffsetKafka: null,
    sourceOffsetStartPos: null,
    targetDataSourceName: '',
    targetObjectCategory: '',
    targetObjectId: null,
    targetObjectType: null,
    targetInsertMode: 1,
    targetCheckFlag: 1,
    checkMode: 1,
    taskDisc: '',
    tableType: 1
  })

  // 初始化表单
  const initForm = () => {
    Object.assign(form, {
      taskType: '1',
      sourceObjectName: '',
      dataNodeFlag: 0,
      dataSet: 0,
      priority: 0,
      sourceTableCondition: '',
      targetObjectName: '',
      sourceDataSourceName: '',
      sourceObjectId: null,
      sourceObjectType: null,
      sourceObjectCategory: '',
      sourceDebeziumObject: 3,
      sourceOffsetStorage: 2,
      sourceDebeziumSnapshot: 0,
      sourceDataBaseObjectType:1,
      sourceOffsetKafka: null,
      sourceOffsetStartPos: null,
      targetDataSourceName: '',
      targetObjectCategory: '',
      targetObjectId: null,
      targetObjectType: null,
      targetInsertMode: 1,
      targetCheckFlag:1,
      checkMode: 1,
      taskDisc: '',
      tableType: 1
    })
    isShardingDBFlag.value = false
  }

  // 打开创建任务抽屉
  const openCreateTaskDrawer = () => {
    initForm()
    createNewTaskDrawer.value = true
  }

  // 关闭创建任务抽屉
  const closeCreateTaskDrawer = () => {
    createNewTaskDrawer.value = false
    initForm()
  }

  // 选择数据源
  const selectDataSource = (isSource) => {
    dialogTableVisible.value = true
    isSourceSelect.value = isSource
  }

  // 处理数据源选择
  const handleDataSourceSelect = (dataSourceRow) => {
    dialogTableVisible.value = false

    if (isSourceSelect.value) {
      form.sourceObjectId = dataSourceRow.dataSourceId
      form.sourceObjectType = dataSourceRow.dataSourceType
      form.sourceDataSourceName = dataSourceRow.dataSourceName
      form.sourceObjectCategory = dataSourceRow.dataSourceCategory
      isShardingDBFlag.value = dataSourceRow.dataSourceType === 1
    } else {
      form.targetObjectId = dataSourceRow.dataSourceId
      form.targetObjectType = dataSourceRow.dataSourceType
      form.targetDataSourceName = dataSourceRow.dataSourceName
      form.targetObjectCategory = dataSourceRow.dataSourceCategory
      isShardingDBFlag.value = dataSourceRow.dataSourceType === 1
    }
  }

  // 输入源表名时同步目标表名
  const inputSourceTableName = () => {
    if(form.sourceObjectCategory === 'database' && form.targetObjectCategory === 'database'){
      form.targetObjectName = form.sourceObjectName + ''
    }
  }

  // 验证表单
  const validateForm = async () => {
    if (!form.sourceObjectId) {
      showWarning('请选择源库数据源')
      return false
    }
    if (!form.sourceObjectName?.trim()) {
      showWarning('请输入源对象名称')
      return false
    }
    if (!form.targetObjectId) {
      showWarning('请选择目标库数据源')
      return false
    }
    if (form.priority < 0) {
      showWarning('请输入任务优先级')
      return false
    }
    if (!form.targetObjectName?.trim()) {
      showWarning('请输入目标对象名称')
      return false
    }
    if (form.sourceTableCondition?.includes(';')) {
      showWarning('数据过滤中包含非法字符【;】！')
      return false
    }
    // Excel文件扩展名验证
    if (form.targetObjectType === 9 && form.targetObjectName?.trim()) {
      const fileName = form.targetObjectName.trim().toLowerCase()
      if (!fileName.endsWith('.xls') && !fileName.endsWith('.xlsx')) {
        try {
          await confirm('目标对象名称未指定Excel文件后缀（.xls或.xlsx），是否继续？')
        } catch {
          return false
        }
      }
    }
    return true
  }

  // 创建任务
  const createDataMoveTask = async (onSuccess) => {
    if (!(await validateForm())) return

    const allowedTaskTypes = ['1', '4', '5', '6']

    if (allowedTaskTypes.includes(form.taskType)) {
      await doCreateTask(onSuccess)
    } else {
      try {
        await confirm(
          `源库(${form.sourceDataSourceName}),表名(${form.sourceObjectName})将做数据删除清理，请确认操作!!）?`
        )
        await doCreateTask(onSuccess)
      } catch {
        // 用户取消
      }
    }
  }

  // 执行创建任务
  const doCreateTask = async (onSuccess) => {
    const request = {
      taskType: form.taskType,
      sourceObjectName: form.sourceObjectName,
      priority: form.priority,
      targetObjectName: form.targetObjectName,
      sourceTableCondition: form.sourceTableCondition,
      systemUserCode: mainStore.getLoginSystemUser.systemUserCode,
      sourceObjectId: form.sourceObjectId,
      sourceObjectType: form.sourceObjectType,
      targetObjectId: form.targetObjectId,
      targetObjectType: form.targetObjectType,
      targetInsertMode: form.targetInsertMode,
      targetCheckFlag: form.targetCheckFlag,
      checkMode: form.checkMode,
      taskDisc: form.taskDisc,
      sourceDebeziumObject: form.sourceDebeziumObject,
      sourceOffsetStorage: form.sourceOffsetStorage,
      sourceDebeziumSnapshot: form.sourceDebeziumSnapshot,
      sourceDataBaseObjectType: form.sourceDataBaseObjectType,
      sourceOffsetKafka: form.sourceOffsetKafka,
      sourceOffsetStartPos: form.sourceOffsetStartPos
    }

    if (isShardingDBFlag.value) {
      request.dataNodeFlag = form.dataNodeFlag
      request.dataSet = form.dataSet
      request.tableType = form.tableType
    }

    try {
      startLoading()
      const res = await http(constant.CREATE_MOVE_TASK, 'post', request)

      if (res.errorCode !== '0') {
        showError(`生成任务失败：${res.errorMsg}`)
        return
      }

      createNewTaskDrawer.value = false
      showSuccess('生成任务成功')
      initForm()

      if (typeof onSuccess === 'function') {
        onSuccess()
      }
    } catch (err) {
      showError(`生成任务失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  return {
    loading,
    createNewTaskDrawer,
    newTaskDrawerDir,
    dialogTableVisible,
    isSourceSelect,
    uniqueId,
    isShardingDBFlag,
    form,
    initForm,
    openCreateTaskDrawer,
    closeCreateTaskDrawer,
    selectDataSource,
    handleDataSourceSelect,
    inputSourceTableName,
    createDataMoveTask
  }
}

