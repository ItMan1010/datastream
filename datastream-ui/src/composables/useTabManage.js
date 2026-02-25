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
 * Tab管理 Composable
 * 提取Tab相关的所有逻辑
 */
import { ref, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * Tab管理Hook
 * @param {Object} options - 配置选项
 * @param {Array} options.menuNameArr - 菜单名称数组
 * @param {Array} options.menuDescArr - 菜单描述数组
 * @param {Array} options.menuIconArr - 菜单图标数组
 * @param {Object} options.crumbArr - 面包屑配置
 * @returns {Object} Tab管理相关状态和方法
 */
export function useTabManage(options = {}) {
  const route = useRoute()
  const router = useRouter()

  const {
    menuNameArr = [],
    menuDescArr = [],
    menuIconArr = [],
    crumbArr = {}
  } = options

  // 状态
  const activeTab = ref('')
  const tabs = ref([])
  const activeMenuIndex = ref('5')
  const breadcrumb = ref([])

  // 计算属性：Tab显示
  const tabsDisplay = computed(() => {
    try {
      return tabs.value.map(t => t.name).join(', ')
    } catch (e) {
      return '无法显示tabs'
    }
  })

  /**
   * 添加Tab
   * @param {string} name - Tab名称（路由名称）
   * @param {string} title - Tab标题
   * @param {string} icon - Tab图标
   */
  const addTab = (name, title, icon) => {
    // 检查tab是否已存在
    const existingTab = tabs.value.find(tab => tab.name === name)
    if (existingTab) {
      activeTab.value = name
      return
    }

    const newTab = {
      name,
      title,
      icon,
      closable: true
    }

    tabs.value.push(newTab)
    activeTab.value = name

    // 更新closable状态
    updateTabClosable()
  }

  /**
   * 移除Tab
   * @param {string} targetName - 要移除的Tab名称
   */
  const removeTab = (targetName) => {
    let activeName = activeTab.value

    if (activeName === targetName) {
      tabs.value.forEach((tab, index) => {
        if (tab.name === targetName) {
          const nextTab = tabs.value[index + 1] || tabs.value[index - 1]
          if (nextTab) {
            activeName = nextTab.name
          }
        }
      })
    }

    activeTab.value = activeName
    tabs.value = tabs.value.filter(tab => tab.name !== targetName)

    // 如果还有tab，跳转到当前激活的tab
    if (tabs.value.length > 0) {
      const activeTabObj = tabs.value.find(tab => tab.name === activeTab.value)
      if (activeTabObj) {
        router.push({ name: activeTabObj.name })
      }
    }

    // 更新closable状态
    updateTabClosable()
  }

  /**
   * 更新Tab的closable状态
   * 如果只有一个Tab，则不可关闭
   */
  const updateTabClosable = () => {
    if (tabs.value.length === 1) {
      tabs.value[0].closable = false
    } else {
      tabs.value.forEach(tab => {
        tab.closable = true
      })
    }
  }

  /**
   * 处理Tab点击
   * @param {Object} tab - Element Plus tab-pane对象
   * @param {Event} ev - 事件对象
   */
  const handleTabClick = (tab, ev) => {
    const tabName = tab?.name || tab?.props?.name || tab
    if (!tabName) return

    // 如果点击的是当前激活的tab，且路由也匹配，直接返回
    if (activeTab.value === tabName && route.name === tabName) {
      return
    }

    // 立即更新activeTab状态
    activeTab.value = tabName

    // 更新菜单相关状态
    updateMenuState(tabName)

    // 执行路由跳转
    router.push({ name: tabName }).catch(err => {
      // 回滚状态
      const currentRoute = route.name
      if (currentRoute) {
        activeTab.value = currentRoute
      }
    })
  }

  /**
   * 更新菜单状态（菜单高亮和面包屑）
   * @param {string} tabName - Tab名称
   */
  const updateMenuState = (tabName) => {
    const tabIndex = menuNameArr.indexOf(tabName)
    if (tabIndex > 0) {
      activeMenuIndex.value = tabIndex.toString()
      // 更新面包屑
      breadcrumb.value = []
      const indexPath = crumbArr[tabIndex] || [tabIndex]
      indexPath.forEach(idx => {
        breadcrumb.value.push(menuDescArr[idx])
      })
    }
  }

  /**
   * 强制同步Tab与路由状态
   * @param {string} routeName - 路由名称
   */
  const forceSyncTabWithRoute = (routeName) => {
    const menuIndex = menuNameArr.indexOf(routeName)
    if (menuIndex > 0) {
      const title = menuDescArr[menuIndex]
      const icon = menuIconArr[menuIndex]
      addTab(routeName, title, icon)
    } else {
      activeTab.value = routeName
    }
  }

  /**
   * 处理菜单选择
   * @param {string} index - 菜单索引
   * @param {Array} indexPath - 索引路径
   */
  const handleMenuSelect = (index, indexPath) => {
    activeMenuIndex.value = index

    // 更新面包屑
    breadcrumb.value = []
    indexPath.forEach(idx => {
      breadcrumb.value.push(menuDescArr[idx])
    })

    const name = menuNameArr[activeMenuIndex.value]
    const title = menuDescArr[activeMenuIndex.value]
    const icon = menuIconArr[activeMenuIndex.value]

    if (name) {
      // 检查tab是否已存在
      const existingTab = tabs.value.find(tab => tab.name === name)
      if (existingTab) {
        activeTab.value = name
      } else {
        addTab(name, title, icon)
      }

      // 跳转路由
      nextTick(() => {
        router.push({ name }).then(() => {
          activeTab.value = name
        }).catch(err => {
          console.error('路由跳转失败:', err)
        })
      })
    }
  }

  /**
   * 快速跳转页面
   * @param {string} pageNameIndex - 页面索引
   */
  const gotoPage = (pageNameIndex) => {
    handleMenuSelect(pageNameIndex, crumbArr[pageNameIndex] || [pageNameIndex])
  }

  /**
   * 初始化Tab系统
   * @param {string} defaultTab - 默认Tab名称
   * @param {string} defaultTitle - 默认Tab标题
   * @param {string} defaultIcon - 默认Tab图标
   */
  const initTabs = (defaultTab = 'overview', defaultTitle = '系统概览', defaultIcon = 'Menu') => {
    addTab(defaultTab, defaultTitle, defaultIcon)
    activeTab.value = defaultTab
  }

  /**
   * 设置路由监听
   */
  const setupRouteWatcher = () => {
    watch(() => route.name, (newRouteName, oldRouteName) => {
      if (!newRouteName) return

      nextTick(() => {
        const existingTab = tabs.value.find(tab => tab.name === newRouteName)
        if (existingTab) {
          activeTab.value = newRouteName
          updateMenuState(newRouteName)
        } else {
          forceSyncTabWithRoute(newRouteName)
        }
      })
    })
  }

  /**
   * 初始化检查
   * 确保当前路由与Tab状态同步
   */
  const checkInitialSync = () => {
    if (route.name && tabs.value.find(tab => tab.name === route.name)) {
      activeTab.value = route.name
      const tabIndex = menuNameArr.indexOf(route.name)
      if (tabIndex > 0) {
        activeMenuIndex.value = tabIndex.toString()
      }
    } else {
      activeTab.value = 'overview'
      handleMenuSelect(activeMenuIndex.value, ['5'])
    }

    // 再次检查状态同步
    nextTick(() => {
      if (activeTab.value !== route.name && route.name) {
        forceSyncTabWithRoute(route.name)
      }
    })
  }

  return {
    // 状态
    activeTab,
    tabs,
    activeMenuIndex,
    breadcrumb,
    tabsDisplay,

    // 方法
    addTab,
    removeTab,
    handleTabClick,
    handleMenuSelect,
    gotoPage,
    initTabs,
    setupRouteWatcher,
    checkInitialSync,
    forceSyncTabWithRoute,
    updateMenuState
  }
}

