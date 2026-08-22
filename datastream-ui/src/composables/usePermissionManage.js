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
 * 权限资源管理业务逻辑 Hook
 * 管理菜单权限树与数据权限列表的查询与权限资源增删改（内置资源保护在前端提示）。
 */
import { ref, reactive, computed } from 'vue'
import { useLoading } from './useLoading'
import { useMessage } from './useMessage'
import {
  queryMenuTree,
  queryDataPermissionList,
  addPermission,
  modifyPermission,
  delPermission
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

export function usePermissionManage() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showSuccess, showError, showWarning, confirm, showSuccessWithCallback } = useMessage()

  const activeTab = ref('menu')
  const menuFlatList = ref([])
  const menuTreeData = ref([])
  const dataPermissionList = ref([])

  // 新增/编辑弹窗
  const dialogVisible = ref(false)
  const dialogMode = ref('add')
  const permissionForm = reactive({})

  const permissionTypeOptions = [
    { label: '菜单权限', value: 1 },
    { label: '数据权限', value: 2 }
  ]

  // 父权限选项（仅菜单权限需要，数据权限无父子关系）
  const parentPermissionOptions = computed(() => {
    const options = [{ label: '顶级菜单', value: null }]
    menuFlatList.value.forEach(p => {
      options.push({ label: p.permissionName, value: p.permissionId })
    })
    return options
  })

  const isBuiltInPermission = (row) => row && row.builtIn === 1
  const permissionTypeLabel = (type) => (type === 1 ? '菜单权限' : '数据权限')
  const builtInLabel = (builtIn) => (builtIn === 1 ? '内置' : '自定义')

  const loadMenuTree = async () => {
    try {
      startLoading()
      const res = await queryMenuTree()
      if (res.errorCode !== '0') {
        showError(`查询菜单权限失败：${res.errorMsg}`)
        return
      }
      menuFlatList.value = res.menuList || []
      menuTreeData.value = buildMenuTree(menuFlatList.value)
    } catch (err) {
      showError(`查询菜单权限失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const loadDataPermissionList = async () => {
    try {
      startLoading()
      const res = await queryDataPermissionList()
      if (res.errorCode !== '0') {
        showError(`查询数据权限失败：${res.errorMsg}`)
        return
      }
      dataPermissionList.value = res.dataPermissionList || []
    } catch (err) {
      showError(`查询数据权限失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const openAddPermission = (permissionType, parentId) => {
    dialogMode.value = 'add'
    Object.keys(permissionForm).forEach(k => delete permissionForm[k])
    permissionForm.permissionType = permissionType
    permissionForm.parentId = permissionType === 1 ? (parentId || null) : null
    permissionForm.sortNo = 1
    dialogVisible.value = true
  }

  const openModifyPermission = (row) => {
    dialogMode.value = 'modify'
    Object.keys(permissionForm).forEach(k => delete permissionForm[k])
    Object.assign(permissionForm, row)
    dialogVisible.value = true
  }

  const savePermission = async () => {
    if (!permissionForm.permissionCode) {
      showWarning('权限编码不能为空')
      return
    }
    if (!permissionForm.permissionName) {
      showWarning('权限名称不能为空')
      return
    }
    // 数据权限不适用菜单路由与父节点
    if (permissionForm.permissionType === 2) {
      permissionForm.parentId = null
      permissionForm.route = null
    }
    try {
      startLoading()
      let res
      if (dialogMode.value === 'add') {
        res = await addPermission(permissionForm)
        if (res.errorCode !== '0') {
          showError(`新增权限失败：${res.errorMsg}`)
          return
        }
      } else {
        res = await modifyPermission(permissionForm)
        if (res.errorCode !== '0') {
          showError(`修改权限失败：${res.errorMsg}`)
          return
        }
      }
      showSuccessWithCallback(dialogMode.value === 'add' ? '新增权限成功' : '修改权限成功', () => {
        dialogVisible.value = false
        reload()
      }, 1000)
    } catch (err) {
      showError(`${dialogMode.value === 'add' ? '新增' : '修改'}权限失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const removePermission = async (row) => {
    if (isBuiltInPermission(row)) {
      showWarning('内置权限不可被删除')
      return
    }
    try {
      await confirm(`是否确认删除权限（${row.permissionName || row.permissionCode}）?`)
      startLoading()
      const res = await delPermission(row.permissionId)
      if (res.errorCode !== '0') {
        showError(`删除权限失败：${res.errorMsg}`)
        return
      }
      showSuccessWithCallback('删除权限成功', () => reload(), 1000)
    } catch (err) {
      if (err !== 'cancel') {
        showError(`删除权限失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  const reload = async () => {
    await Promise.all([loadMenuTree(), loadDataPermissionList()])
  }

  return {
    loading,
    activeTab,
    menuFlatList,
    menuTreeData,
    dataPermissionList,
    dialogVisible,
    dialogMode,
    permissionForm,
    permissionTypeOptions,
    parentPermissionOptions,
    loadMenuTree,
    loadDataPermissionList,
    reload,
    openAddPermission,
    openModifyPermission,
    savePermission,
    removePermission,
    isBuiltInPermission,
    permissionTypeLabel,
    builtInLabel
  }
}