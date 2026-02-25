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
    <template #header>
      <div style="color: #409EFF; font-weight: bold;">
        <label>迁移任务</label> > 创建任务
      </div>
    </template>

    <div class="create-task-content">
      <el-form ref="formRef" :model="form" label-width="120px">
        <el-row :gutter="24" class="task-form-layout">
          <!-- 基本信息 -->
          <el-col :span="8" class="form-column-left">
            <div class="form-section">
              <div class="section-title">基本信息
                <el-popover
                  placement="bottom-end"
                  :width="320"
                  trigger="click">
                  <template #reference>
                    <el-icon class="help-icon-btn">
                      <InfoFilled/>
                    </el-icon>
                  </template>
                  <div class="task-base-help-popover-content">
                    <el-icon class="task-base-help-icon">
                      <InfoFilled/>
                    </el-icon>
                    <p class="task-base-help-text">基本说明：<br>
                      • 数据迁移：支持不同数据源间数据同步，如数据库、文件的双向数据同步<br>
                      • 数据清理：根据目标端已有数据，反向清理源端已同步的数据<br>
                      • 迁移清理：数据同步过程中自动清理源端数据，实现「搬运即清理」<br>
                      • 数据稽核：比对源端与目标端数据差异，一键修复不一致数据<br>
                      • 结构迁移：跨数据库表结构批量转换，支持同构与异构场景<br>
                      • 增量迁移：基于CDC技术实现源端数据实时增量同步
                    </p>
                  </div>
                </el-popover>
              </div>
              <el-form-item label="任务类型：">
                <el-select v-model="form.taskType" style="width: 100%;">
                  <el-option label="数据迁移" value="1"/>
                  <el-option label="数据清理" value="2"/>
                  <el-option label="迁移清理" value="3"/>
                  <el-option label="数据稽核" value="5"/>
                  <el-option label="结构迁移" value="4"/>
                  <el-option label="增量迁移" value="6"/>
                </el-select>
              </el-form-item>
              <el-form-item label="任务描述：">
                <el-input type="textarea" clearable :rows="4" v-model="form.taskDisc"
                          style="width: 100%;" placeholder="请输入任务描述信息"/>
              </el-form-item>
            </div>
          </el-col>

          <!-- 源端配置 -->
          <el-col :span="8" class="form-column-center">
            <div class="form-section">
              <div class="section-title">源端配置
                <el-popover
                  placement="bottom-end"
                  :width="320"
                  trigger="click">
                  <template #reference>
                    <el-icon class="help-icon-btn">
                      <InfoFilled/>
                    </el-icon>
                  </template>
                  <div class="task-base-help-popover-content">
                    <el-icon class="task-base-help-icon">
                      <InfoFilled/>
                    </el-icon>
                    <p class="task-base-help-text" v-html="sourceConfigHelpText"></p>
                  </div>
                </el-popover>
              </div>
              <el-form-item label="源对象选择：">
                <el-input v-model="form.sourceDataSourceName" style="width: 100%;" disabled
                          placeholder="请选择源库数据源">
                  <template #append>
                    <el-button @click="$emit('select-datasource', true)">
                      <el-icon>
                        <Search/>
                      </el-icon>
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="源对象类型：">
                <el-input v-model="sourceObjectCategoryText" disabled style="width: 100%;"/>
              </el-form-item>
              <el-form-item label="源对象名称：">
                <el-input v-model="form.sourceObjectName" clearable style="width: 100%;"
                          placeholder="请输入源对象名称" @input="handleSourceObjectInput"/>
              </el-form-item>
              <el-form-item v-if="showSourceTableCondition" label="源表数据过滤：">
                <el-input type="textarea" clearable :rows="4" v-model="form.sourceTableCondition"
                          style="width: 100%;" placeholder="填写SQL语句WHERE后面的条件"/>
                <div class="form-tip">做简单过滤，填SQL语句WHERE后面条件</div>
              </el-form-item>
              <el-form-item v-if="showDebeziumConfig" label="同步对象：">
                <el-radio-group v-model="form.sourceDebeziumObject" style="width: 100%;">
                  <el-radio :label="3">表结构和数据</el-radio>
                  <el-radio :label="1">表数据</el-radio>
                  <el-radio :label="2">表结构</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="showDebeziumConfig" label="Offset方式：">
                <el-radio-group v-model="form.sourceOffsetStorage" style="width: 100%;">
                  <el-radio :label="2">写入文件</el-radio>
                  <el-radio :label="1">写入元数据库</el-radio>
                  <el-radio :label="3">写入Kafka</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="showDebeziumConfig" label="初始化快照：">
                <el-radio-group v-model="form.sourceDebeziumSnapshot" style="width: 100%;">
                  <el-radio :label="0">不执行</el-radio>
                  <el-radio :label="1">执行</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="showKafkaConfig" label="Kafka地址：">
                <el-input v-model="form.sourceOffsetKafka" clearable style="width: 80%;"
                          placeholder="格式：kafka1:9092,kafka2:9092,kafka3:9092"/>
              </el-form-item>
