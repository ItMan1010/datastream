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
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'
import autoprefixer from 'autoprefixer'

const __dirname = dirname(fileURLToPath(import.meta.url))

// https://vitejs.dev/config/
export default defineConfig({
  base: '/datastream/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      'vue$': 'vue/dist/vue.esm-bundler.js'
    }
  },
  server: {
    port: 3000,
    open: true,
    cors: true,
    proxy: {
      // 只代理 API 请求，不代理静态资源
      '/datastream/api': {
        target: 'http://localhost:9199',
        changeOrigin: true,
        // 排除前端入口文件和静态资源，只代理 API 请求
        bypass: (req, res, options) => {
          const url = req.url;
          // 排除前端入口
          if (
              // 排除静态资源
              url.includes('/static/') ||
              url.endsWith('.css') ||
              url.endsWith('.js') ||
              url.endsWith('.ico') ||
              url.endsWith('.png') ||
              url.endsWith('.jpg') ||
              url.endsWith('.svg') ||
              url.endsWith('.woff') ||
              url.endsWith('.woff2') ||
              url.endsWith('.ttf') ||
              url.endsWith('.eot')) {
            return null; // null 表示不代理，由 Vite 处理
          }
        },
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('Sending Request to the Target:', req.method, req.url);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
          });
        }
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'static',
    sourcemap: false,
    rollupOptions: {
      output: {
        chunkFileNames: 'static/js/[name]-[hash].js',
        entryFileNames: 'static/js/[name]-[hash].js',
        assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
      }
    }
  },
  css: {
    postcss: {
      plugins: [
        autoprefixer
      ]
    }
  },
  define: {
    __VUE_OPTIONS_API__: true,
    __VUE_PROD_DEVTOOLS__: false,
    global: 'globalThis'
  },
  optimizeDeps: {
    include: ['jquery']
  }
})
