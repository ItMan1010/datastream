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
    width="600px"
    :before-close="handleClose"
    append-to-body>

    <el-form
      ref="mqFormRef"
      :model="mqForm"
      :rules="rules"
      label-width="120px"
      :disabled="mode === 'detail'">

      <el-form-item label="Mq ID：">
        <el-input v-model="mqForm.mqConfigId" disabled class="readonly-input"/>
      </el-form-item>

      <el-form-item label="MQ类型：" prop="mqType">
        <el-select v-model="mqForm.mqType" placeholder="请选择MQ类型" style="width: 200px;">
          <el-option label="Kafka" :value="10"/>
        </el-select>
      </el-form-item>

      <el-form-item label="实例名称：" prop="mqConfigName">
        <el-input v-model="mqForm.mqConfigName" placeholder="请输入实例名称"/>
      </el-form-item>

      <el-form-item label="服务地址：" prop="bootstrapServers">
        <el-input
          v-model="mqForm.bootstrapServers"
          placeholder="例如: localhost:9092,localhost:9093"
          type="textarea"
          :rows="2"/>
      </el-form-item>

      <el-form-item label="报文格式：" prop="messageFormat">
        <el-radio-group v-model="mqForm.messageFormat">
          <el-radio :label="1">JSON格式</el-radio>
<!--          <el-radio :label="2">分隔符格式</el-radio>-->
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="mqForm.messageFormat === 2" label="分隔符：" prop="delimiter">
        <el-select v-model="mqForm.delimiter" placeholder="请选择分隔符" style="width: 200px;">
          <el-option label="竖线 |" value="|"/>
          <el-option label="逗号 ," value=","/>
          <el-option label="分号 ;" value=";"/>
          <el-option label="制表符 \\t" value="	"/>
        </el-select>
      </el-form-item>

      <!--      <el-form-item label="Topic前缀：">-->
      <!--        <el-input v-model="mqForm.topicPrefix" placeholder="Topic名称前缀（可选）" />-->
      <!--      </el-form-item>-->

      <el-form-item label="备注：">
        <el-input v-model="mqForm.remark" type="textarea" :rows="2" placeholder="备注信息"/>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="mode === 'add'" type="primary" :loading="loading" @click="saveMqConfig">保存</el-button>
        <el-button v-if="mode === 'modify'" type="primary" :loading="loading" @click="updateMqConfig">保存</el-button>
        <el-button v-if="mode !== 'detail'" @click="resetForm">重置</el-button>
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import {ref, reactive, computed, watch, getCurrentInstance} from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'

export default {
  name: 'MqConfigDetail',
  props: {
    mqDetailVisible: {type: Boolean, default: false},
    mode: {type: String, default: 'add'},
    mqConfigDetail: {type: Object, default: () => ({})}
  },
  setup(props) {
    const instance = getCurrentInstance()
    const bus = instance?.appContext.config.globalProperties.$bus

    const mqFormRef = ref(null)
    const loading = ref(false)

    // 初始表单数据
    const getInitialForm = () => ({
      mqConfigId: '',
      mqConfigName: '',
      mqType: 10,
      bootstrapServers: '',
      messageFormat: 1,
      delimiter: '|',
      topicPrefix: '',
      remark: ''
    })

    const mqForm = reactive(getInitialForm())

    const rules = {
      mqConfigName: [{required: true, message: '请输入实例名称', trigger: 'blur'}],
      bootstrapServers: [{required: true, message: '请输入服务地址', trigger: 'blur'}],
      messageFormat: [{required: true, message: '请选择报文格式', trigger: 'change'}]
    }

    const title = computed(() => {
      const titles = {add: '新增Mq配置', detail: 'Mq配置详情', modify: '修改Mq配置'}
      return titles[props.mode] || 'Mq配置'
    })

    const localVisible = computed({
      get: () => props.mqDetailVisible,
      set: (val) => updateVisible(val)
    })

    // 监听props变化，初始化表单
    watch(() => props.mqConfigDetail, (newVal) => {
      if (newVal && Object.keys(newVal).length > 0) {
        Object.keys(mqForm).forEach(key => {
          if (newVal[key] !== undefined) {
            mqForm[key] = newVal[key]
          }
        })
      } else {
        // 重置为初始值
        Object.assign(mqForm, getInitialForm())
      }
    }, {immediate: true})

    watch(() => props.mqDetailVisible, (newVal) => {
      if (newVal && props.mode === 'add') {
        Object.assign(mqForm, getInitialForm())
      }
    })

    const updateVisible = (visible) => {
      if (bus) bus.$emit('changeMqDetailVisible', visible)
    }

    const handleClose = () => {
      updateVisible(false)
    }

    const resetForm = () => {
      if (props.mode === 'add') {
        Object.assign(mqForm, getInitialForm())
      } else if (props.mqConfigDetail) {
        Object.keys(mqForm).forEach(key => {
          if (props.mqConfigDetail[key] !== undefined) {
            mqForm[key] = props.mqConfigDetail[key]
          }
        })
      }
    }

    const saveMqConfig = async () => {
      try {
        const valid = await mqFormRef.value.validate()
        if (!valid) return
      } catch {
        return
      }

      loading.value = true
      try {
        const res = await http(constant.ADD_MQ_CONFIG, 'post', {mqConfig: mqForm})
        if (res.errorCode !== '0') {
          instance.proxy.$message.error(`新增失败：${res.errorMsg}`)
          return
        }
        instance.proxy.$message.success('新增成功')
        updateVisible(false)
      } catch (err) {
        instance.proxy.$message.error(`新增失败：${err}`)
      } finally {
        loading.value = false
      }
    }

    const updateMqConfig = async () => {
      try {
        const valid = await mqFormRef.value.validate()
        if (!valid) return
      } catch {
        return
      }

      loading.value = true
      try {
        const res = await http(constant.MODIFY_MQ_CONFIG, 'post', {mqConfig: mqForm})
        if (res.errorCode !== '0') {
          instance.proxy.$message.error(`修改失败：${res.errorMsg}`)
          return
        }
        instance.proxy.$message.success('修改成功')
        updateVisible(false)
      } catch (err) {
        instance.proxy.$message.error(`修改失败：${err}`)
      } finally {
        loading.value = false
      }
    }

    return {
      mqFormRef,
      loading,
      mqForm,
      rules,
      title,
      localVisible,
      handleClose,
      resetForm,
      saveMqConfig,
      updateMqConfig
    }
  }
}
</script>

<style scoped>
.dialog-footer {
  text-align: center;
}

.readonly-input :deep(.el-input__wrapper) {
  background-color: #f5f7fa !important;
}
</style>