<!--              <el-form-item v-if="showDebeziumConfig" label="开始位点：">-->
<!--                <el-input v-model="form.sourceOffsetStartPos" clearable style="width: 100%;"-->
<!--                          placeholder="可空<binlog文件名>:<pos位点>"/>-->
<!--              </el-form-item>-->
              <el-form-item v-if="showTableStructureConfig" label="迁移对象：">
                <el-radio-group v-model="form.sourceDataBaseObjectType" style="width: 100%;">
                  <el-radio :label="1">表对象</el-radio>
                  <el-radio :label="2">schema对象</el-radio>
                </el-radio-group>
              </el-form-item>
            </div>
          </el-col>

          <!-- 目标端配置 -->
          <el-col :span="8" class="form-column-right">
            <div class="form-section">
              <div class="section-title">目标端配置
                <el-popover
                  placement="bottom-end"
                  :width="320"
                  trigger="click">
                  <template #reference>
                    <el-icon class="help-icon-btn">
                      <InfoFilled/>
                    </el-icon>
                  </template>
                  <div class="task-base-help-popover-content">
                    <el-icon class="task-base-help-icon">
                      <InfoFilled/>
                    </el-icon>
                    <p class="task-base-help-text" v-html="targetConfigHelpText"></p>
                  </div>
                </el-popover>
              </div>
              <el-form-item label="目标对象选择：">
                <el-input v-model="form.targetDataSourceName" style="width: 100%;" disabled
                          placeholder="请选择目标对象源">
                  <template #append>
                    <el-button @click="$emit('select-datasource', false)">
                      <el-icon>
                        <Search/>
                      </el-icon>
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="目标对象类型：">
                <el-input v-model="targetObjectCategoryText" disabled style="width: 100%;"/>
              </el-form-item>
              <el-form-item label="目标对象名称：">
                <el-input v-model="form.targetObjectName" clearable style="width: 100%;"
                          placeholder="请输入目标对象名称"/>
              </el-form-item>
              <el-form-item v-if="showTargetInsertMode" label="数据写入方式：">
                <el-radio-group v-model="form.targetInsertMode" style="width: 100%;">
                  <el-radio :label="1">静态方式</el-radio>
                  <el-radio :label="2">变量方式</el-radio>
                  <el-radio :label="3">自动调度</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="showTargetCheckFlag" label="校验目标数据：">
                <el-radio-group v-model="form.targetCheckFlag" style="width: 100%;">
                  <el-radio :label="1">校验</el-radio>
                  <el-radio :label="2">不校验</el-radio>
                </el-radio-group>
              </el-form-item>
            </div>
          </el-col>
        </el-row>

        <!-- 扩展配置 -->
        <el-row v-if="form.taskType === '5'" :gutter="24" class="mt-30">
          <el-col :span="24">
            <div class="form-section">
              <div class="section-title">扩展配置</div>
              <el-row :gutter="20">
                <el-col v-if="isShardingDBFlag" :span="8">
                  <el-form-item label="按分片生成：">
                    <el-radio-group v-model="form.dataNodeFlag">
                      <el-radio :label="0">否</el-radio>
                      <el-radio :label="1">是</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col v-if="isShardingDBFlag" :span="8">
                  <el-form-item label="主/从节点：">
                    <el-radio-group v-model="form.dataSet">
                      <el-radio :label="0">写节点</el-radio>
                      <el-radio :label="1">读节点</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col v-if="isShardingDBFlag" :span="8">
                  <el-form-item label="表类型：">
                    <el-radio-group v-model="form.tableType">
                      <el-radio :label="1">分片表</el-radio>
                      <el-radio :label="2">全局表</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="稽核模式：">
                    <el-radio-group v-model="form.checkMode">
                      <el-radio :label="1">正向模式</el-radio>
                      <el-radio :label="2">双向模式</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </el-col>
        </el-row>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <el-button type="default" :loading="loading" @click="$emit('cancel')">取消</el-button>
          <el-button type="info" :loading="loading" @click="$emit('reset')">重置</el-button>
          <el-button type="primary" :loading="loading" @click="$emit('submit')">确认</el-button>
        </div>
      </el-form>
    </div>
  </el-drawer>
