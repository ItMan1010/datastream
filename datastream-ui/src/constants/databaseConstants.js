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
 * 数据库相关常量配置
 */

// 数据库类型映射
export const DATABASE_TYPE_MAP = {
  1: 'H2',
  2: 'MySQL',
  3: 'Oracle',
  4: 'PostgreSQL',
  5: 'Doris'
}

// 数据库类型选项
export const DATABASE_TYPE_OPTIONS = [
  { label: 'ALL', value: '0' },
  { label: 'MySQL', value: '2' },
  { label: 'Oracle', value: '3' },
  { label: 'PostgreSQL', value: '4' },
  { label: 'Doris', value: '5' }
]

// 数据库类型颜色
export const DATABASE_TYPE_COLORS = {
  1: 'info',
  2: 'success',
  3: 'warning',
  4: 'primary',
  5: 'danger'
}

// 数据库状态映射
export const DATABASE_STATE_MAP = {
  1: { text: '下线', type: 'info' },
  2: { text: '上线', type: 'success' }
}

// 获取数据库类型名称
export const getDatabaseTypeName = (type) => {
  return DATABASE_TYPE_MAP[type] || '未知'
}

// 获取数据库类型颜色
export const getDatabaseTypeColor = (type) => {
  return DATABASE_TYPE_COLORS[type] || 'info'
}

// 获取数据库状态信息
export const getDatabaseStateInfo = (state) => {
  return DATABASE_STATE_MAP[state] || { text: '未知', type: 'info' }
}

