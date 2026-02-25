<!--Licensed to the Apache Software Foundation (ASF) under one or more-->
<!--contributor license agreements.  See the NOTICE file distributed with-->
<!--this work for additional information regarding copyright ownership.-->
<!--The ASF licenses this file to You under the Apache License, Version 2.0-->
<!--(the "License"); you may not use this file except in compliance with-->
<!--the License.  You may obtain a copy of the License at-->

<!--http://www.apache.org/licenses/LICENSE-2.0-->

<!--Unless required by applicable law or agreed to in writing, software-->
<!--distributed under the License is distributed on an "AS IS" BASIS,-->
<!--WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.-->
<!--See the License for the specific language governing permissions and-->
<!--limitations under the License.-->
<template>
  <div class="login-page">
    <!-- Background with gradient -->
    <div class="login-bg"></div>

    <!-- Decorative floating elements -->
    <div class="floating-shape shape-1"></div>
    <div class="floating-shape shape-2"></div>
    <div class="floating-shape shape-3"></div>

    <!-- Main content -->
    <div class="login-container">
      <!-- Logo/Brand section -->
      <div class="brand-section">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1 class="brand-title">DataStream</h1>
        <p class="brand-subtitle">数据流管理平台</p>
      </div>

      <!-- Login card -->
      <div class="login-card">
        <h2 class="card-title">欢迎回来</h2>
        <p class="card-subtitle">登录以访问您的数据管理控制台</p>

        <el-form
          :model="loginForm"
          status-icon
          :rules="rules"
          size="large"
          ref="loginForm"
          @submit.native.prevent="submitForm"
        >
          <div class="form-group">
            <label class="form-label" for="username">账号</label>
            <el-input
              id="username"
              v-model.trim="loginForm.systemUserCode"
              placeholder="请输入您的账号"
              class="custom-input"
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </template>
            </el-input>
          </div>

          <div class="form-group">
            <label class="form-label" for="password">密码</label>
            <el-input
              id="password"
              type="password"
              v-model.trim="loginForm.password"
              placeholder="请输入您的密码"
              class="custom-input"
              show-password
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M7 11V7C7 5.67392 7.52678 4.40215 8.46447 3.46447C9.40215 2.52678 10.6739 2 12 2C13.3261 2 14.5979 2.52678 15.5355 3.46447C16.4732 4.40215 17 5.67392 17 7V11" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </template>
            </el-input>
          </div>

          <el-button
            type="primary"
            native-type="submit"
            @click="submitForm"
            :loading="submitLoading"
            class="submit-button"
          >
            <span v-if="!submitLoading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form>

        <div class="card-footer">
          <p class="footer-text">安全登录 · 企业级数据管理</p>
        </div>
      </div>

      <!-- Bottom info -->
      <div class="bottom-info">
        <p>© 2025 DataStream. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<script>
import * as commMethod from '@/comm/commMethod'
import http from '@/utils/request'
import constant from '../comm/constants'
import { useMainStore } from '@/store'
import { ElMessage } from 'element-plus'

