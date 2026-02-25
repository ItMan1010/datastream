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
 * 通用表格数据管理hook
 * 用于管理表格数据的查询、刷新、筛选等操作
 */
import { ref, reactive, computed } from 'vue'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'

export function useTableData(options = {}) {
  const {
    fetchApi,
    transformData,
    defaultQueryForm = {},
    paginationOptions = {}
  } = options

  const { loading, withLoading } = useLoading()
  const pagination = usePagination(paginationOptions)
  const { showError } = useMessage()

  // 表格数据
  const tableData = ref([])

  // 查询表单
  const queryForm = reactive({ ...defaultQueryForm })

  // 是否有数据
  const hasData = computed(() => tableData.value.length > 0)

  // 重置查询表单
  const resetQueryForm = () => {
    Object.keys(defaultQueryForm).forEach(key => {
      queryForm[key] = defaultQueryForm[key]
    })
    pagination.resetPagination()
  }

  // 查询数据
  const fetchData = async (extraParams = {}) => {
    if (!fetchApi) {
      console.warn('fetchApi is not provided')
      return
    }

    try {
      loading.value = true

      const params = {
        ...queryForm,
        ...pagination.getPaginationParams(),
        ...extraParams
      }

      const res = await fetchApi(params)

      if (res.errorCode !== '0') {
        showError(`查询数据失败：${res.errorMsg}`)
        return
      }

      // 数据转换
      let data = res.data || res.list || []
      if (typeof transformData === 'function') {
        data = transformData(data, res)
      }

      tableData.value = data
      pagination.setTotal(res.total || res.count || data.length)

    } catch (error) {
      showError(`查询数据失败：${error}`)
    } finally {
      loading.value = false
    }
  }

  // 刷新数据
  const refresh = () => {
    fetchData()
  }

  // 页码变化处理
  const handlePageChange = (page) => {
    pagination.handlePageChange(page, fetchData)
  }

  // 每页条数变化处理
  const handleSizeChange = (size) => {
    pagination.handleSizeChange(size, fetchData)
  }

  // 清空数据
  const clearData = () => {
    tableData.value = []
    pagination.setTotal(0)
  }

  return {
    loading,
    tableData,
    queryForm,
    hasData,
    pagination,
    resetQueryForm,
    fetchData,
    refresh,
    handlePageChange,
    handleSizeChange,
    clearData
  }
}

