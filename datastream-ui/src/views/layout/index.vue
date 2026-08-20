<!--Licensed to the Apache Software Foundation (ASF) under one or more-->
<!--contributor license agreements.  See the NOTICE file distributed with-->
<!--this work for additional information regarding copyright ownership.-->
<!--The ASF licenses this file to You under the Apache License, Version 2.0-->
<!--(the "License"); you may not use this file except in compliance with-->
<!--the License.  You may obtain a copy of the License at-->

<!--http://www.apache.org/licenses/LICENSE-2.0-->

<!--Unless required by applicable law or agreed to in writing, software-->
<!--distributed under the License is distributed on an "AS IS" BASIS,-->
<!--WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.-->
<!--See the License for the specific language governing permissions and-->
<!--limitations under the License.-->
<template>
  <el-container :class="{ 'menu-is-collapse': isCollapse, 'menu-is-not-collapse': !isCollapse }" style="height: 100%;">
    <!-- 侧边栏 -->
    <el-aside
      :class="['data-stream-menu-aside', { 'collapsed': isCollapse }]"
      :style="{ width: isCollapse ? '64px' : '240px' }">

      <!-- Logo区域 -->
      <div class="app-name">
        <span v-if="!isCollapse">
          <el-icon class="swing-icon" style="font-weight: 500;"><Brush/></el-icon>&nbsp;&nbsp;&nbsp;DataStream
        </span>
        <el-icon v-else class="swing-icon" style="font-weight: 500; font-size: 30px;">
          <Brush/>
        </el-icon>
      </div>

      <!-- 菜单 -->
      <el-menu
        class="data-stream-menu"
        :default-active="activeMenuIndex"
        :collapse="isCollapse"
        :default-openeds="defaultOpeneds"
        @select="handleSelect"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#fff">

        <!-- 系统概览 -->
        <el-menu-item index="5">
          <el-icon><Menu/></el-icon>
          <template #title>{{ menuDescArr[5] }}</template>
        </el-menu-item>

        <!-- 任务管理 -->
        <el-sub-menu index="1">
          <template #title>
            <el-icon><FolderOpened/></el-icon>
            <span>{{ menuDescArr[1] }}</span>
          </template>
          <el-menu-item index="2">
            <el-icon><FolderRemove/></el-icon>
            <template #title>{{ menuDescArr[2] }}</template>
          </el-menu-item>
          <el-menu-item index="3">
            <el-icon><FolderAdd/></el-icon>
            <template #title>{{ menuDescArr[3] }}</template>
          </el-menu-item>
          <el-menu-item index="15">
            <el-icon><Operation/></el-icon>
            <template #title>{{ menuDescArr[15] }}</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 数据检索 -->
        <el-menu-item index="11">
          <el-icon><Search/></el-icon>
          <template #title>{{ menuDescArr[11] }}</template>
        </el-menu-item>

        <!-- 配置管理 -->
        <el-sub-menu index="14">
          <template #title>
            <el-icon><Setting/></el-icon>
            <span>{{ menuDescArr[14] }}</span>
          </template>
          <el-menu-item index="4">
            <el-icon><DataBoard/></el-icon>
            <template #title>{{ menuDescArr[4] }}</template>
          </el-menu-item>
          <el-menu-item index="12">
            <el-icon><Tools/></el-icon>
            <template #title>{{ menuDescArr[12] }}</template>
          </el-menu-item>
          <el-menu-item index="17">
            <el-icon><Notebook/></el-icon>
            <template #title>{{ menuDescArr[17] }}</template>
          </el-menu-item>
          <el-menu-item index="18">
            <el-icon><Message/></el-icon>
            <template #title>{{ menuDescArr[18] }}</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 资源监控 -->
        <el-menu-item index="19">
          <el-icon><DataAnalysis/></el-icon>
          <template #title>{{ menuDescArr[19] }}</template>
        </el-menu-item>

        <!-- 系统管理 -->
        <el-sub-menu index="16">
          <template #title>
            <el-icon><Tools/></el-icon>
            <span>{{ menuDescArr[16] }}</span>
          </template>
          <el-menu-item index="9">
            <el-icon><Tickets/></el-icon>
            <template #title>{{ menuDescArr[9] }}</template>
          </el-menu-item>
          <el-menu-item index="10">
            <el-icon><Document/></el-icon>
            <template #title>{{ menuDescArr[10] }}</template>
          </el-menu-item>
          <el-menu-item index="7">
            <el-icon><ChatLineSquare/></el-icon>
            <template #title>{{ menuDescArr[7] }}</template>
          </el-menu-item>
          <el-menu-item index="13">
            <el-icon><Coin/></el-icon>
            <template #title>{{ menuDescArr[13] }}</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 头部 -->
      <el-header>
        <!-- 折叠按钮 -->
        <div class="menu-collapse" @click="toggleCollapse">
          <el-icon>
            <Fold v-if="!isCollapse"/>
            <Expand v-else/>
          </el-icon>
        </div>

        <!-- 面包屑 -->
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>
            <a href="javascript:void(0);" @click="gotoPage('5')">首页</a>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="idx">{{ item }}</el-breadcrumb-item>
        </el-breadcrumb>

        <!-- 用户信息 -->
        <el-dropdown class="login-info-class" @command="handleLogout">
          <span class="el-dropdown-link" style="display: block; line-height: 60px;">
            <el-icon style="margin-right: 6px; font-size: 15px;"><User/></el-icon>
            {{ systemUserDesc }}
            <el-icon style="margin-left: 6px;"><ArrowDown/></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="logout-class">
              <el-dropdown-item :loading="loading">退出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- Tab 标签栏 -->
      <div class="tab-container" v-if="tabs.length > 0">
        <el-tabs
          v-model="activeTab"
          type="card"
          closable
          @tab-remove="removeTab"
          @tab-click="handleTabClick"
          class="main-tabs">
          <el-tab-pane
            v-for="tab in tabs"
            :key="tab.name"
            :label="tab.title"
            :name="tab.name"
            :closable="tab.closable !== false">
            <template #label>
              <span class="tab-label">
                <el-icon v-if="tab.icon" class="tab-icon">
                  <component :is="tab.icon"/>
                </el-icon>
                {{ tab.title }}
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 主内容区 -->
      <el-main :style="{ padding: tabs.length > 0 ? '0' : '10px' }">
        <!-- 多Tab内容区域 -->
        <div v-if="tabs.length > 0" class="tab-content-container">
          <keep-alive>
            <router-view v-slot="{ Component }">
              <transition name="fade" mode="out-in">
                <component :is="Component" v-if="Component"/>
              </transition>
            </router-view>
          </keep-alive>
        </div>

        <!-- 单页面内容（无tab时） -->
        <transition v-else name="component-fade" mode="out-in">
          <keep-alive>
            <router-view></router-view>
          </keep-alive>
        </transition>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { ref, onMounted, onActivated, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useMenuConfig } from '@/composables/useMenuConfig'
