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
import { createRouter, createWebHashHistory } from 'vue-router'
import request from '@/utils/request'
import constants from '@/comm/constants'
import { useMainStore } from '../store'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: 'login',
      path: '/',
      component: () => import('@/views/LoginView.vue')
    },
    {
      name: 'homePage',
      path: '/datastream/homePage',
      // 使用重构后的布局组件
      component: () => import('@/views/layout/index.vue'),
      children: [
        // 默认重定向到概览页面
        {
          path: '',
          redirect: '/datastream/overview'
        },
        // 概述 - 使用重构后的组件
        {
          name: 'overview',
          path: '/datastream/overview',
          component: () => import('@/views/overview/index.vue')
        },
        // 任务查询 - 使用重构后的组件
        {
          name: 'taskManage',
          path: '/datastream/task/TaskManage',
          component: () => import('@/views/taskmanage/dataMoveTask.vue')
        },
        // 任务执行明细
        {
          name: 'dataMoveInfoList',
          path: '/datastream/task/dataMoveInfoList',
          component: () => import('@/views/taskmanage/components/TaskMoveInfoList.vue')
        },
        // 链接数据任务生成
        {
          name: 'tableLinkTask',
          path: '/datastream/task/tableLinkTask',
          component: () => import('@/views/taskmanage/LinkMoveTask.vue')
        },
        // 表结构迁移管理 - 使用重构后的组件
        {
          name: 'tableManage',
          path: '/datastream/task/tableManage',
          component: () => import('@/views/taskmanage/TableMoveTask.vue')
        },
        // 数据检索
        {
          name: 'DataSearch',
          path: '/datastream/DataSearch',
          component: () => import('@/views/DataSearch.vue')
        },
        // 数据源配置 - 使用重构后的组件
        {
          name: 'dataBaseConfig',
          path: '/datastream/config/dataBaseConfig',
          component: () => import('@/views/database/index.vue')
        },
        // 表链接配置 - 使用重构后的组件
        {
          name: 'tableLinkConfig',
          path: '/datastream/config/tableLinkConfig',
          component: () => import('@/views/tablelink/index.vue')
        },
        // 文件配置 - 使用重构后的组件
        {
          name: 'fileFormatConfig',
          path: '/datastream/config/fileFormatConfig',
          component: () => import('@/views/fileformat/index.vue')
        },
        // Mq配置
        {
          name: 'mqConfig',
          path: '/datastream/config/mqConfig',
          component: () => import('@/views/mq-manage/index.vue')
        },
        // 字段类型配置
        {
          name: 'columnTypeConfig',
          path: '/datastream/config/columnTypeConfig',
          component: () => import('@/views/column-type/index.vue')
        },
        // 登录日志
        {
          name: 'loginLogs',
          path: '/datastream/auditLog/loginLogs',
          component: () => import('@/views/system-manage/LoginLogs.vue')
        },
        // 操作日志
        {
          name: 'operationLogs',
          path: '/datastream/auditLog/operationLogs',
          component: () => import('@/views/system-manage/OperationLogs.vue')
        },
        // H2管理
        {
          name: 'h2Manage',
          path: '/datastream/h2Manage',
          component: () => import('@/views/system-manage/H2Manage.vue')
        },
        // 关于系统
        {
          name: 'aboutTheSystem',
          path: '/datastream/aboutTheSystem',
          component: () => import('@/views/system-manage/AboutSystem.vue')
        },
        // 资源监控
        {
          name: 'resourceMonitor',
          path: '/datastream/resource-monitor',
          component: () => import('@/views/resource-monitor/index.vue')
        },
      ]
    }
  ]
})

// 全局前置路由，对包含token的路由进行登录认证
router.beforeEach((to, from, next) => {
  // 路径包含登录串，进行登录操作
  if (to.fullPath && to.fullPath.includes('?token=')) {
    let params = to.fullPath.substring(to.fullPath.indexOf('?'), to.fullPath.length)
    request(constants.URL_AUTH_LOGIN + params, 'post', null).then(res => {
      // 增加登陆信息
      const store = useMainStore()
      store.setLoginSystemUser(res.data)
    }).catch(err => {
      alert(`登陆失败：${err.resultMsg}`)
    })
  }
  next()
})

export default router
