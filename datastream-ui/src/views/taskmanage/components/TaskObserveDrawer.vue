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
  <el-drawer v-model="visible" direction="btt" :modal-append-to-body="false" size="85%">
    <div style="position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: #EBEEF5;">
      <div class="bcFFF margin-10 pl-20" style="line-height: 40px; color: var(--primary-color);">
        <label class="fontWeight">迁移任务</label> >
        <span v-if="currentTaskType === 4">表结构迁移观测</span>
        <span v-else>数据迁移观测</span>
        <el-button type="text" style="float: right; font-size: 18px;" circle @click="visible = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>

      <div class="pt-10 margin-10 bcFFF pb-20" style="position: absolute; bottom: 10px; top: 50px; left: 0; right: 0;">
        <!-- 数据迁移统计信息 -->
        <div v-if="currentTaskType !== 4">
          <el-row v-if="dataSendMode" :gutter="8"
                  style="padding: 15px 0; background: #f8f9fa; border-radius: 6px; margin: 10px 0;">
            <el-col :span="6">
              <div style="text-align: center;">
                <el-statistic title="数据分发模式" :value="dataSendMode" title-style="font-size: 14px; color: #606266;" />
              </div>
            </el-col>
            <el-col :span="6">
              <div style="text-align: center;">
                <el-statistic title="管道配置个数" :value="queueNumber" title-style="font-size: 14px; color: #606266;" />
              </div>
            </el-col>
            <el-col :span="6">
              <div style="text-align: center;">
                <el-statistic title="管道传输最大值" :value="queueMaxSize" title-style="font-size: 14px; color: #606266;" />
              </div>
            </el-col>
            <el-col :span="6">
              <div style="text-align: center;">
                <el-statistic title="管道传输当前值" :value="queueRunningSize"
                              title-style="font-size: 14px; color: #606266;"
                              @click="$emit('show-queue-metrics')" />
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 数据迁移观测表格 -->
        <div v-if="currentTaskType !== 4" style="height: calc(100% - 21px);">
          <VirtualScroll :data="moveTaskInfoListData" :height="40" :buffer="100" key-prop="infoId">
            <el-table :data="moveTaskInfoListData" fit stripe height="100%" highlight-current-row
                      style="width: 100%; padding: 10px 20px;" row-key="infoId"
                      :cell-style="{ wordBreak: 'break-all', whiteSpace: 'pre-wrap' }">
<!--              <el-table-column prop="index" width="100" />-->
              <el-table-column prop="infoId" label="运行ID" width="120" />
              <el-table-column prop="createDate" label="生成时间" width="160" />
              <el-table-column prop="infoFlagDesc" label="运行标志" width="160" />
              <el-table-column prop="virtualId" label="运行虚拟ID" width="100" :show-overflow-tooltip="true" />
              <el-table-column prop="dataCount" label="运行记录数" width="100" />
              <el-table-column prop="dataActualCount" label="实际记录数" width="100" />
              <el-table-column prop="loopCount" label="迭代次数" width="100" />
              <el-table-column prop="maxCost" label="最大耗时(毫秒)" width="100" />
              <el-table-column prop="minCost" label="最小耗时(毫秒)" width="100" />
              <el-table-column prop="latelyCost" label="最近耗时(毫秒)" width="100" />
              <el-table-column prop="sumCost" label="总耗时(毫秒)" width="100" />
              <el-table-column prop="pageRowStart" label="开始值" width="120" />
              <el-table-column prop="pageRowEnd" label="结束值" width="120" />
              <el-table-column label="任务状态" width="120">
                <template #default="scope">
                  <TaskStateText :state="scope.row.state" />
                </template>
              </el-table-column>
              <el-table-column prop="stateDate" label="状态时间" width="160" />
              <el-table-column prop="errorCode" label="错误编码" width="100" :show-overflow-tooltip="true" />
              <el-table-column prop="errorMsg" label="错误信息" width="200" :show-overflow-tooltip="true" />
              <el-table-column label="操作" width="50">
                <template #default="scope">
                  <el-tooltip content="日志" placement="top">
                    <el-button @click="$emit('show-log', 1, scope.row.infoId)" type="primary" link size="small">
                      <el-icon><Tickets /></el-icon>
                    </el-button>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
          </VirtualScroll>
        </div>

        <!-- 表结构迁移观测表格 -->
        <div v-else style="height: 100%; padding: 10px 20px;">
          <el-table :data="tableMoveListData" fit stripe height="100%" highlight-current-row style="width: 100%;"
                    :cell-style="{ wordBreak: 'break-all', whiteSpace: 'pre-wrap' }">
