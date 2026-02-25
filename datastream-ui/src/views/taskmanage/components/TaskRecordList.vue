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
  <div class="table-container">
    <el-table
      :data="tableData"
      show-summary
      :summary-method="getSummaries"
      fit
      stripe
      highlight-current-row
      style="width: 100%;"
      :height="500"
      :cell-style="{ wordBreak: 'break-all', whiteSpace: 'pre-wrap' }">

      <el-table-column prop="taskId" label="任务ID" width="120" />

      <el-table-column prop="sourceObjectName" label="源对象名称" width="180" :show-overflow-tooltip="false">
        <template #default="scope">
          <div v-if="scope.row.sourceObjectName && scope.row.sourceObjectName.length > 20">
            <span v-if="!isExpanded(scope.row.taskId)">
              {{ scope.row.sourceObjectName.substring(0, 20) }}
              <el-button type="text" size="small" @click="toggleExpand(scope.row.taskId)"
                style="color: #409eff; padding: 0; margin-left: 5px;">...</el-button>
            </span>
            <span v-else>
              {{ scope.row.sourceObjectName }}
              <el-button type="text" size="small" @click="toggleExpand(scope.row.taskId)"
                style="color: #409eff; padding: 0; margin-left: 5px;">...</el-button>
            </span>
          </div>
          <span v-else>{{ scope.row.sourceObjectName }}</span>
        </template>
      </el-table-column>

      <el-table-column label="任务类型" width="180" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ getTaskTypeName(scope.row.taskType) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="createDate" label="创建时间" width="160" />
      <el-table-column prop="systemUserCode" label="创建工号" width="160" :show-overflow-tooltip="false" />

      <el-table-column label="任务状态" width="120">
        <template #default="scope">
          <TaskStateText :state="scope.row.state" />
        </template>
      </el-table-column>

      <el-table-column prop="errorCode" label="错误编码" width="120" :show-overflow-tooltip="false" />
      <el-table-column prop="errorMsg" label="错误信息" min-width="200" :show-overflow-tooltip="false" />

      <el-table-column fixed="right" label="操作" width="210">
        <template #default="scope">
          <TaskOperations
            :row="scope.row"
            @detail="handleDetail"
            @observe="handleObserve"
            @pause="handlePause"
            @restart="handleRestart"
            @copy="handleCopy"
            @check="handleCheck"
            @log="handleLog" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { ref } from 'vue'
import TaskStateText from '@/views/components/common/TaskStateText.vue'
import TaskOperations from './TaskOperations.vue'
import { getTaskTypeName } from '@/constants/taskConstants'

export default {
  name: 'TaskTable',
  components: {
    TaskStateText,
    TaskOperations
  },
  props: {
    tableData: {
      type: Array,
      default: () => []
    }
  },
  emits: ['detail', 'observe', 'pause', 'restart', 'copy', 'check', 'log'],
  setup(props, { emit }) {
    const expandedRows = ref(new Set())

    const isExpanded = (taskId) => {
      return expandedRows.value.has(taskId)
    }

    const toggleExpand = (taskId) => {
      if (expandedRows.value.has(taskId)) {
        expandedRows.value.delete(taskId)
      } else {
        expandedRows.value.add(taskId)
      }
    }

    const getSummaries = (param) => {
      const { columns, data } = param
      const sums = []
      columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = '总计：'
        } else if (index === 6) {
          const values = data.map(item => Number(item[column.property]))
          if (!values.every(value => isNaN(value))) {
            sums[index] = values.reduce((prev, curr) => {
              const value = Number(curr)
              return !isNaN(value) ? prev + 1 : prev
            }, 0)
          }
        } else {
          sums[index] = ''
        }
      })
      return sums
    }

    const handleDetail = (row) => emit('detail', row)
    const handleObserve = (row) => emit('observe', row)
    const handlePause = (row) => emit('pause', row)
    const handleRestart = (row) => emit('restart', row)
    const handleCopy = (row) => emit('copy', row)
    const handleCheck = (row) => emit('check', row.taskId)
    const handleLog = (row) => emit('log', row)

    return {
      expandedRows,
      isExpanded,
      toggleExpand,
      getSummaries,
      getTaskTypeName,
      handleDetail,
      handleObserve,
      handlePause,
      handleRestart,
      handleCopy,
      handleCheck,
      handleLog
    }
  }
}
</script>

<style scoped>
.table-container {
  margin-bottom: 20px;
}
</style>

