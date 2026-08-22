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
 * API端点配置
 * 统一管理所有后端API路径
 */

export default {
  // ============ 认证相关 ============
  URL_AUTH_LOGIN: '/api/auth/login',
  URL_AUTH_LOGOUT: '/api/auth/logout',

  // ============ 任务管理 ============
  QUERY_DATA_MOVE_TASK_LIST: '/api/task/queryDataMoveTaskList',
  CREATE_MOVE_TASK: '/api/task/createMoveTask',
  QUERY_TASK_PROGRESS: '/api/task/queryTaskProgress',
  QUERY_DATA_MOVE_INFO_LIST: '/api/task/queryDataMoveInfoList',
  QUERY_TABLE_MOVE_LIST: '/api/task/queryTableMoveList',
  OPERATE_DATA_MOVE_TASK: '/api/task/operateDataMoveTask',
  QUERY_BATCH_TASK: '/api/task/queryBatchTask',
  CREATE_BATCH_TASK: '/api/task/createBatchTask',

  // ============ 表链接任务 ============
  QUERY_TABLE_LINK_TASK_LIST: '/api/task/queryTableLinkTaskList',
  CREATE_TABLE_LINK_TASK: '/api/task/createTableLinkTask',
  OPERATE_TABLE_LINK_TASK: '/api/task/operateTableLinkTask',

  // ============ 数据库配置 ============
  QUERY_DATA_BASE_ROWS: '/api/database/queryDataBaseRows',
  ADD_DATA_BASE: '/api/database/addDataBase',
  MODIFY_DATA_BASE: '/api/database/modifyDataBase',
  DEL_DATA_BASE: '/api/database/delDataBase',
  ON_OFF_DATA_BASE: '/api/database/onOffDataBase',
  TEST_DATA_BASE: '/api/database/testDataBase',

  // ============ 表链接配置 ============
  QUERY_TABLE_LINK: '/api/link/queryTableLink',
  ON_OFF_TABLE_LINK: '/api/link/onOffTableLink',
  DEL_TABLE_LINK: '/api/link/delTableLink',
  TEST_TABLE_LINK: '/api/link/testTableLink',
  ADD_TABLE_LINK: '/api/link/addTableLink',
  MODIFY_TABLE_LINK: '/api/link/modifyTableLink',
  QUERY_LINK_DETAIL: '/api/link/queryLinkDetail',

  // ============ 文件配置 ============
  QUERY_FILE_ROWS: '/api/file/queryFileRows',
  QUERY_FILE_INFO: '/api/file/queryFileInfo',
  OPERATE_FILE_ROW: '/api/file/operateFileRows',
  ADD_FILE_FORMAT: '/api/file/addFileFormat',
  MODIFY_FILE_FORMAT: '/api/file/modifyFileFormat',

  // ============ Mq配置 ============
  QUERY_MQ_ROWS: '/api/mq/queryMqRows',
  QUERY_MQ_INFO: '/api/mq/queryMqInfo',
  ADD_MQ_CONFIG: '/api/mq/addMqConfig',
  MODIFY_MQ_CONFIG: '/api/mq/modifyMqConfig',
  DEL_MQ_CONFIG: '/api/mq/delMqConfig',
  TEST_MQ_CONFIG: '/api/mq/testMqConfig',
  OPERATE_MQ_CONFIG: '/api/mq/operateMqConfig',

  // ============ 日志相关 ============
  QUERY_SYSTEM_LOG: '/api/log/querySystemLog',
  QUERY_JOB_BACK: '/api/log/queryJobLogback',

  // ============ 统计相关 ============
  STAT_SYSTEM_INFO: '/api/stat/statSystemInfo',

  // ============ 数据检索 ============
  DATA_SEARCH: '/api/search/dataSearch',

  // ============ 数据稽核 ============
  QUERY_DATA_CHECK: '/api/check/queryDataCheckList',
  REPAIR_DATA_CHECK: '/api/check/repairDataCheck',

  // ============ 指标监控 ============
  MONITOR_TASK_RUNNING_QUEUE: '/api/metrics/monitorTaskRunningQueue',

  // ============ 资源监控 ============
  RESOURCE_METRICS_ALL: '/api/metrics/resource/all',
  RESOURCE_METRICS_SYSTEM: '/api/metrics/resource/system',
  RESOURCE_METRICS_TASK: '/api/metrics/resource/task',
  RESOURCE_METRICS_CONNECTION: '/api/metrics/resource/connection',
  RESOURCE_METRICS_THREADPOOL: '/api/metrics/resource/threadpool',

  // ============ 字段类型配置 ============
  QUERY_TYPE_DEFINE_ROWS: '/api/columnTypeConfig/queryTypeDefineRows',
  QUERY_ALL_TYPE_DEFINE: '/api/columnTypeConfig/queryAllTypeDefine',
  QUERY_TYPE_DEFINE_INFO: '/api/columnTypeConfig/queryTypeDefineInfo',
  ADD_TYPE_DEFINE: '/api/columnTypeConfig/addTypeDefine',
  MODIFY_TYPE_DEFINE: '/api/columnTypeConfig/modifyTypeDefine',
  DEL_TYPE_DEFINE: '/api/columnTypeConfig/delTypeDefine',
  QUERY_TYPE_MAP_ROWS: '/api/columnTypeConfig/queryTypeMapRows',
  QUERY_TYPE_MAP_INFO: '/api/columnTypeConfig/queryTypeMapInfo',
  ADD_TYPE_MAP: '/api/columnTypeConfig/addTypeMap',
  MODIFY_TYPE_MAP: '/api/columnTypeConfig/modifyTypeMap',
  DEL_TYPE_MAP: '/api/columnTypeConfig/delTypeMap',

  // ============ 权限管理（RBAC） ============
  QUERY_USER_ROWS: '/api/systemUser/queryUserRows',
  QUERY_USER_INFO: '/api/systemUser/queryUserInfo',
  ADD_USER: '/api/systemUser/addUser',
  MODIFY_USER: '/api/systemUser/modifyUser',
  UPDATE_USER_STATE: '/api/systemUser/updateUserState',
  RESET_USER_PASSWORD: '/api/systemUser/resetPassword',
  DEL_USER: '/api/systemUser/delUser',
  QUERY_ROLE_ROWS: '/api/role/queryRoleRows',
  QUERY_ALL_ROLE: '/api/role/queryAllRole',
  QUERY_ROLE_INFO: '/api/role/queryRoleInfo',
  ADD_ROLE: '/api/role/addRole',
  MODIFY_ROLE: '/api/role/modifyRole',
  DEL_ROLE: '/api/role/delRole',
  ASSIGN_ROLE_PERMISSIONS: '/api/role/assignRolePermissions',
  QUERY_MENU_TREE: '/api/permission/queryMenuTree',
  QUERY_DATA_PERMISSION_LIST: '/api/permission/queryDataPermissionList',
  ADD_PERMISSION: '/api/permission/addPermission',
  MODIFY_PERMISSION: '/api/permission/modifyPermission',
  DEL_PERMISSION: '/api/permission/delPermission',

  // ============ 其他配置 ============
  ENCRYPT_KEY: '1234567812345678'
}

