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
            :placeholder="queryForm.queryFlag === '2' ? '请输入账号' : '请输入名称'"
            style="width: 240px;" />
        </el-form-item>
        <el-form-item>
          <el-button class="ml-20" type="primary" :loading="loading" @click="loadUserRows">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button class="ml-20" type="primary" plain @click="openAddUser">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-divider />

    <div class="mt-10 pl-20 pr-20">
      <el-table :data="userList" fit stripe highlight-current-row style="width: 100%;">
        <el-table-column prop="systemUserId" label="ID" width="80" :show-overflow-tooltip="true" />
        <el-table-column prop="systemUserCode" label="账号" width="140" />
        <el-table-column prop="systemUserName" label="名称" width="140" />
        <el-table-column prop="orgName" label="机构" width="140" :show-overflow-tooltip="true" />
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="stateTagType(scope.row.state)" effect="plain">
              {{ stateLabel(scope.row.state) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createDate" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="scope">
            <div class="datasource-opt">
              <el-button type="warning" size="small" @click="openModifyUser(scope.row)" plain>
                <el-icon><Edit /></el-icon>
                修改
              </el-button>
              <el-button
                type="info"
                size="small"
                :disabled="isBuiltInAdmin(scope.row) && scope.row.state === 1"
                @click="changeState(scope.row)"
                plain>
                {{ scope.row.state === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="primary" size="small" @click="openResetPassword(scope.row)" plain>
                重置密码
              </el-button>
              <el-button
                type="danger"
                size="small"
                :disabled="isBuiltInAdmin(scope.row)"
                @click="removeUser(scope.row)"
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

    <!-- 用户表单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增用户' : '修改用户'"
      width="620px"
      destroy-on-close>
      <el-form :model="userForm" label-width="100px">
        <el-form-item label="登录账号" required>
          <el-input v-model="userForm.systemUserCode" :disabled="dialogMode === 'modify'" placeholder="如 zhangsan" />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="userForm.systemUserName" placeholder="如 张三" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'add'" label="登录密码" required>
          <el-input v-model="userForm.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="机构名称">
          <el-input v-model="userForm.orgName" placeholder="所属机构" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userForm.state">
            <el-radio v-for="opt in stateOptions" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoleIds" multiple filterable placeholder="请选择角色" style="width: 100%;">
            <el-option
              v-for="role in allRoleOptions"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="saveUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetDialogVisible" title="重置密码" width="460px" destroy-on-close>
      <el-form :model="resetForm" label-width="100px">
        <el-form-item label="用户">
          <span>{{ resetForm.systemUserName }}</span>
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="resetForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="doResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { onMounted, onActivated } from 'vue'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import PagePagination from '@/views/components/common/PagePagination.vue'
import { useUserManage } from '@/composables/useUserManage'

export default {
  name: 'UserManage',
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
      userList,
      queryForm,
      allRoleOptions,
      dialogVisible,
      dialogMode,
      userForm,
      selectedRoleIds,
      resetDialogVisible,
      resetForm,
      queryFlagOptions,
      stateOptions,
      changeQueryFlag,
      loadUserRows,
      openAddUser,
      openModifyUser,
      saveUser,
      changeState,
      openResetPassword,
      doResetPassword,
      removeUser,
      isBuiltInAdmin,
      stateLabel,
      stateTagType
    } = useUserManage()

    const handlePageChange = (page) => {
      pagination.pageNum.value = page
      loadUserRows()
    }

    const handleSizeChange = (size) => {
      pagination.pageSize.value = size
      pagination.pageNum.value = 1
      loadUserRows()
    }

    onMounted(() => {
      changeQueryFlag()
    })

    onActivated(() => {
      dialogVisible.value = false
      resetDialogVisible.value = false
    })

    return {
      loading,
      pagination,
      userList,
      queryForm,
      allRoleOptions,
      dialogVisible,
      dialogMode,
      userForm,
      selectedRoleIds,
      resetDialogVisible,
      resetForm,
      queryFlagOptions,
      stateOptions,
      changeQueryFlag,
      loadUserRows,
      openAddUser,
      openModifyUser,
      saveUser,
      changeState,
      openResetPassword,
      doResetPassword,
      removeUser,
      isBuiltInAdmin,
      stateLabel,
      stateTagType,
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
</style>