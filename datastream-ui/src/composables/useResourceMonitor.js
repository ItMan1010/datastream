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
 * 资源监控相关业务逻辑Hook
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllResourceMetrics } from '@/api/resourceMetrics'

export function useResourceMonitor() {
  const metrics = ref({
    systemMetrics: null,
    taskMetrics: null,
    connectionMetrics: null,
    threadPoolMetrics: null,
    queueMetrics: null
  })

  const autoRefresh = ref(false)
  const refreshInterval = ref(3000)
  let refreshTimer = null

  // 历史数据缓存（用于图表）
  const queueChartData = ref([])
  const connectionChartData = ref([])
  const threadPoolChartData = ref([])
  const memoryChartData = ref([])
  const maxHistorySize = 100 // 最多保留100个数据点

  const loading = ref(false)

  // 获取资源指标
  const fetchMetrics = async () => {
    loading.value = true
    try {
      const response = await getAllResourceMetrics()

      if (response.errorCode === '0') {
        metrics.value = response

        // 更新图表数据
        updateChartData(response)
      } else {
        ElMessage.error(`获取资源指标失败: ${response.errorMsg || '未知错误'}`)
      }
    } catch (error) {
      console.error('获取资源指标失败:', error)
      ElMessage.error('获取资源指标失败')
    } finally {
      loading.value = false
    }
  }

  // 更新图表数据
  const updateChartData = (newMetrics) => {
    const timestamp = new Date().toLocaleTimeString()

    // 更新队列数据
    if (newMetrics.queueMetrics) {
      const totalSize = newMetrics.queueMetrics.totalSize || 0
      queueChartData.value.push({
        time: timestamp,
        value: totalSize
      })
      if (queueChartData.value.length > maxHistorySize) {
        queueChartData.value.shift()
      }
    }

    // 更新连接池数据
    if (newMetrics.connectionMetrics && newMetrics.connectionMetrics.poolDetails) {
      const totalConnections = newMetrics.connectionMetrics.poolDetails
        .reduce((sum, pool) => sum + (pool.activeConnections || 0), 0)

      connectionChartData.value.push({
        time: timestamp,
        value: totalConnections
      })
      if (connectionChartData.value.length > maxHistorySize) {
        connectionChartData.value.shift()
      }
    }

    // 更新线程池数据
    if (newMetrics.threadPoolMetrics && newMetrics.threadPoolMetrics.poolDetails) {
      const totalActiveThreads = newMetrics.threadPoolMetrics.poolDetails
        .reduce((sum, pool) => sum + (pool.activeThreads || 0), 0)

      threadPoolChartData.value.push({
        time: timestamp,
        value: totalActiveThreads
      })
      if (threadPoolChartData.value.length > maxHistorySize) {
        threadPoolChartData.value.shift()
      }
    }

    // 更新内存数据
    if (newMetrics.systemMetrics) {
      const memoryUsed = newMetrics.systemMetrics.jvmMemoryUsed || 0
      memoryChartData.value.push({
        time: timestamp,
        value: memoryUsed
      })
      if (memoryChartData.value.length > maxHistorySize) {
        memoryChartData.value.shift()
      }
    }
  }

  // 切换自动刷新
  const toggleAutoRefresh = () => {
    autoRefresh.value = !autoRefresh.value

    if (autoRefresh.value) {
      startAutoRefresh()
    } else {
      stopAutoRefresh()
    }
  }

  // 开始自动刷新
  const startAutoRefresh = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
    }

    refreshTimer = setInterval(() => {
      fetchMetrics()
    }, refreshInterval.value)
  }

  // 停止自动刷新
  const stopAutoRefresh = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }

  // 手动刷新
  const refreshData = () => {
    fetchMetrics()
  }

  // 初始化
  onMounted(() => {
    fetchMetrics()
  })

  // 清理
  onUnmounted(() => {
    stopAutoRefresh()
  })

  return {
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
  }
}

