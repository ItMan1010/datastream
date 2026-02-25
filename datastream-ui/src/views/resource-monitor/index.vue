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
  <div class="main-content resource-monitor">
    <!-- 顶部控制栏 -->
    <el-card class="control-bar" shadow="never">
      <el-space :size="16">
        <el-button
          :type="autoRefresh ? 'primary' : ''"
          :icon="autoRefresh ? VideoPause : VideoPlay"
          @click="toggleAutoRefresh">
          {{ autoRefresh ? '停止刷新' : '开始刷新' }}
        </el-button>
        <el-select v-model="refreshInterval" style="width: 130px" @change="handleIntervalChange">
          <el-option label="1秒" :value="1000" />
          <el-option label="3秒" :value="3000" />
          <el-option label="5秒" :value="5000" />
          <el-option label="10秒" :value="10000" />
        </el-select>
        <el-button @click="refreshData" :loading="loading" :icon="Refresh">
          手动刷新
        </el-button>
        <el-divider direction="vertical" style="height: 24px" />
        <span class="status-text">
          <el-icon :color="autoRefresh ? '#67C23A' : '#909399'">
            <component :is="autoRefresh ? Loading : Clock" />
          </el-icon>
          {{ autoRefresh ? '自动刷新中...' : '已停止刷新' }}
        </span>
      </el-space>
    </el-card>

    <!-- 概览卡片 -->
    <OverviewCards :metrics="metrics" />

    <el-divider />

    <!-- 实时图表 -->
    <div class="chart-section">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card class="chart-card" shadow="never">
            <ResourceChart
              title="任务队列趋势"
              :data="queueChartData"
              type="line"
              :height="280"
              unit="队列大小" />
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="chart-card" shadow="never">
            <ResourceChart
              title="连接池使用趋势"
              :data="connectionChartData"
              type="line"
              :height="280"
              unit="连接数" />
          </el-card>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card class="chart-card" shadow="never">
            <ResourceChart
              title="线程池使用趋势"
              :data="threadPoolChartData"
              type="line"
              :height="280"
              unit="线程数" />
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="chart-card" shadow="never">
            <ResourceChart
              title="内存使用趋势"
              :data="memoryChartData"
              type="line"
              :height="280"
              unit="MB" />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-divider />

    <!-- 详细资源列表 -->
    <el-card class="detail-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">详细资源列表</span>
        </div>
      </template>
      <el-tabs v-model="activeTab" class="resource-tabs">
        <el-tab-pane name="task">
          <template #label>
            <span class="tab-label">
              <el-icon><List /></el-icon>
              任务资源
            </span>
          </template>
          <TaskResourceTable :data="metrics.taskMetrics" />
        </el-tab-pane>
        <el-tab-pane name="connection">
          <template #label>
            <span class="tab-label">
              <el-icon><Connection /></el-icon>
              连接池
            </span>
          </template>
          <ConnectionPoolTable :data="metrics.connectionMetrics" />
        </el-tab-pane>
        <el-tab-pane name="threadpool">
          <template #label>
            <span class="tab-label">
              <el-icon><Cpu /></el-icon>
              线程池
            </span>
          </template>
          <ThreadPoolTable :data="metrics.threadPoolMetrics" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Refresh, VideoPlay, VideoPause, Loading, Clock, List, Connection, Cpu } from '@element-plus/icons-vue'
import { useResourceMonitor } from '@/composables/useResourceMonitor'
import OverviewCards from './components/OverviewCards.vue'
import ResourceChart from './components/ResourceChart.vue'
import TaskResourceTable from './components/TaskResourceTable.vue'
import ConnectionPoolTable from './components/ConnectionPoolTable.vue'
import ThreadPoolTable from './components/ThreadPoolTable.vue'

const {
  metrics,
  autoRefresh,
  refreshInterval,
  toggleAutoRefresh,
  refreshData,
  queueChartData,
  connectionChartData,
  threadPoolChartData,
  memoryChartData,
  loading
} = useResourceMonitor()

const activeTab = ref('task')

// 监听刷新间隔变化
const handleIntervalChange = () => {
  if (autoRefresh.value) {
    toggleAutoRefresh() // 先停止
    toggleAutoRefresh() // 再开始，使用新的间隔
  }
}
</script>

<style scoped>
.resource-monitor {
  background: transparent;
}

.control-bar {
  margin-bottom: 16px;
}

.status-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #606266;
}

.chart-section {
  margin-bottom: 16px;
}

.chart-card {
  height: 100%;
}

.chart-card :deep(.el-card__body) {
  padding: 16px;
}

.detail-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #EBEEF5;
}

.card-header {
  display: flex;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.resource-tabs :deep(.el-tabs__header) {
  margin: 0 0 16px 0;
}

.resource-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
