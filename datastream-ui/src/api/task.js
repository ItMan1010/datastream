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
 * 查询数据迁移任务列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function queryDataMoveTaskList(params) {
  return http(constant.QUERY_DATA_MOVE_TASK_LIST, 'post', params)
}

/**
 * 创建数据迁移任务
 * @param {Object} params - 任务参数
 * @returns {Promise}
 */
export function createMoveTask(params) {
  return http(constant.CREATE_MOVE_TASK, 'post', params)
}

/**
 * 查询任务进度
 * @param {string} taskId - 任务ID
 * @returns {Promise}
 */
export function queryTaskProgress(taskId) {
  return http(constant.QUERY_TASK_PROGRESS, 'post', { taskId })
}

/**
 * 查询任务执行明细
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function queryDataMoveInfoList(params) {
  return http(constant.QUERY_DATA_MOVE_INFO_LIST, 'post', params)
}

/**
 * 查询表结构迁移明细
 * @param {string} taskId - 任务ID
 * @returns {Promise}
 */
export function queryTableMoveList(taskId) {
  return http(constant.QUERY_TABLE_MOVE_LIST, 'post', { taskId })
}

/**
 * 操作数据迁移任务（启动、停止、暂停、重启等）
 * @param {string} taskId - 任务ID
 * @param {string} operate - 操作类型
 * @returns {Promise}
 */
export function operateDataMoveTask(taskId, operate) {
  return http(constant.OPERATE_DATA_MOVE_TASK, 'post', { taskId, operate })
}

/**
 * 查询批量任务
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function queryBatchTask(params) {
  return http(constant.QUERY_BATCH_TASK, 'post', params)
}

/**
 * 创建批量任务
 * @param {Object} params - 任务参数
 * @returns {Promise}
 */
export function createBatchTask(params) {
  return http(constant.CREATE_BATCH_TASK, 'post', params)
}
