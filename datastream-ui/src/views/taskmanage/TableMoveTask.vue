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
  <div class="table-manage-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h2 class="page-title">
            <el-icon class="title-icon"><Operation /></el-icon>结构迁移
          </h2>
          <p class="page-description">选择源端和目标端数据源，配置表结构迁移任务</p>
        </div>
        <div class="action-section">
          <el-button
            type="primary"
            :disabled="!canCreateTask"
            @click="createMigrationTask"
            :loading="creating">
            <el-icon><Plus /></el-icon>
            创建迁移任务
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <el-row :gutter="24" class="content-row">
        <!-- 左侧：源端数据源选择 -->
        <el-col :span="12">
          <el-card class="data-source-card source-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="header-info">
                  <el-icon class="header-icon source-icon"><DataBoard /></el-icon>
                  <span class="header-title">源端数据源</span>
                </div>
                <el-button size="small" type="primary" plain @click="selectSourceDataSource" :loading="loadingSourceTables">
                  <el-icon><Search /></el-icon>
                  选择数据源
                </el-button>
              </div>
            </template>

            <!-- 源端数据源信息 -->
            <div v-if="sourceDataSource" class="datasource-info">
              <div class="info-item">
                <span class="label">数据源名称：</span>
                <el-tag type="success" size="default">{{ sourceDataSource.dataBaseName }}</el-tag>
              </div>
              <div class="info-item">
                <span class="label">数据库类型：</span>
                <el-tag :type="getDatabaseTypeColor(sourceDataSource.dataBaseType)" size="default">
                  {{ getDatabaseTypeName(sourceDataSource.dataBaseType) }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">连接地址：</span>
                <span class="value">{{ sourceDataSource.addr }}</span>
              </div>
            </div>

            <!-- 源端表结构树 -->
            <div v-if="sourceDataSource" class="table-tree-container">
              <div class="tree-header">
                <div class="tree-title">
                  <el-icon><FolderOpened /></el-icon>
                  <span>数据表结构</span>
                  <el-tag size="small" class="table-count">{{ selectedSourceTables.length }}/{{ totalTableCount }}</el-tag>
                  <el-tag v-if="loadingSourceTables" size="small" type="warning" class="loading-tag">
                    <el-icon class="is-loading"><Loading /></el-icon>
                    加载中...
                  </el-tag>
                </div>
                <div class="tree-actions">
                  <el-button size="small" @click="expandAllSource" type="text" :disabled="loadingSourceTables">
                    <el-icon><Plus /></el-icon>
                    全部展开
                  </el-button>
                  <el-button size="small" @click="collapseAllSource" type="text" :disabled="loadingSourceTables">
                    <el-icon><Minus /></el-icon>
                    全部收起
                  </el-button>
                  <el-button size="small" @click="refreshSourceTables" type="text" :loading="loadingSourceTables">
                    <el-icon><Refresh /></el-icon>
                    刷新
                  </el-button>
                </div>
              </div>

              <div class="tree-content">
                <el-input
                  v-model="sourceTableFilter"
                  placeholder="搜索表名..."
                  size="small"
                  clearable
                  class="table-filter"
                  :disabled="loadingSourceTables">
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>

                <!-- 加载状态 -->
                <div v-if="loadingSourceTables" class="loading-container">
                  <el-icon class="is-loading loading-icon"><Loading /></el-icon>
                  <p class="loading-text">正在加载表结构...</p>
                </div>

                <!-- 表结构树 -->
                <el-tree
                  v-if="!loadingSourceTables"
                  ref="sourceTreeRef"
                  :data="filteredSourceTables"
                  :props="treeProps"
                  show-checkbox
                  node-key="id"
                  :default-expand-all="forceExpandAll"
                  :expand-on-click-node="false"
                  :check-strictly="false"
                  @check="handleSourceTableCheck"
                  :key="`tree-${treeRenderKey}-${forceExpandAll ? 'expanded' : (forceCollapseAll ? 'collapsed' : 'normal')}`"
                  class="source-tree">
                  <template #default="{ node, data }">
                    <div class="tree-node">
                      <el-icon v-if="data.type === 'database'" class="node-icon"><DataBoard /></el-icon>
                      <el-icon v-else-if="data.type === 'table'" class="node-icon"><Grid /></el-icon>
                      <span class="node-label">{{ node.label }}</span>
                      <span v-if="data.type === 'table'" class="table-info">
                        <el-tag size="small" type="info">{{ data.tableType || 'TABLE' }}</el-tag>
                        <span v-if="data.rowCount" class="row-count">{{ formatNumber(data.rowCount) }} 行</span>
                        <span v-if="data.tableSize" class="table-size">{{ data.tableSize }}</span>
                      </span>
                    </div>
                  </template>
                </el-tree>

                <!-- 空状态 -->
                <div v-if="!loadingSourceTables && filteredSourceTables.length === 0" class="empty-tables">
                  <el-icon class="empty-icon"><Grid /></el-icon>
                  <p class="empty-text">暂无表数据</p>
                  <p class="empty-desc">请检查数据源连接或刷新重试</p>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else class="empty-state">
              <el-icon class="empty-icon"><DataBoard /></el-icon>
              <p class="empty-text">请选择源端数据源</p>
              <p class="empty-desc">选择数据源后将显示可迁移的表结构</p>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：目标端数据源选择 -->
        <el-col :span="12">
          <el-card class="data-source-card target-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="header-info">
                  <el-icon class="header-icon target-icon"><Connection /></el-icon>
                  <span class="header-title">目标端数据源</span>
                </div>
                <el-button size="small" type="success" plain @click="selectTargetDataSource">
                  <el-icon><Search /></el-icon>
                  选择数据源
                </el-button>
              </div>
            </template>

            <!-- 目标端数据源信息 -->
            <div v-if="targetDataSource" class="datasource-info">
              <div class="info-item">
                <span class="label">数据源名称：</span>
                <el-tag type="success" size="default">{{ targetDataSource.dataBaseName }}</el-tag>
              </div>
              <div class="info-item">
                <span class="label">数据库类型：</span>
                <el-tag :type="getDatabaseTypeColor(targetDataSource.dataBaseType)" size="default">
                  {{ getDatabaseTypeName(targetDataSource.dataBaseType) }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">连接地址：</span>
                <span class="value">{{ targetDataSource.addr }}</span>
              </div>
            </div>

            <!-- 迁移配置 -->
            <div v-if="targetDataSource" class="migration-config">
              <div class="config-title">
                <el-icon><Setting /></el-icon>
                <span>迁移配置</span>
              </div>

              <el-form :model="migrationConfig" label-width="120px" size="default">
                <el-form-item label="迁移模式：">
                  <el-radio-group v-model="migrationConfig.mode">
                    <el-radio label="structure">仅结构</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="冲突处理：">
                  <el-select v-model="migrationConfig.conflictStrategy" placeholder="选择冲突处理策略">
                    <el-option label="跳过" value="skip" />
                    <el-option label="覆盖" value="overwrite" />
                    <el-option label="重命名" value="rename" />
                  </el-select>
                </el-form-item>
              </el-form>
            </div>

            <!-- 空状态 -->
            <div v-else class="empty-state">
              <el-icon class="empty-icon"><Connection /></el-icon>
              <p class="empty-text">请选择目标端数据源</p>
              <p class="empty-desc">选择数据源后可配置迁移参数</p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 迁移预览 -->
      <div v-if="canCreateTask" class="migration-preview">
        <el-card class="preview-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-info">
                <el-icon class="header-icon"><View /></el-icon>
                <span class="header-title">迁移预览</span>
              </div>
              <el-tag :type="migrationConfig.mode === 'both' ? 'warning' : 'success'" size="default">
                {{ getMigrationModeText(migrationConfig.mode) }}
              </el-tag>
            </div>
          </template>

          <div class="preview-content">
            <div class="preview-summary">
              <div class="summary-item">
                <span class="label">源端：</span>
                <span class="value">{{ sourceDataSource?.dataBaseName }} ({{ getDatabaseTypeName(sourceDataSource?.dataBaseType) }})</span>
              </div>
              <div class="summary-item">
                <span class="label">目标端：</span>
                <span class="value">{{ targetDataSource?.dataBaseName }} ({{ getDatabaseTypeName(targetDataSource?.dataBaseType) }})</span>
              </div>
              <div class="summary-item">
                <span class="label">选择表数：</span>
                <span class="value">{{ selectedSourceTables.length }} 个表</span>
              </div>
            </div>

            <div class="selected-tables">
              <div class="tables-title">
                <el-icon><Grid /></el-icon>
                <span>待迁移表列表</span>
              </div>
              <div class="tables-list">
                <el-tag
                  v-for="table in selectedSourceTables"
                  :key="table.id"
                  size="default"
                  class="table-tag"
                  closable
                  @close="removeSelectedTable(table)">
                  {{ table.tableName }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 数据源选择对话框 -->
    <el-dialog
      v-model="dataSourceSelectVisible"
      :title="dataSourceSelectTitle"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close>
      <DataSourceSelect
        :unique-id="dataSourceSelectType"
        :data-source-types-filter="['database']"
        @confirm-select-data-source="handleDataSourceSelect" />
    </el-dialog>
  </div>
</template>

<script>
import { onMounted } from 'vue'
import DataSourceSelect from '@/views/components/DataSourceSelect.vue'
import { useTableStructureMigration } from '@/composables/useTableStructureMigration.js'
import { useEventBus } from '@/composables/useEventBus.js'
import {
  Operation,
  Plus,
  DataBoard,
  Connection,
  Search,
  FolderOpened,
  Minus,
  Grid,
  Setting,
  View,
  Loading,
  Refresh
} from '@element-plus/icons-vue'

export default {
  name: 'TableManage',
  components: {
    DataSourceSelect,
    Operation,
    Plus,
    DataBoard,
    Connection,
    Search,
    FolderOpened,
    Minus,
    Grid,
    Setting,
    View,
    Loading,
    Refresh
  },
  setup() {
    const migration = useTableStructureMigration()
    const { on } = useEventBus()

    onMounted(() => {
      // 监听数据源选择事件
      on('confirmSelectDataSourcesource', migration.handleSourceDataSourceSelect)
      on('confirmSelectDataSourcetarget', migration.handleTargetDataSourceSelect)
    })

    return {
      ...migration
    }
  }
}
</script>

<style scoped>
.table-manage-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
  overflow: hidden;
}

/* 页面头部 */
.page-header {
  flex-shrink: 0;
  padding: 24px 32px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  flex: 1;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.title-icon {
  font-size: 28px;
  color: var(--primary-color);
}

.page-description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.action-section {
  flex-shrink: 0;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

/* 数据源卡片 */
.data-source-card {
  height: 600px;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

.data-source-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
}

.source-icon {
  color: var(--primary-color);
}

.target-icon {
  color: #67C23A;
}

.header-title {
  font-weight: 600;
  font-size: 16px;
}

/* 数据源信息 */
.datasource-info {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-item .label {
  font-weight: 500;
  color: var(--text-secondary);
  margin-right: 8px;
  min-width: 90px;
}

.info-item .value {
  color: var(--text-primary);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
}

/* 表树容器 */
.table-tree-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 12px;
}

.tree-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--text-primary);
}

.table-count {
  margin-left: 8px;
}

.loading-tag {
  margin-left: 8px;
}

.tree-actions {
  display: flex;
  gap: 8px;
}

.tree-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-filter {
  margin-bottom: 12px;
}

.source-tree {
  flex: 1;
  overflow-y: auto;
}

/* 树节点样式 */
.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.node-icon {
  font-size: 16px;
  color: var(--primary-color);
}

.node-label {
  flex: 1;
  font-size: 14px;
}

.table-info {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.row-count,
.table-size {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 加载状态 */
.loading-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
  color: var(--text-secondary);
}

.loading-icon {
  font-size: 32px;
  margin-bottom: 12px;
  color: var(--primary-color);
}

.loading-text {
  margin: 0;
  font-size: 14px;
}

/* 空表状态 */
.empty-tables,
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
  color: var(--text-secondary);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #ddd;
}

.empty-text {
  font-size: 16px;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.empty-desc {
  font-size: 14px;
  margin: 0;
  opacity: 0.7;
}

/* 迁移配置 */
.migration-config {
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.config-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 迁移预览 */
.migration-preview {
  margin-top: 24px;
}

.preview-card {
  border: 2px solid var(--primary-color);
}

.preview-content {
  padding: 8px 0;
}

.preview-summary {
  margin-bottom: 20px;
}

.summary-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.summary-item .label {
  font-weight: 500;
  color: var(--text-secondary);
  margin-right: 12px;
  min-width: 80px;
}

.summary-item .value {
  color: var(--text-primary);
}

.selected-tables {
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
}

.tables-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.tables-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-tag {
  margin: 0;
}
</style>

