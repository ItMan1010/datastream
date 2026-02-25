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
 * 文件配置相关常量
 */

// 文件类型映射
export const FILE_TYPE_MAP = {
  8: 'text',
  9: 'excel'
}

// 文件类型选项
export const FILE_TYPE_OPTIONS = [
  { label: '全部', value: '0' },
  { label: 'text', value: '8' },
  { label: 'excel', value: '9' }
]

// 文件名类型映射
export const FILE_NAME_TYPE_MAP = {
  1: '固定值',
  2: '正则表达',
  3: '自行扩展'
}

// 文件上线状态映射
export const FILE_STATE_MAP = {
  0: { text: '删除', type: 'info' },
  1: { text: '下线', type: 'info' },
  2: { text: '上线', type: 'success' }
}

// 获取文件类型名称
export const getFileTypeName = (type) => {
  return FILE_TYPE_MAP[type] || '其他'
}

// 获取文件名类型名称
export const getFileNameTypeName = (type) => {
  return FILE_NAME_TYPE_MAP[type] || '其他'
}

// 获取文件状态信息
export const getFileStateInfo = (state) => {
  return FILE_STATE_MAP[state] || { text: '未知', type: 'info' }
}

