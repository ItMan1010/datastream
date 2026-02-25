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
  <div class="data-table-container">
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="data"
      :height="height"
      :max-height="maxHeight"
      :stripe="stripe"
      :border="border"
      :fit="fit"
      :show-summary="showSummary"
      :summary-method="summaryMethod"
      :highlight-current-row="highlightCurrentRow"
      :row-key="rowKey"
      :cell-style="cellStyle"
      @selection-change="handleSelectionChange"
      @current-change="handleCurrentChange"
      @row-click="handleRowClick"
      style="width: 100%;">
      <slot></slot>
    </el-table>
  </div>
</template>

<script>
/**
 * 通用数据表格组件
 */
import { ref } from 'vue'

export default {
  name: 'DataTable',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    height: {
      type: [String, Number],
      default: undefined
    },
    maxHeight: {
      type: [String, Number],
      default: undefined
    },
    stripe: {
      type: Boolean,
      default: true
    },
    border: {
      type: Boolean,
      default: false
    },
    fit: {
      type: Boolean,
      default: true
    },
    showSummary: {
      type: Boolean,
      default: false
    },
    summaryMethod: {
      type: Function,
      default: null
    },
    highlightCurrentRow: {
      type: Boolean,
      default: true
    },
    rowKey: {
      type: [String, Function],
      default: 'id'
    },
    cellStyle: {
      type: [Object, Function],
      default: () => ({ wordBreak: 'break-all', whiteSpace: 'pre-wrap' })
    }
  },
  emits: ['selection-change', 'current-change', 'row-click'],
  setup(props, { emit, expose }) {
    const tableRef = ref(null)

    const handleSelectionChange = (selection) => {
      emit('selection-change', selection)
    }

    const handleCurrentChange = (current) => {
      emit('current-change', current)
    }

    const handleRowClick = (row) => {
      emit('row-click', row)
    }

    // 暴露表格方法
    const clearSelection = () => {
      tableRef.value?.clearSelection()
    }

    const toggleRowSelection = (row, selected) => {
      tableRef.value?.toggleRowSelection(row, selected)
    }

    const setCurrentRow = (row) => {
      tableRef.value?.setCurrentRow(row)
    }

    expose({
      tableRef,
      clearSelection,
      toggleRowSelection,
      setCurrentRow
    })

    return {
      tableRef,
      handleSelectionChange,
      handleCurrentChange,
      handleRowClick
    }
  }
}
</script>

<style scoped>
.data-table-container {
  margin-bottom: 20px;
}
</style>

