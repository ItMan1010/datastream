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
  <div :id="id" :class="['root-node-class', isCanvas ? 'node-canvas' : '']"  @mouseover="handleMouseOver($event)" @mouseout="handleMouseout($event)" :desc="'root'" @mousedown="handleMouseDown($event)" @dblclick="enableEdit" @contextmenu="handleRightClick($event)">
    <div class="root-node-header">
      <i class="el-icon-circle-plus-outline root-node-header-i"></i>
      <span>root</span>
      <i v-if="editType.includes(type)" class="el-icon-close root-node-close" @click="removeNode"></i>
    </div>
    <div class="root-node-content">
      <i class="el-icon-edit" @click="openEditModal"></i>
      <el-tooltip class="item" effect="dark" :content="form.tableName" placement="top">
        <span class="node-desc">{{ form.tableName }}</span>
      </el-tooltip>
    </div>
    <div v-if="addRootVisible" class="my-modal"></div>
    <div v-if="addRootVisible" class="my-dialog__wrapper node-create-form" :id="'dialog-' + id" style="z-index: 3013;" @click="closeModal($event)">
      <div class="my-dialog" style="width: 600px;">
        <div class="my-dialog__header">
          <span class="my-dialog__title">{{ isEditable ? '编辑' : '查看'}}root</span>
          <button type="button" aria-label="Close" class="el-dialog__headerbtn" @click="closeModal($event, true)">
            <i class="el-dialog__close el-icon el-icon-close"></i>
          </button>
        </div>
        <div class="my-dialog__body">
          <el-form ref="rootForm" :model="form" :rules="rules" label-width="200px">
<!--            <el-form-item label="选择数据源：" prop="dataSourceName" :required="false">-->
<!--              <el-input v-model="form.dataSourceName" :disabled="!isEditable" clearable style="width: 300px;" placeholder="请选择数据源">-->
<!--                <template #append>-->
<!--                  <el-button @click="selectDataSource" :disabled="!isEditable">-->
<!--                    <el-icon><Search /></el-icon>-->
<!--                  </el-button>-->
<!--                </template>-->
<!--              </el-input>-->
<!--            </el-form-item>-->
            <el-form-item label="输入表名称：" prop="tableName" :required="false">
              <el-input v-model="form.tableName" :disabled="!isEditable" clearable style="width: 300px;"></el-input>
            </el-form-item>
            <el-form-item label="输入表字段：" prop="fieldName" :required="false">
              <el-input v-model="form.fieldName" :disabled="!isEditable" clearable style="width: 300px;"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button v-if="isEditable" type="primary" v-loading.fullscreen.lock="loading" style="width: 80px;" @click="createRoot">确认</el-button>
              <el-button v-if="isEditable" type="info" v-loading.fullscreen.lock="loading" style="width: 80px;" @click="initForm">重置</el-button>
              <el-button v-if="isEditable" v-loading.fullscreen.lock="loading" style="width: 80px;" @click="cancel">取消</el-button>
              <el-button v-if="!isEditable" v-loading.fullscreen.lock="loading" style="width: 80px;" @click="closeModal($event, true)">关闭</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 数据源选择弹窗 -->
    <el-dialog
      title="数据源选择"
      v-model="dataSourceDialogVisible"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body>
      <data-source-select :unique-id="uniqueId" :data-source-types-filter="['database']"></data-source-select>
    </el-dialog>
  </div>
</template>
<script>

import { v4 as uuidv4 } from 'uuid'
import  * as jsPlumbFun from '../../utils/flowUtil.js'
import DataSourceSelect from './DataSourceSelect.vue'

