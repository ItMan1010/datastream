<!--Licensed to the Apache Software Foundation (ASF) under one or more-->
<!--contributor license agreements.  See the NOTICE file distributed with-->
<!--this work for additional information regarding copyright ownership.-->
<!--The ASF licenses this file to You under the Apache License, Version 2.0-->
<!--(the "License"); you may not use this file except in compliance with-->
<!--the License.  You may obtain a copy of the License at-->

<!--http://www.apache.org/licenses/LICENSE-2.0-->

<!--Unless required by applicable law or agreed to in writing, software-->
<!--distributed under the License is distributed on an "AS IS" BASIS,-->
<!--WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.-->
<!--See the License for the specific language governing permissions and-->
<!--limitations under the License.-->
<template>
  <div class="pagination-container" :class="{ 'pagination-center': center }">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="currentPageSize"
      :page-sizes="pageSizes"
      :pager-count="pagerCount"
      :total="total"
      :layout="layout"
      :background="background"
      :small="small"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange">
    </el-pagination>
  </div>
</template>

<script>
/**
 * 通用分页组件
 */
import { computed } from 'vue'

export default {
  name: 'PagePagination',
  props: {
    pageNum: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 20
    },
    total: {
      type: Number,
      default: 0
    },
    pageSizes: {
      type: Array,
      default: () => [10, 20, 50, 100]
    },
    pagerCount: {
      type: Number,
      default: 7
    },
    layout: {
      type: String,
      default: 'total, sizes, prev, pager, next, jumper'
    },
    background: {
      type: Boolean,
      default: false
    },
    small: {
      type: Boolean,
      default: false
    },
    center: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:pageNum', 'update:pageSize', 'size-change', 'current-change', 'change'],
  setup(props, { emit }) {
    const currentPage = computed({
      get: () => props.pageNum,
      set: (val) => emit('update:pageNum', val)
    })

    const currentPageSize = computed({
      get: () => props.pageSize,
      set: (val) => emit('update:pageSize', val)
    })

    const handleSizeChange = (size) => {
      emit('size-change', size)
      emit('change')
    }

    const handleCurrentChange = (page) => {
      emit('current-change', page)
      emit('change')
    }

    return {
      currentPage,
      currentPageSize,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.pagination-container {
  padding: 20px 0;
}

.pagination-center {
  text-align: center;
}
</style>

