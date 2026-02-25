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
 * 获取所有资源指标
 * @returns {Promise}
 */
export function getAllResourceMetrics() {
  return http(constant.RESOURCE_METRICS_ALL, 'post', {})
}

/**
 * 获取系统级资源指标
 * @returns {Promise}
 */
export function getSystemResourceMetrics() {
  return http(constant.RESOURCE_METRICS_SYSTEM, 'post', {})
}

/**
 * 获取任务级资源指标
 * @returns {Promise}
 */
export function getTaskResourceMetrics() {
  return http(constant.RESOURCE_METRICS_TASK, 'post', {})
}

/**
 * 获取连接池资源指标
 * @returns {Promise}
 */
export function getConnectionResourceMetrics() {
  return http(constant.RESOURCE_METRICS_CONNECTION, 'post', {})
}

/**
 * 获取线程池资源指标
 * @returns {Promise}
 */
export function getThreadPoolResourceMetrics() {
  return http(constant.RESOURCE_METRICS_THREADPOOL, 'post', {})
}