export default {
  name: 'RootNode',
  components: {
    DataSourceSelect
  },
  props: {
    isCanvas: {
      type: Boolean,
      default: false
    },
    type: { // node view add modify
      type: String,
      default () {
        return 'node'
      }
    },
    idx: {
      type: Number,
      default () {
        return 0
      }
    },
    nodeInfo: {
      type: Object,
      default () {
        return {
          flowNodeId: null,
          flowDefineId: null,
          tableName: '',
          fieldName: '',
          top: 0,
          left: 0,
        }
      }
    },
  },
  data () {
    return {
      id: new Date().getTime() + '',
      editType: ['add', 'modify'],
      showType: ['view', 'modify'],
      disableType: ['view'],
      addType: ['add'],
      nodeType: ['node'],
      app: null,
      copyId: null,
      addRootVisible: false,
      form: {
        dataBaseName: '', // 数据源名称
        dataBaseId: null, // 数据源ID
        tableName: '', // 表名称
        fieldName: '', // 数据查询字段名称
      },
      loading: false,
      rules: {
        // dataSourceName: [
        //   { required: true, message: '请选择数据源', trigger: 'change' }
        // ],
        tableName: [
          { required: true, message: '请输入表名称', trigger: 'change' }
        ],
        fieldName: [
          { required: true, message: '请输入表字段', trigger: 'change' }
        ],
      },
      inst: null, // jsplumb实例
      endpoint: null, // 端点
      formCopy: null, // 表单数据备份
      editByDblClick: false,
      dataSourceDialogVisible: false, // 数据源选择弹窗
      uniqueId: 'rootNode', // 唯一标识
    }
  },
  computed: {
    isEditable() {
      return this.editByDblClick || this.editType.includes(this.type);
    },
    nodeWidth() {
      return this.isCanvas ? '150px' : '100px'
    }
  },
  watch: {
    addRootVisible: {
      handler (newValue, oldValue) {
        this.handleMouseout(null)
      }
    }
  },
  created() {
    // 使用传入的 idx 作为节点 ID（字符串类型，适配 DOM 和 jsPlumb）
    this.id = String(this.idx);
    this.app = $('#app').eq(0)
    if (this.showType.includes(this.type)) {
      this.form = {
        dataBaseName: this.nodeInfo.dataBaseName || '',
        dataBaseId: this.nodeInfo.dataBaseId || null,
        tableName: this.nodeInfo.tableName,
        fieldName: this.nodeInfo.fieldName,
      }
      jsPlumbFun.addFlowDefineInfo(this.id, this.form)
    }
    // 监听数据源选择确认事件
    this.$bus.$on('confirmSelectDataSource' + this.uniqueId, this.handleDataSourceSelected);
  },
  beforeUnmount() {
    // 移除事件监听
    this.$bus.$off('confirmSelectDataSource' + this.uniqueId, this.handleDataSourceSelected);
  },
  mounted() {
    if (!this.nodeType.includes(this.type)) {
      jsPlumbFun.addRootId(this.id)
      this.inst = jsPlumbFun.getInstance(this, 'busi-config-canvas')
      this.$nextTick(() => {
        this.$el.style.position = 'absolute';
        this.$el.style.top = this.nodeInfo.top + 'px';
        this.$el.style.left = this.nodeInfo.left + 'px';
        this.init();
        if (this.addType.includes(this.type)) {
          this.openEditModal()
        }
      })
    }
  },
  beforeUnmount() {
    if (!this.nodeType.includes(this.type)) {
      jsPlumbFun.delRootId(this.id)
    }
  },
  methods: {
    isEditableGetter() {
      return this.editByDblClick || this.editType.includes(this.type);
    },
    enableEdit() {
      // 仅当当前不是模板节点时允许双击编辑
      if (!this.nodeType.includes(this.type)) {
        this.editByDblClick = true;
        this.openEditModal();
      }
    },
    init() {
      this.endpoint = this.inst.addEndpoint(this.id, {
        container: this.id,
        uuid: this.id + 'out',
        anchors: ['BottomCenter'] // 锚点位置
      }, jsPlumbFun.sourceConfig)
      this.inst.draggable(this.id /*, {
        containment: 'parent', // 限制拖拽区域为父级容器
        grid: [10, 10],
      }*/)
      // 初始化完成，通知父级进行连线
      if (this.showType.includes(this.type)) {
        jsPlumbFun.saveFlowNodeId2Id(this.nodeInfo.flowNodeId, this.id);
      }
      this.endpoint.bind("mouseover", (endpoint, originalEvent) => {
        this.handleMouseOver(null)
      });
      this.endpoint.bind("mouseout", (endpoint, originalEvent) => {
        // originalEvent.buttons, originalEvent.which
        if (originalEvent.buttons !== 1) {
          this.handleMouseout(null)
        }
      });
    },
    handleMouseDown(event) {
      if (this.nodeType.includes(this.type)) {
        let myself = $('#' + this.id).eq(0);
        let offset = myself.offset();
        this.copyId = uuidv4();
        this.$bus.$emit('initPosition', {
          x: event.pageX - offset.left,
          y: event.pageY - offset.top,
          type: 'root',
        });
        let html =`<div id="${this.copyId}" class="root-node-class copy-node-class" style="position: absolute; top: ${offset.top}px; left: ${offset.left}px; z-index: 10;">${myself.html()}</div>`;
        this.app.append(html)
        $('#' + this.copyId).eq(0).css("cursor", "move");
      }
    },
    removeNode() {
      let inst = jsPlumbFun.getInstance(this, 'busi-config-canvas')
      this.$bus.$emit('removeNode', this.idx);
      inst.remove(this.id)
    },
    openEditModal() {
      this.handleMouseout(null);
      this.addRootVisible = true;
      this.$nextTick(() => {
        let myself = $('#' + this.id).eq(0)
        let myOffset = myself.offset();
        let dialog = $('#dialog-' + this.id).eq(0).children().eq(0);
        let dialogWidth = dialog.width();
        let dialogHeight = dialog.height();
        let winWidth = $(window).width();
        let winHeight = $(window).height();
        let left = myOffset.left + (myself.width() / 2) - (dialogWidth / 2);
        left = left > winWidth - dialogWidth ? winWidth - dialogWidth : left;
        left = left < 0 ? 0 : left;
        let top = myOffset.top + (myself.height() / 2) - (dialogHeight / 2);
        top = top > winHeight - dialogHeight ? winHeight - dialogHeight : top;
        top = top < 0 ? 0 : top;
        dialog.css('marginTop', top + 'px');
        dialog.css('marginLeft', left + 'px');
      })
    },
    createRoot() {
      this.$refs['rootForm'].validate((valid) => {
        if (valid) {
          // 额外检查必填字段
          // if (!this.form.dataSourceName) {
          //   this.$message.error('请选择数据源');
          //   return;
          // }
          if (!this.form.tableName) {
            this.$message.error('请输入表名称');
            return;
          }
          if (!this.form.fieldName) {
            this.$message.error('请输入表字段');
            return;
          }

          this.addRootVisible = false;
          this.editByDblClick = false;
          this.formCopy = {
            dataBaseName: this.form.dataBaseName,
            dataBaseId: this.form.dataBaseId,
            tableName: this.form.tableName,
            fieldName: this.form.fieldName,
          }
          jsPlumbFun.addFlowDefineInfo(this.id, this.form)
          this.$message.success('根节点保存成功');
        } else {
          this.$message.error('请完善必填信息');
        }
      })
    },
    initForm() {
      if (this.type === 'add') {
        this.form = {
          dataBaseName: '', // 数据源名称
          dataBaseId: null, // 数据源ID
          tableName: '', // 表名称
          fieldName: '', // 数据查询字段名称
        }
      } else if (this.type === 'modify') {
        this.form = {
          dataBaseName: this.nodeInfo.dataBaseName || '',
          dataBaseId: this.nodeInfo.dataBaseId || null,
          tableName: this.nodeInfo.tableName,
          fieldName: this.nodeInfo.fieldName,
        }
      }
    },
    cancel() {
      this.initForm()
      this.addRootVisible = false;
      this.editByDblClick = false;
      if (this.formCopy) {
        this.form = {
          dataBaseName: this.formCopy.dataBaseName,
          dataBaseId: this.formCopy.dataBaseId,
          tableName: this.formCopy.tableName,
          fieldName: this.formCopy.fieldName,
        }
      }
      if (this.type === 'add') {
        if (!this.formCopy) {
          jsPlumbFun.delFlowDefineInfo(this.id)
        }
      }
    },
    closeModal(event, flag) {
      if (flag) {
        this.addRootVisible = false;
        this.editByDblClick = false;
        return;
      }
      if (!event) {
        this.addRootVisible = false;
        return;
      }
      let wrapper = $('#dialog-' + this.id).eq(0);
      if (!wrapper || wrapper.length === 0) {
        this.addRootVisible = false;
        return;
      }
      let dialog = wrapper.children().eq(0);
      if (!dialog || dialog.length === 0) {
        this.addRootVisible = false;
        return;
      }
      let dialogOffset = dialog.offset();
      if (!dialogOffset) {
        this.addRootVisible = false;
        return;
      }
      let dialogWidth = dialog.width();
      let dialogHeight = dialog.height();
      if (event.pageX < dialogOffset.left || event.pageX > dialogOffset.left + dialogWidth) {
        this.addRootVisible = false;
      }
      if (event.pageY < dialogOffset.top || event.pageY > dialogOffset.top + dialogHeight) {
        this.addRootVisible = false;
      }
    },
    handleMouseOver(event) {
      if (!this.nodeType.includes(this.type)) {
        this.$bus.$emit('showEndpoint', true)
      }
    },
    handleMouseout(event) {
      if (!this.nodeType.includes(this.type)) {
        this.$bus.$emit('showEndpoint', false)
      }
    },
    handleRightClick(event) {
      // 阻止默认右键菜单
      event.preventDefault();
      // 只有非模板节点才显示右键菜单
      if (!this.nodeType.includes(this.type)) {
        this.$confirm('确定要删除此根节点吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.removeNode();
        }).catch(() => {
          // 用户取消删除
        });
      }
    },
    selectDataSource() {
      // 打开数据源选择弹窗
      this.dataSourceDialogVisible = true;
    },
    handleDataSourceSelected(dataSource) {
      // 处理数据源选择结果
      if (dataSource) {
        this.form.dataBaseName = dataSource.dataBaseName;
        this.form.dataBaseId = dataSource.dataBaseId;
        this.dataSourceDialogVisible = false;
      }
    }
  }
}
</script>
<style>
.root-node-class {
  border: 2px solid #cacaca;
  border-radius: 5px;
  text-align: center;
  width: 100px;
  cursor: pointer;
}

.root-node-class.node-canvas {
  width: 150px;
}
.root-node-class .root-node-header {
  font-size: 12px;
  padding: 5px;
  border-bottom: 2px solid #cacaca;
  background-color: #DAECF7;
  font-weight: 600;
}
.root-node-class .root-node-header-i {
  font-weight: 600;
}
.root-node-class .root-node-content {
  padding: 10px;
  font-size: 12px;
  background-color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.root-node-class .root-node-close {
  font-size: 12px;
  position: absolute;
  right: 2px;
  line-height: 18px;
}
.root-node-class .root-node-close:hover {
  color: rgb(33,160,255);
  font-weight: 600;
}
.root-node-class:hover {
  border: 2px solid rgb(33,160,255,0.8);
	box-shadow: 0 2px 12px 0 rgb(33,160,255,0.4);
}
</style>
