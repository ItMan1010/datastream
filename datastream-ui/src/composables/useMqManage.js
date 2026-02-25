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
 * Mq配置管理业务逻辑Hook
 */
import { ref, reactive } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'

export function useMqManage() {
  const { loading, startLoading, stopLoading } = useLoading()
  const pagination = usePagination({ defaultPageSize: 10 })
  const { showSuccess, showError, showWarning, confirm, showSuccessWithCallback } = useMessage()

  // Mq配置列表
  const mqConfigList = ref([])

  // 查询表单
  const queryForm = reactive({
    queryFlag: '1',
    queryValue: null
  })

  // 详情弹窗状态
  const mqDetailVisible = ref(false)
  const mqConfigDetail = ref(null)
  const mode = ref(null)

  // 查询标志选项
  const queryFlagOptions = [
    { label: '全部', value: '1' },
    { label: 'Mq ID', value: '2' },
    { label: '实例名称', value: '3' }
  ]

  // 切换查询标志
  const changeQueryFlag = () => {
    queryForm.queryValue = null
    if (queryForm.queryFlag === '1') {
      queryMqRows()
    }
  }

  // 查询Mq配置列表
  const queryMqRows = async () => {
    if (queryForm.queryFlag !== '1' && !queryForm.queryValue) {
      showWarning('查询值不能为空')
      return
    }

    try {
      startLoading()
      const res = await http(constant.QUERY_MQ_ROWS, 'post', {
        queryFlag: queryForm.queryFlag,
        queryValue: queryForm.queryValue,
        page: pagination.pageNum.value,
        count: pagination.pageSize.value
      })

      if (res.errorCode !== '0') {
        showError(`查询Mq配置失败：${res.errorMsg}`)
        return
      }

      mqConfigList.value = res.mqConfigList || []
      pagination.setTotal(res.total || 0)
    } catch (err) {
      showError(`查询Mq配置失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 新增Mq配置
  const addMqConfig = () => {
    mode.value = 'add'
    mqConfigDetail.value = null
    mqDetailVisible.value = true
  }

  // 查看Mq配置详情
  const showMqDetail = async (row) => {
    try {
      startLoading()
      const res = await http(constant.QUERY_MQ_INFO, 'post', {
        mqConfigId: row.mqConfigId
      })

      if (res.errorCode !== '0') {
        showError(`查询详情失败：${res.errorMsg}`)
        return
      }

      mode.value = 'detail'
      mqConfigDetail.value = res.mqConfig
      mqDetailVisible.value = true
    } catch (err) {
      showError(`查询详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 修改Mq配置
  const modifyMqConfig = async (row) => {
    try {
      startLoading()
      const res = await http(constant.QUERY_MQ_INFO, 'post', {
        mqConfigId: row.mqConfigId
      })

      if (res.errorCode !== '0') {
        showError(`查询详情失败：${res.errorMsg}`)
        return
      }

      mode.value = 'modify'
      mqConfigDetail.value = res.mqConfig
      mqDetailVisible.value = true
    } catch (err) {
      showError(`查询详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 删除Mq配置
  const deleteMqConfig = async (row) => {
    try {
      await confirm(`是否确认删除Mq配置（${row.mqConfigName}：${row.mqConfigId}）?`)

      startLoading()
      const res = await http(constant.DEL_MQ_CONFIG, 'post', {
        mqConfigId: row.mqConfigId
      })

      if (res.errorCode !== '0') {
        showError(`删除Mq配置失败：${res.errorMsg}`)
        return
      }

      showSuccessWithCallback(
        `Mq配置（${row.mqName}）删除成功`,
        () => queryMqRows(),
        1000
      )
    } catch (err) {
      if (err !== 'cancel') {
        showError(`删除Mq配置失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  // 测试Mq连接
  const testMqConfig = async (row) => {
    try {
      startLoading()
      const res = await http(constant.TEST_MQ_CONFIG, 'post', {
        mqConfigId: row.mqConfigId
      })

      if (res.errorCode !== '0') {
        showError(`Mq连接测试失败：${res.errorMsg}`)
        return
      }

      showSuccess(`Mq（${row.mqConfigName}）连接测试成功`)
    } catch (err) {
      showError(`Mq连接测试失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 关闭详情
  const closeDetail = (visible) => {
    mqDetailVisible.value = visible
    if (!visible) {
      queryMqRows()
    }
  }

  // 上线文件
  const execOnLine = async (row) => {
    try {
      await confirm(`是否确认上线操作（${row.mqConfigName}：${row.mqConfigId}）?`)
      await onOffLine(row, 2)
    } catch {
      // 用户取消
    }
  }

  // 下线文件
  const execOffLine = async (row) => {
    try {
      await confirm(`是否确认下线操作（${row.mqConfigName}：${row.mqConfigId}）?`)
      await onOffLine(row, 3)
    } catch {
      // 用户取消
    }
  }

  // 上下线文件
  const onOffLine = async (row, action) => {
    try {
      startLoading()
      const res = await http(constant.OPERATE_MQ_CONFIG, 'post', {
        mqConfigId: row.mqConfigId,
        action
      })

      if (res.errorCode !== '0') {
        showError(`配置(${row.mqConfigName}：${row.mqConfigId})操作失败：${res.errorMsg}`)
        return
      }

      showSuccessWithCallback(
        `配置(${row.mqConfigName}：${row.mqConfigId})操作成功`,
        () => queryMqRows(),
        1000
      )
    } catch (err) {
      showError(`配置(${row.mqConfigName}：${row.mqConfigId})操作失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  return {
    loading,
    mqConfigList,
    queryForm,
    pagination,
    mqDetailVisible,
    mqConfigDetail,
    mode,
    queryFlagOptions,
    changeQueryFlag,
    queryMqRows,
    addMqConfig,
    showMqDetail,
    modifyMqConfig,
    deleteMqConfig,
    testMqConfig,
    closeDetail,
    execOnLine,
    execOffLine
  }
}

