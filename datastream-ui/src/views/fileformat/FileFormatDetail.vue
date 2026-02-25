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
    width="900px"
    :before-close="handleClose"
    append-to-body
    custom-class="file-config-dialog-custom"
    class="file-config-dialog">

    <!-- 基本信息 -->
    <div class="section-card">
      <div class="section-header">
        <span class="section-title">基本信息</span>
      </div>
      <el-form ref="fileFormatForm" :model="fileFormatForm" :rules="rules" label-width="100px" autocomplete="off">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="文件ID" prop="fileFormatId">
              <el-input v-model="fileFormatForm.fileFormatId" disabled class="readonly-input"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="配置名称" prop="fileNameFormat">
              <el-input v-model="fileFormatForm.fileNameFormat" placeholder="请输入配置名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="文件类型" prop="fileType">
              <el-select v-model="fileFormatForm.fileType" placeholder="请选择" style="width: 100%">
                <el-option label="Text" value="8"/>
                <el-option label="Excel" value="9"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="文件目录" prop="localPath">
              <el-input v-model="fileFormatForm.localPath" placeholder="请输入文件目录"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备份操作" prop="fileBakAction">
              <el-select v-model="fileFormatForm.fileBakAction" placeholder="请选择" style="width: 100%" @change="handleBakActionChange">
                <el-option label="不处理" value="1"/>
                <el-option label="直接删除" value="2"/>
                <el-option label="备份目录" value="3"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="备份目录" prop="fileBakPath">
              <el-input v-model="fileFormatForm.fileBakPath" placeholder="请输入备份目录"/>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-show="false">
            <el-form-item label="配置类型" prop="fileNameType">
              <el-select v-model="fileFormatForm.fileNameType" placeholder="请选择">
                <el-option label="固定值" value="1"/>
                <el-option label="正则表达" value="2"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- ftp配置 -->
    <div class="section-card">
      <div class="section-header">
        <span class="section-title">ftp配置</span>
        <el-popover
          placement="bottom-end"
          :width="320"
          trigger="click">
          <template #reference>
            <el-icon class="help-icon-btn">
              <InfoFilled />
            </el-icon>
          </template>
          <div class="ftp-help-popover-content">
            <el-icon class="ftp-help-icon"><InfoFilled /></el-icon>
            <p class="ftp-help-text">配置FTP后，当文件作为数据源时会先从FTP服务器下载到本地</p>
          </div>
        </el-popover>
      </div>
      <el-form ref="ftpFormRef" :model="fileFormatForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="ftp类型">
              <el-select v-model="fileFormatForm.ftpType" placeholder="请选择" style="width: 100%">
                <el-option label="ftp" value="1"/>
                <el-option label="Sftp" value="2"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="ftp地址">
              <el-input v-model="fileFormatForm.ftpHost" placeholder="请输入IP地址"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="ftp端口">
              <el-input v-model.number="fileFormatForm.ftpPort" placeholder="请输入端口"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="ftp账号">
              <el-input v-model="fileFormatForm.ftpUser" placeholder="请输入账号" autocomplete="new-password"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="ftp密码">
              <el-input v-model="fileFormatForm.ftpPasswd" show-password placeholder="请输入密码" autocomplete="new-password"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="ftp路径">
              <el-input v-model="fileFormatForm.ftpPath" placeholder="请输入路径"/>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 数据行配置 -->
    <div class="section-card">
      <div class="section-header">
        <span class="section-title">数据行配置</span>
      </div>
      <el-row :gutter="16" class="config-row">
        <el-col :span="6">
          <div class="field-label">行属性ID</div>
          <el-input v-model="fileFormatForm.fileBody.fileBodyId" disabled class="readonly-input"/>
        </el-col>
        <el-col :span="6" v-if="fileFormatForm.fileType === '8'">
          <div class="field-label">分隔符</div>
          <el-select v-model="fileFormatForm.fileBody.splitFlag" placeholder="请选择" style="width: 100%">
            <el-option label="固定长度" value="1"/>
            <el-option label="竖线|" value="2"/>
            <el-option label="逗号," value="3"/>
            <el-option label="与号&" value="4"/>
          </el-select>
        </el-col>
        <el-col :span="6">
          <div class="field-label">固定行开始</div>
          <el-input-number v-model="fileFormatForm.fileBody.fixBeginLine" :min="1" :max="100" controls-position="right" style="width: 100%"/>
        </el-col>
        <el-col :span="6" class="text-right">
          <el-button v-if="mode !== 'detail'" type="primary" @click="addBodyFieldRow">
            <el-icon><Plus /></el-icon>
            新增字段
          </el-button>
        </el-col>
      </el-row>

      <!-- 有字段数据时显示表格 -->
      <transition name="fade-slide" mode="out-in">
        <div v-if="fileFormatForm.fileBody.fileFieldList.length > 0" key="table" class="table-container">
          <el-table :data="fileFormatForm.fileBody.fileFieldList" size="small" stripe>
            <el-table-column prop="fileFieldId" label="字段ID" width="80" align="center">
              <template #default="{ row }">
                <el-input v-model="row.fileFieldId" disabled class="readonly-input"/>
              </template>
            </el-table-column>
            <el-table-column label="字段名称" align="center">
              <template #default="{ row, $index }">
                <el-form-item :prop="'fileFormatForm.fileBody.fileFieldList.' + $index + '.fieldName'"
                              style="margin-bottom:0">
                  <el-input v-model="row.fieldName" placeholder="请输入字段名称"/>
                </el-form-item>
              </template>
            </el-table-column>
            <el-table-column v-if="fileFormatForm.fileBody.splitFlag === '1' && fileFormatForm.fileType === '8'"
                             label="固定长度" width="100" align="center">
              <template #default="{ row }">
                <el-input v-model="row.fixWidth" placeholder="长度"/>
              </template>
            </el-table-column>
            <el-table-column label="字段占位" width="120" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.position" :min="1" :max="100" controls-position="right" style="width: 100%"/>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ $index }">
                <el-button v-if="mode !== 'detail'" size="small" type="danger" link @click="removeBodyFieldRow($index)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <!-- 无字段数据时显示紧凑提示 -->
        <div v-else key="empty" class="empty-state">
          <el-icon class="empty-icon"><Files /></el-icon>
          <span class="empty-text">暂无字段配置，点击"新增字段"按钮添加</span>
        </div>
      </transition>
    </div>

    <!-- 特殊行配置（可多个） -->
    <div class="section-card">
      <div class="section-header">
        <span class="section-title">特殊行配置</span>
        <el-button v-if="mode !== 'detail'" type="primary" size="small" @click="addSpecialBlock">
          <el-icon><Plus /></el-icon>
          新增属性
        </el-button>
      </div>

      <div v-if="fileFormatForm.fileSpecialList.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Files /></el-icon>
        <span class="empty-text">暂无特殊行配置，点击"新增属性"按钮添加</span>
      </div>

      <div v-for="(fileSpecialIterator, idx) in fileFormatForm.fileSpecialList" :key="fileSpecialIterator.uid"
           class="special-block">
        <div class="special-block-header">
          <span class="special-block-title">特殊行属性 {{ idx + 1 }}</span>
          <el-button v-if="mode !== 'detail'" type="danger" size="small" link @click="removeSpecialBlock(idx)">
            删除属性
          </el-button>
        </div>

        <el-row :gutter="16" class="config-row inline-row">
          <el-col :span="4">
            <div class="field-label">行属性ID</div>
            <el-input v-model="fileSpecialIterator.fileSpecialId" disabled class="readonly-input compact-input"/>
          </el-col>
          <el-col :span="3" v-if="fileFormatForm.fileType === '8'">
            <div class="field-label">分隔符</div>
            <el-select v-model="fileSpecialIterator.splitFlag" class="compact-select">
              <el-option label="固定长度" value="1"/>
              <el-option label="竖线|" value="2"/>
              <el-option label="逗号," value="3"/>
              <el-option label="与号&" value="4"/>
            </el-select>
          </el-col>
          <el-col :span="5">
            <div class="field-label">读取位置</div>
            <el-radio-group v-model="fileSpecialIterator.linePositionMode" @change="(val) => handleLinePositionModeChange(fileSpecialIterator, val)" class="position-radio-group">
              <el-radio label="fixed">固定行号</el-radio>
              <el-radio label="end">文件末行</el-radio>
            </el-radio-group>
          </el-col>
          <el-col :span="5">
            <div class="field-label">行号</div>
            <el-input-number
              v-model="fileSpecialIterator.fixLinePosition"
              :max="100"
              :disabled="fileSpecialIterator.linePositionMode === 'end'"
              controls-position="right"
              placeholder="请输入行号"
              class="compact-input-number"/>
          </el-col>
          <el-col :span="7">
            <div class="field-label">备注</div>
            <el-input v-model="fileSpecialIterator.remark" placeholder="请输入备注" class="compact-input"/>
          </el-col>
        </el-row>

        <div class="config-row">
          <el-button v-if="mode !== 'detail'" type="primary" size="small" @click="addSpecialFieldRow(idx)">
            <el-icon><Plus /></el-icon>
            新增字段
          </el-button>
        </div>

        <!-- 有字段数据时显示表格 -->
        <transition name="fade-slide" mode="out-in">
          <div v-if="fileSpecialIterator.fileFieldList.length > 0" key="table" class="table-container">
            <el-table :data="fileSpecialIterator.fileFieldList" size="small" stripe>
              <el-table-column label="字段ID" width="80" align="center">
                <template #default="{ row }">
                  <el-input v-model="row.fileFieldId" disabled class="readonly-input"/>
                </template>
              </el-table-column>
              <el-table-column label="字段名称" width="150" align="center">
                <template #default="{ row }">
                  <el-input v-model="row.fieldName" placeholder="请输入"/>
                </template>
              </el-table-column>
              <el-table-column v-if="fileSpecialIterator.splitFlag === '1' && fileFormatForm.fileType === '8'"
                               label="固定长度" width="100" align="center">
                <template #default="{ row }">
                  <el-input v-model="row.fixLinePosition" placeholder="长度"/>
                </template>
              </el-table-column>
              <el-table-column label="字段占位" width="120" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.position" :min="1" :max="100" controls-position="right" style="width: 100%"/>
                </template>
              </el-table-column>
              <el-table-column label="汇总方式" width="200" align="center">
                <template #default="{ row }">
                  <el-radio-group v-model="row.sumLineFlag">
                    <el-radio :label="1">数据行数</el-radio>
                    <el-radio :label="2">数据汇总</el-radio>
                  </el-radio-group>
                </template>
              </el-table-column>
              <el-table-column label="汇总字段" width="200" align="center">
                <template #default="{ row }">
                  <el-select
                    v-model="row.sumFieldName"
                    placeholder="请选择或输入"
                    filterable
                    allow-create
                    :disabled="row.sumLineFlag !== 2">
                    <el-option
                      v-for="field in fileFormatForm.fileBody.fileFieldList"
                      :key="field.fieldName"
                      :label="field.fieldName"
                      :value="field.fieldName"/>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center">
                <template #default="{ $index }">
                  <el-button size="small" type="danger" link @click="removeSpecialFieldRow(idx, $index)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <!-- 无字段数据时显示紧凑提示 -->
          <div v-else key="empty" class="empty-state compact">
            <el-icon class="empty-icon"><Files /></el-icon>
            <span class="empty-text">暂无字段配置，点击"新增字段"按钮添加</span>
          </div>
        </transition>

        <el-divider v-if="idx < fileFormatForm.fileSpecialList.length - 1"/>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="mode === 'add'" type="primary" :loading="loading" @click="saveFileFormat">保存</el-button>
        <el-button v-if="mode === 'modify'" type="primary" :loading="loading" @click="modifyFileFormat">保存</el-button>
        <el-button v-if="mode !== 'detail'" @click="resetForm">重置</el-button>
        <el-button v-if="mode !== 'detail'" type="success" :loading="loading" @click="ftpProbe">ftp调测</el-button>
        <el-button @click="cancel">关闭</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- ftp文件列表弹窗 -->
  <el-dialog
    v-model="showFtpFileDialog"
    title="ftp目录文件列表"
    width="500px"
    :append-to-body="true">
    <div v-if="ftpFileList.length === 0" class="empty-state">
      <el-icon class="empty-icon"><FolderOpened /></el-icon>
      <span class="empty-text">ftp目录下暂无文件</span>
    </div>
    <el-table v-else :data="ftpFileList" max-height="400" stripe>
      <el-table-column type="index" label="序号" width="60" align="center"/>
      <el-table-column label="文件名">
        <template #default="{ row }">
          {{ typeof row === 'string' ? row : row }}
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button type="primary" @click="showFtpFileDialog = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script>
import {getCurrentInstance} from 'vue'
import {Plus, Files, FolderOpened, InfoFilled} from '@element-plus/icons-vue'
import constant from "@/comm/constants.js";
import http from "@/utils/request.js";
import {operateFileRows} from "@/api/file.js";

