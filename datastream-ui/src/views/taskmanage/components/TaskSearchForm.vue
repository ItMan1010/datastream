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
  <div class="search-nav">
    <el-form ref="queryForm" :model="queryForm" @keyup.enter="handleSearch">
      <el-row style="line-height: 30px;">
        <el-col :span="16">
          <el-select v-model="queryForm.queryFlag" placeholder="请选择查询类型" style="width: 130px !important;">
            <el-option v-for="opt in queryFlagOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>

          <!-- 任务标识 -->
          <el-input
            v-if="queryForm.queryFlag === '1'"
            class="ml-20"
            type="number"
            v-model="queryForm.taskId"
            onkeyup="value = value.replace(/[^\\d]/g,'')"
            clearable
            placeholder="请输入任务标识"
            style="width: 240px;" />

          <!-- 迁移表名 -->
          <el-input
            v-else-if="queryForm.queryFlag === '2'"
            class="ml-20"
            v-model="queryForm.tableName"
            clearable
            placeholder="请输入迁移表名"
            style="width: 240px;" />

          <!-- 任务状态 -->
          <el-select
            v-else-if="queryForm.queryFlag === '3'"
            class="ml-20"
            v-model="queryForm.state"
            style="width: 120px !important;">
            <el-option v-for="opt in taskStateOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>

          <!-- 迁移时间 -->
          <el-date-picker
            v-else-if="queryForm.queryFlag === '4'"
            class="ml-20"
            v-model="queryForm.moveDate"
            type="datetimerange"
            :shortcuts="pickerOptions.shortcuts"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            align="right" />

          <!-- 复制任务标识 -->
          <el-input
            v-if="queryForm.queryFlag === '6'"
            class="ml-20"
            type="number"
            v-model="queryForm.copyTaskId"
            onkeyup="value = value.replace(/[^\\d]/g,'')"
            clearable
            placeholder="请输入复制任务标识"
            style="width: 240px;" />

          <!-- 任务类型 -->
          <el-select
            v-if="queryForm.queryFlag === '7'"
            class="ml-20"
            v-model="queryForm.taskType"
            style="width: 120px !important;">
            <el-option v-for="opt in taskTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>

          <el-button class="ml-20" type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-col>

        <el-col :span="6">
          <el-button type="primary" plain @click="handleCreate">
            <el-icon><Plus /></el-icon>
            创建任务
          </el-button>
          <el-checkbox class="ml-20" v-model="refreshChecked">20秒定时刷新</el-checkbox>
          <el-button class="ml-10" type="primary" plain @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script>
import { ref, watch } from 'vue'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import { QUERY_FLAG_OPTIONS, TASK_STATE_OPTIONS, TASK_TYPE_OPTIONS, DATE_PICKER_SHORTCUTS } from '@/constants/taskConstants'

export default {
  name: 'TaskSearchForm',
  components: {
    Search,
    Plus,
    Refresh
  },
  props: {
    queryForm: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['search', 'create', 'refresh', 'update:refresh-checked'],
  setup(props, { emit }) {
    const refreshChecked = ref(false)

    const queryFlagOptions = QUERY_FLAG_OPTIONS
    const taskStateOptions = TASK_STATE_OPTIONS
    const taskTypeOptions = TASK_TYPE_OPTIONS
    const pickerOptions = {
      shortcuts: DATE_PICKER_SHORTCUTS
    }

    const handleSearch = () => {
      emit('search')
    }

    const handleCreate = () => {
      emit('create')
    }

    const handleRefresh = () => {
      emit('refresh', refreshChecked.value)
    }

    watch(refreshChecked, (val) => {
      emit('update:refresh-checked', val)
    })

    return {
      refreshChecked,
      queryFlagOptions,
      taskStateOptions,
      taskTypeOptions,
      pickerOptions,
      handleSearch,
      handleCreate,
      handleRefresh
    }
  }
}
</script>

<style scoped>
.search-nav {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.ml-20 {
  margin-left: 20px;
}

.ml-10 {
  margin-left: 10px;
}
</style>

