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
  <div class="data-source-select-container">
    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="data-source-tabs">
      <!-- 数据库数据源 Tab -->
      <el-tab-pane v-if="!dataSourceTypesFilter || dataSourceTypesFilter.includes('database')" label="数据库数据源" name="database">
        <div class="tab-content">
          <!-- 顶部操作区 -->
          <div class="header-section">
            <el-row :gutter="16" align="middle">
              <el-col :span="8">
                <div class="filter-group">
                  <label class="filter-label">数据库类型：</label>
                  <el-select v-model="databaseQueryForm.dataBaseType" placeholder="选择类型" size="default" @change="queryDatabaseSource">
                    <el-option label="全部" value="0"></el-option>
                    <el-option label="MySQL" value="2"></el-option>
                    <el-option label="Oracle" value="3"></el-option>
                    <el-option label="PostgreSQL" value="4"></el-option>
                    <el-option label="Doris" value="5"></el-option>
                  </el-select>
                </div>
              </el-col>
              <el-col :span="16">
                <div class="action-buttons">
                  <el-button type="primary" :loading="databaseLoading" @click="queryDatabaseSource">
                    <el-icon><Search /></el-icon>
                    查询
                  </el-button>
                  <el-button type="success" @click="confirm">
                    <el-icon><Check /></el-icon>
                    确认选择
                  </el-button>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 数据库数据源表格 -->
          <div v-if="!databaseHasData && !databaseLoading" class="empty-state">
            <el-icon class="empty-icon"><Files /></el-icon>
            <p class="empty-text">暂无数据库数据源</p>
            <p class="empty-desc">系统中尚未配置任何数据库数据源，请联系管理员添加数据源配置后再试</p>
            <div class="empty-actions">
              <el-button type="primary" @click="queryDatabaseSource" :loading="databaseLoading">
                <el-icon><RefreshRight /></el-icon>
                重新查询
              </el-button>
            </div>
          </div>

          <div v-else-if="databaseLoading" class="loading-state">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <p>正在查询数据库数据源...</p>
          </div>

          <div v-else class="table-section">
            <div style="padding: 8px 16px; background: #f0f9ff; border: 1px solid #0ea5e9; margin-bottom: 12px; border-radius: 4px; font-size: 12px;">
              📊 数据状态: 共 {{ databaseListData.length }} 条记录，总计 {{ databaseDataTotal }} 条
            </div>
            <el-table
              ref="databaseTableRef"
              :data="databaseListData"
              style="width: 100%;"
              height="240"
              @current-change="handleDatabaseCurrentChange"
              stripe
              highlight-current-row>
              <el-table-column label="选择" width="60" align="center" fixed="left">
                <template #default="scope">
                  <el-radio v-model="databaseSelectId" :label="scope.row.dataBaseId" @change="handleDatabaseRadioChange(scope.row)"></el-radio>
                </template>
              </el-table-column>
              <el-table-column prop="dataBaseName" label="数据库名称" min-width="180" show-overflow-tooltip></el-table-column>
              <el-table-column label="类型" width="90" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.dataBaseType === 2 ? 'success' : scope.row.dataBaseType === 3 ? 'warning' : 'info'" size="small">
                    {{
                      scope.row.dataBaseType === 2 ? 'MySQL' : scope.row.dataBaseType === 3 ? 'Oracle' : scope.row.dataBaseType === 4 ? 'PostgreSQL' : scope.row.dataBaseType === 5 ? 'Doris' : '其他'
                    }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="addr" label="连接地址" min-width="200" show-overflow-tooltip></el-table-column>
              <el-table-column prop="userName" label="用户名" width="120" align="center"></el-table-column>
              <el-table-column prop="createDate" label="创建时间" width="160" align="center"></el-table-column>
            </el-table>
          </div>

          <!-- 数据库分页器 -->
          <div v-if="databaseListData.length > 0" class="pagination-section">
            <el-pagination
              @size-change="handleDatabaseSizeChange"
              @current-change="handleDatabasePageChange"
              v-model:current-page="databasePageNum"
              :page-sizes="[10, 20, 50]"
              :page-size="databasePageSize"
              :pager-count="5"
              :total="databaseDataTotal"
              layout="total, sizes, prev, pager, next, jumper"
              small>
            </el-pagination>
          </div>
        </div>
      </el-tab-pane>

      <!-- 文件数据源 Tab -->
      <el-tab-pane v-if="!dataSourceTypesFilter || dataSourceTypesFilter.includes('file')" label="文件数据源" name="file">
        <div class="tab-content">
          <!-- 顶部操作区 -->
          <div class="header-section">
            <el-row :gutter="16" align="middle">
              <el-col :span="8">
                <div class="filter-group">
                  <label class="filter-label">文件类型：</label>
                  <el-select v-model="fileQueryForm.fileType" placeholder="选择类型" size="default" @change="queryFileSource">
                    <el-option label="全部" value="0"></el-option>
                    <el-option label="text" value="8"></el-option>
                    <el-option label="excel" value="9"></el-option>
                  </el-select>
                </div>
              </el-col>
              <el-col :span="16">
                <div class="action-buttons">
                  <el-button type="primary" :loading="fileLoading" @click="queryFileSource">
                    <el-icon><Search /></el-icon>
                    查询
                  </el-button>
                  <el-button type="success" @click="confirm">
                    <el-icon><Check /></el-icon>
                    确认选择
                  </el-button>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 文件数据源表格 -->
          <div v-if="!fileHasData && !fileLoading" class="empty-state">
            <el-icon class="empty-icon"><Files /></el-icon>
            <p class="empty-text">暂无文件数据源</p>
            <p class="empty-desc">系统中尚未配置任何文件数据源，请联系管理员添加文件配置后再试</p>
            <div class="empty-actions">
              <el-button type="primary" @click="queryFileSource" :loading="fileLoading">
                <el-icon><RefreshRight /></el-icon>
                重新查询
              </el-button>
            </div>
          </div>

          <div v-else-if="fileLoading" class="loading-state">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <p>正在查询文件数据源...</p>
          </div>

          <div v-else class="table-section">
            <div style="padding: 8px 16px; background: #f0f9ff; border: 1px solid #0ea5e9; margin-bottom: 12px; border-radius: 4px; font-size: 12px;">
              📊 数据状态: 共 {{ fileListData.length }} 条记录，总计 {{ fileDataTotal }} 条
            </div>
            <el-table
              ref="fileTableRef"
              :data="fileListData"
              style="width: 100%;"
              height="240"
              @current-change="handleFileCurrentChange"
              stripe
              highlight-current-row>
              <el-table-column label="选择" width="60" align="center" fixed="left">
                <template #default="scope">
                  <el-radio v-model="fileSelectId" :label="scope.row.fileFormatId" @change="handleFileRadioChange(scope.row)"></el-radio>
                </template>
              </el-table-column>
              <el-table-column prop="fileFormatId" label="文件ID" width="80" align="center"></el-table-column>
              <el-table-column label="类型" width="90" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.fileType === 1 ? 'info' : 'success'" size="small">
                    {{ scope.row.fileType === 8 ? 'Text' : scope.row.fileType === 9 ? 'Excel' : '其他' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="fileNameFormat" label="文件名称" min-width="180" show-overflow-tooltip></el-table-column>
              <el-table-column label="名称类型" width="100" align="center">
                <template #default="scope">
                  <el-tag type="info" size="small">
                    {{ scope.row.fileNameType === 1 ? '固定值' : scope.row.fileNameType === 2 ? '正则表达' : '其他' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.onLineFlag === 2 ? 'success' : 'info'" size="small">
                    {{ scope.row.onLineFlag === 2 ? '上线' : '下线' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createDate" label="创建时间" width="160" align="center"></el-table-column>
            </el-table>
          </div>

          <!-- 文件分页器 -->
          <div v-if="fileListData.length > 0" class="pagination-section">
            <el-pagination
              @size-change="handleFileSizeChange"
              @current-change="handleFilePageChange"
              v-model:current-page="filePageNum"
              :page-sizes="[10, 20, 50]"
              :page-size="filePageSize"
              :pager-count="5"
              :total="fileDataTotal"
              layout="total, sizes, prev, pager, next, jumper"
              small>
            </el-pagination>
          </div>
        </div>
      </el-tab-pane>

      <!-- MQ数据源 Tab -->
      <el-tab-pane v-if="!dataSourceTypesFilter || dataSourceTypesFilter.includes('mq')" label="MQ数据源" name="mq">
        <div class="tab-content">
          <!-- 顶部操作区 -->
          <div class="header-section">
            <el-row :gutter="16" align="middle">
              <el-col :span="8">
                <div class="filter-group">
                  <label class="filter-label">MQ类型：</label>
                  <el-select v-model="mqQueryForm.mqType" placeholder="选择类型" size="default" @change="queryMqSource">
                    <el-option label="全部" value="0"></el-option>
                    <el-option label="Kafka" value="kafka"></el-option>
                    <el-option label="RocketMQ" value="rocketmq"></el-option>
                    <el-option label="RabbitMQ" value="rabbitmq"></el-option>
                  </el-select>
                </div>
              </el-col>
              <el-col :span="16">
                <div class="action-buttons">
                  <el-button type="primary" :loading="mqLoading" @click="queryMqSource">
                    <el-icon><Search /></el-icon>
                    查询
                  </el-button>
                  <el-button type="success" @click="confirm">
                    <el-icon><Check /></el-icon>
                    确认选择
                  </el-button>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- MQ数据源表格 -->
          <div v-if="!mqHasData && !mqLoading" class="empty-state">
            <el-icon class="empty-icon"><Files /></el-icon>
            <p class="empty-text">暂无MQ数据源</p>
            <p class="empty-desc">系统中尚未配置任何MQ数据源，请联系管理员添加MQ配置后再试</p>
            <div class="empty-actions">
              <el-button type="primary" @click="queryMqSource" :loading="mqLoading">
                <el-icon><RefreshRight /></el-icon>
                重新查询
              </el-button>
            </div>
          </div>

          <div v-else-if="mqLoading" class="loading-state">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <p>正在查询MQ数据源...</p>
          </div>

          <div v-else class="table-section">
            <div style="padding: 8px 16px; background: #f0f9ff; border: 1px solid #0ea5e9; margin-bottom: 12px; border-radius: 4px; font-size: 12px;">
              📊 数据状态: 共 {{ mqListData.length }} 条记录，总计 {{ mqDataTotal }} 条
            </div>
            <el-table
              ref="mqTableRef"
              :data="mqListData"
              style="width: 100%;"
              height="240"
              @current-change="handleMqCurrentChange"
              stripe
              highlight-current-row>
              <el-table-column label="选择" width="60" align="center" fixed="left">
                <template #default="scope">
                  <el-radio v-model="mqSelectId" :label="scope.row.mqConfigId" @change="handleMqRadioChange(scope.row)"></el-radio>
                </template>
              </el-table-column>
              <el-table-column prop="mqConfigId" label="MQ ID" width="90" align="center"></el-table-column>
              <el-table-column prop="mqConfigName" label="MQ名称" min-width="180" show-overflow-tooltip></el-table-column>
              <el-table-column label="类型" width="100" align="center">
                <template #default="scope">
                  <el-tag v-if="scope.row.mqType === 10" type="info">kafka</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="bootstrapServers" label="连接地址" min-width="200" show-overflow-tooltip></el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.onLineFlag === 2 ? 'success' : 'info'" size="small">
                    {{ scope.row.onLineFlag === 2 ? '上线' : '下线' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createDate" label="创建时间" width="160" align="center"></el-table-column>
            </el-table>
          </div>

          <!-- MQ分页器 -->
          <div v-if="mqListData.length > 0" class="pagination-section">
            <el-pagination
              @size-change="handleMqSizeChange"
              @current-change="handleMqPageChange"
              v-model:current-page="mqPageNum"
              :page-sizes="[10, 20, 50]"
              :page-size="mqPageSize"
              :pager-count="5"
              :total="mqDataTotal"
              layout="total, sizes, prev, pager, next, jumper"
              small>
            </el-pagination>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import { getCurrentInstance } from 'vue'
import { Search, Check, Files, RefreshRight, Loading } from '@element-plus/icons-vue'

export default {
  name: 'DataSourceSelect',
  components: {
    Search,
    Check,
    Files,
    RefreshRight,
    Loading
  },
  setup() {
    const instance = getCurrentInstance()

    return {
      bus: instance?.appContext.config.globalProperties.$bus
    }
  },
  props: {
    uniqueId: {
      type: String,
      default () {
        return 'id'
      }
    },
    // 数据源类型过滤，允许指定的数据源类型显示，如 ['database'] 只显示数据库数据源
    // 不传则显示所有类型
    dataSourceTypesFilter: {
      type: Array,
      default () {
        return null // null 表示显示所有类型
      }
    }
  },
  data () {
    return {
      activeTab: 'database', // 当前激活的 tab

      // 数据库数据源相关
      databasePageSize: 10,
      databasePageNum: 1,
      databaseDataTotal: 0,
      databaseLoading: false,
      databaseQueryForm: {
        dataBaseType: '0'
      },
      databaseListData: [],
      databaseSelectId: null,
      databaseCurRow: null,

      // 文件数据源相关
      filePageSize: 10,
      filePageNum: 1,
      fileDataTotal: 0,
      fileLoading: false,
      fileQueryForm: {
        fileType: '0'
      },
      fileListData: [],
      fileSelectId: null,
      fileCurRow: null,

      // MQ数据源相关
      mqPageSize: 10,
      mqPageNum: 1,
      mqDataTotal: 0,
      mqLoading: false,
      mqQueryForm: {
        mqType: '0'
      },
      mqListData: [],
      mqSelectId: null,
      mqCurRow: null
    }
  },
  computed: {
    databaseHasData() {
      return Array.isArray(this.databaseListData) && this.databaseListData.length > 0
    },
    fileHasData() {
      return Array.isArray(this.fileListData) && this.fileListData.length > 0
    },
    mqHasData() {
      return Array.isArray(this.mqListData) && this.mqListData.length > 0
    }
  },
  mounted() {
    // 确定默认激活的 tab
    this.setDefaultActiveTab()
  },
  methods: {
    // 设置默认激活的 tab
    setDefaultActiveTab() {
      // 如果有过滤条件，设置第一个允许的 tab 为默认激活
      if (this.dataSourceTypesFilter && this.dataSourceTypesFilter.length > 0) {
        this.activeTab = this.dataSourceTypesFilter[0]
      }
      // 否则默认使用 database
      else {
        this.activeTab = 'database'
      }

      // 加载对应 tab 的数据
      if (this.activeTab === 'database') {
        this.queryDatabaseSource()
      } else if (this.activeTab === 'file') {
        this.queryFileSource()
      } else if (this.activeTab === 'mq') {
        this.queryMqSource()
      }
    },
    // Tab 切换处理
    handleTabChange(tabName) {
      if (tabName === 'database') {
        // 切换到数据库 tab，如果没有数据则查询
        if (this.databaseListData.length === 0) {
          this.queryDatabaseSource()
        }
      } else if (tabName === 'file') {
        // 切换到文件 tab，如果没有数据则查询
        if (this.fileListData.length === 0) {
          this.queryFileSource()
        }
      } else if (tabName === 'mq') {
        // 切换到MQ tab，如果没有数据则查询
        if (this.mqListData.length === 0) {
          this.queryMqSource()
        }
      }
    },

    // ========== 数据库数据源相关方法 ==========
    handleDatabaseSizeChange (val) {
      this.databasePageSize = val
      this.queryDatabaseSource()
    },
    handleDatabasePageChange (val) {
      this.databasePageNum = val
      this.queryDatabaseSource()
    },
    queryDatabaseSource() {
      let request = {
        page: this.databasePageNum,
        count: this.databasePageSize,
        state: 2 // 只查询上线状态数据
      }
      if (this.databaseQueryForm.dataBaseType !== '0') {
        request.queryFlag = 2
        request.dataBaseType = this.databaseQueryForm.dataBaseType
      } else {
        request.queryFlag = 1
      }
      this.databaseLoading = true

      http(constant.QUERY_DATA_BASE_ROWS, 'post', request).then(res => {
        this.databaseLoading = false

        if (res.errorCode !== '0') {
          this.$message.error(`查询数据库失败：${res.errorMsg}`)
          return
        }

        let dataList = res.dataBaseList || []
        this.databaseListData.splice(0, this.databaseListData.length, ...dataList)
        this.databaseDataTotal = res.total || dataList.length

        this.$forceUpdate()

        if (dataList.length > 0) {
          this.databaseListData.forEach(item => {
            item.addr = item.url || ''
            item.showPassWord = '*'.repeat(item.passWordLength || 6)
          })
          this.databaseSelectId = this.databaseListData[0].dataBaseId
          this.databaseCurRow = this.databaseListData[0]

          this.$nextTick(() => {
            if (this.$refs.databaseTableRef) {
              this.setDatabaseCurrent(this.databaseListData[0])
            }
          })
        }
      }).catch(err => {
        this.databaseLoading = false
        this.$message.error(`查询数据库数据源失败：${err}`)
      })
    },
    handleDatabaseCurrentChange(row) {
      this.databaseSelectId = (row || {}).dataBaseId
      this.databaseCurRow = row
    },
    handleDatabaseRadioChange(row) {
      this.databaseCurRow = row
      this.databaseSelectId = row.dataBaseId
    },
    setDatabaseCurrent(row) {
      this.databaseCurRow = row
      this.$refs.databaseTableRef.setCurrentRow(row);
    },

    // ========== 文件数据源相关方法 ==========
    handleFileSizeChange (val) {
      this.filePageSize = val
      this.queryFileSource()
    },
    handleFilePageChange (val) {
      this.filePageNum = val
      this.queryFileSource()
    },
    queryFileSource() {
      let request = {
        queryFlag: '1', // 查询全部
        page: this.filePageNum,
        count: this.filePageSize,
      }
      this.fileLoading = true

      http(constant.QUERY_FILE_ROWS, 'post', request).then(res => {
        this.fileLoading = false

        if (res.errorCode !== '0') {
          this.$message.error(`查询文件数据源失败：${res.errorMsg}`)
          return
        }

        let dataList = res.fileFormatList || []
        // 只显示上线状态的文件（onLineFlag === 2）
        dataList = dataList.filter(item => item.onLineFlag === 2)

        // 如果选择了文件类型过滤
        if (this.fileQueryForm.fileType !== '0') {
          dataList = dataList.filter(item => String(item.fileType) === this.fileQueryForm.fileType)
        }

        this.fileListData.splice(0, this.fileListData.length, ...dataList)
        this.fileDataTotal = dataList.length

        this.$forceUpdate()

        if (dataList.length > 0) {
          this.fileSelectId = this.fileListData[0].fileFormatId
          this.fileCurRow = this.fileListData[0]

          this.$nextTick(() => {
            if (this.$refs.fileTableRef) {
              this.setFileCurrent(this.fileListData[0])
            }
          })
        }
      }).catch(err => {
        this.fileLoading = false
        this.$message.error(`查询文件数据源失败：${err}`)
      })
    },
    handleFileCurrentChange(row) {
      this.fileSelectId = (row || {}).fileFormatId
      this.fileCurRow = row
    },
    handleFileRadioChange(row) {
      this.fileCurRow = row
      this.fileSelectId = row.fileFormatId
    },
    setFileCurrent(row) {
      this.fileCurRow = row
      this.$refs.fileTableRef.setCurrentRow(row);
    },

    // ========== MQ数据源相关方法 ==========
    handleMqSizeChange (val) {
      this.mqPageSize = val
      this.queryMqSource()
    },
    handleMqPageChange (val) {
      this.mqPageNum = val
      this.queryMqSource()
    },
    queryMqSource() {
      let request = {
        queryFlag: '1', // 查询全部
        page: this.mqPageNum,
        count: this.mqPageSize,
      }
      this.mqLoading = true

      http(constant.QUERY_MQ_ROWS, 'post', request).then(res => {
        this.mqLoading = false

        if (res.errorCode !== '0') {
          this.$message.error(`查询MQ数据源失败：${res.errorMsg}`)
          return
        }

        let dataList = res.mqConfigList || []
        // 只显示上线状态的MQ（onLineFlag === 2）
        dataList = dataList.filter(item => item.onLineFlag === 2)

        // 如果选择了MQ类型过滤
        if (this.mqQueryForm.mqType !== '0') {
          dataList = dataList.filter(item => String(item.mqType) === this.mqQueryForm.mqType)
        }

        this.mqListData.splice(0, this.mqListData.length, ...dataList)
        this.mqDataTotal = dataList.length

        this.$forceUpdate()

        if (dataList.length > 0) {
          this.mqSelectId = this.mqListData[0].mqId
          this.mqCurRow = this.mqListData[0]

          this.$nextTick(() => {
            if (this.$refs.mqTableRef) {
              this.setMqCurrent(this.mqListData[0])
            }
          })
        }
      }).catch(err => {
        this.mqLoading = false
        this.$message.error(`查询MQ数据源失败：${err}`)
      })
    },
    handleMqCurrentChange(row) {
      this.mqSelectId = (row || {}).mqConfigId
      this.mqCurRow = row
    },
    handleMqRadioChange(row) {
      this.mqCurRow = row
      this.mqSelectId = row.mqConfigId
    },
    setMqCurrent(row) {
      this.mqCurRow = row
      this.$refs.mqTableRef.setCurrentRow(row);
    },

    // ========== 确认选择 ==========
    confirm() {
      let selectedData = null

      if (this.activeTab === 'database') {
        if (!this.databaseSelectId || !this.databaseCurRow) {
          this.$message.error(`请选择数据库数据源！`)
          return
        }
        // 数据库数据源返回格式
        selectedData = {
          ...this.databaseCurRow,
          dataSourceCategory: 'database',
          dataSourceId: this.databaseCurRow.dataBaseId,
          dataSourceType: this.databaseCurRow.dataBaseType,
          dataSourceName: this.databaseCurRow.dataBaseName
        }
      } else if (this.activeTab === 'file') {
        if (!this.fileSelectId || !this.fileCurRow) {
          this.$message.error(`请选择文件数据源！`)
          return
        }
        // 文件数据源返回格式，统一字段名
        selectedData = {
          ...this.fileCurRow,
          dataSourceCategory: 'file',
          dataSourceId: this.fileCurRow.fileFormatId, // 统一使用 dataSourceId
          dataSourceType: this.fileCurRow.fileType,
          dataSourceName: this.fileCurRow.fileNameFormat, // 统一使用 dataSourceName
          // 保留原始字段
          fileFormatId: this.fileCurRow.fileFormatId,
          fileNameFormat: this.fileCurRow.fileNameFormat,
          fileType: this.fileCurRow.fileType
        }
      } else if (this.activeTab === 'mq') {
        if (!this.mqSelectId || !this.mqCurRow) {
          this.$message.error(`请选择MQ数据源！`)
          return
        }
        // MQ数据源返回格式，统一字段名
        selectedData = {
          ...this.mqCurRow,
          dataSourceCategory: 'mq',
          dataSourceId: this.mqCurRow.mqConfigId, // 统一使用 dataSourceId
          dataSourceType: this.mqCurRow.mqType,
          dataSourceName: this.mqCurRow.mqConfigName // 统一使用 dataSourceName
        }
      }

      if (selectedData) {
        const eventName = 'confirmSelectDataSource' + this.uniqueId
        this.bus.$emit(eventName, selectedData)
      }
    }
  }
}
</script>

<style>
.data-source-select-container {
  height: 450px;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

/* Tab 样式 */
.data-source-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.data-source-tabs .el-tabs__header {
  margin: 0;
  padding: 0 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  flex-shrink: 0;
}

.data-source-tabs .el-tabs__content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.data-source-tabs .el-tab-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 头部区域 */
.header-section {
  flex-shrink: 0;
  padding: 12px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-weight: 500;
  color: #495057;
  white-space: nowrap;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #6c757d;
  padding: 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  color: #dee2e6;
}

.empty-text {
  font-size: 14px;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.empty-desc {
  font-size: 12px;
  margin: 0 0 16px 0;
  opacity: 0.7;
}

.empty-actions {
  margin-top: 16px;
}

/* 加载状态 */
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #409eff;
  padding: 20px;
}

.loading-icon {
  font-size: 36px;
  margin-bottom: 12px;
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 表格区域 */
.table-section {
  flex: 1;
  padding: 0 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.data-source-select-container .el-table {
  border: 1px solid #e9ecef;
  border-radius: 6px;
}

.data-source-select-container .el-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #495057;
  padding: 8px 0;
}

.data-source-select-container .el-table th .cell {
  padding: 0 8px;
}

.data-source-select-container .el-table td .cell {
  padding: 0 8px;
}

.data-source-select-container .el-table__row .el-radio .el-radio__label {
  display: none;
}

.data-source-select-container .el-table .el-radio {
  margin: 0;
}

/* 分页区域 */
.pagination-section {
  flex-shrink: 0;
  padding: 12px 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
}

/* 确保对话框内容可见 - 限制为仅影响本组件的弹窗 */
.data-source-select-dialog .el-dialog__body {
  padding: 0;
  overflow: hidden;
  max-height: 70vh;
}

/* 对话框样式优化 - 限制为仅影响本组件的弹窗 */
.data-source-select-dialog {
  border-radius: 8px;
  overflow: hidden;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.data-source-select-dialog .el-dialog__header {
  padding: 12px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  flex-shrink: 0;
}

.data-source-select-dialog .el-dialog__title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.data-source-select-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #909399;
  font-size: 18px;
}

.data-source-select-dialog .el-dialog__headerbtn:hover .el-dialog__close {
  color: #409eff;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .data-source-select-container {
    height: 400px;
  }

  .table-section .el-table {
    height: 240px;
  }
}

@media (max-width: 768px) {
  .header-section {
    padding: 10px 16px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 6px;
  }

  .filter-group {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .data-source-select-container {
    height: 350px;
  }
}

/* 标签颜色优化 */
.el-tag.el-tag--success {
  background-color: #d4edda;
  border-color: #c3e6cb;
  color: #155724;
}

.el-tag.el-tag--warning {
  background-color: #fff3cd;
  border-color: #ffeaa7;
  color: #856404;
}

.el-tag.el-tag--info {
  background-color: #d1ecf1;
  border-color: #bee5eb;
  color: #0c5460;
}
</style>
