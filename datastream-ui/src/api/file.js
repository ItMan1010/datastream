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
 * 查询文件格式列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function queryFileRows(params) {
  return http(constant.QUERY_FILE_ROWS, 'post', params)
}

/**
 * 查询文件格式详情
 * @param {string} fileFormatId - 文件格式ID
 * @returns {Promise}
 */
export function queryFileInfo(fileFormatId) {
  return http(constant.QUERY_FILE_INFO, 'post', { fileFormatId })
}

/**
 * 新增文件格式
 * @param {Object} params - 文件格式信息
 * @returns {Promise}
 */
export function addFileFormat(params) {
  return http(constant.ADD_FILE_FORMAT, 'post', params)
}

/**
 * 修改文件格式
 * @param {Object} params - 文件格式信息
 * @returns {Promise}
 */
export function modifyFileFormat(params) {
  return http(constant.MODIFY_FILE_FORMAT, 'post', params)
}

/**
 * 操作文件格式（启用/禁用/删除）
 * @param {Object} params - 操作参数
 * @returns {Promise}
 */
export function operateFileRows(params) {
  return http(constant.OPERATE_FILE_ROW, 'post', params)
}

