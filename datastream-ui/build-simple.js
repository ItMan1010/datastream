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

console.log('开始简化构建...');

try {
  // 检查依赖
  console.log('检查依赖...');
  execSync('npm run check-deps', { stdio: 'inherit' });

  // 检查 dist 目录是否存在，如果存在则删除
  const distPath = './dist';
  if (fs.existsSync(distPath)) {
    console.log('删除旧的 dist 目录...');
    fs.rmSync(distPath, { recursive: true, force: true });
  }

  // 使用简化的构建命令
  console.log('运行 Vite 构建...');
  execSync('npx vite build --mode production', {
    stdio: 'inherit',
    env: { ...process.env, NODE_ENV: 'production' }
  });

  console.log('构建完成！');
} catch (error) {
  console.error('构建失败:', error.message);
  process.exit(1);
}