export default {
  name: 'LoginPage',
  components: {},
  data () {
    let validateSystemUserCode = (rule, value, callback) => {
      if (!value || !value.trim()) {
        callback(new Error('帐号不可为空'))
      } else {
        callback()
      }
    }
    let validatePassword = (rule, value, callback) => {
      if (!value || !value.trim()) {
        callback(new Error('密码不可为空'))
      } else {
        callback()
      }
    }
    return {
      loginForm: {
        systemUserCode: 'admin',
        password: 'admin'
      },
      submitLoading: false,
      rules: {
        systemUserCode: [
          { validator: validateSystemUserCode, trigger: 'blur' }
        ],
        password: [
          { validator: validatePassword, trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    document.addEventListener('keydown', this.enterKeydown)
  },
  beforeUnmount() {
    document.removeEventListener('keydown', this.enterKeydown);
  },
  methods: {
    enterKeydown(e) {
      if (e.key === 'Enter' || e.keyCode === 13) {
        this.submitForm()
      }
    },
    submitForm () {
      this.$refs['loginForm'].validate((valid) => {
        if (valid) {
          this.submitLoading = true
          let request = {
            systemUserCode: this.loginForm.systemUserCode,
            password: commMethod.encryptByDES(this.loginForm.password, 'task-manage-3826')
          }
          http(constant.URL_AUTH_LOGIN, 'post', request).then(res => {
            this.submitLoading = false
            const store = useMainStore()
            store.setLoginSystemUser(res.data)
            ElMessage({
              type: 'success',
              message: '登录成功，欢迎回来！',
              duration: 2000,
              showClose: false,
              offset: 24
            })
            setTimeout(() => {
              this.$router.push({name: 'homePage'})
            }, 500)
          }).catch(err => {
            this.submitLoading = false
            let errorMsg = '登录失败'
            if (err && typeof err === 'string') {
              errorMsg = err
            } else if (err && err.resultMsg) {
              errorMsg = err.resultMsg
            } else if (err && err.message) {
              errorMsg = err.message
            } else if (err && err.response && err.response.data && err.response.data.resultMsg) {
              errorMsg = err.response.data.resultMsg
            }
            ElMessage({
              type: 'error',
              message: errorMsg,
              duration: 3000,
              showClose: true,
              offset: 24
            })
          })
        } else {
          return false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;
}

/* Background gradient */
.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.login-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.4) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(102, 126, 234, 0.2) 0%, transparent 40%);
}

/* Floating decorative shapes */
.floating-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.6;
  animation: float 6s ease-in-out infinite;
  z-index: 0;
}

.shape-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, transparent 70%);
  bottom: 15%;
  right: 15%;
  animation-delay: 2s;
}

.shape-3 {
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.06) 0%, transparent 70%);
  top: 60%;
  left: 20%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-20px) scale(1.05);
  }
}

/* Login container */
.login-container {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
}

/* Brand section */
.brand-section {
  text-align: center;
  margin-bottom: 2rem;
  animation: fadeInDown 0.6s ease-out;
}

.logo-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 1rem;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.logo-icon svg {
  width: 32px;
  height: 32px;
  color: white;
}

.brand-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: white;
  margin: 0 0 0.5rem 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.brand-subtitle {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
  font-weight: 400;
}

/* Login card */
.login-card {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 2.5rem;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 8px 24px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out 0.2s backwards;
}

.card-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
  text-align: center;
}

.card-subtitle {
  font-size: 0.9375rem;
  color: #64748b;
  margin: 0 0 2rem 0;
  text-align: center;
}

/* Form styles */
.form-group {
  margin-bottom: 1.25rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: #334155;
  margin-bottom: 0.5rem;
}

.custom-input {
  width: 100%;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 0.75rem 1rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  background: #f8fafc;
}

.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  background: #f1f5f9;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15), 0 2px 8px rgba(0, 0, 0, 0.08);
  background: white;
}

.custom-input :deep(.el-input__inner) {
  font-size: 0.9375rem;
  color: #1e293b;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.input-icon {
  width: 18px;
  height: 18px;
  color: #94a3b8;
}

.custom-input :deep(.is-focus .input-icon) {
  color: #667eea;
}

/* Submit button */
.submit-button {
  width: 100%;
  height: 48px;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 12px;
  margin-top: 0.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.2s ease;
}

.submit-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.submit-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.submit-button:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.3), 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* Card footer */
.card-footer {
  margin-top: 1.5rem;
  text-align: center;
}

.footer-text {
  font-size: 0.8125rem;
  color: #94a3b8;
  margin: 0;
}

/* Bottom info */
.bottom-info {
  margin-top: 2rem;
  text-align: center;
  animation: fadeInUp 0.6s ease-out 0.4s backwards;
}

.bottom-info p {
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

/* Animations */
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 640px) {
  .login-card {
    padding: 2rem 1.5rem;
  }

  .brand-title {
    font-size: 2rem;
  }

  .card-title {
    font-size: 1.5rem;
  }

  .shape-1 {
    width: 200px;
    height: 200px;
  }

  .shape-2 {
    width: 150px;
    height: 150px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .floating-shape,
  .brand-section,
  .login-card,
  .bottom-info {
    animation: none;
  }
}
</style>
