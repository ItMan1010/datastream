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
  <div class="main-content">
    <!-- 搜索表单 -->
    <TaskSearchForm
      :query-form="queryForm"
      :loading="loading"
      @search="queryDataMoveTaskList"
      @create="openCreateTaskDrawer"
      @refresh="handleRefresh"
      v-model:refresh-checked="refreshChecked" />

    <el-divider />

    <!-- 数据表格 -->
    <TaskTable
      :table-data="dataMoveTaskListData"
      @detail="handleDetail"
      @observe="handleObserve"
      @pause="handlePause"
      @restart="handleRestart"
      @copy="handleCopy"
      @check="handleDataCheck"
      @log="handleLog" />

    <!-- 分页 -->
    <div v-if="showPagination" class="pagination-container">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        v-model:current-page="pageNum"
        :page-sizes="[20, 50, 100]"
        :page-size="pageSize"
        :pager-count="7"
        :total="dataMoveTaskListTotal"
        layout="total, sizes, prev, pager, next, jumper" />
    </div>

    <!-- 任务详情抽屉 -->
    <TaskDetailDrawer
      v-model="moveTaskProgressDrawer"
      :task-detail="taskDetail"
      :task-execute-list="taskExecuteList" />

    <!-- 任务观测抽屉 -->
    <TaskObserveDrawer
      v-model="moveTaskInfoDrawer"
      :current-task-type="currentTaskType"
      :move-task-info-list-data="moveTaskInfoListData"
      :table-move-list-data="tableMoveListData"
      :data-send-mode="dataSendMode"
      :queue-max-size="queueMaxSize"
      :queue-number="queueNumber"
      :queue-running-size="queueRunningSize"
      @show-log="showTaskLog"
      @show-queue-metrics="showTaskRunningQueueMetrics" />

    <!-- 数据稽核抽屉 -->
    <DataCheckDrawer
      v-model="dataCheckDrawer"
      :data-check-list-data="dataCheckListData"
      :loading="loading"
      @repair="repairDataCheck"
      @repair-all="repairDataCheck" />

    <!-- 任务日志抽屉 -->
    <TaskLogDrawer
      v-model="taskLogDrawer"
      :task-log-info="taskLogInfo" />

    <!-- 创建任务抽屉 -->
    <TaskCreateDrawer
      v-model="createNewTaskDrawer"
      :form="form"
      :loading="loading"
      :is-sharding-d-b-flag="isShardingDBFlag"
      @select-datasource="selectDataSource"
      @cancel="closeCreateTaskDrawer"
      @reset="initForm"
      @submit="createDataMoveTask"
      @source-input="inputSourceTableName" />

    <!-- 数据源选择对话框 -->
    <el-dialog
      title="数据源选择"
      v-model="dialogTableVisible"
      width="75%"
      top="5vh"
      :append-to-body="true"
      :modal-append-to-body="true"
      custom-class="datasource-dialog"
      @close="handleDialogClose">
      <DataSourceSelect :unique-id="uniqueId" />
    </el-dialog>

    <!-- 任务运行队列指标抽屉 -->
    <el-drawer
      title="任务运行队列指标监控"
      v-model="taskRunningQueueDrawerVisible"
      direction="rtl"
      size="90%"
      :modal-append-to-body="false"
      @close="handleTaskRunningQueueDrawerClose">
      <div style="padding: 20px; height: 100%; display: flex; flex-direction: column;">
        <div style="margin-bottom: 20px;">
          <el-row :gutter="20" align="middle">
            <el-col :span="6">
              <div style="text-align: center; padding: 10px; background: #f5f7fa; border-radius: 4px;">
                <div style="font-size: 14px; color: #606266; margin-bottom: 5px;">当前任务ID</div>
                <div style="font-size: 18px; font-weight: bold; color: #409EFF;">{{ currentTaskId }}</div>
              </div>
            </el-col>
            <el-col :span="4">
              <el-button type="primary" @click="refreshTaskRunningQueueMetrics" :loading="taskRunningQueueLoading">
                <el-icon><Refresh /></el-icon>
                刷新数据
              </el-button>
            </el-col>
            <el-col :span="4">
              <el-button type="success" @click="startAutoRefresh" :disabled="autoRefreshInterval !== null">
                <el-icon><VideoPlay /></el-icon>
                开始自动刷新
              </el-button>
            </el-col>
            <el-col :span="4">
              <el-button type="warning" @click="stopAutoRefresh" :disabled="autoRefreshInterval === null">
                <el-icon><VideoPause /></el-icon>
                停止自动刷新
              </el-button>
            </el-col>
            <el-col :span="6">
              <el-select v-model="refreshInterval" placeholder="刷新间隔" style="width: 30%;">
                <el-option label="5秒" :value="5000" />
                <el-option label="10秒" :value="10000" />
                <el-option label="30秒" :value="30000" />
                <el-option label="1分钟" :value="60000" />
              </el-select>
            </el-col>
          </el-row>
        </div>
        <div style="flex: 1; min-height: 0;">
          <TaskRunningQueue
            ref="taskRunningQueueRef"
            :key="taskRunningQueueKey"
            :max-queue-size="queueMaxSize" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onActivated, onDeactivated, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'
