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
 * 节流 Composable
 */
import { ref, watch, onUnmounted } from 'vue'

/**
 * 使用节流值
 * @param {Ref} value - 响应式值
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Ref} 节流后的值
 */
export function useThrottledRef(value, delay = 300) {
  const throttledValue = ref(value.value)
  let lastTime = 0

  watch(value, (newVal) => {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      throttledValue.value = newVal
    }
  })

  return throttledValue
}

/**
 * 使用节流函数
 * @param {Function} fn - 要节流的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export function useThrottledFn(fn, delay = 300) {
  let lastTime = 0
  let timer = null

  const throttledFn = (...args) => {
    const now = Date.now()
    const remaining = delay - (now - lastTime)

    if (remaining <= 0) {
      if (timer) {
        clearTimeout(timer)
        timer = null
      }
      lastTime = now
      fn(...args)
    } else if (!timer) {
      timer = setTimeout(() => {
        lastTime = Date.now()
        timer = null
        fn(...args)
      }, remaining)
    }
  }

  onUnmounted(() => {
    if (timer) clearTimeout(timer)
  })

  return throttledFn
}

