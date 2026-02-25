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
import { ref, reactive } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import * as commMethod from '@/comm/commMethod.js'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'

export function useDatabaseManage() {
  const { loading, startLoading, stopLoading } = useLoading()
  const pagination = usePagination({ defaultPageSize: 10 })
  const { showSuccess, showError, confirm, showSuccessWithCallback } = useMessage()

  // 数据库列表
  const dataBaseListData = ref([])

  // 查询表单
  const queryForm = reactive({
    dataBaseType: '0'
  })

  // 详情弹窗状态
  const dataBaseDetailVisible = ref(false)
  const mode = ref('add')
  const dataBaseInfo = ref({})

  // 查询数据库列表
  const queryDataBaseRows = async () => {
    try {
      dataBaseListData.value = []

      const request = {
        page: pagination.pageNum.value,
        count: pagination.pageSize.value
      }

      if (queryForm.dataBaseType !== '0') {
        request.queryFlag = 2
        request.dataBaseType = queryForm.dataBaseType
      } else {
        request.queryFlag = 1
      }

      startLoading()
      const res = await http(constant.QUERY_DATA_BASE_ROWS, 'post', request)

      if (res.errorCode !== '0') {
        showError(`查询数据库记录失败：${res.errorMsg}`)
        return
      }

      dataBaseListData.value = res.dataBaseList || []
      pagination.setTotal(res.total || 0)

      // 处理数据
      dataBaseListData.value.forEach(item => {
        item.addr = commMethod.extractAddress(item.url)
        item.encryptPassWord = commMethod.getEncryptPassWord(item.passWordLength)
        item.showPassWord = item.encryptPassWord
      })
    } catch (err) {
      showError(`查询数据库记录失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 新增数据库
  const addDataBase = () => {
    dataBaseDetailVisible.value = true
    mode.value = 'add'
    dataBaseInfo.value = {}
  }

  // 查看数据库详情
  const showDataBaseDetail = (row) => {
    dataBaseDetailVisible.value = true
    mode.value = 'detail'
    dataBaseInfo.value = row
  }

  // 修改数据库
  const modifyDataBase = (row) => {
    dataBaseDetailVisible.value = true
    mode.value = 'modify'
    dataBaseInfo.value = row
  }

  // 上线数据库
  const execEff = async (row) => {
    try {
      await confirm(`是否确认上线当前数据库(${row.dataBaseName})?`)
      await onOffDataBase(row.dataBaseId, 2, row.dataBaseName)
    } catch {
      // 用户取消
    }
  }

  // 下线数据库
  const execExp = async (row) => {
    try {
      await confirm(`是否确认下线当前数据库(${row.dataBaseName})?`)
      await onOffDataBase(row.dataBaseId, 1, row.dataBaseName)
    } catch {
      // 用户取消
    }
  }

  // 上下线数据库
  const onOffDataBase = async (dataSourceId, state, dataSourceName) => {
    const opt = state === 1 ? '下线' : '上线'

    try {
      startLoading()
      const res = await http(constant.ON_OFF_DATA_BASE, 'post', {
        dataBaseId: dataSourceId,
        state
      })

      if (res.errorCode !== '0') {
        showError(`数据库(${dataSourceName})${opt}操作失败：${res.errorMsg}`)
        return
      }

      showSuccessWithCallback(
        `数据库(${dataSourceName})${opt}操作成功`,
        () => queryDataBaseRows(),
        1000
      )
    } catch (err) {
      showError(`数据库(${dataSourceName})${opt}操作失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 删除数据库
  const deleteDataBase = async (row) => {
    try {
      await confirm(`是否确认删除当前数据库(${row.dataBaseName})?`)

      startLoading()
      const res = await http(constant.DEL_DATA_BASE, 'post', {
        dataBaseId: row.dataBaseId
      })

      if (res.errorCode !== '0') {
        showError(`数据库(${row.dataBaseName})删除操作失败：${res.errorMsg}`)
        return
      }

      showSuccessWithCallback(
        `数据库(${row.dataBaseName})删除操作成功`,
        () => queryDataBaseRows(),
        1000
      )
    } catch (err) {
      if (err !== 'cancel') {
        showError(`数据库(${row.dataBaseName})删除操作失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  // 分页处理
  const handleSizeChange = (val) => {
    pagination.pageSize.value = val
    queryDataBaseRows()
  }

  const handleCurrentChange = (val) => {
    pagination.pageNum.value = val
    queryDataBaseRows()
  }

  // 关闭详情弹窗
  const closeDetail = () => {
    dataBaseDetailVisible.value = false
  }

  // 刷新列表
  const refreshList = () => {
    dataBaseDetailVisible.value = false
    queryDataBaseRows()
  }

  return {
    loading,
    dataBaseListData,
    queryForm,
    pagination,
    dataBaseDetailVisible,
    mode,
    dataBaseInfo,
    queryDataBaseRows,
    addDataBase,
    showDataBaseDetail,
    modifyDataBase,
    execEff,
    execExp,
    deleteDataBase,
    handleSizeChange,
    handleCurrentChange,
    closeDetail,
    refreshList
  }
}

