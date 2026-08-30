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
  <div style="height: 100%; padding: 20px;">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 类型定义 Tab -->
      <el-tab-pane label="类型定义" name="define">
        <div class="search-nav">
          <el-form :label-position="'left'" :inline="true">
            <el-form-item label="" style="margin-right: 0;">
              <el-select v-model="defineQueryForm.queryFlag" style="width: 140px;" @change="changeDefineQueryFlag">
                <el-option v-for="opt in queryFlagOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="" style="margin-right: 0;">
              <el-input
                v-if="defineQueryForm.queryFlag !== '1'"
                class="ml-20"
                v-model="defineQueryForm.queryValue"
                clearable
                :placeholder="defineQueryForm.queryFlag === '2' ? '请输入数据库类型' : '请输入类型名称'"
                style="width: 240px;" />
            </el-form-item>
            <el-form-item>
              <el-button class="ml-20" type="primary" :loading="loading" @click="loadTypeDefineRows">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
              <el-button class="ml-20" type="primary" plain @click="openAddDefine">
                <el-icon><Plus /></el-icon>
                新增
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-divider />

        <div class="mt-10 pl-20 pr-20">
          <el-table :data="typeDefineList" fit stripe highlight-current-row style="width: 100%;">
            <el-table-column prop="columnTypeDefineId" label="ID" width="70" :show-overflow-tooltip="true" />
            <el-table-column prop="databaseType" label="数据库类型" width="130" />
            <el-table-column prop="columnTypeName" label="类型名称" width="160" />
            <el-table-column label="类型分类" width="120">
              <template #default="scope">
                {{ typeCategoryLabel(scope.row.typeCategory) }}
              </template>
            </el-table-column>
            <el-table-column prop="maxPrecision" label="最大精度" width="90" />
            <el-table-column prop="maxScale" label="最大小数位" width="100" />
            <el-table-column prop="characterMaxLength" label="最大长度" width="90" />
            <el-table-column prop="remark" label="备注" min-width="140" :show-overflow-tooltip="true" />
            <el-table-column prop="systemUserCode" label="创建用户" width="100" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <div class="datasource-opt">
                  <el-button type="warning" size="small" @click="openModifyDefine(scope.row)" plain>
                    <el-icon><Edit /></el-icon>
                    修改
                  </el-button>
                  <el-button type="danger" size="small" @click="removeDefine(scope.row)" plain>
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <PagePagination
            :page-num="definePagination.pageNum.value"
            :page-size="definePagination.pageSize.value"
            :total="definePagination.total.value"
            @size-change="handleDefineSizeChange"
            @current-change="handleDefinePageChange" />
        </div>
      </el-tab-pane>

      <!-- 类型映射 Tab -->
      <el-tab-pane label="类型映射" name="map">
        <div class="search-nav">
          <el-form :label-position="'left'" :inline="true">
            <el-form-item label="" style="margin-right: 0;">
              <el-select v-model="mapQueryForm.queryFlag" style="width: 140px;" @change="changeMapQueryFlag">
                <el-option v-for="opt in mapQueryFlagOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="" style="margin-right: 0;">
              <el-input
                v-if="mapQueryForm.queryFlag !== '1'"
                class="ml-20"
                v-model="mapQueryForm.queryValue"
                clearable
                :placeholder="mapQueryForm.queryFlag === '2' ? '请输入源数据库类型' : '请输入目标数据库类型'"
                style="width: 240px;" />
            </el-form-item>
            <el-form-item>
              <el-button class="ml-20" type="primary" :loading="loading" @click="loadTypeMapRows">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
              <el-button class="ml-20" type="primary" plain @click="openAddMap">
                <el-icon><Plus /></el-icon>
                新增
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-divider />

        <div class="mt-10 pl-20 pr-20">
          <el-table :data="typeMapList" fit stripe highlight-current-row style="width: 100%;">
            <el-table-column prop="columnTypeMapId" label="ID" width="70" :show-overflow-tooltip="true" />
            <el-table-column label="源类型" width="180">
              <template #default="scope">
                {{ typeDefineLabelText(scope.row.databaseTypeA, scope.row.columnTypeNameA) }}
              </template>
            </el-table-column>
            <el-table-column label="目标类型" width="180">
              <template #default="scope">
                {{ typeDefineLabelText(scope.row.databaseTypeB, scope.row.columnTypeNameB) }}
              </template>
            </el-table-column>
            <el-table-column label="匹配级别" width="110">
              <template #default="scope">
                <el-tag :type="matchLevelType(scope.row.matchLevel)" effect="plain">
                  {{ matchLevelLabel(scope.row.matchLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="转换提示" min-width="200">
              <template #default="scope">
                <span v-if="scope.row.matchLevel !== 1 && scope.row.conversionWarning" class="warning-text">
                  <el-icon><Warning /></el-icon>
                  {{ scope.row.conversionWarning }}
                </span>
                <span v-else-if="scope.row.matchLevel !== 1">非精确匹配，可能存在精度/范围差异</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="可逆" width="80">
              <template #default="scope">
                {{ scope.row.isReversible === 1 ? '是' : '否' }}
              </template>
            </el-table-column>
            <el-table-column prop="systemUserCode" label="创建用户" width="100" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <div class="datasource-opt">
                  <el-button type="warning" size="small" @click="openModifyMap(scope.row)" plain>
                    <el-icon><Edit /></el-icon>
                    修改
                  </el-button>
                  <el-button type="danger" size="small" @click="removeMap(scope.row)" plain>
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <PagePagination
            :page-num="mapPagination.pageNum.value"
            :page-size="mapPagination.pageSize.value"
            :total="mapPagination.total.value"
            @size-change="handleMapSizeChange"
            @current-change="handleMapPageChange" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 类型定义表单弹窗 -->
    <el-dialog
      v-model="defineDialogVisible"
      :title="defineMode === 'add' ? '新增类型定义' : '修改类型定义'"
      width="680px"
      destroy-on-close>
      <el-form :model="defineForm" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据库类型" required>
              <el-input v-model="defineForm.databaseType" placeholder="如 mysql / postgresql / oracle" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型名称" required>
              <el-input v-model="defineForm.columnTypeName" placeholder="如 varchar / int4" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型分类" required>
              <el-select v-model="defineForm.typeCategory" style="width: 100%;">
                <el-option v-for="opt in typeCategoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大精度">
              <el-input-number v-model="defineForm.maxPrecision" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大小数位">
              <el-input-number v-model="defineForm.maxScale" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大长度">
              <el-input-number v-model="defineForm.characterMaxLength" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="defineForm.remark" type="textarea" :rows="2" placeholder="简要说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="defineDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="saveDefine">确定</el-button>
      </template>
    </el-dialog>

    <!-- 类型映射表单弹窗 -->
    <el-dialog
      v-model="mapDialogVisible"
      :title="mapMode === 'add' ? '新增类型映射' : '修改类型映射'"
      width="680px"
      destroy-on-close>
      <el-form :model="mapForm" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源类型" required>
              <el-select v-model="mapForm.columnTypeDefineIdA" style="width: 100%;" filterable>
                <el-option
                  v-for="opt in allTypeDefineOptions"
                  :key="opt.columnTypeDefineId"
                  :label="typeDefineLabel(opt)"
                  :value="opt.columnTypeDefineId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标类型" required>
              <el-select v-model="mapForm.columnTypeDefineIdB" style="width: 100%;" filterable>
                <el-option
                  v-for="opt in allTypeDefineOptions"
                  :key="opt.columnTypeDefineId"
                  :label="typeDefineLabel(opt)"
                  :value="opt.columnTypeDefineId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="匹配级别" required>
              <el-select v-model="mapForm.matchLevel" style="width: 100%;">
                <el-option v-for="opt in matchLevelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否可逆">
              <el-radio-group v-model="mapForm.isReversible">
                <el-radio :label="1">是</el-radio>
                <el-radio :label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="精度转换规则">
              <el-input v-model="mapForm.precisionConversionRule" placeholder="如 min(p, 65)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="长度转换规则">
              <el-input v-model="mapForm.lengthConversionRule" placeholder="如 min(len, 65535)" />
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="mapForm.matchLevel && mapForm.matchLevel !== 1">
            <el-alert
              type="warning"
              :closable="false"
              show-icon
              :title="`当前为非精确匹配（${matchLevelLabel(mapForm.matchLevel)}），转换可能存在精度或范围差异，请确认最佳实践。`" />
          </el-col>
          <el-col :span="24">
            <el-form-item label="转换警告">
              <el-input v-model="mapForm.conversionWarning" type="textarea" :rows="2" placeholder="如：精度可能损失" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="mapDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="saveMap">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onActivated } from 'vue'
import { Search, Plus, Edit, Delete, Warning } from '@element-plus/icons-vue'
import PagePagination from '@/views/components/common/PagePagination.vue'
import { useColumnTypeConfig } from '@/composables/useColumnTypeConfig'

export default {
  name: 'ColumnTypeConfig',
  components: {
    PagePagination,
    Search,
    Plus,
    Edit,
    Delete,
    Warning
  },
  setup() {
    const {
      loading,
      definePagination,
      typeDefineList,
      defineQueryForm,
      defineDialogVisible,
      defineMode,
      defineForm,
      changeDefineQueryFlag,
      loadTypeDefineRows,
      openAddDefine,
      openModifyDefine,
      saveDefine,
      removeDefine,
      mapPagination,
      typeMapList,
      mapQueryForm,
      allTypeDefineOptions,
      mapDialogVisible,
      mapMode,
      mapForm,
      changeMapQueryFlag,
      loadTypeMapRows,
      openAddMap,
      openModifyMap,
      saveMap,
      removeMap,
      typeDefineLabel,
      typeDefineLabelText,
      queryFlagOptions,
      typeCategoryOptions,
      matchLevelOptions
    } = useColumnTypeConfig()

    const activeTab = ref('define')

    // 类型映射查询标志（源/目标数据库类型）
    const mapQueryFlagOptions = [
      { label: '全部', value: '1' },
      { label: '源数据库类型', value: '2' },
      { label: '目标数据库类型', value: '3' }
    ]

    const typeCategoryLabel = (cat) => {
      const item = typeCategoryOptions.find(o => o.value === cat)
      return item ? item.label : cat
    }

    const matchLevelLabel = (level) => {
      const item = matchLevelOptions.find(o => o.value === level)
      return item ? item.label : level
    }

    const matchLevelType = (level) => {
      if (level === 1) return 'success'
      if (level === 2) return 'warning'
      return 'danger'
    }

    const handleDefinePageChange = (page) => {
      definePagination.pageNum.value = page
      loadTypeDefineRows()
    }

    const handleDefineSizeChange = (size) => {
      definePagination.pageSize.value = size
      definePagination.pageNum.value = 1
      loadTypeDefineRows()
    }

    const handleMapPageChange = (page) => {
      mapPagination.pageNum.value = page
      loadTypeMapRows()
    }

    const handleMapSizeChange = (size) => {
      mapPagination.pageSize.value = size
      mapPagination.pageNum.value = 1
      loadTypeMapRows()
    }

    onMounted(() => {
      changeDefineQueryFlag()
      loadTypeMapRows()
    })

    onActivated(() => {
      defineDialogVisible.value = false
      mapDialogVisible.value = false
    })

    return {
      activeTab,
      loading,
      definePagination,
      typeDefineList,
      defineQueryForm,
      defineDialogVisible,
      defineMode,
      defineForm,
      changeDefineQueryFlag,
      loadTypeDefineRows,
      openAddDefine,
      openModifyDefine,
      saveDefine,
      removeDefine,
      mapPagination,
      typeMapList,
      mapQueryForm,
      allTypeDefineOptions,
      mapDialogVisible,
      mapMode,
      mapForm,
      changeMapQueryFlag,
      loadTypeMapRows,
      openAddMap,
      openModifyMap,
      saveMap,
      removeMap,
      typeDefineLabel,
      typeDefineLabelText,
      queryFlagOptions,
      mapQueryFlagOptions,
      typeCategoryOptions,
      matchLevelOptions,
      typeCategoryLabel,
      matchLevelLabel,
      matchLevelType,
      handleDefinePageChange,
      handleDefineSizeChange,
      handleMapPageChange,
      handleMapSizeChange
    }
  }
}
</script>

<style scoped>
.search-nav {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
}

.ml-20 {
  margin-left: 20px;
}

.mt-10 {
  margin-top: 10px;
}

.pl-20 {
  padding-left: 20px;
}

.pr-20 {
  padding-right: 20px;
}

.datasource-opt {
  display: flex;
  justify-content: space-around;
}

.warning-text {
  color: #e6a23c;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>