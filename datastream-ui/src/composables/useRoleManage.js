/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 角色管理业务逻辑 Hook
 * 管理角色分页查询、新增/编辑、删除，以及角色-权限授权（菜单树 + 数据权限）。
 */
import { ref, reactive } from 'vue'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'
import {
  queryRoleRows,
  queryRoleInfo,
  addRole,
  modifyRole,
  delRole,
  assignRolePermissions,
  queryMenuTree,
  queryDataPermissionList
} from '@/api/rbac'

/**
 * 将扁平菜单权限列表按 parentId 组成嵌套树
 * @param {Array} flatList - 扁平菜单列表
 */
function buildMenuTree(flatList) {
  const nodes = (flatList || []).map(item => ({ ...item, children: [] }))
  const map = {}
  nodes.forEach(node => { map[node.permissionId] = node })
  const roots = []
  nodes.forEach(node => {
    if (node.parentId != null && map[node.parentId]) {
      map[node.parentId].children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

export function useRoleManage() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showSuccess, showError, showWarning, confirm, showSuccessWithCallback } = useMessage()

  const pagination = usePagination({ defaultPageSize: 10 })
  const roleList = ref([])
  const queryForm = reactive({ queryFlag: '1', queryValue: null })

  // 新增/编辑弹窗
  const dialogVisible = ref(false)
  const dialogMode = ref('add')
  const roleForm = reactive({})

  // 权限授权弹窗
  const assignDialogVisible = ref(false)
  const assignRoleId = ref(null)
  const assignRoleName = ref('')
  const menuTreeData = ref([])
  const assignedMenuIds = ref([])
  const dataPermissionList = ref([])
  const assignedDataPermissionIds = ref([])

  const queryFlagOptions = [
    { label: '全部', value: '1' },
    { label: '编码', value: '2' },
    { label: '名称', value: '3' }
  ]

  const isBuiltInRole = (row) => row && row.builtIn === 1
  const builtInLabel = (builtIn) => (builtIn === 1 ? '内置' : '自定义')

  const changeQueryFlag = () => {
    queryForm.queryValue = null
    if (queryForm.queryFlag === '1') {
      loadRoleRows()
    }
  }

  const loadRoleRows = async () => {
    if (queryForm.queryFlag !== '1' && !queryForm.queryValue) {
      showWarning('查询值不能为空')
      return
    }
    try {
      startLoading()
      const res = await queryRoleRows({
        queryFlag: queryForm.queryFlag,
        queryValue: queryForm.queryValue,
        page: pagination.pageNum.value,
        count: pagination.pageSize.value
      })
      if (res.errorCode !== '0') {
        showError(`查询角色失败：${res.errorMsg}`)
        return
      }
      roleList.value = res.roleList || []
      pagination.setTotal(res.total || 0)
    } catch (err) {
      showError(`查询角色失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const openAddRole = () => {
    dialogMode.value = 'add'
    Object.keys(roleForm).forEach(k => delete roleForm[k])
    dialogVisible.value = true
  }

  const openModifyRole = (row) => {
    dialogMode.value = 'modify'
    Object.keys(roleForm).forEach(k => delete roleForm[k])
    Object.assign(roleForm, row)
    dialogVisible.value = true
  }

  const saveRole = async () => {
    if (!roleForm.roleCode) {
      showWarning('角色编码不能为空')
      return
    }
    if (!roleForm.roleName) {
      showWarning('角色名称不能为空')
      return
    }
    try {
      startLoading()
      let res
      if (dialogMode.value === 'add') {
        res = await addRole(roleForm)
        if (res.errorCode !== '0') {
          showError(`新增角色失败：${res.errorMsg}`)
          return
        }
      } else {
        res = await modifyRole(roleForm)
        if (res.errorCode !== '0') {
          showError(`修改角色失败：${res.errorMsg}`)
          return
        }
      }
      showSuccessWithCallback(dialogMode.value === 'add' ? '新增角色成功' : '修改角色成功', () => {
        dialogVisible.value = false
        loadRoleRows()
      }, 1000)
    } catch (err) {
      showError(`${dialogMode.value === 'add' ? '新增' : '修改'}角色失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const removeRole = async (row) => {
    if (isBuiltInRole(row)) {
      showWarning('内置角色不可被删除')
      return
    }
    try {
      await confirm(`是否确认删除角色（${row.roleName || row.roleCode}）?`)
      startLoading()
      const res = await delRole(row.roleId)
      if (res.errorCode !== '0') {
        showError(`删除角色失败：${res.errorMsg}`)
        return
      }
      showSuccessWithCallback('删除角色成功', () => loadRoleRows(), 1000)
    } catch (err) {
      if (err !== 'cancel') {
        showError(`删除角色失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  const openAssignPermissions = async (row) => {
    if (isBuiltInRole(row)) {
      showWarning('内置角色不可调整权限')
      return
    }
    try {
      startLoading()
      const [infoRes, menuRes, dataRes] = await Promise.all([
        queryRoleInfo(row.roleId),
        queryMenuTree(),
        queryDataPermissionList()
      ])
      if (infoRes.errorCode !== '0') {
        showError(`查询角色详情失败：${infoRes.errorMsg}`)
        return
      }
      if (menuRes.errorCode !== '0') {
        showError(`查询菜单权限失败：${menuRes.errorMsg}`)
        return
      }
      if (dataRes.errorCode !== '0') {
        showError(`查询数据权限失败：${dataRes.errorMsg}`)
        return
      }
      const menuList = menuRes.menuList || []
      const dataList = dataRes.dataPermissionList || []
      const permissionIds = infoRes.permissionIds || []

      const menuIdSet = new Set(menuList.map(p => p.permissionId))
      const dataIdSet = new Set(dataList.map(p => p.permissionId))

      assignRoleId.value = row.roleId
      assignRoleName.value = row.roleName || row.roleCode
      menuTreeData.value = buildMenuTree(menuList)
      assignedMenuIds.value = permissionIds.filter(id => menuIdSet.has(id))
      dataPermissionList.value = dataList
      assignedDataPermissionIds.value = permissionIds.filter(id => dataIdSet.has(id))
      assignDialogVisible.value = true
    } catch (err) {
      showError(`查询权限失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  /**
   * 保存角色授权
   * @param {Array<number>} menuCheckedIds - 菜单树勾选的权限ID
   */
  const saveAssignPermissions = async (menuCheckedIds) => {
    const permissionIds = [...(menuCheckedIds || []), ...assignedDataPermissionIds.value]
    try {
      startLoading()
      const res = await assignRolePermissions(assignRoleId.value, permissionIds)
      if (res.errorCode !== '0') {
        showError(`角色授权失败：${res.errorMsg}`)
        return
      }
      showSuccessWithCallback('角色授权成功', () => {
        assignDialogVisible.value = false
      }, 1000)
    } catch (err) {
      showError(`角色授权失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  return {
    loading,
    pagination,
    roleList,
    queryForm,
    dialogVisible,
    dialogMode,
    roleForm,
    assignDialogVisible,
    assignRoleId,
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
  }
}