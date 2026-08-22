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
  <div style="height: 100%; padding: 20px;">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="菜单权限" name="menu">
        <div class="toolbar">
          <el-button type="primary" plain @click="openAddPermission(1, null)">
            <el-icon><Plus /></el-icon>
            新增顶级菜单
          </el-button>
        </div>
        <div class="mt-10">
          <el-table
            :data="menuTreeData"
            row-key="permissionId"
            :tree-props="{ children: 'children' }"
            fit
            stripe
            highlight-current-row
            style="width: 100%;">
            <el-table-column prop="permissionName" label="菜单名称" width="180" />
            <el-table-column prop="permissionCode" label="权限编码" width="180" />
            <el-table-column prop="route" label="路由名称" width="160" />
            <el-table-column prop="sortNo" label="排序" width="80" />
            <el-table-column label="类型" min-width="90">
              <template #default="scope">
                <el-tag :type="isBuiltInPermission(scope.row) ? 'warning' : 'info'" effect="plain">
                  {{ builtInLabel(scope.row.builtIn) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="scope">
                <div class="datasource-opt">
                  <el-button type="primary" size="small" @click="openAddPermission(1, scope.row.permissionId)" plain>
                    新增子菜单
                  </el-button>
                  <el-button
                    type="warning"
                    size="small"
                    :disabled="isBuiltInPermission(scope.row)"
                    @click="openModifyPermission(scope.row)"
                    plain>
                    <el-icon><Edit /></el-icon>
                    修改
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    :disabled="isBuiltInPermission(scope.row)"
                    @click="removePermission(scope.row)"
                    plain>
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="数据权限" name="data">
        <div class="toolbar">
          <el-button type="primary" plain @click="openAddPermission(2, null)">
            <el-icon><Plus /></el-icon>
            新增数据权限
          </el-button>
        </div>
        <div class="mt-10">
          <el-table :data="dataPermissionList" fit stripe highlight-current-row style="width: 100%;">
            <el-table-column prop="permissionName" label="权限名称" width="180" />
            <el-table-column prop="permissionCode" label="权限编码" width="200" />
            <el-table-column prop="sortNo" label="排序" width="80" />
            <el-table-column label="类型" min-width="90">
              <template #default="scope">
                <el-tag :type="isBuiltInPermission(scope.row) ? 'warning' : 'info'" effect="plain">
                  {{ builtInLabel(scope.row.builtIn) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <div class="datasource-opt">
                  <el-button
                    type="warning"
                    size="small"
                    :disabled="isBuiltInPermission(scope.row)"
                    @click="openModifyPermission(scope.row)"
                    plain>
                    <el-icon><Edit /></el-icon>
                    修改
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    :disabled="isBuiltInPermission(scope.row)"
                    @click="removePermission(scope.row)"
                    plain>
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 权限表单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增权限' : '修改权限'"
      width="560px"
      destroy-on-close>
      <el-form :model="permissionForm" label-width="100px">
        <el-form-item label="权限类型">
          <el-radio-group v-model="permissionForm.permissionType" :disabled="dialogMode === 'modify'">
            <el-radio v-for="opt in permissionTypeOptions" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限编码" required>
          <el-input v-model="permissionForm.permissionCode" :disabled="dialogMode === 'modify'" placeholder="如 task:create" />
        </el-form-item>
        <el-form-item label="权限名称" required>
          <el-input v-model="permissionForm.permissionName" placeholder="如 创建迁移任务" />
        </el-form-item>
        <el-form-item v-if="permissionForm.permissionType === 1" label="父级菜单">
          <el-select v-model="permissionForm.parentId" clearable placeholder="请选择父级菜单" style="width: 100%;">
            <el-option
              v-for="opt in parentPermissionOptions"
              :key="String(opt.value)"
              :label="opt.label"
              :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="permissionForm.permissionType === 1" label="路由名称">
          <el-input v-model="permissionForm.route" placeholder="如 taskManage" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="permissionForm.sortNo" :min="1" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="savePermission">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { onMounted, onActivated } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { usePermissionManage } from '@/composables/usePermissionManage'

export default {
  name: 'PermissionManage',
  components: {
    Plus,
    Edit,
    Delete
  },
  setup() {
    const {
      loading,
      activeTab,
      menuTreeData,
      dataPermissionList,
      dialogVisible,
      dialogMode,
      permissionForm,
      permissionTypeOptions,
      parentPermissionOptions,
      reload,
      openAddPermission,
      openModifyPermission,
      savePermission,
      removePermission,
      isBuiltInPermission,
      builtInLabel
    } = usePermissionManage()

    onMounted(() => {
      reload()
    })

    onActivated(() => {
      dialogVisible.value = false
    })

    return {
      loading,
      activeTab,
      menuTreeData,
      dataPermissionList,
      dialogVisible,
      dialogMode,
      permissionForm,
      permissionTypeOptions,
      parentPermissionOptions,
      openAddPermission,
      openModifyPermission,
      savePermission,
      removePermission,
      isBuiltInPermission,
      builtInLabel
    }
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
}

.mt-10 {
  margin-top: 10px;
}

.datasource-opt {
  display: flex;
  justify-content: flex-start;
  gap: 4px;
  flex-wrap: nowrap;
}
</style>
