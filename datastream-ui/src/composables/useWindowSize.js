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
 * 窗口尺寸 Composable
 */
import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 使用窗口尺寸
 * @returns {Object} 窗口尺寸相关状态
 */
export function useWindowSize() {
  const width = ref(window.innerWidth)
  const height = ref(window.innerHeight)

  const update = () => {
    width.value = window.innerWidth
    height.value = window.innerHeight
  }

  onMounted(() => {
    window.addEventListener('resize', update)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', update)
  })

  return {
    width,
    height
  }
}

/**
 * 使用响应式断点
 * @returns {Object} 响应式断点状态
 */
export function useBreakpoints() {
  const { width } = useWindowSize()

  // Element Plus断点
  const breakpoints = {
    xs: 768,
    sm: 992,
    md: 1200,
    lg: 1920
  }

  const isXs = ref(width.value < breakpoints.xs)
  const isSm = ref(width.value >= breakpoints.xs && width.value < breakpoints.sm)
  const isMd = ref(width.value >= breakpoints.sm && width.value < breakpoints.md)
  const isLg = ref(width.value >= breakpoints.md && width.value < breakpoints.lg)
  const isXl = ref(width.value >= breakpoints.lg)

  const current = ref(getCurrentBreakpoint(width.value))

  function getCurrentBreakpoint(w) {
    if (w < breakpoints.xs) return 'xs'
    if (w < breakpoints.sm) return 'sm'
    if (w < breakpoints.md) return 'md'
    if (w < breakpoints.lg) return 'lg'
    return 'xl'
  }

  const update = () => {
    const w = window.innerWidth
    isXs.value = w < breakpoints.xs
    isSm.value = w >= breakpoints.xs && w < breakpoints.sm
    isMd.value = w >= breakpoints.sm && w < breakpoints.md
    isLg.value = w >= breakpoints.md && w < breakpoints.lg
    isXl.value = w >= breakpoints.lg
    current.value = getCurrentBreakpoint(w)
  }

  onMounted(() => {
    window.addEventListener('resize', update)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', update)
  })

  return {
    isXs,
    isSm,
    isMd,
    isLg,
    isXl,
    current,
    breakpoints
  }
}

