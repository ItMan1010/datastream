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
  <el-dialog
    v-model="localVisible"
    :title="title"
    width="50%"
    :before-close="handleClose"
    append-to-body
    class="data-source-dialog">

    <div style="padding: 20px;">
      <el-form ref="form" :model="form" :rules="rules" label-width="160px">
        <el-form-item v-if="mode !== 'add'" label="数据库ID：" prop="dataBaseId">
          <el-input disabled v-model="form.dataBaseId" clearable></el-input>
        </el-form-item>
        <el-form-item label="数据库类型：" prop="dataBaseType">
          <el-select :disabled="mode === 'detail'" v-model.number="form.dataBaseType">
            <el-option label="Mysql" :value="2"></el-option>
            <el-option label="PostgreSQL" :value="4"></el-option>
            <el-option label="Oracle" :value="3"></el-option>
            <el-option label="Doris" :value="5"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="数据库名称：" prop="dataBaseName">
          <el-input :disabled="mode === 'detail'" v-model="form.dataBaseName" clearable></el-input>
        </el-form-item>

        <el-form-item label="URL：" prop="url">
          <el-input type="textarea" :disabled="mode === 'detail'" v-model="form.url" clearable></el-input>
        </el-form-item>

        <el-form-item label="用户名：" prop="userName">
          <el-input :disabled="mode === 'detail'" v-model="form.userName" clearable></el-input>
        </el-form-item>

        <el-form-item label="密码：" prop="encryptPassWord">
          <el-input :disabled="mode === 'detail'" show-password v-model="form.encryptPassWord" clearable></el-input>
        </el-form-item>

        <el-form-item v-if="mode !== 'add'" label="创建时间：" prop="createDate">
          <el-input disabled v-model="form.createDate" clearable></el-input>
        </el-form-item>

        <el-form-item v-if="mode !== 'add'" label="状态：" prop="state">
          <el-select disabled v-model.number="form.state">
            <el-option label="上线" :value="2"></el-option>
            <el-option label="下线" :value="1"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button v-if="mode !== 'detail'" type="success" :loading="loading" @click="connectionTest">
          连接测试
        </el-button>
        <el-button type="primary" :loading="loading" @click="submit">
          确认
        </el-button>
        <el-button v-if="mode === 'add'" type="info" :loading="loading" @click="init">
          重置
        </el-button>
        <el-button @click="cancel">取消</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import * as commMethod from '@/comm/commMethod.js'
import { getCurrentInstance } from 'vue'

