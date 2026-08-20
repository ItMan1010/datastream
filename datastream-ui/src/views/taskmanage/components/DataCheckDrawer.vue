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
        <label class="fontWeight">稽核任务</label> >
        <span>数据稽核差异</span>
        <el-button type="text" style="float: right; font-size: 18px;" circle @click="visible = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="pt-10 margin-10 bcFFF pb-20"
           style="position: absolute; bottom: 10px; top: 50px; left: 0; right: 0;">
        <div style="height: 90%; padding: 10px 20px;">
          <el-table :data="dataCheckListData" fit stripe height="100%" highlight-current-row style="width: 100%;"
                    :cell-style="{ wordBreak: 'break-all', whiteSpace: 'pre-wrap' }">
            <el-table-column prop="dataCheckId" label="差异ID" width="120" />
            <el-table-column prop="createDate" label="创建时间" width="160" />
            <el-table-column prop="stateDesc" label="状态" width="160" />
            <el-table-column prop="stateDate" label="状态时间" width="160" />
            <el-table-column prop="checkResultDesc" label="差异结果" width="160" />
            <el-table-column prop="checkKeys" label="稽核主键" width="160" />
            <el-table-column prop="errorCode" label="错误编码" width="120" :show-overflow-tooltip="true" />
            <el-table-column prop="errorMsg" label="错误信息" min-width="200" :show-overflow-tooltip="true" />
            <el-table-column label="修订" width="80">
              <template #default="scope">
                <el-tooltip content="修订" placement="top">
                  <el-button @click="handleRepair(scope.row)" type="primary" link size="small">
                    <el-icon><Check /></el-icon>
                  </el-button>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
          <el-button v-if="dataCheckListData.length > 0" type="primary" :loading="loading" style="width: 100px; margin-top: 10px;"
                     @click="handleRepairAll">一键修订</el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { computed } from 'vue'
import { Close, Check } from '@element-plus/icons-vue'

export default {
  name: 'DataCheckDrawer',
  components: {
    Close,
    Check
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    dataCheckListData: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'repair', 'repair-all'],
  setup(props, { emit }) {
    const visible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const handleRepair = (row) => {
      emit('repair', 1, row.taskId, row.dataCheckId)
    }

    const handleRepairAll = () => {
      if (props.dataCheckListData.length > 0) {
        emit('repair-all', 2, props.dataCheckListData[0].taskId, null)
      }
    }

    return {
      visible,
      handleRepair,
      handleRepairAll
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

.pt-10 {
  padding-top: 10px;
}

.pb-20 {
  padding-bottom: 20px;
}

.fontWeight {
  font-weight: bold;
}
</style>

