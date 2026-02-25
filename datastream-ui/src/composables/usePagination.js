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
 * 通用分页管理hook
 * 用于管理表格分页状态
 */
import { ref, computed } from 'vue'

export function usePagination(options = {}) {
  const {
    defaultPage = 1,
    defaultPageSize = 20,
    pageSizes = [10, 20, 50, 100]
  } = options

  const pageNum = ref(defaultPage)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)

  // 计算总页数
  const totalPages = computed(() => {
    return Math.ceil(total.value / pageSize.value)
  })

  // 重置分页
  const resetPagination = () => {
    pageNum.value = defaultPage
  }

  // 设置总数
  const setTotal = (value) => {
    total.value = value || 0
  }

  // 处理页码变化
  const handlePageChange = (page, callback) => {
    pageNum.value = page
    if (typeof callback === 'function') {
      callback()
    }
  }

  // 处理每页条数变化
  const handleSizeChange = (size, callback) => {
    pageSize.value = size
    resetPagination()
    if (typeof callback === 'function') {
      callback()
    }
  }

  // 获取分页参数
  const getPaginationParams = () => {
    return {
      page: pageNum.value,
      count: pageSize.value
    }
  }

  return {
    pageNum,
    pageSize,
    total,
    totalPages,
    pageSizes,
    resetPagination,
    setTotal,
    handlePageChange,
    handleSizeChange,
    getPaginationParams
  }
}

