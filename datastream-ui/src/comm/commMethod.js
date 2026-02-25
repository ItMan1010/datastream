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
import dayjs from 'dayjs'

// ============ 日期相关 ============

/**
 * 比较时间大小
 * @deprecated 使用 @/utils/date 中的函数
 */
export function compare2Time(beginTimeStr, endTimeStr) {
  return dayjs(endTimeStr, 'YYYY-MM-DD HH:mm:ss') > dayjs(beginTimeStr, 'YYYY-MM-DD HH:mm:ss')
}

export function str2DateTime(dateTime, format) {
  if (!format) format = 'YYYY-MM-DD HH:mm:ss'
  return dayjs(dateTime, format).toDate()
}

export function dateFormat(dateTime, format) {
  if (!format) format = 'YYYY-MM-DD HH:mm:ss'
  return dayjs(dateTime).format(format)
}

export function dateFormat6(monthStr) {
  return dayjs(monthStr, 'YYYYMM').format('YYYY-MM')
}

export function dateFormat8(dayStr) {
  return dayjs(dayStr, 'YYYYMMDD').format('YYYY-MM-DD')
}

export function dateFormat14(dateStr) {
  return dayjs(dateStr, 'YYYYMMDDHHmmss').format('YYYY-MM-DD HH:mm:ss')
}

// ============ 排序相关 ============

export function sortByTime(list, fileName, ascFlag) {
  if (ascFlag) {
    return list.sort((x, y) =>
      new Date(x[fileName] || 0).getTime() - new Date(y[fileName] || 0).getTime())
  } else {
    return list.sort((x, y) =>
      new Date(y[fileName] || 0).getTime() - new Date(x[fileName] || 0).getTime())
  }
}

export function sortTaskTypeByIdAsc(taskTypeList) {
  return taskTypeList.sort((x, y) => x.taskTypeId - y.taskTypeId)
}

// ============ 时间范围相关 ============

export function getInitRangeTime() {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  const end = new Date(start.getTime() + 3600 * 1000 * 24)
  return [start, end]
}

export function getTodayTimeRange() {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  const end = new Date(start.getTime() + 3600 * 1000 * 24)
  return [start, end]
}

export function getPickerOptions() {
  return {
    pickerOptions: {
      shortcuts: [
        {
          text: '当天',
          onClick(picker) {
            picker.$emit('pick', getTodayTimeRange())
          }
        },
        {
          text: '最近一周',
          onClick(picker) {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
            picker.$emit('pick', [start, end])
          }
        },
        {
          text: '最近一个月',
          onClick(picker) {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
            picker.$emit('pick', [start, end])
          }
        }
      ]
    }
  }
}

// ============ 加密相关 ============
// @deprecated 请使用 @/utils/crypto 中的函数

import { encryptByDES, encryptByAES, maskPassword } from '@/utils/crypto'
export { encryptByDES, encryptByAES }

/**
 * 密码脱敏
 * @deprecated 请使用 @/utils/crypto 中的 maskPassword
 */
export function getEncryptPassWord(length) {
  return maskPassword(length)
}

// ============ 其他工具 ============

/**
 * 从jdbcUrl中获取地址信息
 */
export function extractAddress(jdbcUrl) {
  if (!jdbcUrl) return ''
  // 匹配 jdbc:mysql://host:port 或类似格式
  const match = jdbcUrl.match(/\/\/([^\/\?]+)/)
  if (match) {
    return match[1]
  }
  return ''
}
