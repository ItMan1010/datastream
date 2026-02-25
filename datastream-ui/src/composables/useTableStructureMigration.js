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
 * 表结构迁移管理业务逻辑Hook
 */
import {ref, reactive, computed} from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import {getTableList} from '@/api/table'
import {createMoveTask} from '@/api/task'
import {useLoading} from './useLoading'
import {useMessage} from './useMessage'
import {useMainStore} from '@/store/index.js'
import {getDatabaseTypeName, getDatabaseTypeColor} from '@/constants/databaseConstants'

export function useTableStructureMigration() {
  const {loading, startLoading, stopLoading} = useLoading()
  const {showSuccess, showError, showWarning} = useMessage()
  const mainStore = useMainStore()

  // 数据源选择相关
  const sourceDataSource = ref(null)
  const targetDataSource = ref(null)
  const dataSourceSelectVisible = ref(false)
  const dataSourceSelectTitle = ref('')
  const dataSourceSelectType = ref('source')

  // 表结构相关
  const sourceTables = ref([])
  const selectedSourceTables = ref([])
  const forceExpandAll = ref(false)
  const forceCollapseAll = ref(false)
  const treeRenderKey = ref(0)
  const loadingSourceTables = ref(false)
  const sourceTableFilter = ref('')
  const sourceTreeRef = ref(null)

  // 树形结构配置
  const treeProps = {
    children: 'children', label: 'name'
  }

  // 迁移配置
  const migrationConfig = reactive({
    mode: 'structure', conflictStrategy: 'skip', batchSize: 1000, threadCount: 2
  })

  // 任务创建状态
  const creating = ref(false)

  // 是否可以创建任务
  const canCreateTask = computed(() => {
    return sourceDataSource.value && targetDataSource.value && selectedSourceTables.value.length > 0
  })

  // 过滤后的源端表
  const filteredSourceTables = computed(() => {
    if (!sourceTableFilter.value) {
      return sourceTables.value
    }
    const filterText = sourceTableFilter.value.toLowerCase()
    return filterTreeData(sourceTables.value, filterText)
  })

  // 获取总表数量
  const totalTableCount = computed(() => {
    let count = 0
    sourceTables.value.forEach(database => {
      if (database.children) {
        count += database.children.length
      }
    })
    return count
  })

  // 选择源端数据源
  const selectSourceDataSource = () => {
    dataSourceSelectType.value = 'source'
    dataSourceSelectTitle.value = '选择源端数据源'
    dataSourceSelectVisible.value = true
  }

  // 选择目标端数据源
  const selectTargetDataSource = () => {
    dataSourceSelectType.value = 'target'
    dataSourceSelectTitle.value = '选择目标端数据源'
    dataSourceSelectVisible.value = true
  }

  // 处理数据源选择
  const handleDataSourceSelect = (dataSource) => {
    dataSourceSelectVisible.value = false

    if (dataSourceSelectType.value === 'source') {
      handleSourceDataSourceSelect(dataSource)
    } else {
      handleTargetDataSourceSelect(dataSource)
    }
  }

  // 处理源端数据源选择
  const handleSourceDataSourceSelect = async (dataSource) => {
    sourceDataSource.value = dataSource
    selectedSourceTables.value = []

    const success = await loadSourceTables()
    if (success) {
      dataSourceSelectVisible.value = false
    }
  }

  // 处理目标端数据源选择
  const handleTargetDataSourceSelect = (dataSource) => {
    targetDataSource.value = dataSource
    dataSourceSelectVisible.value = false
    showSuccess('目标端数据源选择完成')
  }

  // 刷新源端表结构
  const refreshSourceTables = async () => {
    if (!sourceDataSource.value) return
    await loadSourceTables()
  }

  // 加载源端表结构
  const loadSourceTables = async () => {
    if (!sourceDataSource.value) return false

    try {
      loadingSourceTables.value = true

      const response = await getTableList(sourceDataSource.value.dataBaseId)
      const isSuccess = response && (response.errorCode === '0000' || response.errorCode === '0')

      if (isSuccess && response.tableInfoEntityList?.length > 0) {
        sourceTables.value = buildTreeDataFromApi(response.tableInfoEntityList)
        forceExpandAll.value = false
        forceCollapseAll.value = false
        treeRenderKey.value++
        showSuccess(`表结构加载完成，共${response.tableInfoEntityList.length}个表`)
        return true
      } else if (isSuccess && response.tableInfoEntityList?.length === 0) {
        showWarning('该数据源下没有找到任何表')
        sourceTables.value = []
        treeRenderKey.value++
        return true
      } else if (response && !isSuccess) {
        throw new Error(`API错误：${response.errorMsg} (错误码：${response.errorCode})`)
      } else {
        throw new Error('API响应格式异常或数据为空')
      }
    } catch (error) {
      showError(`加载表结构失败：${error.message || '网络错误'}`)
      sourceTables.value = []
      return false
    } finally {
      loadingSourceTables.value = false
    }
  }

  // 将API数据转换为树形结构
  const buildTreeDataFromApi = (tableInfoList) => {
    if (!tableInfoList?.length) return []

    const schemaMap = new Map()

    tableInfoList.forEach((tableInfo, index) => {
      const schemaName = tableInfo.schemaName || 'default'

      if (!schemaMap.has(schemaName)) {
        schemaMap.set(schemaName, {
          id: `database_${schemaName}`, name: schemaName, type: 'database', children: []
        })
      }

      const tableNode = {
        id: `table_${tableInfo.tableName}_${index}`,
        name: tableInfo.tableName,
        tableName: tableInfo.tableName,
        type: 'table',
        tableType: tableInfo.tableType || 'TABLE',
        tableComment: tableInfo.tableComment,
        rowCount: tableInfo.rowCount,
        tableSize: tableInfo.tableSize,
        createTime: tableInfo.createTime,
        updateTime: tableInfo.updateTime,
        columns: tableInfo.columns || []
      }

      schemaMap.get(schemaName).children.push(tableNode)
    })

    return Array.from(schemaMap.values())
  }

  // 处理源端表选择
  const handleSourceTableCheck = (data, checkedInfo) => {
    if (sourceTreeRef.value) {
      const checkedNodes = sourceTreeRef.value.getCheckedNodes(false, true)
      selectedSourceTables.value = checkedNodes.filter(node => node.type === 'table')
    }
  }

  // 展开所有源端节点
  const expandAllSource = () => {
    if (loadingSourceTables.value) {
      showWarning('正在加载中，请稍后再试')
      return
    }

    if (sourceTables.value.length === 0) {
      showWarning('暂无表数据')
      return
    }

    forceExpandAll.value = true
    forceCollapseAll.value = false
    treeRenderKey.value++
  }

  // 收起所有源端节点
  const collapseAllSource = () => {
    if (loadingSourceTables.value) {
      showWarning('正在加载中，请稍后再试')
      return
    }

    forceCollapseAll.value = true
    forceExpandAll.value = false
    treeRenderKey.value++
  }

  // 过滤树形数据
  const filterTreeData = (nodes, filterText) => {
    return nodes.map(node => {
      const newNode = {...node}

      if (node.children) {
        const filteredChildren = filterTreeData(node.children, filterText)
        if (filteredChildren.length > 0) {
          newNode.children = filteredChildren
          return newNode
        }
      }

      if (node.name.toLowerCase().includes(filterText)) {
        return newNode
      }

      return null
    }).filter(node => node !== null)
  }

  // 移除选中的表
  const removeSelectedTable = (table) => {
    if (sourceTreeRef.value) {
      sourceTreeRef.value.setChecked(table.id, false)
      selectedSourceTables.value = selectedSourceTables.value.filter(t => t.id !== table.id)
    }
  }

  // 创建迁移任务
  const createMigrationTask = async () => {
    if (!canCreateTask.value) {
      showWarning('请完成数据源选择和表选择')
      return
    }

    try {
      creating.value = true

      const tableNames = selectedSourceTables.value.map(table => table.tableName)

      const taskData = {
        taskType: 4,
        sourceObjectType: sourceDataSource.value.dataBaseType,
        sourceObjectId: sourceDataSource.value.dataBaseId,
        targetObjectType: targetDataSource.value.dataBaseType,
        targetObjectId: targetDataSource.value.dataBaseId,
        systemUserCode: mainStore.getLoginSystemUser.systemUserCode,
        targetInsertMode: migrationConfig.conflictStrategy === 'overwrite' ? 1 : 0,
        priority: 1,
        dataNodeFlag: 1,
        dataSet: 1,
        tableType: 1,
        sourceObjectName: tableNames.join(','),
        sourceDataBaseObjectType: 1
      }

      if (selectedSourceTables.value.length === 1) {
        taskData.targetObjectName = tableNames[0]
        taskData.taskDisc = '结构迁移'
      } else {
        taskData.taskDisc = '批量表结构迁移'
      }

      const response = await createMoveTask(taskData)
      const isSuccess = response && (response.errorCode === '0' || !response.errorCode)

      if (isSuccess) {
        if (selectedSourceTables.value.length === 1) {
          showSuccess(`表 ${tableNames[0]} 迁移任务创建成功！`)
        } else {
          showSuccess(`批量迁移任务创建成功，共 ${tableNames.length} 个表！`)
        }
      } else {
        throw new Error(`API错误：${response.errorMsg} (错误码：${response.errorCode})`)
      }
    } catch (error) {
      showError(`创建迁移任务失败：${error.message || '网络错误'}`)
    } finally {
      creating.value = false
    }
  }

  // 获取迁移模式文本
  const getMigrationModeText = (mode) => {
    const modeMap = {
      'structure': '仅迁移结构', 'data': '仅迁移数据', 'both': '结构+数据迁移'
    }
    return modeMap[mode] || '未知模式'
  }

  // 格式化数字
  const formatNumber = (num) => {
    if (!num) return '0'
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M'
    } else if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K'
    }
    return num.toString()
  }

  return {
    // 状态
    loading,
    sourceDataSource,
    targetDataSource,
    dataSourceSelectVisible,
    dataSourceSelectTitle,
    dataSourceSelectType,
    sourceTables,
    selectedSourceTables,
    forceExpandAll,
    forceCollapseAll,
    treeRenderKey,
    loadingSourceTables,
    sourceTableFilter,
    sourceTreeRef,
    treeProps,
    migrationConfig,
    creating,

    // 计算属性
    canCreateTask,
    filteredSourceTables,
    totalTableCount,

    // 方法
    selectSourceDataSource,
    selectTargetDataSource,
    handleDataSourceSelect,
    handleSourceDataSourceSelect,
    handleTargetDataSourceSelect,
    refreshSourceTables,
    loadSourceTables,
    handleSourceTableCheck,
    expandAllSource,
    collapseAllSource,
    removeSelectedTable,
    createMigrationTask,
    getMigrationModeText,
    formatNumber,
    getDatabaseTypeName,
    getDatabaseTypeColor
  }
}

