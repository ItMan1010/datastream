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
  <div class="task-progress-card">
    <!-- 增量迁移模式：只显示同步数量 -->
    <div v-if="isIncremental" class="incremental-mode">
      <div class="incremental-label">同步数量</div>
      <div class="incremental-value">{{ formatNumber(dataCount) }}</div>
      <div class="incremental-status">{{ stateName }}</div>
    </div>

    <!-- 普通模式：显示仪表盘和统计数据 -->
    <template v-else>
      <div class="progress-gauge">
        <svg class="gauge-svg" viewBox="0 0 200 120">
          <defs>
            <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#67C23A" />
              <stop offset="50%" stop-color="var(--primary-color)" />
              <stop offset="100%" stop-color="var(--primary-color)" />
            </linearGradient>
          </defs>
          <!-- 背景弧线 -->
          <path class="gauge-bg" d="M20 100 A80 80 0 1 1 180 100" />
          <!-- 进度弧线 -->
          <path class="gauge-progress" :stroke-dasharray="strokeDasharray" d="M20 100 A80 80 0 1 1 180 100" />
          <!-- 百分比文字 -->
          <text x="100" y="95" text-anchor="middle" class="percentage-text">{{ percentage }}%</text>
          <text x="100" y="115" text-anchor="middle" class="status-text">{{ statusText }}</text>
        </svg>
        <div class="gauge-labels">
          <span class="label-start">0%</span>
          <span class="label-mid">50%</span>
          <span class="label-end">100%</span>
        </div>
      </div>
      <div class="progress-stats">
        <div class="stat-item stat-total">
          <div class="stat-icon">📦</div>
          <div class="stat-info">
            <div class="stat-label">数据总量</div>
            <div class="stat-value">{{ formatNumber(dataTotal) }}</div>
          </div>
        </div>
        <div class="stat-item stat-sync">
          <div class="stat-icon">⚡</div>
          <div class="stat-info">
            <div class="stat-label">同步数量</div>
            <div class="stat-value">{{ formatNumber(dataCount) }}</div>
          </div>
        </div>
        <div class="stat-item stat-actual">
          <div class="stat-icon">✓</div>
          <div class="stat-info">
            <div class="stat-label">实际数量</div>
            <div class="stat-value">{{ formatNumber(dataActualCount) }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'TaskProgressCard',
  props: {
    isIncremental: {
      type: Boolean,
      default: false
    },
    stateName: {
      type: String,
      default: ''
    },
    percentage: {
      type: Number,
      default: 0
    },
    dataTotal: {
      type: Number,
      default: 0
    },
    dataCount: {
      type: Number,
      default: 0
    },
    dataActualCount: {
      type: Number,
      default: 0
    }
  },
  setup(props) {
    const circumference = computed(() => 2 * Math.PI * 80)
    const strokeDasharray = computed(() => {
      const progress = Math.min(Math.max(props.percentage, 0), 100)
      const offset = circumference.value * (1 - progress / 100)
      return `${circumference.value * progress / 100} ${circumference.value}`
    })

    const statusText = computed(() => {
      if (props.percentage === 100) return '已完成'
      if (props.percentage >= 80) return '即将完成'
      if (props.percentage >= 50) return '进行中'
      if (props.percentage > 0) return '启动中'
      return '等待中'
    })

    const formatNumber = (num) => {
      if (num == null) return '-'
      if (num >= 1000000) return (num / 1000000).toFixed(2) + 'M'
      if (num >= 1000) return (num / 1000).toFixed(2) + 'K'
      return num.toString()
    }

    return {
      strokeDasharray,
      statusText,
      formatNumber
    }
  }
}
</script>

<style scoped>
.task-progress-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  min-height: 320px;
  justify-content: center;
}

.incremental-mode {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.incremental-label {
  font-size: 14px;
  color: #909399;
}

.incremental-value {
  font-size: 42px;
  font-weight: bold;
  color: var(--primary-color);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.incremental-status {
  font-size: 14px;
  color: #606266;
}

.progress-gauge {
  position: relative;
  width: 240px;
  margin-bottom: 24px;
}

.gauge-svg {
  width: 100%;
  height: auto;
}

.gauge-bg {
  fill: none;
  stroke: #f0f2f5;
  stroke-width: 12;
  stroke-linecap: round;
}

.gauge-progress {
  fill: none;
  stroke: url(#gradient);
  stroke-width: 12;
  stroke-linecap: round;
  transition: stroke-dasharray 0.8s ease-out;
}

.percentage-text {
  font-size: 32px;
  font-weight: bold;
  fill: var(--primary-color);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.status-text {
  font-size: 14px;
  fill: #909399;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.gauge-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 30px;
  margin-top: -50px;
}

.gauge-labels span {
  font-size: 12px;
  color: #c0c4cc;
}

.progress-stats {
  display: flex;
  justify-content: space-around;
  width: 100%;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-total {
  border-left: 3px solid #909399;
}

.stat-sync {
  border-left: 3px solid var(--primary-color);
}

.stat-actual {
  border-left: 3px solid #67c23a;
}

.stat-icon {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