</template>

<script>
import {computed} from 'vue'
import {Search} from '@element-plus/icons-vue'
import {DATASOURCE_TYPE_DESC} from "@/constants/index.js";

export default {
  name: 'TaskCreateDrawer',
  components: {
    Search
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    form: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    isShardingDBFlag: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'select-datasource', 'cancel', 'reset', 'submit', 'source-input'],
  setup(props, {emit}) {
    const visible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    // 显示源表数据过滤
    const showSourceTableCondition = computed(() => {
      return props.form.taskType !== '6' &&
        props.form.taskType !== '4' &&
        props.form.sourceObjectCategory === 'database'
    })

    // 显示增量迁移配置
    const showDebeziumConfig = computed(() => {
      return props.form.taskType === '6' &&
        props.form.sourceObjectCategory === 'database'
    })

    // 显示表结构迁移配置
    const showTableStructureConfig = computed(() => {
      return props.form.taskType === '4' &&
        props.form.sourceObjectCategory === 'database'
    })

    // 显示Kafka配置
    const showKafkaConfig = computed(() => {
      return props.form.taskType === '6' &&
        props.form.sourceObjectCategory === 'database' &&
        props.form.sourceOffsetStorage === 3
    })

    // 显示目标写入方式
    const showTargetInsertMode = computed(() => {
      return (props.form.taskType === '1' ||
          props.form.taskType === '3') &&
        props.form.targetObjectCategory === 'database'
    })

    // 显示目标写入方式
    const showTargetCheckFlag = computed(() => {
      return props.form.taskType === '2' && props.form.targetObjectCategory === 'database'
    })

    const handleSourceObjectInput = () => {
      emit('source-input')
    }

    // 源对象类型文本（用于显示具体类型名称）
    const sourceObjectCategoryText = computed(() => {
      return DATASOURCE_TYPE_DESC[props.form.sourceObjectType] || ''
    })

    // 目标对象类型文本（用于显示具体类型名称）
    const targetObjectCategoryText = computed(() => {
      return DATASOURCE_TYPE_DESC[props.form.targetObjectType] || ''
    })

    // 源端配置帮助文本（根据任务类型动态变化）
    const sourceConfigHelpText = computed(() => {
      const taskType = props.form.taskType
      const helpTexts = {
        '1': '源端配置：<br>• 选择源数据源：支持数据库（MySQL/Oracle/PostgreSQL/Doris）、文件、消息队列等<br>• 配置源对象类型：数据库、文件、Kafka等<br>• 输入源对象名称：表名、文件路径、Topic等<br>• 数据过滤：支持SQL WHERE条件过滤数据',
        '2': '源端配置：<br>• 选择源数据源：需要清理的源端数据库<br>• 配置源对象类型：数据库类型<br>• 输入源对象名称：需要清理的表名',
        '3': '源端配置：<br>• 选择源数据源：需要迁移并清理的源端数据源<br>• 配置源对象类型：数据库、文件等<br>• 输入源对象名称：迁移的对象名称<br>• 注意：迁移完成后会自动清理源端数据',
        '4': '源端配置：<br>• 选择源数据源：源端数据库<br>• 配置迁移对象：表对象或schema对象<br>• 支持同构与异构数据库结构转换',
        '5': '源端配置：<br>• 选择源数据源：源端数据库或文件<br>• 配置稽核对象：源端数据源<br>• 支持分片表、全局表的稽核配置<br>• 正向/双向模式选择',
        '6': '源端配置：<br>• 选择源数据源：支持CDC的数据库,目前只支持Mysql<br>• 源对象名称：输入schema或指定表名称,多表逗号分割<br>• 同步对象：表数据、表结构或表结构和数据<br>• Offset方式：写入数据库/文件/Kafka<br>• 初始化快照：执行会锁数据库'
      }
      return helpTexts[taskType] || '源端配置'
    })

    // 目标端配置帮助文本（根据任务类型动态变化）
    const targetConfigHelpText = computed(() => {
      const taskType = props.form.taskType
      const helpTexts = {
        '1': '目标端配置：<br>• 选择目标数据源：支持数据库、文件、消息队列等<br>• 配置目标对象类型：与源端类型匹配<br>• 输入目标对象名称：表名、文件路径、Topic等<br>• 数据写入方式：静态方式/变量方式/自动调度',
        '2': '目标端配置：<br>• 选择目标数据源：已同步数据的目标数据库<br>• 根据目标端数据状态反向清理源端数据<br>• 支持数据校验',
        '3': '目标端配置：<br>• 选择目标数据源：数据迁移的目标端<br>• 数据写入方式：支持静态、变量、自动调度<br>• 实现「搬运即清理」模式',
        '4': '目标端配置：<br>• 选择目标数据源：目标端数据库<br>• 配置迁移对象：表对象或schema对象<br>• 跨数据库表结构批量转换',
        '5': '目标端配置：<br>• 选择目标数据源：目标端数据库或文件<br>• 比对源端与目标端数据差异<br>• 一键修复不一致数据',
        '6': '目标端配置：<br>• 选择目标数据源：增量同步的目标端<br>• 基于CDC技术实现源端数据实时增量同步'
      }
      return helpTexts[taskType] || '目标端配置'
    })

    return {
      visible,
      showSourceTableCondition,
      showDebeziumConfig,
      showTableStructureConfig,
      showKafkaConfig,
      showTargetInsertMode,
      showTargetCheckFlag,
      sourceObjectCategoryText,
      targetObjectCategoryText,
      sourceConfigHelpText,
      targetConfigHelpText,
      handleSourceObjectInput
    }
  }
}
</script>

