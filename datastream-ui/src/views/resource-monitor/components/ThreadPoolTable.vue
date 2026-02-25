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
      <el-table-column prop="poolName" label="线程池名称" min-width="200" :show-overflow-tooltip="true">
        <template #default="scope">
          <span class="pool-name">{{ scope.row.poolName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="corePoolSize" label="核心线程数" width="110" align="center" />
      <el-table-column prop="maxPoolSize" label="最大线程数" width="110" align="center" />
      <el-table-column prop="activeThreads" label="活跃线程数" width="120" align="center">
        <template #default="scope">
          <span class="active-value">{{ scope.row.activeThreads }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="queueSize" label="队列大小" width="100" align="center" />
      <el-table-column prop="queueCapacity" label="队列容量" width="110" align="center" />
      <el-table-column label="队列使用率" width="150" align="center">
        <template #default="scope">
          <div class="progress-wrapper">
            <el-progress
              :percentage="getQueueUsageRate(scope.row)"
              :color="getUsageColor(scope.row.queueSize, scope.row.queueCapacity)"
              :stroke-width="8"
              :show-text="true" />
          </div>
        </template>
      </el-table-column>
      <el-table-column label="线程使用率" width="150" align="center">
        <template #default="scope">
          <div class="progress-wrapper">
            <el-progress
              :percentage="getThreadUsageRate(scope.row)"
              :color="getUsageColor(scope.row.activeThreads, scope.row.maxPoolSize)"
              :stroke-width="8"
              :show-text="true" />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="completedTaskCount" label="已完成任务数" width="130" align="center">
        <template #default="scope">
          <span class="count-value">{{ formatNumber(scope.row.completedTaskCount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="rejectedTaskCount" label="拒绝任务数" width="110" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.rejectedTaskCount > 0 ? 'danger' : 'success'" size="small">
            {{ scope.row.rejectedTaskCount || 0 }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">
            {{ scope.row.status || '正常' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <div v-else class="empty-data">
      <el-empty description="暂无线程池数据" :image-size="100" />
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
  return props.data?.poolDetails || []
})

const getQueueUsageRate = (row) => {
  if (row.usageRate !== undefined && row.usageRate !== null) {
    return Math.round(row.usageRate * 100)
  }
  if (row.queueCapacity && row.queueCapacity > 0) {
    return Math.round((row.queueSize / row.queueCapacity) * 100)
  }
  return 0
}

const getThreadUsageRate = (row) => {
  if (row.maxPoolSize && row.maxPoolSize > 0) {
    return Math.round((row.activeThreads / row.maxPoolSize) * 100)
  }
  return 0
}

const getUsageColor = (current, max) => {
  if (!max || max === 0) return '#67C23A'
  const rate = (current / max) * 100
  if (rate >= 90) return '#F56C6C'
  if (rate >= 70) return '#E6A23C'
  return '#67C23A'
}

const getStatusType = (status) => {
  if (status === '正常') return 'success'
  if (status === '告警') return 'warning'
  if (status === '异常') return 'danger'
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

.pool-name {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.active-value {
  font-weight: 600;
  color: #409EFF;
}

.progress-wrapper {
  padding: 0 8px;
}

.count-value {
  font-family: 'Monaco', 'Consolas', monospace;
  font-weight: 600;
  color: #67C23A;
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
