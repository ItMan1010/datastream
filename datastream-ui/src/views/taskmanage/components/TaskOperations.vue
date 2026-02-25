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
  <div class="task-operations">
    <!-- 详情 -->
    <el-tooltip content="详情" placement="top">
      <el-button @click="$emit('detail', row)" type="primary" link size="small">
        <el-icon><ChatLineSquare /></el-icon>
      </el-button>
    </el-tooltip>

    <!-- 观测 -->
    <el-tooltip content="观测" placement="top">
      <el-button @click="$emit('observe', row)" type="primary" link size="small">
        <el-icon><View /></el-icon>
      </el-button>
    </el-tooltip>

    <!-- 暂停（等待运行/运行中） -->
    <el-tooltip v-if="row.state === 0 || row.state === 1" content="暂停" placement="top">
      <el-button @click="$emit('pause', row)" type="warning" link size="small">
        <el-icon><VideoPause /></el-icon>
      </el-button>
    </el-tooltip>

    <!-- 重启（运行失败/运行暂停） -->
    <el-tooltip v-if="row.state === 3 || row.state === 4" content="重启" placement="top">
      <el-button @click="$emit('restart', row)" type="success" link size="small">
        <el-icon><VideoPlay /></el-icon>
      </el-button>
    </el-tooltip>

    <!-- 复制 -->
    <el-tooltip content="复制" placement="top">
      <el-button @click="$emit('copy', row)" type="info" link size="small">
        <el-icon><CopyDocument /></el-icon>
      </el-button>
    </el-tooltip>

    <!-- 差异（数据稽核任务） -->
    <el-tooltip v-if="row.taskType === 5" content="差异" placement="top">
      <el-button @click="$emit('check', row)" type="primary" link size="small">
        <el-icon><Switch /></el-icon>
      </el-button>
    </el-tooltip>

    <!-- 日志 -->
    <el-tooltip content="日志" placement="top">
      <el-button @click="$emit('log', row)" type="primary" link size="small">
        <el-icon><Tickets /></el-icon>
      </el-button>
    </el-tooltip>
  </div>
</template>

<script>
import {
  ChatLineSquare,
  View,
  VideoPause,
  VideoPlay,
  CopyDocument,
  Switch,
  Tickets
} from '@element-plus/icons-vue'

export default {
  name: 'TaskOperations',
  components: {
    ChatLineSquare,
    View,
    VideoPause,
    VideoPlay,
    CopyDocument,
    Switch,
    Tickets
  },
  props: {
    row: {
      type: Object,
      required: true
    }
  },
  emits: ['detail', 'observe', 'pause', 'restart', 'copy', 'check', 'log']
}
</script>

<style scoped>
.task-operations {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.task-operations .el-button {
  margin-right: 5px;
}
</style>

