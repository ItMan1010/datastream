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
  <div style="height: 100%;">
    <div class="main-content">
      <!-- 搜索区域 -->
      <div class="search-nav">
        <el-form ref="queryFormRef" :label-position="'left'" :inline="true" :model="queryForm">
          <el-row style="line-height: 30px;">
            <el-col :span="18">
              <el-form-item label="" style="margin-right: 0;">
                <el-select v-model="queryForm.queryFlag" style="width: 120px;" @change="changeQueryFlag">
                  <el-option v-for="opt in queryFlagOptions" :key="opt.value" :label="opt.label" :value="opt.value"/>
                </el-select>
              </el-form-item>
              <el-form-item label="" style="margin-right: 0;">
                <el-input
                  v-if="queryForm.queryFlag === '2'"
                  class="ml-20"
                  type="number"
                  v-model="queryForm.queryValue"
                  clearable
                  placeholder="请输入Mq ID"
                  style="width: 240px;"/>
                <el-input
                  v-if="queryForm.queryFlag === '3'"
                  class="ml-20"
                  v-model="queryForm.queryValue"
                  clearable
                  placeholder="请输入实例名称"
                  style="width: 240px;"/>
              </el-form-item>
              <el-form-item>
                <el-button class="ml-20" type="primary" :loading="loading" @click="queryMqRows">
                  <el-icon>
                    <Search/>
                  </el-icon>
                  查询
                </el-button>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-button style="float: right; margin-right: 300px;" type="primary" plain @click="addMqConfig">
                <el-icon>
                  <Plus/>
                </el-icon>
                新增
              </el-button>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <el-divider/>

      <!-- 数据表格 -->
      <div class="mt-10 pl-20 pr-20">
        <el-table :data="mqConfigList" fit stripe highlight-current-row style="width: 100%;">
          <el-table-column prop="mqConfigId" label="MQ ID" width="100"/>
          <el-table-column label="类型" width="80">
            <template #default="scope">
              <el-tag v-if="scope.row.mqType === 10" type="info">kafka</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="mqConfigName" label="实例名称" width="100" :show-overflow-tooltip="true"/>
          <el-table-column prop="bootstrapServers" label="服务地址" min-width="100" :show-overflow-tooltip="true"/>
          <el-table-column label="报文格式" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.messageFormat === 1" type="success" effect="dark">JSON</el-tag>
              <el-tag v-else-if="scope.row.messageFormat === 2" type="warning" effect="dark">分隔符</el-tag>
              <el-tag v-else type="info" effect="dark">未知</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="systemUserCode" label="创建用户" width="100"/>
          <el-table-column prop="createDate" label="创建时间" width="160"/>
          <el-table-column label="状态" width="80">
            <template #default="scope">
              <el-tag v-if="scope.row.onLineFlag === 2" type="success" effect="dark">上线</el-tag>
              <el-tag v-else type="info" effect="dark">下线</el-tag>
            </template>
          </el-table-column>
          <el-table-column fixed="right" label="操作" width="400">
            <template #default="scope">
              <div class="opt-btns">
                <el-button type="primary" size="small" @click="showMqDetail(scope.row)" plain>
                  <el-icon>
                    <View/>
                  </el-icon>
                  详情
                </el-button>
                <el-button type="success" size="small" @click="testMqConfig(scope.row)" plain>
                  <el-icon>
                    <Connection/>
                  </el-icon>
                  校验
                </el-button>
                <el-button v-if="scope.row.onLineFlag === 1" type="success" size="small" @click="execOnLine(scope.row)"
                           plain>
                  <el-icon>
                    <Switch/>
                  </el-icon>
                  上线
                </el-button>
                <el-button v-if="scope.row.onLineFlag === 2" type="info" size="small" @click="execOffLine(scope.row)"
                           plain>
                  <el-icon>
                    <Switch/>
                  </el-icon>
                  下线
                </el-button>
                <el-button type="warning" size="small" @click="modifyMqConfig(scope.row)" plain>
                  <el-icon>
                    <Edit/>
                  </el-icon>
                  修改
                </el-button>
                <el-button type="danger" size="small" @click="deleteMqConfig(scope.row)" plain>
                  <el-icon>
                    <Delete/>
                  </el-icon>
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          v-model:current-page="pagination.pageNum.value"
          :page-sizes="[10, 20, 50]"
          :page-size="pagination.pageSize.value"
          :total="pagination.total.value"
          layout="total, sizes, prev, pager, next, jumper"/>
      </div>
    </div>

    <!-- Mq配置详情弹窗 -->
    <MqConfigDetail
      v-if="mqDetailVisible"
      :mq-detail-visible="mqDetailVisible"
      :mode="mode"
      :mq-config-detail="mqConfigDetail"/>
  </div>
</template>

<script>
import {onMounted, onActivated} from 'vue'
import {Search, Plus, View, Connection, Edit, Delete} from '@element-plus/icons-vue'
import MqConfigDetail from './mqConfigDetail.vue'
import {useMqManage} from '@/composables/useMqManage'
import {useEventBus} from '@/composables/useEventBus'

export default {
  name: 'MqManage',
  components: {
    MqConfigDetail: MqConfigDetail,
    Search,
    Plus,
    View,
    Connection,
    Edit,
    Delete
  },
  setup() {
    const mqManage = useMqManage()
    const {on} = useEventBus()

    // 分页处理
    const handleSizeChange = (val) => {
      mqManage.pagination.pageSize.value = val
      mqManage.queryMqRows()
    }

    const handleCurrentChange = (val) => {
      mqManage.pagination.pageNum.value = val
      mqManage.queryMqRows()
    }

    onMounted(() => {
      on('changeMqDetailVisible', (visible) => {
        mqManage.closeDetail(visible)
      })
      mqManage.changeQueryFlag()
    })

    onActivated(() => {
      mqManage.mqDetailVisible.value = false
    })

    return {
      ...mqManage,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.main-content {
  padding: 20px;
}

.search-nav {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
}

.ml-20 {
  margin-left: 20px;
}

.mt-10 {
  margin-top: 10px;
}

.pl-20 {
  padding-left: 20px;
}

.pr-20 {
  padding-right: 20px;
}

.opt-btns {
  display: flex;
  gap: 8px;
}

.pagination-container {
  text-align: center;
  padding: 20px 0;
}
</style>

