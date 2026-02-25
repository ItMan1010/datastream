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
 * 文件格式管理业务逻辑Hook
 */
import { ref, reactive } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'

export function useFileFormatManage() {
  const { loading, startLoading, stopLoading } = useLoading()
  const pagination = usePagination({ defaultPageSize: 10 })
  const { showSuccess, showError, showWarning, confirm, showSuccessWithCallback } = useMessage()

  // 文件格式列表
  const fileFormatList = ref([])

  // 查询表单
  const queryForm = reactive({
    queryFlag: '1',
    queryValue: null
  })

  // 详情弹窗状态
  const fileDetailVisible = ref(false)
  const fileFormatDetail = ref(null)
  const mode = ref(null)

  // 查询标志选项
  const queryFlagOptions = [
    { label: '全部', value: '1' },
    { label: '文件ID', value: '2' },
    { label: '文件名称', value: '3' }
  ]

  // 切换查询标志
  const changeQueryFlag = () => {
    queryForm.queryValue = null
    if (queryForm.queryFlag === '1') {
      queryFileRows()
    }
  }

  // 查询文件格式列表
  const queryFileRows = async () => {
    if (queryForm.queryFlag !== '1' && !queryForm.queryValue) {
      showWarning('查询值不能为空')
      return
    }

    try {
      startLoading()
      const res = await http(constant.QUERY_FILE_ROWS, 'post', {
        queryFlag: queryForm.queryFlag,
        queryValue: queryForm.queryValue,
        page: pagination.pageNum.value,
        count: pagination.pageSize.value
      })

      if (res.errorCode !== '0') {
        showError(`查询明细失败：${res.errorMsg}`)
        return
      }

      fileFormatList.value = res.fileFormatList || []
      pagination.setTotal(res.total || 0)
    } catch (err) {
      showError(`查询明细失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 新增文件格式
  const addFileDefine = () => {
    mode.value = 'add'
    fileFormatDetail.value = null
    fileDetailVisible.value = true
  }

  // 查看文件格式详情
  const showFileDetail = async (row) => {
    try {
      startLoading()
      const res = await http(constant.QUERY_FILE_INFO, 'post', {
        fileFormatId: row.fileFormatId
      })

      if (res.errorCode !== '0') {
        showError(`查询详情失败：${res.errorMsg}`)
        return
      }

      mode.value = 'detail'
      fileFormatDetail.value = res.fileFormat
      fileDetailVisible.value = true
    } catch (err) {
      showError(`查询详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 修改文件格式
  const modifyFile = async (row) => {
    try {
      startLoading()
      const res = await http(constant.QUERY_FILE_INFO, 'post', {
        fileFormatId: row.fileFormatId
      })

      if (res.errorCode !== '0') {
        showError(`查询详情失败：${res.errorMsg}`)
        return
      }

      mode.value = 'modify'
      fileFormatDetail.value = res.fileFormat
      fileDetailVisible.value = true
    } catch (err) {
      showError(`查询详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 上线文件
  const execEff = async (row) => {
    try {
      await confirm(`是否确认上线当前文件（${row.fileNameFormat}：${row.fileFormatId}）?`)
      await onOffFile(row, 2)
    } catch {
      // 用户取消
    }
  }

  // 下线文件
  const execExp = async (row) => {
    try {
      await confirm(`是否确认下线当前文件（${row.fileNameFormat}：${row.fileFormatId}）?`)
      await onOffFile(row, 3)
    } catch {
      // 用户取消
    }
  }

  // 上下线文件
  const onOffFile = async (row, action) => {
    try {
      startLoading()
      const res = await http(constant.OPERATE_FILE_ROW, 'post', {
        fileFormatId: row.fileFormatId,
        action
      })

      if (res.errorCode !== '0') {
        showError(`文件(${row.fileNameFormat}：${row.fileFormatId})操作失败：${res.errorMsg}`)
        return
      }

      showSuccessWithCallback(
        `文件(${row.fileNameFormat}：${row.fileFormatId})操作成功`,
        () => queryFileRows(),
        1000
      )
    } catch (err) {
      showError(`文件(${row.fileNameFormat}：${row.fileFormatId})操作失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 删除文件
  const deleteFile = async (row) => {
    try {
      await confirm(`是否确认删除当前文件（${row.fileNameFormat}：${row.fileFormatId}）?`)

      startLoading()
      const res = await http(constant.OPERATE_FILE_ROW, 'post', {
        fileFormatId: row.fileFormatId,
        action: 1
      })

      if (res.errorCode !== '0') {
        showError(`链文件（${row.fileNameFormat}：${row.fileFormatId}）删除操作失败：${res.errorMsg}`)
        return
      }

      showSuccessWithCallback(
        `文件（${row.fileNameFormat}：${row.fileFormatId}）删除操作成功`,
        () => queryFileRows(),
        1000
      )
    } catch (err) {
      if (err !== 'cancel') {
        showError(`文件（${row.fileNameFormat}：${row.fileFormatId}）删除操作失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  // 文件校验
  const validateFile = async (row) => {
    try {
      startLoading()
      const res = await http(constant.OPERATE_FILE_ROW, 'post', {
        fileFormatId: row.fileFormatId,
        action: 5
      })

      if (res.errorCode !== '0') {
        showError(`文件校验失败（${row.fileNameFormat}：${row.fileFormatId}）：${res.errorMsg}`)
        return
      }

      showSuccess(`文件（${row.fileNameFormat}：${row.fileFormatId}）校验成功！`)
    } catch (err) {
      showError(`文件校验失败（${row.fileNameFormat}：${row.fileFormatId}）：${err}`)
    } finally {
      stopLoading()
    }
  }

  // 关闭详情
  const closeDetail = (visible) => {
    fileDetailVisible.value = visible
    if (!visible) {
      queryFileRows()
    }
  }

  return {
    loading,
    fileFormatList,
    queryForm,
    pagination,
    fileDetailVisible,
    fileFormatDetail,
    mode,
    queryFlagOptions,
    changeQueryFlag,
    queryFileRows,
    addFileDefine,
    showFileDetail,
    modifyFile,
    execEff,
    execExp,
    deleteFile,
    validateFile,
    closeDetail
  }
}

