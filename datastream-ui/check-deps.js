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
import { execSync } from 'child_process';
import fs from 'fs';

console.log('检查依赖...');

try {
  // 检查 node_modules 是否存在
  if (!fs.existsSync('./node_modules')) {
    console.log('node_modules 不存在，正在安装依赖...');
    execSync('npm install', { stdio: 'inherit' });
  }

  // 检查关键依赖是否存在
  const keyDeps = [
    'vue',
    'vite',
    'element-plus',
    'axios',
    'qs',
    'pinia',
    'vue-router',
    'dayjs',
    'crypto-js',
    'mitt',
    'jquery',
    'echarts',
    'jsplumb',
    'marked',
    'uuid',
    'xlsx'
  ];

  console.log('检查关键依赖...');
  keyDeps.forEach(dep => {
    const depPath = `./node_modules/${dep}`;
    if (fs.existsSync(depPath)) {
      console.log(`✅ ${dep} 已安装`);
    } else {
      console.log(`❌ ${dep} 未安装`);
    }
  });

  // 重新安装依赖以确保所有依赖都正确
  console.log('重新安装依赖以确保完整性...');
  execSync('npm install', { stdio: 'inherit' });

  console.log('依赖检查完成！');

} catch (error) {
  console.error('依赖检查失败:', error.message);
  process.exit(1);
}
