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
 * 工具函数统一导出
 */

// 日期工具
export * from './date'

// 格式化工具
export * from './format'

// 验证工具
export * from './validation'

// 存储工具
export * from './storage'

// 加密工具
export * from './crypto'

// 防抖函数
export function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

// 节流函数
export function throttle(fn, delay = 300) {
  let last = 0
  return function (...args) {
    const now = Date.now()
    if (now - last >= delay) {
      last = now
      fn.apply(this, args)
    }
  }
}

// 深拷贝
export function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj

  if (obj instanceof Date) return new Date(obj)
  if (obj instanceof RegExp) return new RegExp(obj)

  const clone = Array.isArray(obj) ? [] : {}

  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      clone[key] = deepClone(obj[key])
    }
  }

  return clone
}

// 获取URL参数
export function getUrlParams(url = window.location.href) {
  const params = {}
  const urlObj = new URL(url)
  urlObj.searchParams.forEach((value, key) => {
    params[key] = value
  })
  return params
}

// 生成UUID
export function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// 下载文件
export function downloadFile(url, filename) {
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'download'
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 复制到剪贴板
export async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch (err) {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    const result = document.execCommand('copy')
    document.body.removeChild(textarea)
    return result
  }
}

// 对象转查询字符串
export function objectToQueryString(obj) {
  if (!obj) return ''
  return Object.keys(obj)
    .filter(key => obj[key] !== undefined && obj[key] !== null && obj[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(obj[key])}`)
    .join('&')
}

// 数组去重
export function uniqueArray(arr, key) {
  if (!key) return [...new Set(arr)]

  const map = new Map()
  return arr.filter(item => {
    const val = item[key]
    if (map.has(val)) return false
    map.set(val, true)
    return true
  })
}

// 树形结构扁平化
export function flattenTree(tree, childrenKey = 'children') {
  const result = []

  function traverse(nodes) {
    nodes.forEach(node => {
      const { [childrenKey]: children, ...rest } = node
      result.push(rest)
      if (children && children.length) {
        traverse(children)
      }
    })
  }

  traverse(tree)
  return result
}

// 列表转树形结构
export function listToTree(list, options = {}) {
  const { idKey = 'id', parentKey = 'parentId', childrenKey = 'children' } = options

  const map = {}
  const roots = []

  list.forEach(item => {
    map[item[idKey]] = { ...item, [childrenKey]: [] }
  })

  list.forEach(item => {
    const node = map[item[idKey]]
    const parentId = item[parentKey]

    if (parentId && map[parentId]) {
      map[parentId][childrenKey].push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
}