import { useMainStore } from '@/store/index.js'
import { Refresh, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

// 子组件
import {
  TaskSearchForm,
  TaskRecordList,
  TaskDetailDrawer,
  TaskObserveDrawer,
  TaskCreateDrawer,
  TaskLogDrawer,
  DataCheckDrawer
} from './components'

// 公共组件
import DataSourceSelect from '../components/DataSourceSelect.vue'
import TaskRunningQueue from '../metrics/TaskRunningQueue.vue'

// Composables
import { useTaskManage } from '@/composables/useTaskManage'
import { useTaskDetail } from '@/composables/useTaskDetail'
import { useTaskObserve } from '@/composables/useTaskObserve'
import { useTaskCreate } from '@/composables/useTaskCreate'
import { useTaskLog } from '@/composables/useTaskLog'
import { useDataCheck } from '@/composables/useDataCheck'
import { useTaskRunningQueue } from '@/composables/useTaskRunningQueue'
import { useEventBus } from '@/composables/useEventBus'

export default {
  name: 'TaskManage',
  components: {
    TaskSearchForm,
    TaskTable: TaskRecordList,
    TaskDetailDrawer,
    TaskObserveDrawer,
    TaskCreateDrawer,
    TaskLogDrawer,
    DataCheckDrawer,
    DataSourceSelect,
    TaskRunningQueue,
    Refresh,
    VideoPlay,
    VideoPause
  },
  setup() {
    const instance = getCurrentInstance()
    const mainStore = useMainStore()
    const { on, emit } = useEventBus()

    // 使用任务管理相关hooks
    const taskManage = useTaskManage()
    const taskDetail = useTaskDetail()
    const taskObserve = useTaskObserve()
    const taskCreate = useTaskCreate()
    const taskLog = useTaskLog()
    const dataCheck = useDataCheck()
    const taskRunningQueue = useTaskRunningQueue()

    // 本地状态
    const refreshChecked = ref(false)
    const timer = ref(null)
    const pageNum = ref(1)
    const pageSize = ref(50)
    const dataMoveTaskListTotal = ref(0)

    // 查询表单
    const queryForm = reactive({
      taskId: '',
      queryFlag: '4',
      tableName: '',
      state: '0',
      moveDate: [],
      batchTaskId: '',
      copyTaskId: null,
      taskType: '1'
    })

    // 任务列表数据
    const dataMoveTaskListData = ref([])

    // 加载状态
    const loading = ref(false)

    // 是否显示分页
    const showPagination = computed(() => {
      return ['2', '3', '4'].includes(queryForm.queryFlag)
    })

    // 初始化日期范围
    const initDateRange = () => {
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      start.setHours(0, 0, 0, 0)
      const end = new Date()
      end.setHours(23, 59, 59, 999)
      queryForm.moveDate = [start, end]
    }

    // 查询任务列表（复用原有逻辑）
    const queryDataMoveTaskList = async () => {
      // 同步本地queryForm到taskManage.queryForm
      Object.assign(taskManage.queryForm, queryForm)

      loading.value = true
      try {
        await taskManage.queryDataMoveTaskList()
        dataMoveTaskListData.value = taskManage.dataMoveTaskListData.value
        dataMoveTaskListTotal.value = taskManage.pagination.total.value
      } finally {
        loading.value = false
      }
    }

    // 分页处理
    const handleSizeChange = (val) => {
      pageSize.value = val
      taskManage.pagination.pageSize.value = val
      queryDataMoveTaskList()
    }

    const handleCurrentChange = (val) => {
      pageNum.value = val
      taskManage.pagination.pageNum.value = val
      queryDataMoveTaskList()
    }

    // 刷新处理
    const handleRefresh = (autoRefresh) => {
      queryDataMoveTaskList()
      if (!autoRefresh) return

      if (timer.value) {
        clearInterval(timer.value)
      }

      timer.value = setInterval(() => {
        if (!refreshChecked.value) {
          clearInterval(timer.value)
          return
        }
        queryDataMoveTaskList()
      }, 20000)
    }

    // 任务详情
    const handleDetail = (row) => {
      taskDetail.showTaskProgress(row)
    }

    // 任务观测
    const handleObserve = (row) => {
      taskObserve.handleObserve(row)
    }

    // 暂停任务
    const handlePause = (row) => {
      taskManage.operateDataMoveTask(row, 1)
    }

    // 重启任务
    const handleRestart = (row) => {
      taskManage.operateDataMoveTask(row, 2)
    }

    // 复制任务
    const handleCopy = (row) => {
      taskManage.operateDataMoveTaskCopy(row)
    }

    // 任务日志
    const handleLog = (row) => {
      taskLog.showTaskLog(3, row.taskId)
    }

    // 数据稽核
    const handleDataCheck = (taskId) => {
      dataCheck.handleDataCheck(taskId)
    }

    // 修订数据稽核
    const repairDataCheck = (checkType, taskId, dataCheckId) => {
      dataCheck.repairDataCheck(checkType, taskId, dataCheckId)
    }

    // 显示任务日志
    const showTaskLog = (jobType, jobId) => {
      taskLog.showTaskLog(jobType, jobId)
    }

    // 任务运行队列指标
    const showTaskRunningQueueMetrics = () => {
      const success = taskRunningQueue.showTaskRunningQueueMetrics(
        taskObserve.currentObservedTaskId.value,
        taskObserve.moveTaskInfoListData.value
      )
      if (success) {
        nextTick(() => {
          setTimeout(() => {
            taskRunningQueue.refreshTaskRunningQueueMetrics()
          }, 100)
        })
      }
    }

    // 创建任务相关
    const openCreateTaskDrawer = () => {
      taskCreate.openCreateTaskDrawer()
    }

    const closeCreateTaskDrawer = () => {
      taskCreate.closeCreateTaskDrawer()
    }

    const selectDataSource = (isSource) => {
      taskCreate.selectDataSource(isSource)
    }

    const handleDialogClose = () => {
      taskCreate.dialogTableVisible.value = false
    }

    const inputSourceTableName = () => {
      taskCreate.inputSourceTableName()
    }

    const initForm = () => {
      taskCreate.initForm()
    }

    const createDataMoveTask = () => {
      taskCreate.createDataMoveTask(queryDataMoveTaskList)
    }

    // 生命周期
    onMounted(() => {
      nextTick(() => {
        initDateRange()
        // 同步queryForm到taskManage
        Object.assign(taskManage.queryForm, queryForm)
        queryDataMoveTaskList()
      })

      // 监听数据源选择事件
      on('confirmSelectDataSource' + taskCreate.uniqueId.value, (dataSourceRow) => {
        taskCreate.handleDataSourceSelect(dataSourceRow)
      })
    })

    onActivated(() => {
      nextTick(() => {
        if (mainStore.commQueryParams) {
          Object.assign(queryForm, mainStore.commQueryParams)
          Object.assign(taskManage.queryForm, queryForm)
          queryDataMoveTaskList()
          mainStore.commQueryParams = null
        }
      })
    })

    onDeactivated(() => {
      // 组件停用时的处理
    })

    onBeforeUnmount(() => {
      // 清理自动刷新定时器
      taskRunningQueue.stopAutoRefresh()
      if (timer.value) {
        clearInterval(timer.value)
      }
    })

    return {
      // 状态
      loading,
      refreshChecked,
      pageNum,
      pageSize,
      dataMoveTaskListTotal,
      queryForm,
      dataMoveTaskListData,
      showPagination,

      // 任务详情
      moveTaskProgressDrawer: taskDetail.moveTaskProgressDrawer,
      taskDetail: taskDetail.taskDetail,
      taskExecuteList: taskDetail.taskExecuteList,

      // 任务观测
      moveTaskInfoDrawer: taskObserve.moveTaskInfoDrawer,
      currentTaskType: taskObserve.currentTaskType,
      moveTaskInfoListData: taskObserve.moveTaskInfoListData,
      tableMoveListData: taskObserve.tableMoveListData,
      dataSendMode: taskObserve.dataSendMode,
      queueMaxSize: taskObserve.queueMaxSize,
      queueNumber: taskObserve.queueNumber,
      queueRunningSize: taskObserve.queueRunningSize,

      // 任务创建
      createNewTaskDrawer: taskCreate.createNewTaskDrawer,
      dialogTableVisible: taskCreate.dialogTableVisible,
      isShardingDBFlag: taskCreate.isShardingDBFlag,
      form: taskCreate.form,
      uniqueId: taskCreate.uniqueId,

      // 任务日志
      taskLogDrawer: taskLog.taskLogDrawer,
      taskLogInfo: taskLog.taskLogInfo,

      // 数据稽核
      dataCheckDrawer: dataCheck.dataCheckDrawer,
      dataCheckListData: dataCheck.dataCheckListData,

      // 任务运行队列
      taskRunningQueueDrawerVisible: taskRunningQueue.taskRunningQueueDrawerVisible,
      currentTaskId: taskRunningQueue.currentTaskId,
      taskRunningQueueLoading: taskRunningQueue.taskRunningQueueLoading,
      taskRunningQueueKey: taskRunningQueue.taskRunningQueueKey,
      autoRefreshInterval: taskRunningQueue.autoRefreshInterval,
      refreshInterval: taskRunningQueue.refreshInterval,
      taskRunningQueueRef: taskRunningQueue.taskRunningQueueRef,

      // 方法
      queryDataMoveTaskList,
      handleSizeChange,
      handleCurrentChange,
      handleRefresh,
      handleDetail,
      handleObserve,
      handlePause,
      handleRestart,
      handleCopy,
      handleLog,
      handleDataCheck,
      repairDataCheck,
      showTaskLog,
      showTaskRunningQueueMetrics,
      refreshTaskRunningQueueMetrics: taskRunningQueue.refreshTaskRunningQueueMetrics,
      startAutoRefresh: taskRunningQueue.startAutoRefresh,
      stopAutoRefresh: taskRunningQueue.stopAutoRefresh,
      handleTaskRunningQueueDrawerClose: taskRunningQueue.handleTaskRunningQueueDrawerClose,
      openCreateTaskDrawer,
      closeCreateTaskDrawer,
      selectDataSource,
      handleDialogClose,
      inputSourceTableName,
      initForm,
      createDataMoveTask
    }
  }
}
</script>

<style scoped>
.main-content {
  padding: 20px;
}

.pagination-container {
  text-align: center;
  padding: 20px 0;
}

/* 数据源选择对话框定位 */
:deep(.datasource-dialog) {
  margin-left: 200px !important;
  margin-right: 50px !important;
}

:deep(.datasource-dialog .el-dialog) {
  margin: 5vh auto 0 !important;
  transform: translateX(120px) !important;
}
</style>

