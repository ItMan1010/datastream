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
 * 查询系统日志
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function querySystemLog(params) {
  return http(constant.QUERY_SYSTEM_LOG, 'post', params)
}

/**
 * 查询任务日志
 * @param {Object} params - 查询参数 { jobType, jobId }
 * @returns {Promise}
 */
export function queryJobLog(params) {
  return http(constant.QUERY_JOB_BACK, 'post', params)
}

