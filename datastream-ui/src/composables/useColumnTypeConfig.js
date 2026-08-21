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
 * 字段类型定义与映射配置业务逻辑Hook
 * 管理「类型定义」与「类型映射」两套列表的分页、查询、详情/编辑弹窗与增删改。
 */
import { ref, reactive } from 'vue'
import { useLoading } from './useLoading'
import { usePagination } from './usePagination'
import { useMessage } from './useMessage'
import {
  queryTypeDefineRows,
  queryAllTypeDefine,
  queryTypeDefineInfo,
  addTypeDefine,
  modifyTypeDefine,
  delTypeDefine,
  queryTypeMapRows,
  queryTypeMapInfo,
  addTypeMap,
  modifyTypeMap,
  delTypeMap
} from '@/api/columnType'

export function useColumnTypeConfig() {
  const { loading, startLoading, stopLoading } = useLoading()
  const { showSuccess, showError, showWarning, confirm, showSuccessWithCallback } = useMessage()

  // ==================== 类型定义 ====================
  const definePagination = usePagination({ defaultPageSize: 20 })
  const typeDefineList = ref([])
  const defineQueryForm = reactive({ queryFlag: '1', queryValue: null })

  // ==================== 类型映射 ====================
  const mapPagination = usePagination({ defaultPageSize: 20 })
  const typeMapList = ref([])
  const mapQueryForm = reactive({ queryFlag: '1', queryValue: null })

  // 全部类型定义（用于映射的源/目标下拉选项）
  const allTypeDefineOptions = ref([])

  // 弹窗状态
  const defineDialogVisible = ref(false)
  const defineMode = ref('add')
  const defineForm = reactive({})

  const mapDialogVisible = ref(false)
  const mapMode = ref('add')
  const mapForm = reactive({})

  // 选项
  const queryFlagOptions = [
    { label: '全部', value: '1' },
    { label: '数据库类型', value: '2' },
    { label: '类型名称', value: '3' }
  ]
  const typeCategoryOptions = [
    { label: '整数', value: 'NUMERIC_INTEGER' },
    { label: '定点数', value: 'NUMERIC_FIXED_POINT' },
    { label: '浮点数', value: 'NUMERIC_FLOATING_POINT' },
    { label: '短字符串', value: 'STRING_SHORT' },
    { label: '长字符串', value: 'STRING_LONG' },
    { label: '日期', value: 'DATETIME_DATE' },
    { label: '时间', value: 'DATETIME_TIME' },
    { label: '时间戳', value: 'DATETIME_TIMESTAMP' },
    { label: '二进制', value: 'BINARY' },
    { label: '布尔', value: 'BOOLEAN' },
    { label: 'JSON', value: 'JSON' },
    { label: '数组', value: 'ARRAY' },
    { label: 'UUID', value: 'UUID' },
    { label: '其他', value: 'OTHER' }
  ]
  const matchLevelOptions = [
    { label: '精确匹配', value: 1 },
    { label: '兼容匹配', value: 2 },
    { label: '降级匹配', value: 3 }
  ]
  const matchLevelLabel = (level) => {
    const opt = matchLevelOptions.find(o => o.value === level)
    return opt ? opt.label : level
  }

  // 类型定义复合展示（数据库类型.类型名称）
  const typeDefineLabel = (row) => (row ? `${row.databaseType}.${row.columnTypeName}` : '')

  // ==================== 类型定义 CRUD ====================

  const changeDefineQueryFlag = () => {
    defineQueryForm.queryValue = null
    if (defineQueryForm.queryFlag === '1') {
      loadTypeDefineRows()
    }
  }

  const loadTypeDefineRows = async () => {
    if (defineQueryForm.queryFlag !== '1' && !defineQueryForm.queryValue) {
      showWarning('查询值不能为空')
      return
    }
    try {
      startLoading()
      const res = await queryTypeDefineRows({
        queryFlag: defineQueryForm.queryFlag,
        queryValue: defineQueryForm.queryValue,
        page: definePagination.pageNum.value,
        count: definePagination.pageSize.value
      })
      if (res.errorCode !== '0') {
        showError(`查询类型定义失败：${res.errorMsg}`)
        return
      }
      typeDefineList.value = res.typeDefineList || []
      definePagination.setTotal(res.total || 0)
    } catch (err) {
      showError(`查询类型定义失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const openAddDefine = () => {
    defineMode.value = 'add'
    Object.keys(defineForm).forEach(k => delete defineForm[k])
    defineDialogVisible.value = true
  }

  const openModifyDefine = async (row) => {
    try {
      startLoading()
      const res = await queryTypeDefineInfo(row.columnTypeDefineId)
      if (res.errorCode !== '0') {
        showError(`查询类型定义详情失败：${res.errorMsg}`)
        return
      }
      Object.keys(defineForm).forEach(k => delete defineForm[k])
      Object.assign(defineForm, res.typeDefine || {})
      defineMode.value = 'modify'
      defineDialogVisible.value = true
    } catch (err) {
      showError(`查询类型定义详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const saveDefine = async () => {
    if (!defineForm.databaseType) {
      showWarning('数据库类型不能为空')
      return
    }
    if (!defineForm.columnTypeName) {
      showWarning('类型名称不能为空')
      return
    }
    if (!defineForm.typeCategory) {
      showWarning('类型分类不能为空')
      return
    }
    try {
      startLoading()
      let res
      if (defineMode.value === 'add') {
        res = await addTypeDefine(defineForm)
        if (res.errorCode !== '0') {
          showError(`新增类型定义失败：${res.errorMsg}`)
          return
        }
      } else {
        res = await modifyTypeDefine(defineForm)
        if (res.errorCode !== '0') {
          showError(`修改类型定义失败：${res.errorMsg}`)
          return
        }
      }
      showSuccessWithCallback(defineMode.value === 'add' ? '新增类型定义成功' : '修改类型定义成功', () => {
        defineDialogVisible.value = false
        loadTypeDefineRows()
      }, 1000)
    } catch (err) {
      showError(`${defineMode.value === 'add' ? '新增' : '修改'}类型定义失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const removeDefine = async (row) => {
    try {
      await confirm(`是否确认删除类型定义（${typeDefineLabel(row)}）?`)
      startLoading()
      const res = await delTypeDefine(row.columnTypeDefineId)
      if (res.errorCode !== '0') {
        showError(`删除类型定义失败：${res.errorMsg}`)
        return
      }
      showSuccessWithCallback(`类型定义（${typeDefineLabel(row)}）删除成功`, () => loadTypeDefineRows(), 1000)
    } catch (err) {
      if (err !== 'cancel') {
        showError(`删除类型定义失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  // ==================== 类型映射 CRUD ====================

  const loadAllTypeDefine = async () => {
    try {
      const res = await queryAllTypeDefine({})
      if (res.errorCode !== '0') {
        showError(`查询全部类型定义失败：${res.errorMsg}`)
        return
      }
      allTypeDefineOptions.value = res.typeDefineList || []
    } catch (err) {
      showError(`查询全部类型定义失败：${err}`)
    }
  }

  const changeMapQueryFlag = () => {
    mapQueryForm.queryValue = null
    if (mapQueryForm.queryFlag === '1') {
      loadTypeMapRows()
    }
  }

  const loadTypeMapRows = async () => {
    if (mapQueryForm.queryFlag !== '1' && !mapQueryForm.queryValue) {
      showWarning('查询值不能为空')
      return
    }
    try {
      startLoading()
      const res = await queryTypeMapRows({
        queryFlag: mapQueryForm.queryFlag,
        queryValue: mapQueryForm.queryValue,
        page: mapPagination.pageNum.value,
        count: mapPagination.pageSize.value
      })
      if (res.errorCode !== '0') {
        showError(`查询类型映射失败：${res.errorMsg}`)
        return
      }
      typeMapList.value = res.typeMapList || []
      mapPagination.setTotal(res.total || 0)
    } catch (err) {
      showError(`查询类型映射失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const openAddMap = async () => {
    await loadAllTypeDefine()
    mapMode.value = 'add'
    Object.keys(mapForm).forEach(k => delete mapForm[k])
    mapDialogVisible.value = true
  }

  const openModifyMap = async (row) => {
    try {
      startLoading()
      const [infoRes] = await Promise.all([queryTypeMapInfo(row.columnTypeMapId), loadAllTypeDefine()])
      if (infoRes.errorCode !== '0') {
        showError(`查询类型映射详情失败：${infoRes.errorMsg}`)
        return
      }
      Object.keys(mapForm).forEach(k => delete mapForm[k])
      Object.assign(mapForm, infoRes.typeMap || {})
      mapMode.value = 'modify'
      mapDialogVisible.value = true
    } catch (err) {
      showError(`查询类型映射详情失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const saveMap = async () => {
    if (!mapForm.columnTypeDefineIdA) {
      showWarning('源类型不能为空')
      return
    }
    if (!mapForm.columnTypeDefineIdB) {
      showWarning('目标类型不能为空')
      return
    }
    if (!mapForm.matchLevel) {
      showWarning('匹配级别不能为空')
      return
    }
    if (mapForm.matchLevel !== 1) {
      showWarning(`当前为「${matchLevelLabel(mapForm.matchLevel)}」，可能存在精度损失或类型转换风险，请确认`)
    }
    try {
      startLoading()
      let res
      if (mapMode.value === 'add') {
        res = await addTypeMap(mapForm)
        if (res.errorCode !== '0') {
          showError(`新增类型映射失败：${res.errorMsg}`)
          return
        }
      } else {
        res = await modifyTypeMap(mapForm)
        if (res.errorCode !== '0') {
          showError(`修改类型映射失败：${res.errorMsg}`)
          return
        }
      }
      showSuccessWithCallback(mapMode.value === 'add' ? '新增类型映射成功' : '修改类型映射成功', () => {
        mapDialogVisible.value = false
        loadTypeMapRows()
      }, 1000)
    } catch (err) {
      showError(`${mapMode.value === 'add' ? '新增' : '修改'}类型映射失败：${err}`)
    } finally {
      stopLoading()
    }
  }

  const removeMap = async (row) => {
    const label = `${typeDefineLabelText(row.databaseTypeA, row.columnTypeNameA)} -> ${typeDefineLabelText(row.databaseTypeB, row.columnTypeNameB)}`
    try {
      await confirm(`是否确认删除类型映射（${label}）?`)
      startLoading()
      const res = await delTypeMap(row.columnTypeMapId)
      if (res.errorCode !== '0') {
        showError(`删除类型映射失败：${res.errorMsg}`)
        return
      }
      showSuccessWithCallback(`类型映射（${label}）删除成功`, () => loadTypeMapRows(), 1000)
    } catch (err) {
      if (err !== 'cancel') {
        showError(`删除类型映射失败：${err}`)
      }
    } finally {
      stopLoading()
    }
  }

  const typeDefineLabelText = (db, name) => (db && name ? `${db}.${name}` : '')

  return {
    loading,
    // 定义
    definePagination,
    typeDefineList,
    defineQueryForm,
    defineDialogVisible,
    defineMode,
    defineForm,
    changeDefineQueryFlag,
    loadTypeDefineRows,
    openAddDefine,
    openModifyDefine,
    saveDefine,
    removeDefine,
    // 映射
    mapPagination,
    typeMapList,
    mapQueryForm,
    allTypeDefineOptions,
    mapDialogVisible,
    mapMode,
    mapForm,
    changeMapQueryFlag,
    loadTypeMapRows,
    openAddMap,
    openModifyMap,
    saveMap,
    removeMap,
    // 公共
    typeDefineLabel,
    typeDefineLabelText,
    queryFlagOptions,
    typeCategoryOptions,
    matchLevelOptions
  }
}