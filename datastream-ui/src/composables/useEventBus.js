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
 * 事件总线hook
 * 用于组件间通信
 */
import { getCurrentInstance, onBeforeUnmount } from 'vue'

export function useEventBus() {
  const instance = getCurrentInstance()
  const bus = instance?.appContext.config.globalProperties.$bus

  // 存储已注册的事件，用于自动清理
  const registeredEvents = []

  // 监听事件
  const on = (event, callback) => {
    if (bus) {
      bus.$on(event, callback)
      registeredEvents.push({ event, callback })
    }
  }

  // 触发事件
  const emit = (event, ...args) => {
    if (bus) {
      bus.$emit(event, ...args)
    }
  }

  // 移除事件监听
  const off = (event, callback) => {
    if (bus) {
      bus.$off(event, callback)
    }
  }

  // 只监听一次
  const once = (event, callback) => {
    if (bus) {
      bus.$once(event, callback)
    }
  }

  // 自动清理所有已注册的事件
  const cleanup = () => {
    registeredEvents.forEach(({ event, callback }) => {
      off(event, callback)
    })
    registeredEvents.length = 0
  }

  // 组件卸载时自动清理
  onBeforeUnmount(() => {
    cleanup()
  })

  return {
    bus,
    on,
    emit,
    off,
    once,
    cleanup
  }
}

