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
// 引入时间处理插件
import dayjs from 'dayjs'

export const transforDataSourceFlagDesc = dataSourceFlag => {
  switch (dataSourceFlag) {
    case 0:
      return '无数据源'
    case 1:
      return '有数据源'
    default:
      return '未知'
  }
}

export const transforDataSourceTypeDesc = dataSourceType => {
  switch (dataSourceType) {
    case 1:
      return '文件'
    default:
      return '未知'
  }
}

export const transforMsgOrderFlagDesc = msgOrderFlag => {
  switch (msgOrderFlag) {
    case 0:
      return '无序'
    case 1:
      return '有序'
    default:
      return '未知'
  }
}

export const transforTaskTypeStatusDesc = status => {
  switch (status) {
    case 0:
      return '未生效'
    case 1:
      return '已生效'
    default:
      return '未知'
  }
}

export const dateFormat14 = dateStr => {
  return dayjs(dateStr, 'YYYYMMDDHHmmss').format('YYYY-MM-DD HH:mm:ss')
}

export const timestampFormat14 = timestamp => {
  if (!timestamp) {
    return ''
  }
  return dayjs(new Date(timestamp)).format('YYYY-MM-DD HH:mm:ss')
}

export const transforTaskInstStatusDesc = status => {
  switch (status) {
    case 0:
      return '初始化'
    case 100:
      return '数据输入中'
    case 111:
      return '读取解析异常'
    case 112:
      return '入库异常'
    case 113:
      return '数据解析入库不一致'
    case 200:
      return '处理中'
    case 311:
      return '手动/异常终止'
    case 312:
      return '处理超时'
    case 201:
      return '重试'
    case 300:
      return '结束'
    default:
      return '未知'
  }
}

export const transforTaskDataStatusDesc = status => {
  switch (status) {
    case 0:
      return '未发送'
    case 11:
      return '发送失败'
    case 20:
      return '已发送'
    case 21:
      return '调用异常'
    case 22:
      return '处理超时'
    case 31:
      return '手动/异常终止'
    case 30:
      return '已处理'
    case 40:
      return '重试'
    default:
      return '未知'
  }
}

export const transforTaskFileInfoStatusDesc = status => {
  switch (status) {
    case 0:
      return '正常'
    case 1:
      return '无效'
    default:
      return '未知'
  }
}

export const transforTaskPlanPlanTypeDesc = planType => {
  switch (planType) {
    case 1:
      return '立即执行'
    case 2:
      return '指定时间'
    case 3:
      return '定期执行'
    default:
      return '未知'
  }
}

export const transforTaskPlanStatusDesc = status => {
  switch (status) {
    case 0:
      return '未生效'
    case 1:
      return '生效'
    case 2:
      return '结束'
    default:
      return '未知'
  }
}
