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
// 引入element插件，也可以不用，看具体项目需求
import { ElMessage } from 'element-plus'
// 引入路由，登录失效时跳转回登录页
import router from '../router/index'
// 文件处理方法
import { downLoadFileByForm, downLoadFile } from './file'
// 引入axios
import axios from 'axios'

// 智能token处理函数，适配后端实际格式
function processToken(token) {
  if (!token) {
    console.log('token为空，无需处理')
    return null
  }

  console.log('=== 智能Token处理 ===')
  console.log('原始token:', token)
  console.log('token长度:', token.length)

  // 重要：后端SecurityConstant.TOKEN_PREFIX = "Bearer"（无空格）
  // 后端验证：tokenHeader.startsWith("Bearer")（无空格）
  // 所以我们必须发送无空格的格式："Bearer<token>"
  let finalToken = token

  if (token.startsWith('Bearer ')) {
    // 标准格式，需要移除空格适配后端
    finalToken = token.replace('Bearer ', 'Bearer')
    console.log('标准格式token，移除空格适配后端:', finalToken)
  } else if (token.startsWith('Bearer')) {
    // 已经是后端期望的格式
    finalToken = token
    console.log('已是后端期望格式，直接使用:', finalToken)
  } else {
    // 纯token，添加Bearer前缀（无空格）
    finalToken = `Bearer${token}`
    console.log('纯token，添加Bearer前缀（无空格）:', finalToken)
  }

  console.log('最终token（完整）:', finalToken)
  console.log('最终token长度:', finalToken.length)
  console.log('=====================')
  return finalToken
}

// 设置默认请求头
axios.defaults.headers.common['Content-Type'] = 'application/json;charset=UTF-8'
axios.defaults.headers.post['Content-Type'] = 'application/json;charset=UTF-8'
// 注意：不在这里设置Authorization，而是在请求拦截器中动态设置
// 超时时间
const fetch = axios.create({
  timeout: 40 * 1000
})

// 请求拦截器
fetch.interceptors.request.use(config => {
  console.log('发送请求:', config.method?.toUpperCase(), config.url)
  console.log('请求配置:', {
    url: config.url,
    method: config.method,
    data: config.data
  })

  // 获取token
  const token = sessionStorage.getItem('token')
  console.log('从sessionStorage获取的token:', token ? token.substring(0, 20) + '...' : 'null')
  console.log('sessionStorage完整内容:', Object.keys(sessionStorage).map(key => `${key}: ${sessionStorage.getItem(key)?.substring(0, 20)}...`))

  if (token) {
    // 确保headers对象存在
    if (!config.headers) {
      config.headers = {}
    }

    // 使用智能token处理
    const finalToken = processToken(token)
    if (finalToken) {
      // 直接设置到headers，不使用common
      config.headers['Authorization'] = finalToken
      console.log('设置Authorization头到headers:', finalToken.substring(0, 30) + '...')
      console.log('最终token（完整）:', finalToken)
      console.log('最终token长度:', finalToken.length)
    }
  } else {
    console.log('没有找到token，跳过Authorization头设置')
  }

  console.log('最终请求头:', config.headers)
  return config
}, error => {
  console.error('请求拦截器错误:', error)
  return Promise.reject(error)
})


fetch.interceptors.response.use(res => {
    // 判断返回数据类型，如果是文件，则执行下载；否则判断接口是否请求成功，具体状态与后端协商，这里以res.data.success举例
    if (res.headers['content-type'] === 'application/vnd.ms-excel;charset=UTF-8') {
      // 下载文件
      downLoadFile(res)
    } else if (res.headers['content-type'] === 'multipart/form-data') {
      downLoadFileByForm(res)
    } else if (res.status === 200) {
      if (res.headers.token) {
        sessionStorage.setItem('token', res.headers.token)
      }
      return res.data
    } else {
      return Promise.reject(res.data)
    }
  }, error => {
    const stateCode = error.response.status
    // 这里只添加了401状态码，如有需求可自行添加更多状态码
    if (stateCode === 401) {
      // 这里记得清除token
      Message.closeAll()
      router.push({name: 'login'})
      if (error.response.data && error.response.data.resultMsg) {
        return Promise.reject(error.response.data.resultMsg)
      } else {
        Message.error('登录已失效，请重新登录！')
      }
      return Promise.reject(error)
    } else {
      // 清除多余失败提示框
      Message.closeAll()
      return Promise.reject(error)
    }
  }
)

export default fetch
