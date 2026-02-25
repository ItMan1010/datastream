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
 * 查询表链接列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function queryTableLink(params) {
  return http(constant.QUERY_TABLE_LINK, 'post', params)
}

/**
 * 查询表链接详情
 * @param {string} tableLinkId - 表链接ID
 * @returns {Promise}
 */
export function queryLinkDetail(tableLinkId) {
  return http(constant.QUERY_LINK_DETAIL, 'post', { tableLinkId })
}

/**
 * 新增表链接
 * @param {Object} params - 表链接信息
 * @returns {Promise}
 */
export function addTableLink(params) {
  return http(constant.ADD_TABLE_LINK, 'post', params)
}

/**
 * 修改表链接
 * @param {Object} params - 表链接信息
 * @returns {Promise}
 */
export function modifyTableLink(params) {
  return http(constant.MODIFY_TABLE_LINK, 'post', params)
}

/**
 * 删除表链接
 * @param {string} tableLinkId - 表链接ID
 * @returns {Promise}
 */
export function delTableLink(tableLinkId) {
  return http(constant.DEL_TABLE_LINK, 'post', { tableLinkId })
}

/**
 * 启用/禁用表链接
 * @param {string} tableLinkId - 表链接ID
 * @param {string} state - 状态
 * @returns {Promise}
 */
export function onOffTableLink(tableLinkId, state) {
  return http(constant.ON_OFF_TABLE_LINK, 'post', { tableLinkId, state })
}

/**
 * 测试表链接
 * @param {Object} params - 测试参数
 * @returns {Promise}
 */
export function testTableLink(params) {
  return http(constant.TEST_TABLE_LINK, 'post', params)
}

/**
 * 查询表链接任务列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function queryTableLinkTaskList(params) {
  return http(constant.QUERY_TABLE_LINK_TASK_LIST, 'post', params)
}

/**
 * 创建表链接任务
 * @param {Object} params - 任务参数
 * @returns {Promise}
 */
export function createTableLinkTask(params) {
  return http(constant.CREATE_TABLE_LINK_TASK, 'post', params)
}

/**
 * 操作表链接任务
 * @param {Object} params - 操作参数
 * @returns {Promise}
 */
export function operateDataBackTask(params) {
  return http(constant.OPERATE_TABLE_LINK_TASK, 'post', params)
}

