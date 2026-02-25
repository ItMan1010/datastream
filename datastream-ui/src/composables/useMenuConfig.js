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
 * 菜单配置 Composable
 * 集中管理菜单配置数据
 */
import { ref, computed } from 'vue'

/**
 * 菜单配置Hook
 * @returns {Object} 菜单配置相关数据
 */
export function useMenuConfig() {
  // 菜单名称数组（路由名称）
  // 索引: 0    1          2                3                4                   5           6           7                 8                9            10               11               12                 13          14         15             16        17              18              19
  const menuNameArr = ref([
    '',           // 0: 空
    '',           // 1: 任务管理（父菜单）
    'taskManage', // 2: 迁移任务
    'tableLinkTask', // 3: 链接任务
    'dataBaseConfig', // 4: 数据库配置
    'overview',   // 5: 系统概览
    'auditLog',   // 6: 审计日志（父菜单）
    'aboutTheSystem', // 7: 关于系统
    'batchMoveTask', // 8: 批量任务
    'loginLogs',  // 9: 登录日志
    'operationLogs', // 10: 操作日志
    'DataSearch', // 11: 数据检索
    'tableLinkConfig', // 12: 表链接配置
    'h2Manage',   // 13: H2管理
    '',           // 14: 配置管理（父菜单）
    'tableManage', // 15: 结构迁移
    '',           // 16: 系统管理（父菜单）
    'fileFormatConfig', // 17: 文件配置
    'mqConfig', // 18: MQ配置
    'resourceMonitor' // 19: 资源监控
  ])

  // 菜单描述数组
  const menuDescArr = ref([
    '',           // 0: 空
    '任务管理',   // 1
    '迁移任务',   // 2
    '链接任务',   // 3
    '数据库配置', // 4
    '系统概览',   // 5
    '审计日志',   // 6
    '关于系统',   // 7
    '批量任务',   // 8
    '登录日志',   // 9
    '操作日志',   // 10
    '数据检索',   // 11
    '表链接配置', // 12
    'H2管理',     // 13
    '配置管理',   // 14
    '结构迁移',   // 15
    '系统管理',   // 16
    '文件配置',   // 17
    'MQ配置',   // 18
    '资源监控'    // 19
  ])

  // 菜单图标数组
  const menuIconArr = ref([
    '',             // 0
    '',             // 1
    'FolderOpened', // 2
    'FolderRemove', // 3
    'DataBoard',    // 4
    'Menu',         // 5
    'Tickets',      // 6
    'ChatLineSquare', // 7
    'Notebook',     // 8
    'Tickets',      // 9
    'Document',     // 10
    'Search',       // 11
    'Tools',        // 12
    'Coin',         // 13
    '',             // 14
    'Operation',    // 15
    '',             // 16
    'Notebook',     // 17
    'Message',      // 18: MQ配置图标
    'DataAnalysis'  // 19: 资源监控图标
  ])

  // 面包屑配置
  const crumbArr = ref({
    // 系统概览
    '5': ['5'],
    // 任务管理
    '2': ['1', '2'],
    '3': ['1', '3'],
    '8': ['1', '8'],
    '15': ['1', '15'],
    // 数据检索
    '11': ['11'],
    // 配置管理
    '4': ['14', '4'],
    '12': ['14', '12'],
    '17': ['14', '17'],
    '18': ['14', '18'], // MQ配置
    // 系统管理
    '9': ['16', '9'],
    '10': ['16', '10'],
    '7': ['16', '7'],
    '13': ['16', '13'],
    // 资源监控
    '19': ['19']
  })

  // 默认展开的菜单
  const defaultOpeneds = ref(['1', '14', '15'])

  // 默认激活的菜单
  const defaultActiveIndex = ref('5')

  /**
   * 根据索引获取菜单信息
   * @param {number|string} index - 菜单索引
   * @returns {Object} 菜单信息
   */
  const getMenuInfo = (index) => {
    const idx = typeof index === 'string' ? parseInt(index) : index
    return {
      name: menuNameArr.value[idx] || '',
      desc: menuDescArr.value[idx] || '',
      icon: menuIconArr.value[idx] || ''
    }
  }

  /**
   * 根据路由名称获取菜单索引
   * @param {string} routeName - 路由名称
   * @returns {number} 菜单索引，未找到返回-1
   */
  const getMenuIndex = (routeName) => {
    return menuNameArr.value.indexOf(routeName)
  }

  /**
   * 获取面包屑路径
   * @param {number|string} index - 菜单索引
   * @returns {Array} 面包屑描述数组
   */
  const getBreadcrumbPath = (index) => {
    const idx = typeof index === 'string' ? index : index.toString()
    const path = crumbArr.value[idx] || [idx]
    return path.map(i => menuDescArr.value[i])
  }

  return {
    menuNameArr: menuNameArr.value,
    menuDescArr: menuDescArr.value,
    menuIconArr: menuIconArr.value,
    crumbArr: crumbArr.value,
    defaultOpeneds: defaultOpeneds.value,
    defaultActiveIndex: defaultActiveIndex.value,
    getMenuInfo,
    getMenuIndex,
    getBreadcrumbPath
  }
}

