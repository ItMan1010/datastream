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
import http from '@/utils/request'

/**
 * 获取表列表
 * @param {string} dataBaseId - 数据源ID
 * @returns {Promise}
 */
export function getTableList(dataBaseId) {
  return http('/api/table/tableList', 'post', { dataBaseId })
}

/**
 * 获取表详细信息
 * @param {string} dataBaseId - 数据源ID
 * @param {string} tableName - 表名
 * @returns {Promise}
 */
export function getTableDetail(dataBaseId, tableName) {
  return http('/api/table/tableDetail', 'post', { dataBaseId, tableName })
}

/**
 * 获取表列信息
 * @param {string} dataBaseId - 数据源ID
 * @param {string} tableName - 表名
 * @returns {Promise}
 */
export function getTableColumns(dataBaseId, tableName) {
  return http('/api/table/tableColumns', 'post', { dataBaseId, tableName })
}

