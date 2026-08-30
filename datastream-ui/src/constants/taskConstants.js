/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 任务相关常量配置
 */

// 任务类型映射
export const TASK_TYPE_MAP = {
  1: '数据迁移',
  2: '数据清理',
  3: '迁移清理',
  4: '结构迁移',
  5: '数据稽核',
  6: '增量迁移'
}

// 任务类型与数据权限编码映射（与后端 PermissionService.TASK_TYPE_PERMISSION_CODE 保持一致）
export const TASK_TYPE_PERMISSION_MAP = {
  '1': 'task:type:migrate',
  '2': 'task:type:clean',
  '3': 'task:type:migrate-clean',
  '4': 'task:type:structure',
  '5': 'task:type:data-check',
  '6': 'task:type:cdc'
}

// 任务类型选项
export const TASK_TYPE_OPTIONS = [
  { label: '数据迁移', value: '1', permission: 'task:type:migrate' },
  { label: '数据清理', value: '2', permission: 'task:type:clean' },
  { label: '迁移清理', value: '3', permission: 'task:type:migrate-clean' },
  { label: '结构迁移', value: '4', permission: 'task:type:structure' },
  { label: '数据稽核', value: '5', permission: 'task:type:data-check' },
  { label: '增量迁移', value: '6', permission: 'task:type:cdc' }
]

// 任务状态映射
export const TASK_STATE_MAP = {
  0: { text: '等待中', color: '#909399' },
  1: { text: '运行中', color: '#67C23A', animate: true },
  2: { text: '运行结束', color: '#909399' },
  3: { text: '运行失败', color: '#F56C6C' },
  4: { text: '运行暂停', color: '#2563EB' }
}

// 任务状态选项
export const TASK_STATE_OPTIONS = [
  { label: '等待运行', value: '0' },
  { label: '运行中', value: '1' },
  { label: '运行结束', value: '2' },
  { label: '运行失败', value: '3' },
  { label: '运行暂停', value: '4' }
]

// 查询标志选项
export const QUERY_FLAG_OPTIONS = [
  { label: '任务标识', value: '1' },
  { label: '迁移表名', value: '2' },
  { label: '任务状态', value: '3' },
  { label: '迁移时间', value: '4' },
  { label: '复制任务标识', value: '6' },
  { label: '任务类型', value: '7' }
]

// 数据源类型描述
export const DATASOURCE_TYPE_DESC = {
  1: 'ShardingDB',
  2: 'MySQL',
  3: 'Oracle',
  4: 'PostgreSQL',
  5: 'Doris',
  6: 'Mdb',
  7: 'H2',
  8: 'Text',
  9: 'Excel',
  10: 'Kafka',
  15: '达梦'
}

// 数据源加载策略描述
export const SOURCE_LOAD_STRATEGY_DESC = {
  1: '分页加载',
  2: '分段加载'
}

// 同步对象描述
export const SOURCE_DEBEZIUM_OBJECT_DESC = {
  1: '表数据',
  2: '表结构',
  3: '表结构和数据'
}

// Offset存储方式描述
export const SOURCE_OFFSET_STORAGE_DESC = {
  1: '写入元数据库',
  2: '写入文件',
  3: '写入Kafka'
}

export const SOURCE_DEBEZIUM_SNAPSHOT_DESC = {
  0: '不执行',
  1: '执行',
}

// 数据库处理对象类型
export const SOURCE_DATABASE_OBJECT_TYPE_DESC = {
  1: '表对象',
  2: 'schema对象',
}

export const TARGET_CHECK_FLAG_DESC = {
  1: '校验',
  2: '不校验',
}

// 目标插入模式
export const TARGET_INSERT_MODE_OPTIONS = [
  { label: '静态方式', value: 1 },
  { label: '变量方式', value: 2 },
  { label: '自动调度', value: 3 }
]

// 稽核模式
export const CHECK_MODE_OPTIONS = [
  { label: '正向模式', value: 1 },
  { label: '双向模式', value: 2 }
]

// 日期快捷选项
export const DATE_PICKER_SHORTCUTS = [
  {
    text: '今天',
    value: () => {
      const start = new Date()
      start.setHours(0, 0, 0, 0)
      const end = new Date()
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  },
  {
    text: '昨天',
    value: () => {
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24)
      start.setHours(0, 0, 0, 0)
      const end = new Date()
      end.setTime(end.getTime() - 3600 * 1000 * 24)
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  },
  {
    text: '最近一周',
    value: () => {
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      start.setHours(0, 0, 0, 0)
      const end = new Date()
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  }
]

// 进度条颜色配置
export const PROGRESS_COLORS = [
  { color: '#99FFFF', percentage: 20 },
  { color: '#99FF00', percentage: 40 },
  { color: '#66FF99', percentage: 60 },
  { color: '#33FF99', percentage: 70 },
  { color: '#66FF99', percentage: 80 },
  { color: '#00FF99', percentage: 90 },
  { color: '#00CC00', percentage: 100 }
]

// 获取任务类型名称
export const getTaskTypeName = (type) => {
  return TASK_TYPE_MAP[type] || '未知任务'
}

// 获取任务状态信息
export const getTaskStateInfo = (state) => {
  return TASK_STATE_MAP[state] || { text: '未知状态', color: '#ffa07a' }
}

// 获取数据源类型名称
export const getDatasourceTypeName = (type) => {
  return DATASOURCE_TYPE_DESC[type] || '未知'
}

