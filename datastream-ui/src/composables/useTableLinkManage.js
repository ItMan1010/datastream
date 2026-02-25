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
 * 表链接管理业务逻辑Hook
 */
import { ref, reactive } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'

export function useTableLinkManage() {
  const { loading, startLoading, stopLoading } = useLoading()
  const pagination = usePagination({ defaultPageSize: 10 })
  const { showSuccess, showError, showWarning, confirm, showSuccessWithCallback } = useMessage()

  // 表链接列表
  const tableData = ref([])

  // 查询表单
  const queryForm = reactive({
    queryFlag: '1',
    queryValue: null
  })

  // 配置页面状态
  const showBusiConfig = ref(false)
  const flowDetail = ref(null)
  const mode = ref(null)

  // 查询标志选项
  const queryFlagOptions = [
    { label: '全部', value: '1' },
    { label: '链接ID', value: '2' },
    { label: '链接名称', value: '3' },
    { label: '表名称', value: '4' }
  ]

  // 切换查询标志
  const changeQueryFlag = () => {
    queryForm.queryValue = null
    if (queryForm.queryFlag === '1') {
      queryTableLink()
    }
  }

  // 查询表链接列表
  const queryTableLink = async () => {
    if (queryForm.queryFlag !== '1' && !queryForm.queryValue) {
      showWarning('查询值不能为空')
      return
    }

    try {
      startLoading()
      const res = await http(constant.QUERY_TABLE_LINK, 'post', {
        queryFlag: queryForm.queryFlag,
        queryValue: queryForm.queryValue,
        page: pagination.pageNum.value,
        count: pagination.pageSize.value
      })

      if (res.errorCode !== '0') {
        showError(`查询明细信息失败：${res.errorMsg}`)
        return
      }

      tableData.value = res.tableLinkList || []
      pagination.setTotal(res.total || 0)
    } catch (err) {
      showError(`查询明细信息失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 新增表链接
  const addFlowDefine = () => {
    mode.value = 'add'
    flowDetail.value = null
    showBusiConfig.value = true
  }

  // 查看表链接详情
  const showFlowDetail = async (row) => {
    try {
      startLoading()
      const res = await http(constant.QUERY_LINK_DETAIL, 'post', {
        tableLinkId: row.tableLinkId
      })

      if (res.errorCode !== '0') {
        showError(`查询链接详情失败：${res.errorMsg}`)
        return
      }

      mode.value = 'view'
      flowDetail.value = res
      flowDetail.value.tableLinkId = row.tableLinkId
      flowDetail.value.state = row.state === 1 ? '下线' : '上线'
      showBusiConfig.value = true
    } catch (err) {
      showError(`查询链接详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 修改表链接
  const modifyFlow = async (row) => {
    try {
      startLoading()
      const res = await http(constant.QUERY_LINK_DETAIL, 'post', {
        tableLinkId: row.tableLinkId
      })

      if (res.errorCode !== '0') {
        showError(`查询链接详情失败：${res.errorMsg}`)
        return
      }

      mode.value = 'modify'
      flowDetail.value = res
      flowDetail.value.flowDefineId = row.flowDefineId
      flowDetail.value.state = row.state === 1 ? '下线' : '上线'
      showBusiConfig.value = true
    } catch (err) {
      showError(`查询链接详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 上线表链接
  const execEff = async (row) => {
    try {
      await confirm(`是否确认上线当前链接（${row.tableLinkName}：${row.tableLinkId}）?`)
      await onOffTableLink(row.tableLinkId, 2, row.tableLinkName)
    } catch {
      // 用户取消
    }
  }

  // 下线表链接
  const execExp = async (row) => {
    try {
      await confirm(`是否确认下线当前流链接（${row.tableLinkName}：${row.tableLinkId}）?`)
      await onOffTableLink(row.tableLinkId, 1, row.tableLinkName)
    } catch {
      // 用户取消
    }
  }

  // 上下线表链接
  const onOffTableLink = async (tableLinkId, state, tableLinkName) => {
    const desc = state === 1 ? '下线' : '上线'

    try {
      startLoading()
      const res = await http(constant.ON_OFF_TABLE_LINK, 'post', {
        tableLinkId,
        state
      })

      if (res.errorCode !== '0') {
        showError(`链接（${tableLinkName}：${tableLinkId}）${desc}操作失败：${res.errorMsg}`)
        return
      }

      showSuccessWithCallback(
        `流程（${tableLinkName}：${tableLinkId}）${desc}操作成功`,
        () => queryTableLink(),
        1000
      )
    } catch (err) {
      showError(`流程（${tableLinkName}：${tableLinkId}）${desc}操作失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 删除表链接
  const deleteTableLink = async (row) => {
    try {
      await confirm(`是否确认删除当前链接（${row.tableLinkName}：${row.tableLinkId}）?`)

      startLoading()
      const res = await http(constant.DEL_TABLE_LINK, 'post', {
        tableLinkId: row.tableLinkId
      })

      if (res.errorCode !== '0') {
        showError(`链接（${row.tableLinkName}：${row.tableLinkId}）删除操作失败：${res.errorMsg}`)
        return
      }

      showSuccessWithCallback(
        `流链接程（${row.tableLinkName}：${row.tableLinkId}）删除操作成功`,
        () => queryTableLink(),
        1000
      )
    } catch (err) {
      if (err !== 'cancel') {
        showError(`链接（${row.tableLinkName}：${row.tableLinkId}）删除操作失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  // 返回列表
  const goBack = () => {
    showBusiConfig.value = false
    queryForm.queryValue = null
    queryForm.queryFlag = '1'
    queryTableLink()
  }

  // 分页处理
  const handleSizeChange = (val) => {
    pagination.pageSize.value = val
    queryTableLink()
  }

  const handleCurrentChange = (val) => {
    pagination.pageNum.value = val
    queryTableLink()
  }

  return {
    loading,
    tableData,
    queryForm,
    pagination,
    showBusiConfig,
    flowDetail,
    mode,
    queryFlagOptions,
    changeQueryFlag,
    queryTableLink,
    addFlowDefine,
    showFlowDetail,
    modifyFlow,
    execEff,
    execExp,
    deleteTableLink,
    goBack,
    handleSizeChange,
    handleCurrentChange
  }
}

