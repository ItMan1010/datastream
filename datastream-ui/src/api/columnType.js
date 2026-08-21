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
 * 分页查询类型定义
 * @param {Object} params - 查询参数（queryFlag/queryValue/page/count）
 * @returns {Promise}
 */
export function queryTypeDefineRows(params) {
  return http(constant.QUERY_TYPE_DEFINE_ROWS, 'post', params)
}

/**
 * 查询全部类型定义（用于映射下拉选项）
 * @param {Object} params - 空参数
 * @returns {Promise}
 */
export function queryAllTypeDefine(params) {
  return http(constant.QUERY_ALL_TYPE_DEFINE, 'post', params)
}

/**
 * 查询类型定义详情
 * @param {number} columnTypeDefineId - 类型定义ID
 * @returns {Promise}
 */
export function queryTypeDefineInfo(columnTypeDefineId) {
  return http(constant.QUERY_TYPE_DEFINE_INFO, 'post', { columnTypeDefineId })
}

/**
 * 新增类型定义
 * @param {Object} typeDefine - 类型定义实体
 * @returns {Promise}
 */
export function addTypeDefine(typeDefine) {
  return http(constant.ADD_TYPE_DEFINE, 'post', { typeDefine })
}

/**
 * 修改类型定义
 * @param {Object} typeDefine - 类型定义实体
 * @returns {Promise}
 */
export function modifyTypeDefine(typeDefine) {
  return http(constant.MODIFY_TYPE_DEFINE, 'post', { typeDefine })
}

/**
 * 删除类型定义
 * @param {number} columnTypeDefineId - 类型定义ID
 * @returns {Promise}
 */
export function delTypeDefine(columnTypeDefineId) {
  return http(constant.DEL_TYPE_DEFINE, 'post', { columnTypeDefineId })
}

/**
 * 分页查询类型映射
 * @param {Object} params - 查询参数（queryFlag/queryValue/page/count）
 * @returns {Promise}
 */
export function queryTypeMapRows(params) {
  return http(constant.QUERY_TYPE_MAP_ROWS, 'post', params)
}

/**
 * 查询类型映射详情
 * @param {number} columnTypeMapId - 类型映射ID
 * @returns {Promise}
 */
export function queryTypeMapInfo(columnTypeMapId) {
  return http(constant.QUERY_TYPE_MAP_INFO, 'post', { columnTypeMapId })
}

/**
 * 新增类型映射
 * @param {Object} typeMap - 类型映射实体
 * @returns {Promise}
 */
export function addTypeMap(typeMap) {
  return http(constant.ADD_TYPE_MAP, 'post', { typeMap })
}

/**
 * 修改类型映射
 * @param {Object} typeMap - 类型映射实体
 * @returns {Promise}
 */
export function modifyTypeMap(typeMap) {
  return http(constant.MODIFY_TYPE_MAP, 'post', { typeMap })
}

/**
 * 删除类型映射
 * @param {number} columnTypeMapId - 类型映射ID
 * @returns {Promise}
 */
export function delTypeMap(columnTypeMapId) {
  return http(constant.DEL_TYPE_MAP, 'post', { columnTypeMapId })
}