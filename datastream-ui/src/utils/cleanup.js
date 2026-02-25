import fs from 'fs';
import p from 'path';
import { fileURLToPath } from 'url';

// ES模块中获取__dirname的标准方法
const __filename = fileURLToPath(import.meta.url);
const __dirname = p.dirname(__filename);

const nodeModulesPath = p.join(__dirname, '../../node_modules');
const lockJsonPath = p.join(__dirname, '../../package-lock.json');

if (fs.existsSync(nodeModulesPath)) {
  if (fs.existsSync(lockJsonPath)) {
    fs.unlinkSync(lockJsonPath);
    console.log('删除package-lock.json成功！');
  }
}