export default {
  name: 'DataBaseDetail',
  setup() {
    const instance = getCurrentInstance()
    return {
      bus: instance?.appContext.config.globalProperties.$bus
    }
  },
  computed: {
    localVisible: {
      get() {
        return this.dataBaseDetailVisible
      },
      set(value) {
        this.updateVisible(value)
      }
    }
  },
  props: {
    dataBaseDetailVisible: {
      type: Boolean,
      default: false
    },
    mode: {
      type: String,
      default: 'add'
    },
    dataBaseInfo: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      title: '新增',
      loading: false,
      form: {
        dataBaseId: '',
        dataBaseType: 2,
        dataBaseName: '',
        url: '',
        userName: '',
        encryptPassWord: '',
        createDate: '',
        state: 2,
        stateDate: '',
        showPassWord: '',
        passWord: '',
        tableKeyNotSupported: 0
      },
      rules: {
        dataBaseType: [
          {required: true, message: '请选择数据源类型', trigger: 'blur'}
        ],
        dataBaseName: [
          {required: true, message: '请输入数据库名称', trigger: 'blur'},
          {min: 1, max: 32, message: '数据源名称最大长度32个字符', trigger: 'blur'}
        ],
        url: [
          {required: true, message: '请输入url', trigger: 'blur'}
        ],
        userName: [
          {required: true, message: '请输入用户名', trigger: 'blur'}
        ],
        encryptPassWord: [
          {required: true, message: '请输入密码', trigger: 'blur'}
        ]
      }
    }
  },
  mounted() {
    this.updateTitle()
    if (this.mode !== 'add') {
      this.initDataBaseInfo()
    }
  },
  watch: {
    mode() {
      this.updateTitle()
      if (this.mode !== 'add') {
        this.initDataBaseInfo()
      }
    },
    dataBaseInfo: {
      handler() {
        if (this.mode !== 'add' && this.dataBaseDetailVisible) {
          this.initDataBaseInfo()
        }
      },
      deep: true
    },
    dataBaseDetailVisible(newVal) {
      if (newVal) {
        this.updateTitle()
        if (this.mode !== 'add') {
          this.initDataBaseInfo()
        } else {
          this.init()
        }
      }
    }
  },
  methods: {
    updateTitle() {
      console.log("----------------------mode="+this.mode)
      if (this.mode === 'add') {
        this.title = '新增数据库'
      } else if (this.mode === 'detail') {
        this.title = '数据库详情'
      } else if (this.mode === 'modify') {
        this.title = '修改数据库'
      }
    },
    initDataBaseInfo() {
      if (this.dataBaseInfo && Object.keys(this.dataBaseInfo).length > 0) {
        let tableKeyValue = this.dataBaseInfo.tableKeyNotSupported;
        if (tableKeyValue === '1') {
          tableKeyValue = 1;
        } else if (tableKeyValue === '0' || !tableKeyValue) {
          tableKeyValue = 0;
        } else {
          tableKeyValue = parseInt(tableKeyValue) || 0;
        }

        this.form = {
          dataBaseId: this.dataBaseInfo.dataBaseId || '',
          dataBaseType: this.dataBaseInfo.dataBaseType || 2,
          dataBaseName: this.dataBaseInfo.dataBaseName || '',
          url: this.dataBaseInfo.url || '',
          userName: this.dataBaseInfo.userName || '',
          encryptPassWord: this.dataBaseInfo.encryptPassWord || '',
          createDate: this.dataBaseInfo.createDate || '',
          state: this.dataBaseInfo.state || 2,
          stateDate: this.dataBaseInfo.stateDate || '',
          showPassWord: this.dataBaseInfo.showPassWord || '',
          passWord: this.dataBaseInfo.passWord || '',
          tableKeyNotSupported: tableKeyValue
        }
      }
    },
    connectionTest() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          let request = {
            dataBaseType: this.form.dataBaseType,
            dataBaseName: this.form.dataBaseName,
            url: this.form.url,
            userName: this.form.userName
          }
          if (this.form.encryptPassWord !== this.form.showPassWord) {
            request.passWord = commMethod.encryptByAES(this.form.encryptPassWord, constant.ENCRYPT_KEY)
          } else {
            request.passWord = this.form.passWord
          }
          this.loading = true
          http(constant.TEST_DATA_BASE, 'post', request).then(res => {
            this.loading = false
            if (res.errorCode !== '0') {
              this.$message.error(`连接失败：${res.errorMsg}`)
              return
            }
            this.$message.success(`连接成功！`)
          }).catch(err => {
            this.loading = false
            this.$message.error(`连接失败：${err}`)
          })
        }
      })
    },
    submit() {
      if (this.mode === 'add') {
        this.addDataBase()
      } else if (this.mode === 'detail') {
        this.cancel()
      } else if (this.mode === 'modify') {
        this.modifyDataBase()
      }
    },
    addDataBase() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          let request = {
            dataBaseType: this.form.dataBaseType,
            dataBaseName: this.form.dataBaseName,
            url: this.form.url,
            userName: this.form.userName,
            passWord: commMethod.encryptByAES(this.form.encryptPassWord, constant.ENCRYPT_KEY),
            tableKeyNotSupported: this.form.tableKeyNotSupported
          }
          this.loading = true
          http(constant.ADD_DATA_BASE, 'post', request).then(res => {
            this.loading = false
            if (res.errorCode !== '0') {
              this.$message.error(`新增数据源失败：${res.errorMsg}`)
              return
            }
            this.$message.success({
              message: `新增数据源成功！`,
              duration: 1000,
              onClose: () => {
                if (this.bus) {
                  this.bus.$emit('refreshDataSourceList', false)
                }
              }
            })
          }).catch(err => {
            this.loading = false
            this.$message.error(`新增数据源失败：${err}`)
          })
        }
      })
    },
    modifyDataBase() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          let request = {
            dataBaseId: this.form.dataBaseId,
            dataBaseType: this.form.dataBaseType,
            dataBaseName: this.form.dataBaseName,
            url: this.form.url,
            userName: this.form.userName,
            tableKeyNotSupported: this.form.tableKeyNotSupported
          }
          if (this.form.encryptPassWord !== this.form.showPassWord) {
            request.passWord = commMethod.encryptByAES(this.form.encryptPassWord, constant.ENCRYPT_KEY)
          } else {
            request.passWord = this.form.passWord
          }
          this.loading = true
          http(constant.MODIFY_DATA_BASE, 'post', request).then(res => {
            this.loading = false
            if (res.errorCode !== '0') {
              this.$message.error(`修改数据源（${this.form.dataBaseId}）失败：${res.errorMsg}`)
              return
            }
            this.$message.success({
              message: `修改数据源（${this.form.dataBaseId}）成功！`,
              duration: 1000,
              onClose: () => {
                if (this.bus) {
                  this.bus.$emit('refreshDataSourceList', false)
                }
              }
            })
          }).catch(err => {
            this.loading = false
            this.$message.error(`修改数据源（${this.form.dataBaseId}）失败：${err}`)
          })
        }
      })
    },
    init() {
      this.form = {
        dataBaseId: '',
        dataBaseType: 2,
        dataBaseName: '',
        url: '',
        userName: '',
        encryptPassWord: '',
        createDate: '',
        state: 2,
        stateDate: '',
        showPassWord: '',
        passWord: '',
        tableKeyNotSupported: 0
      }
    },
    cancel() {
      this.updateVisible(false)
    },
    updateVisible(visible) {
      if (this.bus) {
        this.bus.$emit('changeDataBaseDetailVisible', visible)
      }
    },
    handleClose() {
      this.cancel()
    }
  }
}
</script>

<style>
.dialog-footer {
  text-align: center;
}

/* 响应式对话框宽度 */
.data-source-dialog .el-dialog {
  max-width: 800px;
  min-width: 600px;
}

/* 在小屏幕上调整宽度 */
@media (max-width: 1200px) {
  .data-source-dialog .el-dialog {
    width: 70% !important;
    max-width: 700px;
  }
}

@media (max-width: 768px) {
  .data-source-dialog .el-dialog {
    width: 90% !important;
    max-width: 500px;
    min-width: 400px;
  }
}

/* 表单项间距优化 */
.data-source-dialog .el-form-item {
  margin-bottom: 20px;
}

/* 文本域高度优化 */
.data-source-dialog .el-textarea .el-textarea__inner {
  min-height: 80px;
}
</style>
