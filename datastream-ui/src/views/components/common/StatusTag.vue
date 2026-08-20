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
  <el-tag :type="tagType" :effect="effect" :size="size">
    {{ displayText }}
  </el-tag>
</template>

<script>
/**
 * 通用状态标签组件
 * 支持任务状态、数据库状态、文件状态等多种类型
 */
import { computed } from 'vue'
import { getTaskStateInfo } from '@/constants/taskConstants'
import { getDatabaseStateInfo } from '@/constants/databaseConstants'
import { getFileStateInfo } from '@/constants/fileConstants'

export default {
  name: 'StatusTag',
  props: {
    // 状态值
    status: {
      type: [Number, String],
      required: true
    },
    // 状态类型: task, database, file, custom
    type: {
      type: String,
      default: 'task'
    },
    // 自定义状态映射
    customMap: {
      type: Object,
      default: null
    },
    // tag 效果
    effect: {
      type: String,
      default: 'dark'
    },
    // tag 大小
    size: {
      type: String,
      default: 'default'
    }
  },
  setup(props) {
    const statusInfo = computed(() => {
      const status = Number(props.status)

      if (props.customMap) {
        return props.customMap[status] || { text: '未知', type: 'info' }
      }

      switch (props.type) {
        case 'task':
          const taskInfo = getTaskStateInfo(status)
          return { text: taskInfo.text, type: getElTagType(taskInfo.color) }
        case 'database':
          return getDatabaseStateInfo(status)
        case 'file':
          return getFileStateInfo(status)
        default:
          return { text: '未知', type: 'info' }
      }
    })

    const displayText = computed(() => statusInfo.value.text)
    const tagType = computed(() => statusInfo.value.type || 'info')

    // 将颜色转换为 el-tag type
    function getElTagType(color) {
      if (!color) return 'info'
      const colorMap = {
        '#0099CC': 'primary',
        '#2563EB': 'primary',
        '#67C23A': 'success',
        '#FF0000': 'danger',
        '#ffa07a': 'warning'
      }
      return colorMap[color] || 'info'
    }

    return {
      displayText,
      tagType
    }
  }
}
</script>