export default {
  name: 'FileFormatDetail',
  components: {
    Plus,
    Files,
    FolderOpened,
    InfoFilled
  },
  setup() {
    const instance = getCurrentInstance()
    return {
      bus: instance?.appContext.config.globalProperties.$bus
    }
  },
  computed: {
    localVisible: {
      get() {
        return this.fileDetailVisible
      },
      set(value) {
        this.updateVisible(value)
      }
    }
  },
  props: {
    fileDetailVisible: {type: Boolean, default: false},
    mode: {type: String, default: 'add'},
    fileFormatDetail: {type: Object, default: () => ({})}
  },
  data() {
    return {
      title: '文件配置',
      loading: false,
      showFtpFileDialog: false,
      ftpFileList: [],
      fileFormatForm: {
        fileFormatId: '',
        fileType: '8',
        fileNameType: '1',
        fileNameFormat: '',
        localPath: '',
        fileBakAction: '1',
        fileBakPath: '',
        ftpType: '',
        ftpHost: '',
        ftpPort: null,
        ftpUser: '',
        ftpPasswd: '',
        ftpPath: '',
        fileBody: {
          fileBodyId: '',
          splitFlag: '2',
          fixBeginLine: '',
          fileFieldList: []
        },
        fileSpecialList: []
      },
      rules: {
        fileNameFormat: [{required: true, message: '请输入配置名称', trigger: 'blur'}],
        fileType: [{required: true, message: '请选择文件类型', trigger: 'change'}],
        localPath: [{required: true, message: '请输入文件目录', trigger: 'blur'}],
        fileBakPath: [
          {
            validator: (rule, value, callback) => {
              if (this.fileFormatForm.fileBakAction === '3' && !value) {
                callback(new Error('备份操作选择备份目录时，备份目录不能为空'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      }
    }
  },
  mounted() {
    this.updateTitle()
    this.syncFileFormatFromConfig()
  },
  methods: {
    updateTitle() {
      if (this.mode === 'add') {
        this.title = '新增文件配置'
      } else if (this.mode === 'detail') {
        this.title = '文件配置详情'
      } else if (this.mode === 'modify') {
        this.title = '修改文件配置'
      }
    },
    handleBakActionChange() {
      // 当备份操作改变时，如果选择了"备份目录"，触发备份目录的验证
      if (this.fileFormatForm.fileBakAction === '3') {
        this.$nextTick(() => {
          this.$refs.fileFormatForm.validateField('fileBakPath')
        })
      }
    },
    syncFileFormatFromConfig() {
      if (!this.fileFormatDetail || Object.keys(this.fileFormatDetail).length === 0) return
      this.fileFormatForm = Object.assign({}, this.fileFormatForm, this.fileFormatDetail)

      if (this.fileFormatForm.fileType !== undefined && this.fileFormatForm.fileType !== null) {
        this.fileFormatForm.fileType = String(this.fileFormatForm.fileType)
      }
      if (this.fileFormatForm.fileNameType !== undefined && this.fileFormatForm.fileNameType !== null) {
        this.fileFormatForm.fileNameType = String(this.fileFormatForm.fileNameType)
      }
      if (this.fileFormatForm.fileBakAction !== undefined && this.fileFormatForm.fileBakAction !== null) {
        this.fileFormatForm.fileBakAction = String(this.fileFormatForm.fileBakAction)
      }
      if (this.fileFormatForm.ftpType !== undefined && this.fileFormatForm.ftpType !== null) {
        this.fileFormatForm.ftpType = String(this.fileFormatForm.ftpType)
      }

      if (this.fileFormatForm.fileBody && this.fileFormatForm.fileBody.splitFlag !== undefined && this.fileFormatForm.fileBody.splitFlag !== null) {
        this.fileFormatForm.fileBody.splitFlag = String(this.fileFormatForm.fileBody.splitFlag)
      }

      if (Array.isArray(this.fileFormatForm.fileBody.fileFieldList) && this.fileFormatForm.fileBody.fileFieldList.length > 0) {
        let cur = 0
        this.fileFormatForm.fileBody.fileFieldList = this.fileFormatForm.fileBody.fileFieldList.map(f => {
          let pos = parseInt(f.position)
          if (!pos || pos <= 0) {
            cur = cur + 1
            return {...f, position: cur}
          } else {
            cur = Math.max(cur, pos)
            return {...f, position: pos}
          }
        })
      } else {
        this.fileFormatForm.fileBody.fileFieldList = []
      }

      if (Array.isArray(this.fileFormatForm.fileSpecialList)) {
        this.fileFormatForm.fileSpecialList = this.fileFormatForm.fileSpecialList.map(special => {
          if (special.splitFlag !== undefined && special.splitFlag !== null) {
            special.splitFlag = String(special.splitFlag)
          }
          if (special.linePositionMode === undefined) {
            special.linePositionMode = 'fixed'
          }
          if (special.fixLinePosition === -1) {
            special.linePositionMode = 'end'
          }
          if (Array.isArray(special.fileFieldList)) {
            special.fileFieldList = special.fileFieldList.map(field => {
              if (field.sumLineFlag !== undefined && field.sumLineFlag !== null) {
                field.sumLineFlag = Number(field.sumLineFlag)
              }
              if (field.sumFieldFlag !== undefined && field.sumFieldFlag !== null) {
                field.sumFieldFlag = Number(field.sumFieldFlag)
              }
              return field
            })
          }
          return special
        })
      } else {
        this.fileFormatForm.fileSpecialList = []
      }
    },
    getNextPos(list) {
      if (!Array.isArray(list) || list.length === 0) return 1
      let maxPos = 0
      list.forEach(it => {
        const p = parseInt(it.position)
        if (!isNaN(p)) maxPos = Math.max(maxPos, p)
      })
      return maxPos + 1
    },
    addBodyFieldRow() {
      const next = this.getNextPos(this.fileFormatForm.fileBody.fileFieldList)
      this.fileFormatForm.fileBody.fileFieldList.push({fileFieldId: '', fieldName: '', position: next})
    },
    removeBodyFieldRow(index) {
      this.fileFormatForm.fileBody.fileFieldList.splice(index, 1)
    },
    addSpecialBlock() {
      this.fileFormatForm.fileSpecialList.push({
        uid: Date.now() + Math.random().toString(16).slice(2),
        fileSpecialId: '',
        splitFlag: '2',
        linePositionMode: 'fixed',
        fixLinePosition: '',
        remark: '',
        fileFieldList: []
      })
    },
    handleLinePositionModeChange(row, mode) {
      // 固定行号模式：fixLinePosition 为具体的行号
      // 文件末行模式：fixLinePosition 固定为 -1（从文件末尾读取的标识）
      if (mode === 'end') {
        // 选择文件末行时，fixLinePosition 固定为 -1
        row.fixLinePosition = -1
      } else {
        // 选择固定行号时，清空 -1（允许用户输入行号）
        if (row.fixLinePosition === -1) {
          row.fixLinePosition = ''
        }
      }
      // 强制更新 Vue 响应式，确保输入框禁用状态立即生效
      this.$nextTick(() => {
        this.$forceUpdate()
      })
    },
    removeSpecialBlock(index) {
      this.fileFormatForm.fileSpecialList.splice(index, 1)
    },
    addSpecialFieldRow(blockIndex) {
      const fileSpecialIterator = this.fileFormatForm.fileSpecialList[blockIndex]
      const next = this.getNextPos(fileSpecialIterator.fileFieldList)
      fileSpecialIterator.fileFieldList.push({
        fileFieldId: '',
        fieldName: '',
        position: next,
        sumLineFlag: 1,
        sumFieldFlag: 0,
        sumFieldName: ''
      })
    },
    removeSpecialFieldRow(blockIndex, rowIndex) {
      const fileFieldList = this.fileFormatForm.fileSpecialList[blockIndex].fileFieldList
      fileFieldList.splice(rowIndex, 1)
    },
    validateFieldPositions() {
      // 校验数据行配置中字段占位是否有重复
      const positions = this.fileFormatForm.fileBody.fileFieldList.map(f => parseInt(f.position))
      const validPositions = positions.filter(p => !isNaN(p))
      const uniquePositions = new Set(validPositions)

      if (validPositions.length !== uniquePositions.size) {
        // 找出重复的占位值
        const duplicates = validPositions.filter((p, index) => validPositions.indexOf(p) !== index)
        const duplicateValues = [...new Set(duplicates)]
        this.$message.error(`数据行配置中字段占位存在重复值：${duplicateValues.join('、')}`)
        return false
      }
      return true
    },
    validateFieldNames() {
      // 校验字段名称是否重复
      const allFieldNames = []

      // 收集数据行配置中的字段名称
      const bodyFieldNames = this.fileFormatForm.fileBody.fileFieldList.map(f => f.fieldName?.trim()).filter(n => n)
      allFieldNames.push(...bodyFieldNames)

      // 收集特殊行配置中的字段名称
      const specialFieldNames = []
      this.fileFormatForm.fileSpecialList.forEach(special => {
        special.fileFieldList.forEach(field => {
          const name = field.fieldName?.trim()
          if (name) {
            specialFieldNames.push(name)
            allFieldNames.push(name)
          }
        })
      })

      // 检查是否有重复的字段名称
      const uniqueNames = new Set(allFieldNames)
      if (allFieldNames.length !== uniqueNames.size) {
        // 找出重复的字段名称
        const duplicates = allFieldNames.filter((name, index) => allFieldNames.indexOf(name) !== index)
        const duplicateValues = [...new Set(duplicates)]
        this.$message.error(`字段名称存在重复：${duplicateValues.join('、')}`)
        return false
      }

      // 检查数据行配置内部是否有重复
      const uniqueBodyNames = new Set(bodyFieldNames)
      if (bodyFieldNames.length !== uniqueBodyNames.size) {
        const duplicates = bodyFieldNames.filter((name, index) => bodyFieldNames.indexOf(name) !== index)
        const duplicateValues = [...new Set(duplicates)]
        this.$message.error(`数据行配置中字段名称存在重复：${duplicateValues.join('、')}`)
        return false
      }

      return true
    },
    saveFileFormat() {
      this.$refs.fileFormatForm.validate(valid => {
        if (!valid) return

        // 校验字段名称重复
        if (!this.validateFieldNames()) return

        // 校验字段占位重复
        if (!this.validateFieldPositions()) return

        let request = {
          fileFormat: this.fileFormatForm
        }
        this.loading = true
        http(constant.ADD_FILE_FORMAT, 'post', request).then(res => {
          this.loading = false
          if (res.errorCode !== '0') {
            this.$message.error(`新增失败：${res.errorMsg}`)
            return
          }
          this.$message.success({
            message: `新增成功！`,
            duration: 1000
          })
          this.updateVisible(false)
        }).catch(err => {
          this.loading = false
          this.$message.error(`新增失败：${err}`)
        })
      })
    },
    modifyFileFormat() {
      this.$refs.fileFormatForm.validate(valid => {
        if (!valid) return

        // 校验字段名称重复
        if (!this.validateFieldNames()) return

        // 校验字段占位重复
        if (!this.validateFieldPositions()) return

        let request = {
          fileFormat: this.fileFormatForm
        }
        this.loading = true
        http(constant.MODIFY_FILE_FORMAT, 'post', request).then(res => {
          this.loading = false
          if (res.errorCode !== '0') {
            this.$message.error(`修改失败：${res.errorMsg}`)
            return
          }
          this.$message.success({
            message: `修改成功！`,
            duration: 1000
          })
          this.updateVisible(false)
        }).catch(err => {
          this.loading = false
          this.$message.error(`修改失败：${err}`)
        })
      })
    },
    resetForm() {
      this.fileFormatForm = {
        fileFormatId: '', fileType: '8', fileNameType: '1', fileNameFormat: '',
        localPath: '', fileBakAction: '1', fileBakPath: '', ftpType: '', ftpHost: '', ftpPort: null,
        ftpUser: '', ftpPasswd: '', ftpPath: '',
        fileBody: {
          fileBodyId: '',
          splitFlag: '2',
          fixBeginLine: '',
          fileFieldList: []
        },
        fileSpecialList: []
      }
    },
    ftpProbe() {
      if (!this.fileFormatForm.ftpType) {
        this.$message.warning('请先选择ftp类型')
        return
      }
      if (!this.fileFormatForm.ftpHost) {
        this.$message.warning('请先填写ftp地址')
        return
      }
      const ipPattern = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
      if (!ipPattern.test(this.fileFormatForm.ftpHost)) {
        this.$message.warning('ftp地址格式不正确，请输入有效的IP地址（如：192.168.1.1）')
        return
      }
      if (!this.fileFormatForm.ftpPort) {
        this.$message.warning('请先填写ftp端口')
        return
      }
      if (!this.fileFormatForm.ftpUser) {
        this.$message.warning('请先填写ftp账号')
        return
      }
      if (!this.fileFormatForm.ftpPasswd) {
        this.$message.warning('请先填写ftp密码')
        return
      }
      if (!this.fileFormatForm.ftpPath) {
        this.$message.warning('请先填写ftp路径')
        return
      }

      this.loading = true
      const params = {
        fileFormat: this.fileFormatForm,
        action: 6
      }

      operateFileRows(params).then(res => {
        this.loading = false
        if (res.errorCode !== '0') {
          this.$message.error(`ftp测试失败：${res.errorMsg}`)
          return
        }
        const fileNameList = res.fileNameList || []
        if (!fileNameList || fileNameList.length === 0) {
          this.$message.warning('ftp目录下文件为空')
        } else {
          this.ftpFileList = fileNameList
          this.showFtpFileDialog = true
        }
      }).catch(err => {
        this.loading = false
        this.$message.error(`ftp测试失败：${err}`)
      })
    },
    cancel() {
      this.updateVisible(false)
    },
    updateVisible(visible) {
      if (this.bus) this.bus.$emit('changeFileFormatDetailVisible', visible)
    },
    handleClose() {
      this.cancel()
    }
  }
}
</script>

<style>
/* 弹窗全局样式 - custom-class 需要应用到全局 DOM */
.file-config-dialog-custom {
  margin-top: 5vh !important;
}

/* 使用更高的优先级确保滚动样式生效 */
.file-config-dialog-custom .el-dialog__body,
body .file-config-dialog-custom .el-dialog__body {
  max-height: calc(100vh - 200px) !important;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  padding-right: 12px !important;
}

/* 确保内容区域也可以滚动 */
.file-config-dialog-custom .el-dialog__body > * {
  max-height: none !important;
}

.file-config-dialog-custom .el-dialog__body::-webkit-scrollbar {
  width: 6px;
}

.file-config-dialog-custom .el-dialog__body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.file-config-dialog-custom .el-dialog__body::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.file-config-dialog-custom .el-dialog__body::-webkit-scrollbar-thumb:hover {
  background: #909399;
}
</style>

<style scoped>
.section-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.config-row {
  margin-bottom: 12px;
}

/* 内联行布局：所有字段在同一行显示 */
.inline-row {
  align-items: center;
}

.inline-row .field-label {
  margin-bottom: 6px;
  white-space: nowrap;
}

.inline-row .el-input,
.inline-row .el-select,
.inline-row .el-input-number,
.inline-row .el-radio-group {
  width: 100%;
}

.config-row:last-child {
  margin-bottom: 0;
}

.field-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

/* 紧凑输入框 - 缩小高度 */
.compact-input :deep(.el-input__wrapper) {
  padding: 0 11px;
}

.compact-input :deep(.el-input__inner) {
  height: 30px;
}

.compact-select :deep(.el-input__wrapper) {
  padding: 0 11px;
}

.compact-select :deep(.el-select__wrapper) {
  padding: 0 11px;
}

.compact-input-number :deep(.el-input__wrapper) {
  padding: 0 11px;
}

.compact-input-number :deep(.el-input__inner) {
  height: 30px;
}

/* 位置单选组 - 紧凑布局 */
.position-radio-group :deep(.el-radio) {
  margin-right: 8px;
}

.table-container {
  margin-top: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  margin-top: 16px;
}

.empty-state.compact {
  padding: 20px 16px;
  margin-top: 12px;
}

.empty-icon {
  font-size: 32px;
  color: #c0c4cc;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 13px;
  color: #909399;
}

.special-block {
  background: #f9fafb;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 16px;
}

.special-block:last-child {
  margin-bottom: 0;
}

.special-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.special-block-title {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.readonly-input :deep(.el-input__wrapper) {
  background-color: #f5f7fa;
}

.text-right {
  text-align: right;
}

/* 过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease-out;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

/* FTP配置说明图标按钮 */
.help-icon-btn {
  font-size: 18px;
  color: #909399;
  cursor: pointer;
  transition: color 0.3s;
}

.help-icon-btn:hover {
  color: #409eff;
}

/* FTP配置说明Popover样式 */
.ftp-help-popover-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.ftp-help-icon {
  font-size: 20px;
  color: #409eff;
  flex-shrink: 0;
  margin-top: 2px;
}

.ftp-help-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  margin: 0;
}
</style>
