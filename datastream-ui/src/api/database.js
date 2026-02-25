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
import constant from '@/comm/constants'

/**
 * 查询数据库列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function queryDataBaseRows(params) {
  return http(constant.QUERY_DATA_BASE_ROWS, 'post', params)
}

/**
 * 新增数据库
 * @param {Object} params - 数据库信息
 * @returns {Promise}
 */
export function addDataBase(params) {
  return http(constant.ADD_DATA_BASE, 'post', params)
}

/**
 * 修改数据库
 * @param {Object} params - 数据库信息
 * @returns {Promise}
 */
export function modifyDataBase(params) {
  return http(constant.MODIFY_DATA_BASE, 'post', params)
}

/**
 * 删除数据库
 * @param {string} dataBaseId - 数据库ID
 * @returns {Promise}
 */
export function delDataBase(dataBaseId) {
  return http(constant.DEL_DATA_BASE, 'post', { dataBaseId })
}

/**
 * 启用/禁用数据库
 * @param {string} dataBaseId - 数据库ID
 * @param {string} state - 状态
 * @returns {Promise}
 */
export function onOffDataBase(dataBaseId, state) {
  return http(constant.ON_OFF_DATA_BASE, 'post', { dataBaseId, state })
}

/**
 * 测试数据库连接
 * @param {Object} params - 连接参数
 * @returns {Promise}
 */
export function testDataBase(params) {
  return http(constant.TEST_DATA_BASE, 'post', params)
}

