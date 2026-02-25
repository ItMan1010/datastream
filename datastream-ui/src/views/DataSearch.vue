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
        <el-form ref="queryForm" :label-position="'left'" :inline="true" :model="queryForm">
          <el-form-item label="源库选择：" prop="sourceDataSourceName">
            <el-input v-model="queryForm.sourceDataSourceName" clearable style="width: 170px;" disabled placeholder="请选择源库数据源">
              <template #append>
              <el-button @click="selectDataSource()">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
            </el-input>
          </el-form-item>
          <el-form-item label="单页记录数:">
            <el-select v-model.number="queryForm.count" style="width: 80px;">
              <el-option label="10" value="10"></el-option>
              <el-option label="20" value="20"></el-option>
              <el-option label="50" value="50"></el-option>
              <el-option label="100" value="100"></el-option>
              <el-option label="200" value="200"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="表名称:">
            <el-autocomplete
              class="inline-input"
              v-model="queryForm.tableName"
              :fetch-suggestions="querySearch"
              placeholder="请输入表名称"
              clearable
              @select="handleSelect"
            ></el-autocomplete>
          </el-form-item>
          <template>
            <el-tooltip class="item" effect="dark" :content="queryConditionTip" placement="top">
              <el-form-item label="过滤条件:">
                <el-input v-model="queryForm.queryCondition" clearable style="width: 200px;"></el-input>
              </el-form-item>
            </el-tooltip>
          </template>
          <el-form-item>
            <el-button class="ml-20" type="primary" :loading="loading" @click="queryOpt">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
<!--          <el-form-item>-->
<!--            <el-button class="ml-20" :loading="loading" @click="queryCount">数量查询</el-button>-->
<!--          </el-form-item>-->
        </el-form>

      </div>
    <el-divider></el-divider>
      <div class="mt-10 pl-20 pr-20">
        <el-table :data="dataBaseListData" fit stripe  highlight-current-row style="width: 100%;">
          <el-table-column type="expand">
            <template #default="props">
              <el-form label-position="right" inline class="detail-table-expand">
                <el-form-item v-for="(item, idx) in tableColumnNameList" :key="idx + 10000" :label="item.column + ':'">
                  <span>{{ (props.row[item.column] === null || props.row[item.column] === undefined) ? '(NULL)' :  props.row[item.column] }}</span>
                </el-form-item>
              </el-form>
            </template>
          </el-table-column>
          <template v-if="tableColumnNameList.length === 0">
            <el-table-column prop="" label=""></el-table-column>
          </template>
          <template v-else>
            <el-table-column v-for="(item, idx) in tableColumnNameList" :key="idx" :prop="item.column" :label="item.column" :min-width="item.minWidth" :show-overflow-tooltip="true">
              <template #default="scope">
                <span>{{ (scope.row[item.column] === null || scope.row[item.column] === undefined) ? '(NULL)' :  scope.row[item.column]}}</span>
              </template>
            </el-table-column>
          </template>
        </el-table>
        <div class="pt-20" style="text-align: center">
          <div style="margin-right: 10px; line-height: 32px; font-size: 14px; float: left;"><span>总记录数：{{ detailCount }}</span></div>
          <el-pagination
            @current-change="handleCurrentChange"
            v-model:current-page="pageNum"
            :page-size="queryForm.count"
            :total="dataMoveTaskListTotal"
            layout="prev, pager, next">
          </el-pagination>
        </div>
      </div>
    <el-dialog
      title="数据源选择"
      v-model="dialogTableVisible"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close>
      <data-source-select :unique-id="uniqueId" :data-source-types-filter="['database']"></data-source-select>
    </el-dialog>
  </div>
