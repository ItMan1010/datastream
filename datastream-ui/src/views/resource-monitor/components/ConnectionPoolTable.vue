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
      <el-table-column prop="jdbcUrl" label="JDBC URL" min-width="300" :show-overflow-tooltip="true">
        <template #default="scope">
          <span class="url-text">{{ scope.row.jdbcUrl }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalConnections" label="总连接数" width="110" align="center" />
      <el-table-column prop="idleConnections" label="空闲连接数" width="120" align="center" />
      <el-table-column prop="activeConnections" label="活跃连接数" width="120" align="center">
        <template #default="scope">
          <span class="active-value">{{ scope.row.activeConnections }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="maxPoolSize" label="最大连接数" width="110" align="center" />
      <el-table-column label="使用率" width="160" align="center">
        <template #default="scope">
          <div class="progress-wrapper">
            <el-progress
              :percentage="getUsageRate(scope.row)"
              :color="getUsageColor(scope.row.usageRate)"
              :stroke-width="8"
              :show-text="true" />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">
            {{ scope.row.status || '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最大存活时间" width="120" align="center">
        <template #default="scope">
          <span class="time-value">{{ formatTime(scope.row.maxLifetimeMs) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="平均借用时长" width="120" align="center">
        <template #default="scope">
          <span class="time-value">{{ formatTime(scope.row.avgBorrowDurationMs) }}</span>
        </template>
      </el-table-column>
    </el-table>
    <div v-else class="empty-data">
      <el-empty description="暂无连接池数据" :image-size="100" />
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

const getUsageRate = (row) => {
  if (row.usageRate !== undefined && row.usageRate !== null) {
    return Math.round(row.usageRate * 100)
  }
  if (row.maxPoolSize && row.maxPoolSize > 0) {
    return Math.round((row.activeConnections / row.maxPoolSize) * 100)
  }
  return 0
}

const getUsageColor = (rate) => {
  if (rate === undefined || rate === null) return '#67C23A'
  const percentage = rate * 100
  if (percentage >= 90) return '#F56C6C'
  if (percentage >= 70) return '#E6A23C'
  return '#67C23A'
}

const getStatusType = (status) => {
  if (status === '正常') return 'success'
  if (status === '告警') return 'warning'
  if (status === '异常') return 'danger'
  return 'info'
}

const formatTime = (ms) => {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`
  if (ms < 3600000) return `${(ms / 60000).toFixed(2)}min`
  return `${(ms / 3600000).toFixed(2)}h`
}
</script>

<style scoped>
.table-container {
  width: 100%;
}

.url-text {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #606266;
}

.active-value {
  font-weight: 600;
  color: #409EFF;
}

.progress-wrapper {
  padding: 0 8px;
}

.time-value {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #909399;
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
