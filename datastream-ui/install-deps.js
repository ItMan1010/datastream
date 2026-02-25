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
import path from 'path';

console.log('开始安装依赖...');

try {
  // 清理 node_modules 和 package-lock.json
  if (fs.existsSync('node_modules')) {
    console.log('删除 node_modules...');
    fs.rmSync('node_modules', { recursive: true, force: true });
  }

  if (fs.existsSync('package-lock.json')) {
    console.log('删除 package-lock.json...');
    fs.unlinkSync('package-lock.json');
  }

  // 清理 npm 缓存
  console.log('清理 npm 缓存...');
  execSync('npm cache clean --force', { stdio: 'inherit' });

  // 安装依赖
  console.log('安装依赖...');
  execSync('npm install --registry=https://registry.npmmirror.com/', { stdio: 'inherit' });

  console.log('依赖安装完成！');
} catch (error) {
  console.error('安装失败:', error.message);
  process.exit(1);
}