</template>
<script>
import http from '@/utils/request'
import constant from '@/comm/constants'
import tableNameStore from '@/comm/tableNameStore'
import * as commMethod from '../comm/commMethod'
import DataSourceSelect from './components/DataSourceSelect.vue'
import { getCurrentInstance } from 'vue'
import { Search } from '@element-plus/icons-vue'
export default {
  name: 'DataSearch',
  components: {
    DataSourceSelect,
    Search
  },
  setup() {
    const instance = getCurrentInstance()
    return {
      $bus: instance?.appContext.config.globalProperties.$bus
    }
  },
  data () {
    return {
      uniqueId: 'data-search',
      pageSize: 10,
      pageNum: 1,
      dataMoveTaskListTotal: 0,
      loading: false,
      queryForm: {
        sourceDataSourceName: '',
        sourceDataSourceId: null,
        count: 10,
        tableName: '',
        queryCondition: ''
      },
      dataBaseListData: [],
      dialogTableVisible: false,
      tableNameList: [],
      tableColumnNameList: [],
      detailCount: null,
    }
  },
  computed: {
    queryConditionTip () {
      return this.queryForm.queryCondition || '请输入过滤条件'
    }
  },
  mounted () {
    this.tableNameList = this.loadAll(tableNameStore)
    this.$bus.$on('confirmSelectDataSource' + this.uniqueId, (dataSourceRow) => {
      this.dialogTableVisible = false
      this.queryForm.sourceDataSourceId = dataSourceRow.dataBaseId
      this.queryForm.sourceDataSourceName = dataSourceRow.dataBaseName
    })
  },
  beforeUnmount() {
    this.$bus.$off('confirmSelectDataSource' + this.uniqueId)
  },
  methods: {
    selectDataSource() {
      this.dialogTableVisible = true
    },
    querySearch(queryString, cb) {
      let tableNameList = this.tableNameList
      let results = queryString ? this.createFilter(queryString) : tableNameList.slice(0, 20)
      cb(results)
    },
    createFilter(queryString) {
      let results = []
      for (let i = 0; i < this.tableNameList.length; i++) {
        let element = this.tableNameList[i]
        if (element.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0) {
          results.push(element)
        }
        if (results.length >= 20) {
          break
        }
      }
      return results
    },
    loadAll(tableNameStore) {
      let list = []
      tableNameStore.forEach(element => {
        list.push({
          value: element
        })
      })
      return list
    },
    handleSelect() {
    },
    handleSizeChange (val) {
      this.pageSize = val
      this.queryDataSourceList()
    },
    handleCurrentChange (val) {
      this.pageNum = val
      this.queryData(val)
    },
    queryOpt() {
      this.pageNum = 1
      this.queryData(1)
    },
    queryData(pageNum) {
      this.detailCount = null
      this.dataBaseListData = []
      this.tableColumnNameList = []
      if (pageNum === 1) {
        this.dataMoveTaskListTotal = 0
      }
      if (!this.queryForm.sourceDataSourceId && this.queryForm.sourceDataSourceId !== 0) {
        this.$message.error(`请选择数据源！`)
        return
      }
      if (!this.queryForm.tableName) {
        this.$message.error(`请输入表名！`)
        return
      }
      if (this.queryForm.queryCondition && this.queryForm.queryCondition.includes(';')) {
        this.$message.error(`过滤条件中包含非法字符【;】！`)
        return
      }
      let request = {
        flag: 3,
        dataSourceId: this.queryForm.sourceDataSourceId,
        page: pageNum,
        count: this.queryForm.count,
        tableName: this.queryForm.tableName,
        queryCondition: this.queryForm.queryCondition
      }
      this.pageSize = this.queryForm.count
      this.loading = true
      http(constant.DATA_SEARCH, 'post', request).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`数据检索失败：${res.errorMsg}`)
          return
        }
        this.detailCount = res.recordSum || 0
        let dateList = res.dataRecordList || []
        let list = res.tableColumnNameList || []
        list.forEach(item => {
          this.tableColumnNameList.push({
            column: item,
            minWidth: item.length > 10 ? item.length * 12 : 120
          })
        })
        dateList.forEach(item => {
          let data = {}
          for (let key in item) {
            data[key] = item[key]
          }
          this.dataBaseListData.push(data)
        })
        if (this.dataBaseListData.length >= this.queryForm.count) {
          this.dataMoveTaskListTotal = pageNum * this.queryForm.count + 1
        } else {
          this.dataMoveTaskListTotal = pageNum * this.queryForm.count
        }
      }).catch(err => {
        this.loading = false
        this.$message.error(`数据检索失败：${err}`)
      })
    },
    queryCount() {
      if (!this.queryForm.sourceDataSourceId && this.queryForm.sourceDataSourceId !== 0) {
        this.$message.error(`请选择数据源！`)
        return
      }
      if (!this.queryForm.tableName) {
        this.$message.error(`请输入表名！`)
        return
      }
      let request = {
        flag: 1,
        page: 1,
        count: this.queryForm.count,
        dataSourceId: this.queryForm.sourceDataSourceId,
        tableName: this.queryForm.tableName,
        queryCondition: this.queryForm.queryCondition
      }
      this.loading = true
      http(constant.DATA_SEARCH, 'post', request).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`总记录数查询失败：${res.errorMsg}`)
          return
        }
        this.detailCount = res.recordSum || 0
      }).catch(err => {
        this.loading = false
        this.$message.error(`总记录数查询失败：${err}`)
      })
    }
  }
}
</script>
