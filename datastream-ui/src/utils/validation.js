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
 * 验证工具函数
 */

/**
 * 验证邮箱
 * @param {string} email - 邮箱地址
 * @returns {boolean} 是否有效
 */
export function isValidEmail(email) {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return re.test(email)
}

/**
 * 验证手机号
 * @param {string} phone - 手机号
 * @returns {boolean} 是否有效
 */
export function isValidPhone(phone) {
  const re = /^1[3-9]\d{9}$/
  return re.test(phone)
}

/**
 * 验证URL
 * @param {string} url - URL地址
 * @returns {boolean} 是否有效
 */
export function isValidUrl(url) {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

/**
 * 验证IP地址
 * @param {string} ip - IP地址
 * @returns {boolean} 是否有效
 */
export function isValidIP(ip) {
  const re = /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/
  return re.test(ip)
}

/**
 * 验证端口号
 * @param {number|string} port - 端口号
 * @returns {boolean} 是否有效
 */
export function isValidPort(port) {
  const portNum = parseInt(port)
  return !isNaN(portNum) && portNum >= 0 && portNum <= 65535
}

/**
 * 检查是否为空
 * @param {*} value - 值
 * @returns {boolean} 是否为空
 */
export function isEmpty(value) {
  if (value === null || value === undefined) return true
  if (typeof value === 'string') return value.trim() === ''
  if (Array.isArray(value)) return value.length === 0
  if (typeof value === 'object') return Object.keys(value).length === 0
  return false
}

/**
 * 检查是否为数字
 * @param {*} value - 值
 * @returns {boolean} 是否为数字
 */
export function isNumber(value) {
  return typeof value === 'number' && !isNaN(value)
}

/**
 * 检查是否为整数
 * @param {*} value - 值
 * @returns {boolean} 是否为整数
 */
export function isInteger(value) {
  return Number.isInteger(value)
}

/**
 * Element Plus 表单验证规则生成器
 */
export const FormRules = {
  /**
   * 必填规则
   * @param {string} message - 错误消息
   * @param {string} trigger - 触发方式
   */
  required: (message = '此项为必填项', trigger = 'blur') => ({
    required: true,
    message,
    trigger
  }),

  /**
   * 长度范围规则
   * @param {number} min - 最小长度
   * @param {number} max - 最大长度
   * @param {string} message - 错误消息
   */
  length: (min, max, message) => ({
    min,
    max,
    message: message || `长度在 ${min} 到 ${max} 个字符之间`,
    trigger: 'blur'
  }),

  /**
   * 邮箱规则
   */
  email: () => ({
    type: 'email',
    message: '请输入正确的邮箱地址',
    trigger: 'blur'
  }),

  /**
   * 自定义验证规则
   * @param {Function} validator - 验证函数
   * @param {string} trigger - 触发方式
   */
  custom: (validator, trigger = 'blur') => ({
    validator,
    trigger
  })
}

