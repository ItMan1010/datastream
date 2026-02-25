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
    <div v-show="false">
      <el-form ref="queryForm" :label-position="'left'" :inline="true" :model="queryForm">
        <el-form-item>
          <el-button class="ml-20" type="primary" :loading="loading">增加遮蔽罩
          </el-button>
        </el-form-item>
      </el-form>
    </div>
      <div class="pl-20 pr-20" style="padding-top: 20px;">
        <el-table :data="tableData" fit stripe highlight-current-row style="width: 100%;">
          <el-table-column prop="systemLogId" label="日志ID" width="110" :show-overflow-tooltip="true"></el-table-column>
          <el-table-column prop="createDate" label="日志时间" width="160"></el-table-column>
          <el-table-column prop="username" label="操作用户" width="120" :show-overflow-tooltip="true"></el-table-column>
          <el-table-column prop="ipAddress" label="请求IP" width="140"></el-table-column>
          <el-table-column prop="moduleName" label="操作类型" width="120" :show-overflow-tooltip="true"></el-table-column>
          <el-table-column prop="content" label="操作描述" width="200" :show-overflow-tooltip="true"></el-table-column>
          <el-table-column prop="urlPath" label="请求路径" :show-overflow-tooltip="true"></el-table-column>
        </el-table>
        <div class="pt-20" style="text-align: center">
                  <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          v-model:current-page="pageNum"
            :page-sizes="[10, 20, 50]"
            :page-size="pageSize"
            :pager-count="7"
            :total="dataTotal"
            layout="total, sizes, prev, pager, next, jumper">
          </el-pagination>
        </div>
      </div>
  </div>
</template>

<script>
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import * as commMethod from '../../comm/commMethod.js'

export default {
  name: 'OperationLogs',
  data() {
    return {
      pageSize: 10,
      pageNum: 1,
      dataTotal: 0,
      loading: false,
      queryForm: {},
      tableData: []
    }
  },
  mounted() {
    this.queryLog()
  },
  activated() {
    this.queryLog()
  },
  methods: {
    handleSizeChange(val) {
      this.pageSize = val
      this.queryLog()
    },
    handleCurrentChange(val) {
      this.pageNum = val
      this.queryLog()
    },
    // 查询数据源列表
    queryLog() {
      this.tableData = []
      let request = {
        page: this.pageNum,
        count: this.pageSize,
        type: 2
      }
      this.loading = true
      http(constant.QUERY_SYSTEM_LOG, 'post', request).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`查询操作日志失败：${res.errorMsg}`)
          return
        }
        this.tableData = res.canalSystemLogList || []
        this.dataTotal = res.total || 0
      }).catch(err => {
        this.loading = false
        this.$message.error(`查询操作日志失败：${err}`)
      })
    }
  }
}
</script>
