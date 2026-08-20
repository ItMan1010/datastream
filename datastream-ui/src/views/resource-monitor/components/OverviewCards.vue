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
  <el-row :gutter="16" class="overview-cards">
    <el-col :span="6" v-for="card in cards" :key="card.key">
      <el-card class="metric-card" :class="card.status" shadow="hover">
        <div class="card-content">
          <div class="card-left">
            <div class="card-icon" :style="{ backgroundColor: card.bgColor }">
              <el-icon :size="28" :color="card.color">
                <component :is="card.icon" />
              </el-icon>
            </div>
          </div>
          <div class="card-right">
            <div class="card-value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="card-label">{{ card.label }}</div>
            <div class="card-footer">
              <div class="card-trend" v-if="card.trend !== undefined">
                <span class="trend-label">使用率</span>
                <el-progress
                  :percentage="card.trend"
                  :color="getProgressColor(card.trend)"
                  :show-text="false"
                  :stroke-width="4"
                  class="trend-progress" />
                <span class="trend-value">{{ card.trend }}%</span>
              </div>
              <div class="card-status" :class="card.status">
                <el-icon><CircleCheck v-if="card.status === 'normal'" /><Warning v-else-if="card.status === 'warning'" /><CircleClose v-else /></el-icon>
                <span>{{ getStatusText(card.status) }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { computed } from 'vue'
import { FolderOpened, Connection, Cpu, DataLine, CircleCheck, Warning, CircleClose } from '@element-plus/icons-vue'

const props = defineProps({
  metrics: {
    type: Object,
    default: () => null
  }
})

// 计算卡片数据
const cards = computed(() => {
  const systemMetrics = props.metrics?.systemMetrics || {}
  const connectionMetrics = props.metrics?.connectionMetrics || {}
  const threadPoolMetrics = props.metrics?.threadPoolMetrics || {}

  // 计算连接使用率
  const connectionUsageRate = connectionMetrics.poolDetails
    ? connectionMetrics.poolDetails.reduce((sum, pool) => {
        const usage = pool.usageRate || 0
        return sum + usage
      }, 0) / (connectionMetrics.poolDetails.length || 1)
    : 0

  // 计算线程池使用率
  const threadPoolUsageRate = threadPoolMetrics.poolDetails
    ? threadPoolMetrics.poolDetails.reduce((sum, pool) => {
        const usage = pool.usageRate || 0
        return sum + usage
      }, 0) / (threadPoolMetrics.poolDetails.length || 1)
    : 0

  // 获取状态
  const getStatus = (rate) => {
    if (rate >= 0.9) return 'danger'
    if (rate >= 0.7) return 'warning'
    return 'normal'
  }

  return [
    {
      key: 'runningTask',
      label: '运行中任务',
      value: systemMetrics.runningTaskCount || 0,
      color: '#2563EB',
      bgColor: 'rgba(64, 158, 255, 0.1)',
      icon: 'FolderOpened',
      status: 'normal',
      trend: undefined
    },
    {
      key: 'activeConnection',
      label: '活跃连接数',
      value: systemMetrics.activeConnectionCount || 0,
      color: '#67C23A',
      bgColor: 'rgba(103, 194, 58, 0.1)',
      icon: 'Connection',
      status: getStatus(connectionUsageRate),
      trend: connectionUsageRate > 0 ? Math.round(connectionUsageRate * 100) : undefined
    },
    {
      key: 'threadPool',
      label: '线程池使用率',
      value: `${Math.round(threadPoolUsageRate * 100)}%`,
      color: '#E6A23C',
      bgColor: 'rgba(230, 162, 60, 0.1)',
      icon: 'Cpu',
      status: getStatus(threadPoolUsageRate),
      trend: threadPoolUsageRate > 0 ? Math.round(threadPoolUsageRate * 100) : undefined
    },
    {
      key: 'memory',
      label: 'JVM内存使用',
      value: formatMemory(systemMetrics.jvmMemoryUsed || 0),
      color: '#F56C6C',
      bgColor: 'rgba(245, 108, 108, 0.1)',
      icon: 'DataLine',
      status: getStatus(systemMetrics.memoryUsageRate || 0),
      trend: systemMetrics.memoryUsageRate ? Math.round(systemMetrics.memoryUsageRate * 100) : undefined
    }
  ]
})

// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage >= 90) return '#F56C6C'
  if (percentage >= 70) return '#E6A23C'
  return '#67C23A'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    normal: '正常',
    warning: '告警',
    danger: '异常'
  }
  return statusMap[status] || '未知'
}

// 格式化内存
const formatMemory = (mb) => {
  if (mb < 1024) {
    return `${mb} MB`
  }
  return `${(mb / 1024).toFixed(2)} GB`
}
</script>

<style scoped>
.overview-cards {
  margin-bottom: 16px;
}

.metric-card {
  height: 130px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  position: relative;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--status-color, #67C23A);
  transition: background 0.3s ease;
}

.metric-card.normal {
  --status-color: #67C23A;
}

.metric-card.warning {
  --status-color: #E6A23C;
}

.metric-card.danger {
  --status-color: #F56C6C;
}

.metric-card:hover {
  transform: translateY(-4px);
}

.card-content {
  display: flex;
  height: 100%;
  padding: 8px;
}

.card-left {
  flex-shrink: 0;
  margin-right: 16px;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.metric-card:hover .card-icon {
  transform: scale(1.1);
}

.card-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.card-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 4px;
}

.card-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-trend {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.trend-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.trend-progress {
  flex: 1;
  min-width: 0;
}

.trend-value {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  white-space: nowrap;
}

.card-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.card-status.normal {
  color: #67C23A;
}

.card-status.warning {
  color: #E6A23C;
}

.card-status.danger {
  color: #F56C6C;
}
</style>
