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
  <div class="search-form-container">
    <el-form
      ref="formRef"
      :model="modelValue"
      :inline="inline"
      :label-position="labelPosition"
      :label-width="labelWidth"
      @keyup.enter="handleSearch"
      class="search-form">
      <slot></slot>

      <!-- 默认操作按钮 -->
      <el-form-item v-if="showActions" class="form-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">
          <el-icon><Search /></el-icon>
          {{ searchText }}
        </el-button>
        <el-button v-if="showReset" @click="handleReset">
          <el-icon><Refresh /></el-icon>
          {{ resetText }}
        </el-button>
        <slot name="extra-actions"></slot>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
/**
 * 通用搜索表单组件
 */
import { ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'SearchForm',
  components: {
    Search,
    Refresh
  },
  props: {
    modelValue: {
      type: Object,
      default: () => ({})
    },
    inline: {
      type: Boolean,
      default: true
    },
    labelPosition: {
      type: String,
      default: 'left'
    },
    labelWidth: {
      type: String,
      default: 'auto'
    },
    loading: {
      type: Boolean,
      default: false
    },
    showActions: {
      type: Boolean,
      default: true
    },
    showReset: {
      type: Boolean,
      default: false
    },
    searchText: {
      type: String,
      default: '查询'
    },
    resetText: {
      type: String,
      default: '重置'
    }
  },
  emits: ['search', 'reset', 'update:modelValue'],
  setup(props, { emit }) {
    const formRef = ref(null)

    const handleSearch = () => {
      emit('search')
    }

    const handleReset = () => {
      emit('reset')
    }

    return {
      formRef,
      handleSearch,
      handleReset
    }
  }
}
</script>

<style scoped>
.search-form-container {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.form-actions {
  margin-left: auto;
}
</style>

