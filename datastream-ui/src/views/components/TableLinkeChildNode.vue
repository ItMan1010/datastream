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
  <div :id="id" :class="['child-node-class', isCanvas ? 'node-canvas' : '']"  @mouseover="handleMouseOver($event)" @mouseout="handleMouseout($event)" :desc="'child-' + seq" @mousedown="handleMouseDown($event)" @dblclick="enableEdit" @contextmenu="handleRightClick($event)">
    <div v-if="form.parentFieldName" class="node-ship">
      <div>
        <span>父：{{ form.parentFieldName }}</span>
      </div>
      <div>
        <span>子：{{ form.fieldName }}</span>
      </div>
    </div>
    <div class="child-node-header">
      <i class="el-icon-remove-outline child-node-header-i"></i>
      <span v-if="seq === 0">child</span>
      <span v-else>child-{{ seq }}</span>
      <i v-if="editType.includes(type)" class="el-icon-close child-node-close" @click="removeNode"></i>
    </div>
    <div class="child-node-content">
      <i class="el-icon-edit" @click="openEditModal"></i>
      <el-tooltip class="item" effect="dark" :content="form.tableName" placement="top">
        <span class="node-desc">{{ form.tableName }}</span>
      </el-tooltip>
    </div>
    <div v-if="addChildVisible" class="my-modal"></div>
    <div v-if="addChildVisible" class="my-dialog__wrapper node-create-form" :id="'dialog-' + id" style="z-index: 3013;" @click="closeModal($event)">
      <div class="my-dialog" style="width: 600px;">
        <div class="my-dialog__header">
          <span class="my-dialog__title">{{ isEditable ? '编辑' : '查看'}}child-{{ seq }}</span>
          <button type="button" aria-label="Close" class="el-dialog__headerbtn" @click="closeModal($event, true)">
            <i class="el-dialog__close el-icon el-icon-close"></i>
          </button>
        </div>
        <div class="my-dialog__body">
          <el-form :ref="id + 'ChildForm'" :model="form" :rules="rules" label-width="200px">
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
            <el-form-item label="选择链接节点：" prop="parentFieldName" :required="false">
              <el-select v-model="parentSelectedId" :disabled="!isEditable" placeholder="请选择上级节点" clearable style="width: 300px;" @change="handleParentChange">
                <el-option
                  v-for="item in parentNodeList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="输入链接字段：" prop="fieldName" :required="false">
              <el-input v-model="form.parentFieldName" :disabled="!isEditable" clearable style="width: 300px;"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button v-if="isEditable" type="primary" v-loading.fullscreen.lock="loading" style="width: 80px;" @click="createChild">确认</el-button>
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
  name: 'ChildBasicNode',
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
          parentFieldName: '',
          parentFlowNodeId: null,
          top: 0,
          left: 0,
        }
      }
    },
    seq: {
      type: Number,
      default () {
        return 0
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
      addChildVisible: false,
      form: {
        dataBaseName: '', // 数据源名称
        dataBaseId: null, // 数据源ID
        tableName: '', // 表名称
        fieldName: '', // 数据查询字段名称
        parentFieldName: '', // 父级数据字段名称，如果是顶层节点，则关联回迁流程实例业务流水字段
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
          { required: true, message: '请输入链接字段', trigger: 'change' }
        ],
        parentFieldName: [
          // 上级节点为可选，不设置必填验证
        ],
      },
      formCopy: null, // 表单数据备份
      editByDblClick: false,
      parentSelectedId: null,
      parentNodeList: [],
      dataSourceDialogVisible: false, // 数据源选择弹窗
      uniqueId: 'childNode', // 唯一标识
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
    addChildVisible: {
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
        parentFieldName: this.nodeInfo.parentFieldName,
        parentFlowNodeId: this.nodeInfo.parentFlowNodeId,
      }
      jsPlumbFun.addFlowDefineInfo(this.id, this.form)
    }
    // 监听数据源选择确认事件
    this.$bus.$on('confirmSelectDataSource' + this.uniqueId, this.handleDataSourceSelected);
  },
  beforeUnmount() {
    if (!this.nodeType.includes(this.type)) {
      jsPlumbFun.delChildrenId(this.id);
    }
    // 移除事件监听
    this.$bus.$off('confirmSelectDataSource' + this.uniqueId, this.handleDataSourceSelected);
  },
  mounted() {
    if (!this.nodeType.includes(this.type)) {
      jsPlumbFun.addChildrenId(this.id);
      let inst = jsPlumbFun.getInstance(this, 'busi-config-canvas')
      this.$nextTick(() => {
        this.$el.style.position = 'absolute';
        this.$el.style.top = this.nodeInfo.top + 'px';
        this.$el.style.left = this.nodeInfo.left + 'px';
        this.init(inst);
        this.refreshParentNodeList();
        if (this.addType.includes(this.type)) {
          this.openEditModal()
        }
      })
    }
  },
  methods: {
    enableEdit() {
      if (!this.nodeType.includes(this.type)) {
        this.editByDblClick = true;
        this.openEditModal();
      }
    },
    refreshParentNodeList() {
      // 从画布内扫描可选上级节点（排除自身和拖拽复制体）
      const self = this;
      const nodes = [];
      const flowInfo = jsPlumbFun.getFlowDefineInfo();
      // 根节点
      $('#busi-config-canvas .root-node-class').each(function() {
        if ($(this).hasClass('copy-node-class')) return;
        const id = this.id; // DOM id 是字符串
        if (id) {
          const nodeInfo = flowInfo[id];
          const name = nodeInfo && nodeInfo.tableName ? nodeInfo.tableName : '根节点';
          nodes.push({ id, name });
        }
      });
      // child
      $('#busi-config-canvas .child-node-class').each(function() {
        if ($(this).hasClass('copy-node-class')) return;
        const id = this.id;
        // id 是字符串，需要转换为数字进行比较
        if (id && Number(id) !== Number(self.id)) {
          const nodeInfo = flowInfo[id];
          const name = nodeInfo && nodeInfo.tableName ? nodeInfo.tableName : 'child';
          nodes.push({ id, name });
        }
      });
      self.parentNodeList = nodes.filter(n => Number(n.id) !== Number(self.id));
      // 默认选中已有父节点（如果有），转换为字符串以匹配下拉框的value
      if (!self.parentSelectedId && self.nodeInfo && self.nodeInfo.parentFlowNodeId) {
        self.parentSelectedId = String(self.nodeInfo.parentFlowNodeId);
      }
    },
    handleParentChange(val) {
      if (val) {
        // 选择了上级节点
        const found = this.parentNodeList.find(x => x.id === val);
        if (found) {
          // 显示为所选节点名称
          this.form.parentLinkNodeId = found.name;
          // 自动连线：从父节点到当前子节点
          this.createConnection(found.id, this.id);
        }
      } else {
        // 清空选择（选择为空）
        this.form.parentLinkNodeId = '';
        // 断开所有入线
        this.disconnectAllIncomingConnections(this.id);
        console.log(`已清空上级节点选择，断开连线: ${this.id}`);
      }
    },
    createConnection(parentId, childId) {
      try {
        const inst = jsPlumbFun.getInstance(this, 'busi-config-canvas');
        if (inst) {
          // 先断开当前子节点的所有入线
          this.disconnectAllIncomingConnections(childId);
          // 创建新连线：从父节点的out端点到子节点的in端点
          inst.connect({
            uuids: [parentId + 'out', childId + 'in']
          });
          console.log(`已自动连线: ${parentId} -> ${childId}`);
        }
      } catch (error) {
        console.warn('自动连线失败:', error);
      }
    },
    disconnectAllIncomingConnections(nodeId) {
      try {
        const inst = jsPlumbFun.getInstance(this, 'busi-config-canvas');
        if (inst) {
          const connections = inst.getConnections();
          connections.forEach(conn => {
            if (conn.targetId === nodeId) {
              inst.deleteConnection(conn);
            }
          });
        }
      } catch (error) {
        console.warn('断开连线失败:', error);
      }
    },
    disconnectAllOutgoingConnections(nodeId) {
      try {
        const inst = jsPlumbFun.getInstance(this, 'busi-config-canvas');
        if (inst) {
          const connections = inst.getConnections();
          connections.forEach(conn => {
            if (conn.sourceId === nodeId) {
              inst.deleteConnection(conn);
            }
          });
        }
      } catch (error) {
        console.warn('断开连线失败:', error);
      }
    },
    init(inst) {
      let sourceEndpoint = inst.addEndpoint(this.id, {
        container: this.id,
        uuid: this.id + 'in',
        anchors: ['TopCenter'] // 锚点位置
      }, jsPlumbFun.targetConfig)
      let targetEndpoint = inst.addEndpoint(this.id, {
        container: this.id,
        uuid: this.id + 'out',
        anchors: ['BottomCenter'] // 锚点位置
      }, jsPlumbFun.sourceConfig)
      inst.draggable(this.id /*, {
        containment: 'parent', // 限制拖拽区域为父级容器
        grid: [10, 10],
      }*/)
      // 初始化完成，通知父级进行连线
      if (this.showType.includes(this.type)) {
        jsPlumbFun.saveFlowNodeId2Id(this.nodeInfo.flowNodeId, this.id, this.nodeInfo.parentFlowNodeId);
      }
      sourceEndpoint.bind("mouseover", (endpoint, originalEvent) => {
        this.handleMouseOver(null)
      });
      sourceEndpoint.bind("mouseout", (endpoint, originalEvent) => {
        // originalEvent.buttons, originalEvent.which
        if (originalEvent.buttons !== 1) {
          this.handleMouseout(null)
        }
      });
      targetEndpoint.bind("mouseover", (endpoint, originalEvent) => {
        this.handleMouseOver(null)
      });
      targetEndpoint.bind("mouseout", (endpoint, originalEvent) => {
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
          type: 'child',
        });
        let html =`<div id="${this.copyId}" class="child-node-class copy-node-class" style="position: absolute; top: ${offset.top}px; left: ${offset.left}px; z-index: 10;">${myself.html()}</div>`;
        this.app.append(html)
        $('#' + this.copyId).eq(0).css("cursor", "move");
      }
    },
    removeNode() {
      // 删除前先断开所有连线
      this.disconnectAllIncomingConnections(this.id);
      this.disconnectAllOutgoingConnections(this.id);
      let inst = jsPlumbFun.getInstance(this, 'busi-config-canvas')
      this.$bus.$emit('removeChildNode', this.idx);
      inst.remove(this.id)
    },
    openEditModal() {
      this.handleMouseout(null);
      // 打开前刷新一次上级节点列表
      this.refreshParentNodeList();
      this.addChildVisible = true;
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
    createChild() {
      this.$refs[this.id + 'ChildForm'].validate((valid) => {
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
            this.$message.error('请输入链接字段');
            return;
          }

          this.addChildVisible = false;
          this.editByDblClick = false;
          this.formCopy = {
            dataBaseName: this.form.dataBaseName,
            dataBaseId: this.form.dataBaseId,
            tableName: this.form.tableName,
            fieldName: this.form.fieldName,
            parentFieldName: this.form.parentFieldName,
          },
          jsPlumbFun.addFlowDefineInfo(this.id, this.form)
          this.$message.success('子节点保存成功');
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
          parentFieldName: '', // 父级数据字段名称，如果是顶层节点，则关联回迁流程实例业务流水字段
        }
      } else if (this.type === 'modify') {
        this.form = {
          dataBaseName: this.nodeInfo.dataBaseName || '',
          dataBaseId: this.nodeInfo.dataBaseId || null,
          tableName: this.nodeInfo.tableName,
          fieldName: this.nodeInfo.fieldName,
          parentFieldName: this.nodeInfo.parentFieldName,
        }
      }
    },
    cancel() {
      // 取消时恢复原来的连线状态
      if (this.formCopy && this.formCopy.parentFieldName) {
        // 如果有备份的父节点信息，恢复连线
        const parentNode = this.parentNodeList.find(n => n.name === this.formCopy.parentFieldName);
        if (parentNode) {
          this.createConnection(parentNode.id, this.id);
        }
      } else {
        // 如果没有备份，断开所有入线
        this.disconnectAllIncomingConnections(this.id);
      }
      this.initForm();
      this.addChildVisible = false;
      this.editByDblClick = false;
      if (this.formCopy) {
        this.form = {
          dataBaseName: this.formCopy.dataBaseName,
          dataBaseId: this.formCopy.dataBaseId,
          tableName: this.formCopy.tableName,
          fieldName: this.formCopy.fieldName,
          parentFieldName: this.formCopy.parentFieldName,
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
        this.addChildVisible = false;
        this.editByDblClick = false;
        return;
      }
      if (!event) {
        this.addChildVisible = false;
        this.editByDblClick = false;
        return;
      }
      let wrapper = $('#dialog-' + this.id).eq(0);
      if (!wrapper || wrapper.length === 0) {
        this.addChildVisible = false;
        this.editByDblClick = false;
        return;
      }
      let dialog = wrapper.children().eq(0);
      if (!dialog || dialog.length === 0) {
        this.addChildVisible = false;
        this.editByDblClick = false;
        return;
      }
      let dialogOffset = dialog.offset();
      if (!dialogOffset) {
        this.addChildVisible = false;
        this.editByDblClick = false;
        return;
      }
      let dialogWidth = dialog.width();
      let dialogHeight = dialog.height();
      if (event.pageX < dialogOffset.left || event.pageX > dialogOffset.left + dialogWidth) {
        this.addChildVisible = false;
      }
      if (event.pageY < dialogOffset.top || event.pageY > dialogOffset.top + dialogHeight) {
        this.addChildVisible = false;
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
        this.$confirm('确定要删除此子节点吗？', '提示', {
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
.child-node-class {
  border: 2px solid #cacaca;
  border-radius: 5px;
  text-align: center;
  width: 100px;
  cursor: pointer;
}

.child-node-class.node-canvas {
  width: 150px;
}
.child-node-class .child-node-header {
  font-size: 12px;
  padding: 5px;
  border-bottom: 2px solid #cacaca;
  background-color: #DAECF7;
  font-weight: 600;
}
.child-node-class .child-node-header-i {
  font-weight: 600;
}
.child-node-class .child-node-content {
  padding: 10px;
  font-size: 12px;
  background-color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.child-node-class .child-node-close {
  font-size: 12px;
  position: absolute;
  right: 2px;
  line-height: 18px;
}
.child-node-class .child-node-close:hover {
  color: rgb(33,160,255);
  font-weight: 600;
}
.child-node-class:hover {
  border: 2px solid rgb(33,160,255,0.8);
	box-shadow: 0 2px 12px 0 rgb(33,160,255,0.4);
}
.node-ship {
  position: absolute;
  top: -40px;
  font-size: 10px;
  width: 100%;
  color: #67C23A;
  white-space: nowrap;
}
</style>
