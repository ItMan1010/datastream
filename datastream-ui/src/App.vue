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
  --bg-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  --bg-secondary: rgba(255, 255, 255, 0.95);
  --text-primary: #2c3e50;
  --text-secondary: #606266;
  --border-color: #e4e7ed;
  --header-bg: #409EFF;
  --card-bg: #ffffff;
  --shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 暗黑模式变量 */
.dark-mode {
  --bg-primary: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  --bg-secondary: rgba(52, 73, 94, 0.95);
  --text-primary: #ffffff;
  --text-secondary: #bdc3c7;
  --border-color: #4a5568;
  --header-bg: #2c5aa0;
  --card-bg: #34495e;
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

.app-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(circle at 20% 50%, rgba(79, 172, 254, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(0, 242, 254, 0.05) 0%, transparent 50%);
  z-index: 1;
  transition: opacity 0.3s ease;
}

.dark-mode::before {
  background:
    radial-gradient(circle at 20% 50%, rgba(79, 172, 254, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(0, 242, 254, 0.03) 0%, transparent 50%);
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
  background-color: #409EFF !important;
  border-color: #409EFF !important;
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