<!--            <el-table-column prop="index" label="序号" width="80" />-->
            <el-table-column prop="moveTableId" label="迁移ID" width="120" />
            <el-table-column prop="sourceTableName" label="源表对象名称" width="200" :show-overflow-tooltip="false" />
            <el-table-column prop="createDate" label="创建时间" width="160" />
            <el-table-column label="迁移状态" width="120">
              <template #default="scope">
                <span :style="{ color: getMigrationStateColor(scope.row.state) }">
                  {{ getMigrationStateName(scope.row.state) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="stateDate" label="状态时间" width="160" />
            <el-table-column prop="errorCode" label="错误编码" width="120" :show-overflow-tooltip="true" />
            <el-table-column prop="errorMsg" label="错误信息" min-width="200" :show-overflow-tooltip="true" />
            <el-table-column label="预览" width="70" align="center">
              <template #default="scope">
                <el-tooltip content="预览建表语句" placement="top" v-if="scope.row.tableSql">
                  <el-button @click="showSqlPreview(scope.row)" type="success" link size="small">
                    <el-icon><View /></el-icon>
                  </el-button>
                </el-tooltip>
                <span v-else style="color: #C0C4CC;">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="scope">
                <el-tooltip content="日志" placement="top">
                  <el-button @click="$emit('show-log', 4, scope.row.moveTableId)" type="primary" link size="small">
                    <el-icon><Tickets /></el-icon>
                  </el-button>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- SQL 预览对话框 -->
    <el-dialog
      v-model="sqlDialogVisible"
      title="建表语句预览"
      width="70%"
      :close-on-click-modal="false"
      class="sql-preview-dialog">
      <div class="sql-preview-header" v-if="currentPreviewData">
        <div class="sql-info-item">
          <span class="sql-info-label">源表名:</span>
          <span class="sql-info-value">{{ currentPreviewData.sourceTableName || '-' }}</span>
        </div>
        <div class="sql-info-item">
          <span class="sql-info-label">目标表名:</span>
          <span class="sql-info-value">{{ currentPreviewData.targetTableName || '-' }}</span>
        </div>
      </div>
      <div class="sql-preview-content">
        <pre class="sql-code-block" v-html="formattedSql"></pre>
      </div>
      <template #footer>
        <el-button @click="copySql" :icon="DocumentCopy">复制建表语句</el-button>
        <el-button type="primary" @click="sqlDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script>
import { computed, ref } from 'vue'
import { Close, Tickets, View, DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import VirtualScroll from '@/views/components/VirtualScroll.vue'
import TaskStateText from '@/views/components/common/TaskStateText.vue'

export default {
  name: 'TaskObserveDrawer',
  components: {
    Close,
    Tickets,
    View,
    DocumentCopy,
    VirtualScroll,
    TaskStateText
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    currentTaskType: {
      type: Number,
      default: null
    },
    moveTaskInfoListData: {
      type: Array,
      default: () => []
    },
    tableMoveListData: {
      type: Array,
      default: () => []
    },
    dataSendMode: {
      type: String,
      default: ''
    },
    queueMaxSize: {
      type: [String, Number],
      default: ''
    },
    queueNumber: {
      type: [String, Number],
      default: ''
    },
    queueRunningSize: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue', 'show-log', 'show-queue-metrics'],
  setup(props, { emit }) {
    const visible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const sqlDialogVisible = ref(false)
    const currentPreviewData = ref(null)
    const formattedSql = ref('')

    const getMigrationStateName = (state) => {
      const stateMap = {
        0: '等待迁移',
        1: '迁移中',
        2: '迁移结束',
        3: '迁移失败',
        4: '迁移暂停'
      }
      return stateMap[state] || '未知状态'
    }

    const getMigrationStateColor = (state) => {
      const colorMap = {
        0: '#0099CC',
        1: '#2563EB',
        2: '#67C23A',
        3: '#FF0000',
        4: '#FF6600'
      }
      return colorMap[state] || '#ffa07a'
    }

    // 格式化 SQL 语句
    const formatSql = (sql) => {
      if (!sql) return ''

      // 移除多余空白，但保留换行
      let formatted = sql.trim()

      // 统一换行符
      formatted = formatted.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

      // 按行分割，处理每行
      const lines = formatted.split('\n')
      const result = []

      for (let i = 0; i < lines.length; i++) {
        let line = lines[i].trim()
        if (!line) continue

        const upperLine = line.toUpperCase()

        // CREATE TABLE 语句保持在一行
        if (upperLine.match(/CREATE\s+TABLE[^()]*/)) {
          // 如果 CREATE TABLE 后没有括号，继续处理
          if (!line.includes('(')) {
            // 查找下一行是否有括号
            let combined = line
            for (let j = i + 1; j < lines.length && j < i + 5; j++) {
              const nextLine = lines[j].trim()
              combined += ' ' + nextLine
              if (nextLine.includes('(')) {
                i = j
                break
              }
            }
            result.push(combined)
            continue
          }
        }

        // 处理字段定义行 - 保持原样，只做轻微清理
        // 跳过单独的逗号行
        if (line === ',') continue

        // 处理行首的逗号（MySQL导出常见格式）
        if (line.startsWith(',')) {
          line = '  ' + line.substring(1).trim()
        } else if (!line.startsWith('CREATE') && !line.startsWith(')')) {
          // 字段定义行，添加缩进
          // 只有当不是以缩进开头时才添加
          if (!line.match(/^\s/)) {
            line = '  ' + line
          }
        }

        // 移除行尾多余的逗号（某些情况）
        const nextLine = i < lines.length - 1 ? lines[i + 1].trim() : ''
        if (line.endsWith(',') && (nextLine.startsWith(')') || nextLine === '')) {
          line = line.slice(0, -1).trim()
        }

        result.push(line)
      }

      return result.join('\n')
    }

    // SQL 语法高亮
    const highlightSql = (sql) => {
      if (!sql) return ''

      const keywords = [
        'CREATE', 'TABLE', 'ALTER', 'DROP', 'INDEX', 'PRIMARY', 'KEY', 'FOREIGN', 'REFERENCES',
        'UNIQUE', 'NOT', 'NULL', 'DEFAULT', 'AUTO_INCREMENT', 'COMMENT', 'ENGINE', 'CHARSET',
        'COLLATE', 'INT', 'VARCHAR', 'TEXT', 'DATE', 'DATETIME', 'TIMESTAMP', 'DECIMAL',
        'DOUBLE', 'FLOAT', 'BIGINT', 'SMALLINT', 'TINYINT', 'BOOLEAN', 'BLOB', 'LONGBLOB',
        'MEDIUMTEXT', 'LONGTEXT', 'TINYTEXT', 'UNSIGNED', 'ZEROFILL'
      ]

      let highlighted = sql

      // 高亮关键字
      keywords.forEach(keyword => {
        const regex = new RegExp(`\\b${keyword}\\b`, 'gi')
        highlighted = highlighted.replace(regex, `<span class="sql-keyword">${keyword}</span>`)
      })

      // 高亮字符串
      highlighted = highlighted.replace(/'([^']*)'/g, `<span class="sql-string">'$1'</span>`)
      highlighted = highlighted.replace(/`([^`]*)`/g, `<span class="sql-string">\`$1\`</span>`)

      // 高亮数字
      highlighted = highlighted.replace(/\b(\d+)\b/g, `<span class="sql-number">$1</span>`)

      // 高亮注释
      highlighted = highlighted.replace(/(--[^\n]*)/g, `<span class="sql-comment">$1</span>`)
      highlighted = highlighted.replace(/(\/\*[\s\S]*?\*\/)/g, `<span class="sql-comment">$1</span>`)

      return highlighted
    }

    // 显示 SQL 预览
    const showSqlPreview = (row) => {
      if (!row.tableSql) {
        ElMessage.warning('暂无建表语句')
        return
      }

      currentPreviewData.value = row
      const formatted = formatSql(row.tableSql)
      formattedSql.value = highlightSql(formatted)
      sqlDialogVisible.value = true
    }

    // 复制 SQL
    const copySql = () => {
      if (!currentPreviewData.value?.tableSql) return

      const textarea = document.createElement('textarea')
      textarea.value = currentPreviewData.value.tableSql
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)

      ElMessage.success('建表语句已复制到剪贴板')
    }

    return {
      visible,
      sqlDialogVisible,
      currentPreviewData,
      formattedSql,
      getMigrationStateName,
      getMigrationStateColor,
      showSqlPreview,
      copySql
    }
  }
}
</script>

