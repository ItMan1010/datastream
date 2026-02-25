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
  <div class="data-move-task location">
    <template>
      <h1 class="title1 pt-20">查询条件</h1>
    </template>
    <el-divider></el-divider>
    <template>
      <div class="mt-20">
        <el-form ref="queryForm" :model="form" label-width="140px">
          <el-row style="line-height: 30px;">
            <el-col :span="7">
              <el-form-item label="数据迁移标识：">
                <el-select v-model="form.queryFlag">
                  <el-option label="任务标识" value="1"></el-option>
                  <el-option label="任务运行标识" value="2"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item  v-if="form.queryFlag == '1'" label="任务标识查询：">
                <el-input type="number" v-model="form.taskId" onkeyup="value = value.replace(/[^\d]/g,'')" clearable
                          style="width: 240px;"></el-input>
              </el-form-item>
              <el-form-item  v-else label="任务运行标识查询：">
                <el-input type="number" v-model="form.infoId" onkeyup="value = value.replace(/[^\d]/g,'')" clearable
                          style="width: 240px;"></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="5" style="padding-bottom: 10px;">
              <el-button type="primary" icon="el-icon-search" v-loading.fullscreen.lock="loading" @click="queryDataMoveInfoList">查询</el-button>
            </el-col>
          </el-row>
        </el-form>
      </div>
    </template>
    <el-divider></el-divider>
    <template>
      <div class="pt-20">
        <h1 class="title1">查询结果</h1>
      </div>
      <el-divider></el-divider>
      <template>
        <el-table :data="tableData" fit stripe  highlight-current-row style="width: 100%;" height=460>
          <el-table-column prop="taskId" fixed="left" label="任务ID" width="120"></el-table-column>
          <el-table-column prop="infoId" fixed="left" label="运行ID" width="120"></el-table-column>
          <el-table-column prop="tableName" label="迁移表名" width="160"></el-table-column>
          <el-table-column prop="dataNode" label="teledb分片节点" width="120"></el-table-column>
          <el-table-column prop="createDate" label="生成时间" width="160"></el-table-column>
          <el-table-column prop="hostName" label="主机名称" width="180"></el-table-column>
          <el-table-column prop="hostIp" label="主机IP" width="140"></el-table-column>
          <el-table-column prop="virtualId" label="运行虚拟ID" width="100"></el-table-column>
          <el-table-column prop="threadName" label="线程名称" width="160"></el-table-column>
          <el-table-column prop="dataCount" label="处理记录数" width="120"></el-table-column>
          <el-table-column prop="pageRowStart" label="开始值" width="160"></el-table-column>
          <el-table-column prop="pageRowEnd" label="结束值" width="160"></el-table-column>
          <el-table-column label="任务状态" width="100">
            <template slot-scope="scope">
              <span v-if="scope.row.state === 0">等待中</span>
              <span v-else-if="scope.row.state === 1">处理中</span>
              <span v-else-if="scope.row.state === 2">处理结束</span>
              <span v-else>迁移失败</span>
            </template>
          </el-table-column>
          <el-table-column prop="stateDate" label="状态时间" width="160"></el-table-column>
          <el-table-column prop="errorCode" label="错误编码" width="120"></el-table-column>
          <el-table-column prop="errorMsg" label="错误信息" width="180"></el-table-column>
        </el-table>
      </template>
    </template>
  </div>
</template>

<script>
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
export default {
  name: 'dataMoveInfoList',
  components: {},
  data () {
    return {
      loading: false,
      form: {
        queryFlag: '1', // 1（任务标识查询）、2(任务运行标识查询)
        taskId: '',
        infoId: ''
      },
      dataBaseListData: []
    }
  },
  methods: {
    queryDataMoveInfoList () {
      let request = {
        queryFlag: this.form.queryFlag
      }
      if (this.form.queryFlag === '1') {
        if (!this.form.taskId || Number(this.form.taskId) < 1) {
          this.$message.warning('任务标识不能为空')
          return
        }
        request.taskId = this.form.taskId
      } else {
        if (!this.form.infoId || Number(this.form.infoId) < 1) {
          this.$message.warning('任务运行标识不能为空')
          return
        }
        request.infoId = this.form.infoId
      }
      this.loading = true
      http(constant.QUERY_DATA_MOVE_INFO_LIST, 'post', request).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`查询数据迁移任务执行明细失败：${res.errorMsg}`)
          return
        }
        this.tableData = res.dataMoveInfoList || []
        this.$message.success('查询数据迁移任务执行明细成功')
      }).catch(err => {
        this.loading = false
        this.$message.error(`查询数据迁移任务执行明细失败：${err}`)
      })
    }
  }
}
</script>

<style scoped>
@import '../../../assets/css/public.css';

</style>