import { useTabManage } from '@/composables/useTabManage'
import { useAuth } from '@/composables/useAuth'
import { useEventBus } from '@/composables/useEventBus'
import {
  Brush,
  Menu,
  FolderOpened,
  FolderRemove,
  FolderAdd,
  Search,
  DataBoard,
  Setting,
  User,
  ArrowDown,
  Fold,
  Expand,
  Notebook,
  Tickets,
  Document,
  ChatLineSquare,
  Coin,
  Tools,
  Operation,
  Message,
  DataAnalysis
} from '@element-plus/icons-vue'

export default {
  name: 'HomePageSidebar',
  components: {
    Brush,
    Menu,
    FolderOpened,
    FolderRemove,
    FolderAdd,
    Search,
    DataBoard,
    Setting,
    User,
    ArrowDown,
    Fold,
    Expand,
    Notebook,
    Tickets,
    Document,
    ChatLineSquare,
    Coin,
    Tools,
    Operation,
    Message,
    DataAnalysis
  },
  setup() {
    const instance = getCurrentInstance()
    const $bus = instance?.appContext.config.globalProperties.$bus

    // 使用菜单配置
    const menuConfig = useMenuConfig()
    const { menuDescArr, defaultOpeneds, crumbArr } = menuConfig

    // 使用Tab管理
    const tabManage = useTabManage({
      menuNameArr: menuConfig.menuNameArr,
      menuDescArr: menuConfig.menuDescArr,
      menuIconArr: menuConfig.menuIconArr,
      crumbArr: menuConfig.crumbArr
    })

    // 使用认证
    const auth = useAuth()

    // 侧边栏折叠状态
    const isCollapse = ref(false)

    // 切换折叠状态
    const toggleCollapse = () => {
      isCollapse.value = !isCollapse.value
      $bus?.$emit('changeChart')
    }

    // 处理菜单选择
    const handleSelect = (index, indexPath) => {
      tabManage.handleMenuSelect(index, indexPath)
    }

    // 处理登出
    const handleLogout = () => {
      auth.logout()
    }

    // 初始化
    onMounted(() => {
      // 检查Token
      if (!auth.validateAndRedirect()) {
        return
      }

      // 初始化Tab系统
      tabManage.initTabs('overview', '系统概览', 'Menu')

      // 设置事件监听
      $bus?.$on('gotoPage', (pageNameIndex) => {
        tabManage.gotoPage(pageNameIndex + '')
      })

      // 设置路由监听
      tabManage.setupRouteWatcher()

      // 延迟初始化检查
      setTimeout(() => {
        tabManage.checkInitialSync()
      }, 100)
    })

    onActivated(() => {
      if (!auth.validateAndRedirect()) {
        return
      }
    })

    onBeforeUnmount(() => {
      $bus?.$off('gotoPage')
    })

    return {
      // 菜单配置
      menuDescArr,
      defaultOpeneds,
      crumbArr,

      // Tab管理
      activeTab: tabManage.activeTab,
      tabs: tabManage.tabs,
      activeMenuIndex: tabManage.activeMenuIndex,
      breadcrumb: tabManage.breadcrumb,
      removeTab: tabManage.removeTab,
      handleTabClick: tabManage.handleTabClick,
      gotoPage: tabManage.gotoPage,

      // 认证
      systemUserDesc: auth.systemUserDesc,
      loading: auth.loading,
      handleLogout,

      // 侧边栏
      isCollapse,
      toggleCollapse,
      handleSelect
    }
  }
}
</script>

