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
  <el-tooltip :content="tooltip" placement="top" :disabled="!tooltip">
    <el-button
      v-if="visible"
      :type="type"
      :link="link"
      :size="size"
      :loading="loading"
      :disabled="disabled"
      :plain="plain"
      @click="handleClick"
      class="action-button">
      <el-icon v-if="icon">
        <component :is="icon" />
      </el-icon>
      <span v-if="label">{{ label }}</span>
    </el-button>
  </el-tooltip>
</template>

<script>
/**
 * 通用操作按钮组件
 * 支持图标、提示、加载状态等
 */
export default {
  name: 'ActionButton',
  props: {
    // 按钮类型
    type: {
      type: String,
      default: 'primary'
    },
    // 按钮大小
    size: {
      type: String,
      default: 'small'
    },
    // 图标组件
    icon: {
      type: [Object, String],
      default: null
    },
    // 按钮文字
    label: {
      type: String,
      default: ''
    },
    // 提示文字
    tooltip: {
      type: String,
      default: ''
    },
    // 是否link样式
    link: {
      type: Boolean,
      default: true
    },
    // 是否plain样式
    plain: {
      type: Boolean,
      default: false
    },
    // 加载状态
    loading: {
      type: Boolean,
      default: false
    },
    // 禁用状态
    disabled: {
      type: Boolean,
      default: false
    },
    // 是否显示
    visible: {
      type: Boolean,
      default: true
    }
  },
  emits: ['click'],
  setup(props, { emit }) {
    const handleClick = () => {
      if (!props.disabled && !props.loading) {
        emit('click')
      }
    }

    return {
      handleClick
    }
  }
}
</script>

<style scoped>
.action-button {
  margin-right: 5px;
}
.action-button:last-child {
  margin-right: 0;
}
</style>

