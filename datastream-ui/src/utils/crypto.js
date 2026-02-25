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
 * 加密工具函数
 */
import CryptoJS from 'crypto-js'

// 默认加密密钥
const DEFAULT_KEY = '1234567812345678'

/**
 * DES加密
 * @param {string} message - 待加密内容
 * @param {string} key - 密钥，默认使用系统密钥
 * @returns {string} 加密后的字符串
 */
export function encryptByDES(message, key = DEFAULT_KEY) {
  const keyHex = CryptoJS.enc.Utf8.parse(key)
  const encrypted = CryptoJS.DES.encrypt(message, keyHex, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  })
  return encrypted.toString()
}

/**
 * AES加密
 * @param {string} message - 待加密内容
 * @param {string} key - 密钥，默认使用系统密钥
 * @returns {string} 加密后的字符串
 */
export function encryptByAES(message, key = DEFAULT_KEY) {
  const keyHex = CryptoJS.enc.Utf8.parse(key)
  const encrypted = CryptoJS.AES.encrypt(message, keyHex, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  })
  return encrypted.toString()
}

/**
 * 密码脱敏（生成*号）
 * @param {number} length - 长度
 * @returns {string} 脱敏后的字符串
 */
export function maskPassword(length) {
  if (length < 1) return ''
  return '*'.repeat(length)
}

/**
 * 手机号脱敏
 * @param {string} phone - 手机号
 * @returns {string} 脱敏后的手机号
 */
export function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

/**
 * 邮箱脱敏
 * @param {string} email - 邮箱
 * @returns {string} 脱敏后的邮箱
 */
export function maskEmail(email) {
  if (!email || !email.includes('@')) return email
  const [name, domain] = email.split('@')
  const maskedName = name.length > 2
    ? name.substring(0, 2) + '***'
    : name.substring(0, 1) + '***'
  return maskedName + '@' + domain
}