<style>
@import '@/assets/css/public.css';

/* Tab切换过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-enter-to,
.fade-leave-from {
  opacity: 1;
}

.data-stream-menu-aside {
  position: relative;
  transition: width 0.3s ease;
  overflow: visible;
  background: linear-gradient(180deg, #1E3A5F 0%, #16324F 50%, #1E3A5F 100%);
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
}

.data-stream-menu-aside.collapsed {
  width: 64px !important;
}

.el-container {
  transition: all 0.3s ease;
}

.app-name {
  height: 60px;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  line-height: 60px;
  text-align: center;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  background: #1E3A5F;
  border-bottom: 1px solid rgba(30, 58, 95, 0.3);
  box-shadow: 0 2px 8px rgba(30, 58, 95, 0.2);
}

.el-header {
  padding: 0 20px 0 0 !important;
  background-color: #ffffff;
  position: relative;
  display: flex;
}

.menu-collapse {
  font-size: 30px;
  line-height: 60px;
  margin-right: 15px;
  margin-left: 5px;
  cursor: pointer;
}

.el-main {
  background-color: #E9EEF3;
  overflow-x: hidden;
}

.el-menu {
  border: none !important;
  background: linear-gradient(180deg, #1E3A5F 0%, #16324F 50%, #1E3A5F 100%) !important;
}

.el-menu--collapse {
  width: 64px;
}

.el-menu--collapse .el-sub-menu__title {
  padding: 0 20px !important;
}

.el-menu--collapse .el-menu-item {
  padding: 0 20px !important;
}

.el-menu-item {
  color: #fff !important;
  transition: all 0.3s ease;
}

.el-menu-item:hover {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #fff !important;
}

.el-sub-menu__title {
  color: #fff !important;
  transition: all 0.3s ease;
}

.el-sub-menu__title:hover {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #fff !important;
}

.el-breadcrumb {
  line-height: 60px !important;
  font-size: 16px !important;
}

.el-breadcrumb__inner {
  font-weight: 600;
  color: #000;
}

.el-breadcrumb__item:first-child .el-breadcrumb__inner {
  font-weight: 600 !important;
  color: #000 !important;
}

.login-info-class {
  position: absolute !important;
  top: 0px;
  right: 20px;
  line-height: 60px;
  font-size: 12px !important;
  font-weight: 600;
  cursor: pointer;
}

.login-info-class:hover {
  color: #1890ff !important;
}

.logout-class {
  margin: 5px 0 0 0;
  min-width: 120px !important;
}

.logout-class .el-dropdown-menu__item {
  line-height: 40px;
  padding: 0 25px;
  min-width: 120px !important;
  width: 120px !important;
  text-align: center !important;
  white-space: nowrap !important;
}

@keyframes swing {
  0% { transform: rotate(180deg); }
  25% { transform: rotate(160deg); }
  50% { transform: rotate(180deg); }
  75% { transform: rotate(200deg); }
  100% { transform: rotate(180deg); }
}

.swing-icon:hover {
  display: inline-block;
  animation: swing 0.8s infinite;
  font-size: 30px;
}

.swing-icon {
  transform: rotate(180deg);
}

.el-icon {
  vertical-align: middle;
}

.el-menu .is-active {
  background: #2563EB !important;
  color: #fff !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
  border-radius: 4px;
  margin: 2px 8px;
}

.el-menu .is-active:hover {
  background: #3B82F6 !important;
  color: #fff !important;
}

.el-menu-item:hover {
  background: rgba(37, 99, 235, 0.15) !important;
  color: #fff !important;
  border-radius: 4px;
  margin: 2px 8px;
}

.el-submenu__title:hover {
  background: rgba(37, 99, 235, 0.15) !important;
  color: #fff !important;
  border-radius: 4px;
  margin: 2px 8px;
}

.el-table__empty-block {
  width: 100% !important;
  min-height: 420px !important;
}

.pr-20 {
  padding-right: 20px;
}

.el-divider--horizontal {
  margin-top: 6px !important;
}

.datasource-opt {
  display: flex;
  justify-content: space-around;
}

/* 确保折叠菜单的弹出层正确显示 */
.el-menu--collapse .el-sub-menu .el-menu {
  position: absolute;
  left: 64px;
  top: 0;
  min-width: 200px;
  z-index: 999;
  background: linear-gradient(180deg, #1E3A5F 0%, #16324F 50%, #1E3A5F 100%) !important;
  border: 1px solid rgba(30, 58, 95, 0.3);
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(30, 58, 95, 0.2);
}

.data-stream-menu-aside {
  z-index: 100;
}

.el-container {
  position: relative;
  z-index: 1;
}

/* Tab 样式 */
.tab-container {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}

.main-tabs {
  margin: 0;
}

.main-tabs .el-tabs__header {
  margin: 0;
  background: #f5f7fa;
}

.main-tabs .el-tabs__nav-wrap {
  padding: 0 20px;
}

.main-tabs .el-tabs__item {
  height: 40px;
  line-height: 40px;
  font-size: 14px;
  color: #606266;
  border: 1px solid #e4e7ed;
  border-bottom: none;
  background: #fff;
  margin-right: 4px;
  border-radius: 4px 4px 0 0;
}

.main-tabs .el-tabs__item.is-active {
  color: #2563EB;
  background: #fff;
  border-color: #2563EB;
}

.main-tabs .el-tabs__item:hover {
  color: #2563EB;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-icon {
  font-size: 14px;
}

.tab-content-container {
  height: 100%;
  overflow: auto;
}

.tab-pane-content {
  height: 100%;
  overflow: auto;
  padding: 20px;
}
</style>

