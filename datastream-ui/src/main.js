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
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 引入时间处理插件
import dayjs from 'dayjs'
import { createPinia } from 'pinia'
// 复制组件
import VueClipboard from 'vue-clipboard3'
// 引入全局过滤器
import './filters/filter'
// 图表
import * as echarts from 'echarts'
// 绘图组件
import jsPlumb from 'jsplumb'
// 引入jquery
import $ from 'jquery'
// 事件总线
import eventBus from './utils/eventBus'
// axios
import axios from 'axios'

const app = createApp(App)

// 创建pinia实例
const pinia = createPinia()

// 将$和jQuery添加到全局变量中
window.$ = $
window.jQuery = $

// 全局属性配置
app.config.globalProperties.dayjs = dayjs
app.config.globalProperties.$axios = axios
app.config.globalProperties.$date = dayjs
app.config.globalProperties.$echarts = echarts
app.config.globalProperties.$jsPlumb = jsPlumb.jsPlumb
// 创建 Vue 2 兼容的事件总线接口
const bus = {
  $on: (event, handler) => eventBus.on(event, handler),
  $off: (event, handler) => eventBus.off(event, handler),
  $emit: (event, data) => eventBus.emit(event, data)
}
app.config.globalProperties.$bus = bus

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 使用插件
app.use(ElementPlus, {size: 'small', zIndex: 3000})
app.use(VueClipboard)
app.use(pinia)
app.use(router)

// 挂载应用
app.mount('#app')
