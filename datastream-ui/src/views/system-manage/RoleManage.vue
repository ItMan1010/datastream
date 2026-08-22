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
    <div class="search-nav">
      <el-form :label-position="'left'" :inline="true">
        <el-form-item style="margin-right: 0;">
          <el-select v-model="queryForm.queryFlag" style="width: 140px;" @change="changeQueryFlag">
            <el-option v-for="opt in queryFlagOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item style="margin-right: 0;">
          <el-input
            v-if="queryForm.queryFlag !== '1'"
            class="ml-20"
            v-model="queryForm.queryValue"
            clearable
            :placeholder="queryForm.queryFlag === '2' ? '请输入角色编码' : '请输入角色名称'"
            style="width: 240px;" />
        </el-form-item>
        <el-form-item>
          <el-button class="ml-20" type="primary" :loading="loading" @click="loadRoleRows">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button class="ml-20" type="primary" plain @click="openAddRole">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-divider />

    <div class="mt-10 pl-20 pr-20">
      <el-table :data="roleList" fit stripe highlight-current-row style="width: 100%;">
        <el-table-column prop="roleId" label="ID" width="80" :show-overflow-tooltip="true" />
        <el-table-column prop="roleCode" label="角色编码" width="160" />
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="description" label="描述" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column label="类型" width="90">
          <template #default="scope">
            <el-tag :type="isBuiltInRole(scope.row) ? 'warning' : 'info'" effect="plain">
              {{ builtInLabel(scope.row.builtIn) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createDate" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <div class="datasource-opt">
              <el-button
                type="warning"
                size="small"
                :disabled="isBuiltInRole(scope.row)"
                @click="openModifyRole(scope.row)"
                plain>
                <el-icon><Edit /></el-icon>
                修改
              </el-button>
              <el-button type="primary" size="small" @click="openAssignPermissions(scope.row)" plain>
                权限授权
              </el-button>
              <el-button
                type="danger"
                size="small"
                :disabled="isBuiltInRole(scope.row)"
                @click="removeRole(scope.row)"
                plain>
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <PagePagination
        :page-num="pagination.pageNum.value"
        :page-size="pagination.pageSize.value"
        :total="pagination.total.value"
        @size-change="handleSizeChange"
        @current-change="handlePageChange" />
    </div>

    <!-- 角色表单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增角色' : '修改角色'"
      width="520px"
      destroy-on-close>
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色编码" required>
          <el-input v-model="roleForm.roleCode" :disabled="dialogMode === 'modify'" placeholder="如 OPERATOR" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" placeholder="如 操作员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="角色职责说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="saveRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限授权弹窗 -->
    <el-dialog
      v-model="assignDialogVisible"
      :title="`权限授权（${assignRoleName}）`"
      width="680px"
      destroy-on-close>
      <el-tabs type="border-card">
        <el-tab-pane label="菜单权限">
          <el-tree
            ref="menuTreeRef"
            :data="menuTreeData"
            node-key="permissionId"
            show-checkbox
            default-expand-all
            :default-checked-keys="assignedMenuIds"
            :props="{ label: 'permissionName', children: 'children' }" />
        </el-tab-pane>
        <el-tab-pane label="数据权限">
          <el-checkbox-group v-model="assignedDataPermissionIds">
            <div v-for="perm in dataPermissionList" :key="perm.permissionId" class="data-perm-item">
              <el-checkbox :label="perm.permissionId">
                <span class="data-perm-name">{{ perm.permissionName }}</span>
                <span class="data-perm-code">{{ perm.permissionCode }}</span>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="saveAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onActivated } from 'vue'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import PagePagination from '@/views/components/common/PagePagination.vue'
import { useRoleManage } from '@/composables/useRoleManage'

export default {
  name: 'RoleManage',
  components: {
    PagePagination,
    Search,
    Plus,
    Edit,
    Delete
  },
  setup() {
    const {
      loading,
      pagination,
      roleList,
      queryForm,
      dialogVisible,
      dialogMode,
      roleForm,
      assignDialogVisible,
      assignRoleName,
      menuTreeData,
      assignedMenuIds,
      dataPermissionList,
      assignedDataPermissionIds,
      queryFlagOptions,
      changeQueryFlag,
      loadRoleRows,
      openAddRole,
      openModifyRole,
      saveRole,
      removeRole,
      openAssignPermissions,
      saveAssignPermissions,
      isBuiltInRole,
      builtInLabel
    } = useRoleManage()

    const menuTreeRef = ref(null)

    const handlePageChange = (page) => {
      pagination.pageNum.value = page
      loadRoleRows()
    }

    const handleSizeChange = (size) => {
      pagination.pageSize.value = size
      pagination.pageNum.value = 1
      loadRoleRows()
    }

    const saveAssign = () => {
      const checked = menuTreeRef.value ? menuTreeRef.value.getCheckedKeys() : []
      const halfChecked = menuTreeRef.value ? menuTreeRef.value.getHalfCheckedKeys() : []
      const menuCheckedIds = [...new Set([...checked, ...halfChecked])]
      saveAssignPermissions(menuCheckedIds)
    }

    onMounted(() => {
      changeQueryFlag()
    })

    onActivated(() => {
      dialogVisible.value = false
      assignDialogVisible.value = false
    })

    return {
      loading,
      pagination,
      roleList,
      queryForm,
      dialogVisible,
      dialogMode,
      roleForm,
      assignDialogVisible,
      assignRoleName,
      menuTreeData,
      assignedMenuIds,
      dataPermissionList,
      assignedDataPermissionIds,
      queryFlagOptions,
      changeQueryFlag,
      loadRoleRows,
      openAddRole,
      openModifyRole,
      saveRole,
      removeRole,
      openAssignPermissions,
      isBuiltInRole,
      builtInLabel,
      menuTreeRef,
      saveAssign,
      handlePageChange,
      handleSizeChange
    }
  }
}
</script>

<style scoped>
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
  flex-wrap: wrap;
  gap: 4px;
}

.data-perm-item {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.data-perm-name {
  font-weight: 600;
  margin-right: 8px;
}

.data-perm-code {
  color: #909399;
  font-size: 12px;
}
</style>