<style scoped>
.create-task-content {
  padding: 30px;
  background: #f5f7fa;
  min-height: 100%;
}

.create-task-content .el-form {
  background: #fff;
  padding: 30px 40px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.task-form-layout {
  margin-bottom: 0;
}

.form-section {
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 20px;
  height: 100%;
  transition: all 0.3s ease;
}

.form-section:hover {
  border-color: #c0c4cc;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  line-height: 1.5;
}

.form-tip-warning {
  color: #f56c6c;
}

.form-column-left .form-section {
  border-left: 3px solid #409eff;
}

.form-column-center .form-section {
  border-left: 3px solid #67c23a;
}

.form-column-right .form-section {
  border-left: 3px solid #e6a23c;
}

.mt-30 {
  margin-top: 30px;
}

.form-actions {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #fafbfc;
  margin-left: -40px;
  margin-right: -40px;
  margin-bottom: -30px;
  padding: 20px 40px 24px;
  border-radius: 0 0 8px 8px;
}

.form-actions .el-button {
  min-width: 90px;
  height: 36px;
}

/* task-base配置说明图标按钮 */
.help-icon-btn {
  font-size: 18px;
  color: #909399;
  cursor: pointer;
  transition: color 0.3s;
}

.help-icon-btn:hover {
  color: #409eff;
}

/* task-base配置说明Popover样式 */
.task-base-help-popover-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.task-base-help-icon {
  font-size: 20px;
  color: #409eff;
  flex-shrink: 0;
  margin-top: 2px;
}

.task-base-help-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  margin: 0;
}
</style>

