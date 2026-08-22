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
 * 系统用户管理业务逻辑 Hook
 * 管理用户分页查询、新增/编辑、禁用/启用、重置密码、删除与角色派发。
 */
import { ref, reactive } from 'vue'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'
import {
  queryUserRows,
  queryUserInfo,
  addUser,
  modifyUser,
  updateUserState,
  resetUserPassword,
  delUser,
  queryAllRole
} from '@/api/rbac'

export function useUserManage() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showSuccess, showError, showWarning, confirm, showSuccessWithCallback } = useMessage()

  const pagination = usePagination({ defaultPageSize: 10 })
  const userList = ref([])
  const queryForm = reactive({ queryFlag: '1', queryValue: null })

  // 全部角色（用于用户-角色派发下拉）
  const allRoleOptions = ref([])

  // 新增/编辑弹窗
  const dialogVisible = ref(false)
  const dialogMode = ref('add')
  const userForm = reactive({})
  const selectedRoleIds = ref([])

  // 重置密码弹窗
  const resetDialogVisible = ref(false)
  const resetForm = reactive({})

  const queryFlagOptions = [
    { label: '全部', value: '1' },
    { label: '账号', value: '2' },
    { label: '名称', value: '3' }
  ]

  const stateOptions = [
    { label: '启用', value: 1 },
    { label: '禁用', value: 0 }
  ]

  const isBuiltInAdmin = (row) => row && (row.systemUserCode === 'admin' || row.systemUserId === 1)

  const stateLabel = (state) => (state === 1 ? '启用' : '禁用')
  const stateTagType = (state) => (state === 1 ? 'success' : 'info')

  const changeQueryFlag = () => {
    queryForm.queryValue = null
    if (queryForm.queryFlag === '1') {
      loadUserRows()
    }
  }

  const loadUserRows = async () => {
    if (queryForm.queryFlag !== '1' && !queryForm.queryValue) {
      showWarning('查询值不能为空')
      return
    }
    try {
      startLoading()
      const res = await queryUserRows({
        queryFlag: queryForm.queryFlag,
        queryValue: queryForm.queryValue,
        page: pagination.pageNum.value,
        count: pagination.pageSize.value
      })
      if (res.errorCode !== '0') {
        showError(`查询用户失败：${res.errorMsg}`)
        return
      }
      userList.value = res.userList || []
      pagination.setTotal(res.total || 0)
    } catch (err) {
      showError(`查询用户失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const loadAllRoles = async () => {
    try {
      const res = await queryAllRole()
      if (res.errorCode !== '0') {
        showError(`查询角色失败：${res.errorMsg}`)
        return
      }
      allRoleOptions.value = res.roleList || []
    } catch (err) {
      showError(`查询角色失败：${err}`)
    }
  }

  const openAddUser = async () => {
    await loadAllRoles()
    dialogMode.value = 'add'
    Object.keys(userForm).forEach(k => delete userForm[k])
    userForm.state = 1
    selectedRoleIds.value = []
    dialogVisible.value = true
  }

  const openModifyUser = async (row) => {
    try {
      startLoading()
      const [infoRes] = await Promise.all([queryUserInfo(row.systemUserId), loadAllRoles()])
      if (infoRes.errorCode !== '0') {
        showError(`查询用户详情失败：${infoRes.errorMsg}`)
        return
      }
      Object.keys(userForm).forEach(k => delete userForm[k])
      Object.assign(userForm, infoRes.user || {})
      delete userForm.password
      selectedRoleIds.value = infoRes.roleIds || []
      dialogMode.value = 'modify'
      dialogVisible.value = true
    } catch (err) {
      showError(`查询用户详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const saveUser = async () => {
    if (!userForm.systemUserCode) {
      showWarning('登录账号不能为空')
      return
    }
    if (!userForm.systemUserName) {
      showWarning('显示名不能为空')
      return
    }
    if (dialogMode.value === 'add' && !userForm.password) {
      showWarning('登录密码不能为空')
      return
    }
    try {
      startLoading()
      let res
      if (dialogMode.value === 'add') {
        res = await addUser(userForm, selectedRoleIds.value)
        if (res.errorCode !== '0') {
          showError(`新增用户失败：${res.errorMsg}`)
          return
        }
      } else {
        res = await modifyUser(userForm, selectedRoleIds.value)
        if (res.errorCode !== '0') {
          showError(`修改用户失败：${res.errorMsg}`)
          return
        }
      }
      showSuccessWithCallback(dialogMode.value === 'add' ? '新增用户成功' : '修改用户成功', () => {
        dialogVisible.value = false
        loadUserRows()
      }, 1000)
    } catch (err) {
      showError(`${dialogMode.value === 'add' ? '新增' : '修改'}用户失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const changeState = async (row) => {
    if (isBuiltInAdmin(row) && row.state === 1) {
      showWarning('内置管理员不可被禁用')
      return
    }
    const targetState = row.state === 1 ? 0 : 1
    const actionText = targetState === 1 ? '启用' : '禁用'
    try {
      await confirm(`是否确认${actionText}用户（${row.systemUserName || row.systemUserCode}）?`)
      startLoading()
      const res = await updateUserState(row.systemUserId, targetState)
      if (res.errorCode !== '0') {
        showError(`${actionText}用户失败：${res.errorMsg}`)
        return
      }
      showSuccessWithCallback(`${actionText}用户成功`, () => loadUserRows(), 1000)
    } catch (err) {
      if (err !== 'cancel') {
        showError(`${actionText}用户失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  const openResetPassword = (row) => {
    Object.keys(resetForm).forEach(k => delete resetForm[k])
    resetForm.systemUserId = row.systemUserId
    resetForm.systemUserName = row.systemUserName || row.systemUserCode
    resetForm.password = ''
    resetDialogVisible.value = true
  }

  const doResetPassword = async () => {
    if (!resetForm.password) {
      showWarning('新密码不能为空')
      return
    }
    try {
      startLoading()
      const res = await resetUserPassword(resetForm.systemUserId, resetForm.password)
      if (res.errorCode !== '0') {
        showError(`重置密码失败：${res.errorMsg}`)
        return
      }
      showSuccessWithCallback('重置密码成功', () => {
        resetDialogVisible.value = false
      }, 1000)
    } catch (err) {
      showError(`重置密码失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const removeUser = async (row) => {
    if (isBuiltInAdmin(row)) {
      showWarning('内置管理员不可被删除')
      return
    }
    try {
      await confirm(`是否确认删除用户（${row.systemUserName || row.systemUserCode}）?`)
      startLoading()
      const res = await delUser(row.systemUserId)
      if (res.errorCode !== '0') {
        showError(`删除用户失败：${res.errorMsg}`)
        return
      }
      showSuccessWithCallback('删除用户成功', () => loadUserRows(), 1000)
    } catch (err) {
      if (err !== 'cancel') {
        showError(`删除用户失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

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
    stateTagType
  }
}