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
        <label class="fontWeight">迁移任务</label> > 任务执行日志
        <el-button type="text" style="float: right; font-size: 18px;" circle @click="visible = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="margin-10 bcFFF log-container">
        <div class="exceed-line-feed">
          <li v-for="(item, idx) in taskLogInfo" :key="idx">{{ item }}</li>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { computed } from 'vue'
import { Close } from '@element-plus/icons-vue'

export default {
  name: 'TaskLogDrawer',
  components: {
    Close
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    taskLogInfo: {
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

    return {
      visible
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

.fontWeight {
  font-weight: bold;
}

.log-container {
  overflow-y: auto !important;
  overflow-x: hidden !important;
  padding: 20px;
  position: absolute;
  inset: 50px 0px 10px;
  background-color: #000 !important;
  color: #fff;
}

.exceed-line-feed {
  white-space: pre-wrap;
  word-wrap: break-word;
}

.exceed-line-feed li {
  list-style: none;
  margin: 2px 0;
}
</style>

