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
      v-if="tableData && tableData.length > 0"
      :data="tableData"
      fit
      stripe
      style="width: 100%"
      :header-cell-style="{ background: '#F5F7FA', color: '#606266', fontWeight: '600' }">
      <el-table-column prop="taskId" label="任务ID" width="120" align="center" />
      <el-table-column prop="queueSize" label="队列大小" width="120" align="center" />
      <el-table-column prop="queueMaxSize" label="队列最大容量" width="140" align="center" />
      <el-table-column label="队列使用率" width="160" align="center">
        <template #default="scope">
          <div class="progress-wrapper">
            <el-progress
              :percentage="getUsageRate(scope.row.queueSize, scope.row.queueMaxSize)"
              :color="getUsageColor(scope.row.queueSize, scope.row.queueMaxSize)"
              :stroke-width="8"
              :show-text="true" />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="sourceThreadCount" label="源端线程数" width="120" align="center" />
      <el-table-column prop="targetThreadCount" label="目标端线程数" width="140" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">
            {{ scope.row.status || '运行中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dataProcessed" label="已处理数据量" width="140" align="center">
        <template #default="scope">
          <span class="data-value">{{ formatNumber(scope.row.dataProcessed) }}</span>
        </template>
      </el-table-column>
    </el-table>
    <div v-else class="empty-data">
      <el-empty description="暂无任务资源数据" :image-size="100" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => null
  }
})

const tableData = computed(() => {
  return props.data?.taskDetails || []
})

const getUsageRate = (current, max) => {
  if (!max || max === 0) return 0
  return Math.round((current / max) * 100)
}

const getUsageColor = (current, max) => {
  const rate = getUsageRate(current, max)
  if (rate >= 90) return '#F56C6C'
  if (rate >= 70) return '#E6A23C'
  return '#67C23A'
}

const getStatusType = (status) => {
  if (status === '运行中') return 'success'
  if (status === '暂停') return 'warning'
  if (status === '停止') return 'danger'
  return 'info'
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 1000000) {
    return (num / 1000000).toFixed(2) + 'M'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(2) + 'K'
  }
  return num.toString()
}
</script>

<style scoped>
.table-container {
  width: 100%;
}

.progress-wrapper {
  padding: 0 8px;
}

.data-value {
  font-family: 'Monaco', 'Consolas', monospace;
  font-weight: 600;
  color: #409EFF;
}

.empty-data {
  padding: 60px 0;
  text-align: center;
}

:deep(.el-progress__text) {
  font-size: 12px !important;
  font-weight: 600;
}
</style>
