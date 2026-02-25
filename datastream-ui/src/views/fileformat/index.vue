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
    <transition name="component-fade" mode="out-in">
      <div style="height: 100%;" :key="fileDetailVisible ? 'config' : 'list'">
        <!-- 列表页面 -->
        <div v-if="!fileDetailVisible" style="height: 100%;">
          <div class="main-content">
            <!-- 搜索区域 -->
            <div class="search-nav">
              <el-form ref="queryFormRef" :label-position="'left'" :inline="true" :model="queryForm">
                <el-row style="line-height: 30px;">
                  <el-col :span="18">
                    <el-form-item label="" style="margin-right: 0;">
                      <el-select v-model="queryForm.queryFlag" style="width: 100px;" @change="changeQueryFlag">
                        <el-option v-for="opt in queryFlagOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="" style="margin-right: 0;">
                      <el-input
                        v-if="queryForm.queryFlag === '2'"
                        class="ml-20"
                        type="number"
                        v-model="queryForm.queryValue"
                        onkeyup="value = value.replace(/[^\d]/g,'')"
                        clearable
                        placeholder="请输入文件ID"
                        style="width: 240px;" />
                      <el-input
                        v-if="queryForm.queryFlag === '3'"
                        class="ml-20"
                        v-model="queryForm.queryValue"
                        clearable
                        placeholder="请输入文件名称"
                        style="width: 240px;" />
                    </el-form-item>
                    <el-form-item>
                      <el-button class="ml-20" type="primary" :loading="loading" @click="queryFileRows">
                        <el-icon><Search /></el-icon>
                        查询
                      </el-button>
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-button style="float: right; margin-right: 300px;" type="primary" plain @click="addFileDefine">
                      <el-icon><Plus /></el-icon>
                      新增
                    </el-button>
                  </el-col>
                </el-row>
              </el-form>
            </div>

            <el-divider />

            <!-- 数据表格 -->
            <div class="mt-10 pl-20 pr-20">
              <el-table :data="fileFormatList" fit stripe highlight-current-row style="width: 100%;">
                <el-table-column prop="fileFormatId" label="文件ID" width="60" :show-overflow-tooltip="true" />
                <el-table-column prop="fileNameFormat" label="配置名称" width="160" />
                <el-table-column label="类型" width="80">
                  <template #default="scope">
                    <el-tag v-if="scope.row.fileType === 8" type="info" effect="dark">text</el-tag>
                    <el-tag v-if="scope.row.fileType === 9" type="info" effect="dark">excel</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createDate" label="创建时间" width="160" />
                <el-table-column label="状态" width="80">
                  <template #default="scope">
                    <el-tag v-if="scope.row.onLineFlag === 2" type="success" effect="dark">上线</el-tag>
                    <el-tag v-if="scope.row.onLineFlag === 1" type="info" effect="dark">下线</el-tag>
                    <el-tag v-if="scope.row.onLineFlag === 0" type="info" effect="dark">删除</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="stateDate" label="状态时间" width="160" />
                <el-table-column fixed="right" label="操作" width="450">
                  <template #default="scope">
                    <div class="datasource-opt">
                      <el-button type="primary" size="small" @click="showFileDetail(scope.row)" plain>
                        <el-icon><Search /></el-icon>
                        详情
                      </el-button>
                      <el-button type="info" size="small" @click="validateFile(scope.row)" plain>
                        校验
                      </el-button>
                      <el-button v-if="scope.row.onLineFlag === 1" type="success" size="small" @click="execEff(scope.row)" plain>
                        <el-icon><Switch /></el-icon>
                        上线
                      </el-button>
                      <el-button v-if="scope.row.onLineFlag === 2" type="info" size="small" @click="execExp(scope.row)" plain>
                        <el-icon><Switch /></el-icon>
                        下线
                      </el-button>
                      <el-button type="warning" size="small" @click="modifyFile(scope.row)" plain>
                        <el-icon><Edit /></el-icon>
                        修改
                      </el-button>
                      <el-button type="danger" size="small" @click="deleteFile(scope.row)" plain>
                        <el-icon><Delete /></el-icon>
                        删除
                      </el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 文件格式详情弹窗 -->
    <FileFormatDetail
      v-if="fileDetailVisible"
      :fileDetailVisible="fileDetailVisible"
      :mode="mode"
      :fileFormatDetail="fileFormatDetail" />
  </div>
</template>

<script>
import { onMounted, onActivated } from 'vue'
import { Search, Plus, Switch, Edit, Delete } from '@element-plus/icons-vue'
import FileFormatDetail from '@/views/fileformat/FileFormatDetail.vue'
import { useFileFormatManage } from '@/composables/useFileFormatManage'
import { useEventBus } from '@/composables/useEventBus'

export default {
  name: 'FileFormatConfig',
  components: {
    FileFormatDetail,
    Search,
    Plus,
    Switch,
    Edit,
    Delete
  },
  setup() {
    const fileFormatManage = useFileFormatManage()
    const { on } = useEventBus()

    onMounted(() => {
      // 监听事件
      on('changeFileFormatDetailVisible', (visible) => {
        fileFormatManage.closeDetail(visible)
      })

      // 查询数据
      fileFormatManage.changeQueryFlag()
    })

    onActivated(() => {
      fileFormatManage.fileDetailVisible.value = false
    })

    return {
      ...fileFormatManage
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

.datasource-opt {
  display: flex;
  justify-content: space-around;
}

/* 过渡动画 */
.component-fade-enter-active,
.component-fade-leave-active {
  transition: opacity 0.3s ease;
}

.component-fade-enter-from,
.component-fade-leave-to {
  opacity: 0;
}
</style>

