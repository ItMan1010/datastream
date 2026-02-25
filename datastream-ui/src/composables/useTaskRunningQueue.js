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
 * 任务运行队列监控相关业务逻辑Hook
 */
import { ref } from 'vue'
import { useMessage } from './useMessage'

export function useTaskRunningQueue() {
  const { showSuccess, showWarning, showInfo } = useMessage()

  // 抽屉状态
  const taskRunningQueueDrawerVisible = ref(false)

  // 当前任务ID
  const currentTaskId = ref('')

  // 加载状态
  const taskRunningQueueLoading = ref(false)

  // 组件key（用于强制刷新）
  const taskRunningQueueKey = ref(0)

  // 自动刷新定时器
  const autoRefreshInterval = ref(null)

  // 刷新间隔
  const refreshInterval = ref(10000) // 默认10秒

  // 队列大小
  const queueMaxSize = ref(null)

  // 组件引用
  const taskRunningQueueRef = ref(null)

  // 显示任务运行队列指标
  const showTaskRunningQueueMetrics = (observedTaskId, moveTaskInfoListData) => {
    // 使用当前观测的任务ID
    if (observedTaskId) {
      currentTaskId.value = observedTaskId.toString()
    } else if (moveTaskInfoListData?.length > 0) {
      const currentTask = moveTaskInfoListData[0]
      if (currentTask?.taskId) {
        currentTaskId.value = currentTask.taskId.toString()
      }
    }

    // 如果没有任务ID，提示用户
    if (!currentTaskId.value) {
      showWarning('请先选择一个任务进行观测，再查看运行队列指标')
      return false
    }

    // 打开抽屉
    taskRunningQueueDrawerVisible.value = true
    taskRunningQueueKey.value++

    return true
  }

  // 刷新任务运行队列指标
  const refreshTaskRunningQueueMetrics = async () => {
    if (!currentTaskId.value?.toString().trim()) {
      console.warn('没有有效的任务ID，跳过API调用')
      return
    }

    taskRunningQueueLoading.value = true
    try {
      if (taskRunningQueueRef.value) {
        await taskRunningQueueRef.value.getMonitorTaskRunningQueueApi(parseInt(currentTaskId.value))
      } else {
        console.warn('子组件引用不存在，等待组件渲染完成')
      }
    } catch (error) {
      console.error('刷新任务运行队列指标失败:', error)
    } finally {
      taskRunningQueueLoading.value = false
    }
  }

  // 开始自动刷新
  const startAutoRefresh = () => {
    if (autoRefreshInterval.value) {
      stopAutoRefresh()
    }

    autoRefreshInterval.value = setInterval(() => {
      refreshTaskRunningQueueMetrics()
    }, refreshInterval.value)

    showSuccess(`已开始自动刷新，间隔${refreshInterval.value / 1000}秒`)
  }

  // 停止自动刷新
  const stopAutoRefresh = () => {
    if (autoRefreshInterval.value) {
      clearInterval(autoRefreshInterval.value)
      autoRefreshInterval.value = null
      showInfo('已停止自动刷新')
    }
  }

  // 关闭抽屉
  const handleTaskRunningQueueDrawerClose = () => {
    stopAutoRefresh()
    taskRunningQueueDrawerVisible.value = false
    currentTaskId.value = ''
  }

  return {
    taskRunningQueueDrawerVisible,
    currentTaskId,
    taskRunningQueueLoading,
    taskRunningQueueKey,
    autoRefreshInterval,
    refreshInterval,
    queueMaxSize,
    taskRunningQueueRef,
    showTaskRunningQueueMetrics,
    refreshTaskRunningQueueMetrics,
    startAutoRefresh,
    stopAutoRefresh,
    handleTaskRunningQueueDrawerClose
  }
}

