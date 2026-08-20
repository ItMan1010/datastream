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
  <el-drawer v-model="visible" direction="btt" :modal-append-to-body="false" size="85%">
    <div style="position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: #EBEEF5;">
      <div class="bcFFF margin-10 pl-20" style="line-height: 40px; color: var(--primary-color);">
        <label class="fontWeight">迁移任务</label> > 迁移详情
      </div>
      <div class="pt-10 margin-10 bcFFF progress-container"
           style="position: absolute; bottom: 10px; top: 50px; left: 0; right: 0; overflow-y: auto;">
        <div style="display: flex; justify-content: flex-start; padding: 10px;">
          <!-- 进度卡片 -->
          <div style="width: 30%; border-right: 1px dashed rgb(220, 223, 230); display: flex; justify-content: center;">
            <div style="display: flex; flex-direction: column; justify-content: center; width: 100%;">
              <TaskProgressCard
                :is-incremental="taskDetail.taskType === 6"
                :state-name="taskDetail.stateName"
                :percentage="taskDetail.percentage"
                :dataTotal="taskDetail.dataMoveTotal"
                :dataCount="taskDetail.dataCount"
                :dataActualCount="taskDetail.dataActualCount" />
            </div>
          </div>

          <!-- 任务详情 -->
          <div style="width: 70%; padding: 20px;">
            <div class="detail-card">
              <div style="height: 45%; width: 50%; border-right: 1px solid #ebeef5;">
                <table class="pl-20 detail-table">
                  <tr>任务ID：{{ taskDetail.taskId }}</tr>
                  <tr>任务类型：{{ taskDetail.taskTypeName }}</tr>
                  <tr>创建工号：{{ taskDetail.systemUserCode }}</tr>
                  <tr>创建时间：{{ taskDetail.createDate }}</tr>
                  <tr>任务状态：{{ taskDetail.stateName }}</tr>
                  <tr>状态时间：{{ taskDetail.stateDate }}</tr>
                  <tr v-if="taskDetail.taskType !== 4">
                    源端线程数：{{ taskDetail.sourceThreadCount }}、目标端线程数：{{ taskDetail.targetThreadCount }}
                  </tr>
                  <tr>任务描述：{{ taskDetail.taskDisc }}</tr>
                  <tr v-if="taskDetail.taskType === 5">稽核差异数量：{{ taskDetail.dataCheckCount }}</tr>
                  <tr v-if="isNotEmpty(taskDetail.errorCode)">错误编码：{{ taskDetail.errorCode }}</tr>
                  <tr v-if="isNotEmpty(taskDetail.errorMsg)">错误信息：{{ taskDetail.errorMsg }}</tr>
                </table>
              </div>

              <div style="width: 50%; display: flex; flex-direction: column;">
                <!-- 源端数据源 -->
                <div style="height: 50%; padding: 20px; border-bottom: 1px solid #ebeef5;">
                  <label class="fontWeight pt-20">源端数据源</label>
                  <table class="mt-10 detail-table">
                    <tr>源对象类型：{{ taskDetail.sourceDataSourceTypeDesc }}</tr>
                    <tr>源对象配置名称：{{ taskDetail.sourceDataSourceDesc }}</tr>
                    <tr>源对象实例名称：{{ taskDetail.sourceObjectName }}</tr>
                    <tr v-if="taskDetail.taskType !== 4 && taskDetail.sourceObjectKeys !== null">源端加载主键：{{ taskDetail.sourceObjectKeys }}
                    </tr>
                    <tr v-if="showSourceLoadStrategy">源端加载策略：{{ taskDetail.sourceLoadStrategyDesc }}</tr>
                    <tr v-if="showSourceDebeziumObject">增量同步对象：{{ taskDetail.sourceDebeziumObjectDesc }}</tr>
                    <tr v-if="showSourceDebeziumObject">Offset方式：{{ taskDetail.sourceOffsetStorageDesc }}</tr>
                    <tr v-if="showSourceDebeziumObject">初始化快照：{{ taskDetail.sourceDebeziumSnapshotDesc }}</tr>
                    <tr v-if="taskDetail.taskType === 4">迁移对象：{{ taskDetail.sourceDataBaseObjectTypeDesc }}</tr>
                  </table>
                </div>

                <!-- 目标端数据源 -->
                <div style="height: 50%; padding: 20px;">
                  <label class="fontWeight">目标端数据源</label>
                  <table class="mt-10 detail-table">
                    <tr>目标对象类型：{{ taskDetail.targetDataSourceTypeDesc }}</tr>
                    <tr>目标对象配置名称：{{ taskDetail.targetDataSourceDesc }}</tr>
                    <tr>目标对象实例名称：{{ taskDetail.targetObjectName }}</tr>
                    <tr v-if="taskDetail.taskType === 2" >校验目标数据：{{ taskDetail.targetCheckFlagDesc }}</tr>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 任务启动日志 -->
        <div style="margin: 20px;">
          <h1 class="title1">任务启动日志：</h1>
          <el-divider />
          <el-table :data="taskExecuteList" fit stripe highlight-current-row style="width: 100%;"
                    :cell-style="{ wordBreak: 'break-all', whiteSpace: 'pre-wrap' }">
            <el-table-column prop="taskExecuteId" label="序列" />
            <el-table-column prop="createDate" label="创建时间" />
            <el-table-column label="状态">
              <template #default="scope">
                <TaskStateText :state="scope.row.state" />
              </template>
            </el-table-column>
            <el-table-column prop="stateDate" label="状态时间" />
            <el-table-column prop="hostName" label="主机名称" />
            <el-table-column prop="hostIp" label="主机IP" />
          </el-table>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { computed } from 'vue'
import TaskStateText from '@/views/components/common/TaskStateText.vue'
import TaskProgressCard from '@/views/components/common/TaskProgressCard.vue'

export default {
  name: 'TaskDetailDrawer',
  components: {
    TaskStateText,
    TaskProgressCard
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    taskDetail: {
      type: Object,
      required: true
    },
    taskExecuteList: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const visible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const showSourceLoadStrategy = computed(() => {
      return [1, 2, 3].includes(props.taskDetail.taskType)
    })

    const showSourceDebeziumObject = computed(() => {
      return [6].includes(props.taskDetail.taskType)
    })

    const isNotEmpty = (value) => {
      return value != null && value !== ''
    }

    return {
      visible,
      showSourceLoadStrategy,
      showSourceDebeziumObject,
      isNotEmpty
    }
  }
}
</script>

<style scoped>
.bcFFF {
  background-color: #fff;
}

.margin-10 {
  margin: 10px;
}

.pl-20 {
  padding-left: 20px;
}

.pt-20 {
  padding-top: 20px;
}

.mt-10 {
  margin-top: 10px;
}

.mt-20 {
  margin-top: 20px;
}

.fontWeight {
  font-weight: bold;
}

.title1 {
  font-size: 18px;
  font-weight: bold;
  color: var(--primary-color);
}

.detail-card {
  display: flex;
  justify-content: flex-start;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,.1);
}

.detail-table {
  border-collapse: separate;
  border-spacing: 0px 15px;
  table-layout: fixed;
  overflow: hidden;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>

