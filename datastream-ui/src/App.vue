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
  <div id="app" :class="{ 'dark-mode': isDarkMode }" class="app-container">
    <router-view/>
  </div>
</template>

<script>
import { useMainStore } from '@/store'
import { computed } from 'vue'

export default {
  name: 'App',
  setup() {
    const mainStore = useMainStore()
    const isDarkMode = computed(() => mainStore.isDarkMode)

    return {
      isDarkMode
    }
  },
  mounted() {
    // 初始化暗黑模式设置
    const mainStore = useMainStore()
    mainStore.initDarkMode()
  }
}
</script>

<style>
/* CSS 变量定义 */
:root {
  /* 浅色模式 */
  --bg-primary: #F8FAFC;
  --bg-secondary: #FFFFFF;
  --text-primary: #0F172A;
  --text-secondary: #475569;
  --border-color: #E2E8F0;
  --header-bg: #1E3A5F;
  --sidebar-bg: #1E3A5F;
  --primary-color: #2563EB;
  --card-bg: #FFFFFF;
  --shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 暗黑模式变量 */
.dark-mode {
  --bg-primary: #0F172A;
  --bg-secondary: #1E293B;
  --text-primary: #E2E8F0;
  --text-secondary: #94A3B8;
  --border-color: #334155;
  --header-bg: #0F172A;
  --sidebar-bg: #0F172A;
  --card-bg: #1E293B;
  --shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
}

.app-container {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
  position: relative;
  overflow: hidden;

  /* 使用与登录页面呼应的背景 */
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: all 0.3s ease;
}

.app-container > * {
  position: relative;
  z-index: 2;
}

#app {
  height: 100%;
}

body {
  display: block;
  margin: 0 !important;
  background: #f5f7fa;
  transition: background 0.3s ease;
}

.dark-mode body {
  background: #2c3e50;
}

div[data-name='mojs-shape'] {
  z-index: 9999;
}

/* Element Plus 暗黑模式适配 */
.dark-mode .el-header {
  background-color: var(--header-bg) !important;
}

.dark-mode .el-main {
  background-color: transparent !important;
  color: var(--text-primary) !important;
}

.dark-mode .el-card {
  background-color: var(--card-bg) !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary) !important;
}

.dark-mode .el-table {
  background-color: var(--card-bg) !important;
  color: var(--text-primary) !important;
}

.dark-mode .el-table th {
  background-color: var(--bg-secondary) !important;
  color: var(--text-primary) !important;
}

.dark-mode .el-table td {
  border-color: var(--border-color) !important;
  color: var(--text-primary) !important;
}

.dark-mode .el-input__wrapper {
  background-color: var(--card-bg) !important;
  color: var(--text-primary) !important;
  border-color: var(--border-color) !important;
}

.dark-mode .el-select .el-input__wrapper {
  background-color: var(--card-bg) !important;
}

/* 更多Element Plus组件暗黑模式适配 */
.dark-mode .el-menu {
  background-color: var(--header-bg) !important;
}

.dark-mode .el-menu-item {
  color: var(--text-primary) !important;
}

.dark-mode .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

.dark-mode .el-dialog {
  background-color: var(--card-bg) !important;
  color: var(--text-primary) !important;
}

.dark-mode .el-dialog__header {
  color: var(--text-primary) !important;
}

.dark-mode .el-drawer {
  background-color: var(--card-bg) !important;
  color: var(--text-primary) !important;
}

.dark-mode .el-form-item__label {
  color: var(--text-primary) !important;
}

.dark-mode .el-button {
  border-color: var(--border-color) !important;
}

.dark-mode .el-button--primary {
  background-color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
}

.dark-mode .el-pagination {
  color: var(--text-primary) !important;
}

.dark-mode .el-pagination .btn-prev,
.dark-mode .el-pagination .btn-next {
  color: var(--text-primary) !important;
}

.dark-mode .el-pager li {
  color: var(--text-primary) !important;
  background-color: var(--card-bg) !important;
}

.dark-mode .el-select-dropdown {
  background-color: var(--card-bg) !important;
  border-color: var(--border-color) !important;
}

.dark-mode .el-option {
  color: var(--text-primary) !important;
}

.dark-mode .el-option:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

.dark-mode .el-tag {
  background-color: var(--bg-secondary) !important;
  color: var(--text-primary) !important;
  border-color: var(--border-color) !important;
}

.dark-mode .el-divider {
  border-color: var(--border-color) !important;
}
</style>
