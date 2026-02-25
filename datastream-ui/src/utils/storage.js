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
 * 本地存储工具函数
 */

const PREFIX = 'datastream_'

/**
 * 设置localStorage
 * @param {string} key - 键
 * @param {*} value - 值
 */
export function setLocalStorage(key, value) {
  try {
    const data = JSON.stringify(value)
    localStorage.setItem(PREFIX + key, data)
  } catch (e) {
    console.error('localStorage设置失败:', e)
  }
}

/**
 * 获取localStorage
 * @param {string} key - 键
 * @param {*} defaultValue - 默认值
 * @returns {*} 存储的值
 */
export function getLocalStorage(key, defaultValue = null) {
  try {
    const data = localStorage.getItem(PREFIX + key)
    return data ? JSON.parse(data) : defaultValue
  } catch (e) {
    console.error('localStorage获取失败:', e)
    return defaultValue
  }
}

/**
 * 移除localStorage
 * @param {string} key - 键
 */
export function removeLocalStorage(key) {
  localStorage.removeItem(PREFIX + key)
}

/**
 * 清空所有localStorage
 */
export function clearLocalStorage() {
  const keys = Object.keys(localStorage)
  keys.forEach(key => {
    if (key.startsWith(PREFIX)) {
      localStorage.removeItem(key)
    }
  })
}

/**
 * 设置sessionStorage
 * @param {string} key - 键
 * @param {*} value - 值
 */
export function setSessionStorage(key, value) {
  try {
    const data = JSON.stringify(value)
    sessionStorage.setItem(PREFIX + key, data)
  } catch (e) {
    console.error('sessionStorage设置失败:', e)
  }
}

/**
 * 获取sessionStorage
 * @param {string} key - 键
 * @param {*} defaultValue - 默认值
 * @returns {*} 存储的值
 */
export function getSessionStorage(key, defaultValue = null) {
  try {
    const data = sessionStorage.getItem(PREFIX + key)
    return data ? JSON.parse(data) : defaultValue
  } catch (e) {
    console.error('sessionStorage获取失败:', e)
    return defaultValue
  }
}

/**
 * 移除sessionStorage
 * @param {string} key - 键
 */
export function removeSessionStorage(key) {
  sessionStorage.removeItem(PREFIX + key)
}

/**
 * 清空所有sessionStorage
 */
export function clearSessionStorage() {
  const keys = Object.keys(sessionStorage)
  keys.forEach(key => {
    if (key.startsWith(PREFIX)) {
      sessionStorage.removeItem(key)
    }
  })
}

