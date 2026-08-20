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
  <div class="main-content">
    <div class="search-nav">
      <el-form ref="queryForm" :model="queryForm" label-width="140px" @keyup.native.enter="queryTableLinkTaskList">
        <el-row style="line-height: 30px;">
          <el-col :span="18">
            <el-select v-model="queryForm.queryFlag" style="width: 110px !important;">
              <el-option label="任务标识" value="1"></el-option>
              <el-option label="任务方式" value="2"></el-option>
              <el-option label="任务状态" value="3"></el-option>
              <el-option label="创建时间" value="4"></el-option>
            </el-select>
            <el-input class="pl-20" v-if="queryForm.queryFlag == '1'" type="number" v-model="queryForm.linkTaskId"
                      onkeyup="value = value.replace(/[^\d]/g,'')" clearable style="width: 200px;"></el-input>
            <el-select class="pl-20" v-if="queryForm.queryFlag == '2'" v-model="queryForm.taskType"
                       style="width: 150px;">
              <el-option label="按链接迁移" value="1"></el-option>
            </el-select>
            <el-input class="pl-20" v-if="queryForm.queryFlag == '2'" type="number" v-model="queryForm.linkTaskId"
                      onkeyup="value = value.replace(/[^\d]/g,'')" clearable style="width: 200px;"></el-input>
            <el-select class="pl-20" v-if="queryForm.queryFlag == '3'" v-model="queryForm.state"
                       style="width: 110px !important;">
              <el-option label="等待回迁" value="0"></el-option>
              <el-option label="回迁中" value="1"></el-option>
              <el-option label="回迁成功" value="2"></el-option>
              <el-option label="回迁失败" value="3"></el-option>
            </el-select>
            <el-date-picker
              class="ml-20"
              v-if="queryForm.queryFlag === '4'"
              v-model="queryForm.backDate"
              type="datetimerange"
              :shortcuts="pickerOptions.shortcuts"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              align="right">
            </el-date-picker>
            <el-button class="ml-20" type="primary" :loading="loading" @click="queryTableLinkTaskList">
              <el-icon>
                <Search/>
              </el-icon>
              查询
            </el-button>
          </el-col>
          <el-col :span="5">
            <el-button type="primary" plain @click="openCreateDrawer">
              <el-icon>
                <Plus/>
              </el-icon>
              创建任务
            </el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <el-divider></el-divider>
    <div class="mt-10 pl-20" style="padding-right: 20px;">
      <el-table :data="tableData" fit stripe highlight-current-row style="width: 100%;" :height="500">
        <el-table-column prop="linkTaskId" label="任务ID" width="120" :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="createDate" label="创建时间" width="160"></el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <span v-if="scope.row.state === 0" style="color: #0099CC;">等待中</span>
            <span v-else-if="scope.row.state === 1" style="color: var(--primary-color)">处理中</span>
            <span v-else-if="scope.row.state === 2" style="color: #67C23A;">处理成功</span>
            <span v-else-if="scope.row.state === 3" style="color: #FF0000;">处理失败</span>
            <span v-else-if="scope.row.state === 4" style="color: #ffa07a;">处理暂停</span>
          </template>
        </el-table-column>

        <el-table-column label="源数据源" width="180">
          <template #default="scope">
            <span>{{ scope.row.targetDataSource ? scope.row.targetDataSource.dataBaseName : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="目标数据源" width="180">
          <template #default="scope">
            <span>{{ scope.row.sourceDataSource ? scope.row.sourceDataSource.dataBaseName : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tableLinkName" label="链接名称" width="180"
                         :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="businessId" label="业务流水" width="180"></el-table-column>
        <el-table-column prop="taskDisc" label="任务描述" width="180" :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="errorCode" label="错误编码" width="120"></el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" width="180" :show-overflow-tooltip="true"></el-table-column>
        <el-table-column fixed="right" label="操作" width="80">
          <template #default="scope">
            <el-tooltip class="item" effect="dark" content="复制" placement="top">
              <el-button @click="copyBackTask(scope.row)" type="info" link size="small">
                <el-icon>
                  <CopyDocument/>
                </el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip class="item" effect="dark" content="日志" placement="top">
              <el-button @click="showTaskLog(2, scope.row.linkTaskId)" type="primary" link size="small"
                         style="margin-right: 5px;">
                <el-icon>
                  <Tickets/>
                </el-icon>
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="queryForm.queryFlag === '3' || queryForm.queryFlag === '4'" class="pt-20" style="text-align: center">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          v-model:current-page="pageNum"
          :page-sizes="[20, 50, 100]"
          :page-size="pageSize"
          :total="dataTotal"
          layout="total, sizes, prev, pager, next, jumper">
        </el-pagination>
      </div>
    </div>


    <el-drawer v-model="createDataBackDrawer" :direction="newTaskDrawerDir" :modal-append-to-body="false" size="85%"
               class="newTaskClass">
      <div style="position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: #EBEEF5;">
        <div class="bcFFF margin-10 pl-20" style="line-height: 40px; color: var(--primary-color);">
          <label class="fontWeight">链接任务管理</label> > 创建任务
        </div>
        <div class="pt-10 margin-10 bcFFF pb-20"
             style="position: absolute; bottom: 10px; top: 50px; left: 0; right: 0; overflow-y: auto !important; overflow-x: hidden !important;">
          <div class="form-container">
            <el-form class="create-task-form" ref="form" :model="form" label-width="140px">
              <el-form-item label="选择源数据源：">
                <el-input v-model="form.sourceDataSourceName" clearable style="width: 400px;" disabled
                          placeholder="请选择源数据源">
                  <template #append>
                    <el-button @click="selectDataSource(false)">
                      <el-icon>
                        <Search/>
                      </el-icon>
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="选择目标数据源：">
                <el-input v-model="form.targetDataSourceName" clearable style="width: 400px;" disabled
                          placeholder="请选择目标数据源">
                  <template #append>
                    <el-button @click="selectDataSource(true)">
                      <el-icon>
                        <Search/>
                      </el-icon>
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="选择表链接配置：">
                <el-select v-model="form.tableLinkId" style="width: 400px;">
                  <el-option v-for="(item, idx) in tableLinkList" :key="idx" :label="item.label"
                             :value="item.value"></el-option>
                </el-select>
              </el-form-item>

              <el-form-item label="输入业务流水：">
                <el-input v-model="form.businessId" clearable style="width: 400px;"></el-input>
              </el-form-item>

              <el-form-item label="输入任务描述：">
                <el-input type="textarea" clearable rows="3" v-model="form.taskDisc" style="width: 400px;">
                </el-input>
              </el-form-item>

              <el-form-item class="button-group">
                <el-button type="primary" :loading="loading" @click="createTableLinkTask">
                  <el-icon>
                    <Check/>
                  </el-icon>
                  确认
                </el-button>
                <el-button type="info" :loading="loading" @click="init">
                  <el-icon>
                    <Refresh/>
                  </el-icon>
                  重置
                </el-button>
                <el-button :loading="loading" @click="cancel">
                  <el-icon>
                    <Close/>
                  </el-icon>
                  取消
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog
      title="数据源选择"
      v-model="dialogTableVisible"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body>
      <data-source-select :unique-id="uniqueId" :data-source-types-filter="['database']"></data-source-select>
    </el-dialog>
    <el-drawer v-model="taskLogDrawer"
               :direction="newTaskDrawerDir"
               :modal-append-to-body="false"
               size="85%"
               class="newTaskClass">
      <div style="position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: #EBEEF5;">
        <div class="bcFFF margin-10 pl-20" style="line-height: 40px; color: var(--primary-color);">
          <label class="fontWeight">回迁任务</label> > 回迁执行日志
          <el-button type="text" style="float: right; font-size: 18px;" circle @click="taskLogDrawer = false">
            <el-icon>
              <Close/>
            </el-icon>
          </el-button>
        </div>
        <div class="margin-10 bcFFF"
             style="overflow-y: auto !important; overflow-x: hidden !important; padding: 20px; position: absolute; inset: 50px 0px 10px; background-color: #000; color: #fff;">
          <div class="exceed-line-feed">
            <li v-for="(item, idx) in taskLogInfo" :key="idx">{{ item }}</li>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import DataSourceSelect from '../components/DataSourceSelect.vue';
import {useMainStore} from '@/store/index.js'
import {getCurrentInstance} from 'vue'
import dayjs from 'dayjs'
import {
  Search,
  Plus,
  Tickets,
  CopyDocument,
  Close,
  Check,
  Refresh
} from '@element-plus/icons-vue'

export default {
  name: 'TableLinkTask',
  components: {
    DataSourceSelect,
    Search,
    Plus,
    Tickets,
    CopyDocument,
    Close,
    Check,
    Refresh
  },
  setup() {
    const instance = getCurrentInstance()
    const mainStore = useMainStore()
    return {
      mainStore,
      $bus: instance?.appContext.config.globalProperties.$bus,
      dayjs
    }
  },
  data() {
    return {
      loading: false,
      queryForm: {
        queryFlag: '4',
        linkTaskId: '',
        state: '0',
        backDate: []
      },
      pickerOptions: {
        shortcuts: [
          {
            text: '今天',
            value: () => {
              let start = new Date()
              start.setHours(0)
              start.setMinutes(0)
              start.setSeconds(0)
              let end = new Date()
              end.setHours(23)
              end.setMinutes(59)
              end.setSeconds(59)
              return [start, end]
            }
          },
          {
            text: '昨天',
            value: () => {
              let end = new Date()
              let start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24)
              start.setHours(0)
              start.setMinutes(0)
              start.setSeconds(0)
              end.setTime(end.getTime() - 3600 * 1000 * 24)
              end.setHours(23)
              end.setMinutes(59)
              end.setSeconds(59)
              return [start, end]
            }
          },
          {
            text: '最近一周',
            value: () => {
              let end = new Date()
              let start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
              start.setHours(0)
              start.setMinutes(0)
              start.setSeconds(0)
              end.setHours(23)
              end.setMinutes(59)
              end.setSeconds(59)
              return [start, end]
            }
          }
        ]
      },
      dataBaseListData: [],
      tableData: [],
      dataTotal: 0,
      pageNum: 1,
      pageSize: 50,
      dataMoveTaskListTotal: 0,
      createDataBackDrawer: false,
      newTaskDrawerDir: 'btt',
      tableLinkList: [],
      form: {
        tableLinkId: '',
        conValue: '',
        businessId: '',
        targetDataSourceId: null,
        sourceDataSourceId: null,
        targetDataSourceName: null,
        sourceDataSourceName: null,
        histFlag: false,
      },
      dialogTableVisible: false,
      isTargetDataSource: false,
      uniqueId: '_back',
      taskLogInfo: [],
      taskLogDrawer: false,
    }
  },
  mounted() {
    this.initDefaultTimeRange()
    this.queryTableLinkTaskList()
    this.queryTableLinkList()
    this.$bus.$on('confirmSelectDataSource' + this.uniqueId, (dataSourceRow) => {
      this.dialogTableVisible = false
      if (this.isTargetDataSource) {
        this.form.targetDataSourceId = dataSourceRow.dataBaseId
        this.form.targetDataSourceName = dataSourceRow.dataBaseName
      } else {
        this.form.sourceDataSourceId = dataSourceRow.dataBaseId
        this.form.sourceDataSourceName = dataSourceRow.dataBaseName
      }
    })
  },
  activated() {
    if (this.mainStore.commQueryParams) {
      this.queryForm = {
        ...{
          queryFlag: '4',
          linkTaskId: '',
          taskType: '1',
          state: '0',
          backDate: []
        },
        ...this.mainStore.commQueryParams
      }
      this.queryTableLinkTaskList()
      this.mainStore.commQueryParams = null
    } else {
      // 如果没有外部查询参数，确保显示默认时间范围并自动查询
      this.initDefaultTimeRange()
      this.queryTableLinkTaskList()
    }
  },
  beforeUnmount() {
    this.$bus.$off('confirmSelectDataSource' + this.uniqueId)
  },
  methods: {
    initDefaultTimeRange() {
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      start.setHours(0)
      start.setMinutes(0)
      start.setSeconds(0)
      const end = new Date()
      end.setHours(23)
      end.setMinutes(59)
      end.setSeconds(59)
      this.queryForm.queryFlag = '4'
      // 使用 dayjs 转换为字符串格式，与 el-date-picker 的 value-format 保持一致
      this.queryForm.backDate = [
        this.dayjs(start).format('YYYY-MM-DD HH:mm:ss'),
        this.dayjs(end).format('YYYY-MM-DD HH:mm:ss')
      ]
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.queryTableLinkTaskList()
    },
    handleCurrentChange(val) {
      this.pageNum = val
      this.queryTableLinkTaskList()
    },
    queryTableLinkTaskList() {
      this.tableData = []
      let request = {
        queryFlag: this.queryForm.queryFlag
      }
      if (this.queryForm.queryFlag === '1') {
        if (!this.queryForm.linkTaskId || !this.queryForm.linkTaskId.trim() || this.queryForm.linkTaskId < 1) {
          this.$message.warning('请输入数据的任务标识')
          return
        }
        request.linkTaskId = this.queryForm.linkTaskId
      } else if (this.queryForm.queryFlag === '2') {
        if (!this.queryForm.taskType || !this.queryForm.taskType.trim()) {
          this.$message.warning('请输入数据回迁方式')
          return
        }
      } else if (this.queryForm.queryFlag === '3') {
        if (!this.queryForm.state || !this.queryForm.state.trim()) {
          this.$message.warning('请输入数据回迁任务状态')
          return
        }
        request.state = this.queryForm.state
        request.page = this.pageNum
        request.count = this.pageSize
      } else {
        let beginDate = this.queryForm.backDate[0]
        let endDate = this.queryForm.backDate[1]
        if (!beginDate) {
          this.$message.warning('按时间查询任务时，开始时间不能不能为空')
          return
        }
        if (!endDate) {
          this.$message.warning('按时间查询任务时，结束时间不能不能为空')
          return
        }
        // 修复：使用 dayjs 进行时间比较，因为现在 beginDate 和 endDate 是字符串格式
        if (this.dayjs(beginDate).isAfter(this.dayjs(endDate))) {
          this.$message.warning('按时间查询任务时，开始时间不能大于结束时间')
          return
        }
        request.beginDate = this.dayjs(beginDate).format('YYYYMMDDHHmmss')
        request.endDate = this.dayjs(endDate).format('YYYYMMDDHHmmss')
        request.page = this.pageNum
        request.count = this.pageSize
      }
      this.loading = true
      http(constant.QUERY_TABLE_LINK_TASK_LIST, 'post', request).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`查询数据回迁任务失败：${res.errorMsg}`)
          return
        }
        this.tableData = res.tableLinkTaskList || []
        this.dataTotal = res.count
      }).catch(err => {
        this.loading = false
        this.$message.error(`查询数据回迁任务失败：${err}`)
      })
    },
    queryTableLinkList() {
      this.tableLinkList = []
      let request = {
        queryFlag: 5,
        queryValue: 2,
        page: 1,
        count: 1000,
      }
      this.loading = true
      http(constant.QUERY_TABLE_LINK, 'post', request).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`查询链接信息失败：${res.errorMsg}`)
          return
        }
        let tableData = res.tableLinkList || []
        this.tableLinkList = []
        tableData.forEach(item => {
          this.tableLinkList.push({
            label: item.tableLinkName,
            value: item.tableLinkId,
          })
        })
        if (this.tableLinkList.length > 0) {
          this.form.tableLinkId = this.tableLinkList[0].value
        }
      }).catch(err => {
        this.loading = false
        this.$message.error(`查询链接明细信息失败：${err}`)
      })
    },
    createTableLinkTask() {
      let request = {
        systemUserCode: this.mainStore.getLoginSystemUser.systemUserCode
      }
      if (!this.form.targetDataSourceId) {
        this.$message.warning('请选择源数据源')
        return
      }
      if (!this.form.sourceDataSourceId) {
        this.$message.warning('请选择目标数据源')
        return
      }

      if (!this.form.businessId || !this.form.businessId.trim()) {
        this.$message.warning('请输入业务流水')
        return
      }
      request.tableLinkId = this.form.tableLinkId
      request.businessId = this.form.businessId
      request.targetDataBaseId = this.form.targetDataSourceId
      request.sourceDataBaseId = this.form.sourceDataSourceId
      request.taskDisc = this.form.taskDisc
      this.loading = true
      http(constant.CREATE_TABLE_LINK_TASK, 'post', request).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`生成链接任务失败：${res.errorMsg}`)
          return
        }
        this.pageNum = 1
        this.queryTableLinkTaskList()
        this.createDataBackDrawer = false
        this.$message.success('生成链接任务成功')
      }).catch(err => {
        this.loading = false
        this.$message.error(`生成链接任务失败：${err}`)
      })
    },
    selectDataSource(isTargetDataSource) {
      this.dialogTableVisible = true
      this.isTargetDataSource = isTargetDataSource
    },
    init() {
      this.form = {
        tableLinkId: this.tableLinkList.length > 0 ? this.tableLinkList[0].value : '',
        conValue: '',
        businessId: '',
        targetDataSourceId: null,
        sourceDataSourceId: null,
        targetDataSourceName: null,
        sourceDataSourceName: null,
      }
    },
    openCreateDrawer() {
      this.init();
      this.createDataBackDrawer = true;
    },
    cancel() {
      this.init()
      this.createDataBackDrawer = false
    },
    showTaskLog(jobType, jobId) {
      this.taskLogInfo = []
      let request = {
        jobType: jobType,
        jobId: jobId,
      }
      this.loading = true
      http(constant.QUERY_JOB_BACK, 'post', request).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`查询任务日志出错：${res.errorMsg}`)
          return
        }
        (res.jobLogbackList || []).forEach(item => {
          let logDesc = `[${item.jobLogbackId}] [${item.createDate}] `
          if (item.content) {
            let list = item.content.replace(/\\\\t/g, '  ').split(/\\\\n/g);
            list.forEach((item, idx, arr) => arr[idx] = logDesc + item)
            this.taskLogInfo = this.taskLogInfo.concat(list)
          }
        })
        this.taskLogDrawer = true;
      }).catch(err => {
        this.loading = false
        this.$message.error(`查询任务日志出错：${err}`)
      })
    },
    copyBackTask(row) {
      this.$confirm(`是否确认复制当前任务（${row.linkTaskId}）?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        let request = {
          operate: 3,
          linkTaskId: row.linkTaskId,
        }
        this.loading = true
        http(constant.OPERATE_TABLE_LINK_TASK, 'post', request).then(res => {
          this.loading = false
          if (res.errorCode !== '0') {
            this.$message.error(`复制任务出错：${res.errorMsg}`)
            return
          }
          this.$message.success({
            message: `复制任务（${row.linkTaskId}）成功！`,
            duration: 1000,
            onClose: (e) => {
              this.queryTableLinkTaskList()
            }
          })
        }).catch(err => {
          this.loading = false
          this.$message.error(`复制任务出错：${err}`)
        })
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消复制操作！'
        })
      })
    }
  }
}
</script>

<style scoped>
.newTaskClass .el-drawer__body {
  overflow-x: hidden;
}

.bcFFF {
  background-color: #fff;
}

.margin-10 {
  margin: 10px;
}

.pl-20 {
  padding-left: 20px;
}

.fontWeight {
  font-weight: bold;
}

.pt-10 {
  padding-top: 10px;
}

.pb-20 {
  padding-bottom: 20px;
}

.pt-20 {
  padding-top: 20px;
}

.mt-20 {
  margin-top: 20px;
}

.ml-20 {
  margin-left: 20px;
}

.exceed-line-feed {
  white-space: pre-wrap;
  word-break: break-all;
}

.detail-table-expand {
  font-size: 14px;
}

.detail-table-expand .el-form-item {
  margin-bottom: 0;
  width: 33%;
}

/* 创建任务表单居中样式 */
.form-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100%;
  padding: 40px 20px;
}

.create-task-form {
  width: 100%;
  max-width: 600px;
  background: #fff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.create-task-form .el-form-item {
  margin-bottom: 24px;
}

.create-task-form .el-form-item__label {
  font-weight: 500;
  color: #303133;
}

.button-group {
  text-align: center;
  margin-top: 40px !important;
}

.button-group .el-button {
  margin: 0 8px;
  min-width: 100px;
  height: 40px;
}

.button-group .el-button .el-icon {
  margin-right: 4px;
}
</style>