<style scoped>
.bcFFF {
  background-color: #fff;
}

.margin-10 {
  margin: 10px;
}

.pl-20 {
  padding-left: 20px;
}

.pt-10 {
  padding-top: 10px;
}

.pb-20 {
  padding-bottom: 20px;
}

.fontWeight {
  font-weight: bold;
}

/* SQL 预览对话框样式 */
.sql-preview-dialog :deep(.el-dialog__body) {
  padding: 16px 20px;
  max-height: 60vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sql-preview-header {
  display: flex;
  gap: 24px;
  padding: 12px 16px;
  background: #F5F7FA;
  border-radius: 6px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.sql-info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sql-info-label {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.sql-info-value {
  font-size: 13px;
  color: #303133;
  font-weight: 600;
  font-family: 'Monaco', 'Consolas', monospace;
}

.sql-preview-content {
  flex: 1;
  overflow: auto;
  background: #282C34;
  border-radius: 6px;
  padding: 16px;
}

.sql-code-block {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #ABB2BF;
  white-space: pre-wrap;
  word-break: break-all;
}

/* SQL 语法高亮 */
.sql-code-block :deep(.sql-keyword) {
  color: #C678DD;
  font-weight: 600;
}

.sql-code-block :deep(.sql-string) {
  color: #98C379;
}

.sql-code-block :deep(.sql-number) {
  color: #D19A66;
}

.sql-code-block :deep(.sql-comment) {
  color: #5C6370;
  font-style: italic;
}

/* 滚动条样式 */
.sql-preview-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.sql-preview-content::-webkit-scrollbar-track {
  background: #1E2227;
  border-radius: 4px;
}

.sql-preview-content::-webkit-scrollbar-thumb {
  background: #4B5263;
  border-radius: 4px;
}

.sql-preview-content::-webkit-scrollbar-thumb:hover {
  background: #5C6370;
}
</style>
