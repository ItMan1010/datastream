import fs from 'fs';
import p from 'path';
import {fileURLToPath} from 'url';

// ES模块中获取__dirname的标准方法
const __filename = fileURLToPath(import.meta.url);
const __dirname = p.dirname(__filename);


function deleteFolder(path) {
  if (fs.existsSync(path)) {
    if (fs.statSync(path).isDirectory()) {
      const files = fs.readdirSync(path);
      files.forEach((file) => {
        const curPath = p.join(path, file);
        if (fs.statSync(curPath).isDirectory()) {
          deleteFolder(curPath);
        } else {
          fs.unlinkSync(curPath);
        }
      });
      fs.rmdirSync(path);
    } else {
      fs.unlinkSync(path);
    }
  }
}

function copyFolder(from, to) {
  if (!fs.existsSync(from)) {
    console.error(`源目录不存在: ${from}`);
    return 0;
  }

  // 如果目标目录不存在则创建
  if (!fs.existsSync(to)) {
    fs.mkdirSync(to, {recursive: true});
    console.log(`创建目标目录: ${to}`);
  }

  let fileCount = 0;
  const files = fs.readdirSync(from);

  files.forEach((file) => {
    const targetPath = p.join(from, file);
    const toPath = p.join(to, file);

    if (fs.statSync(targetPath).isDirectory()) {
      // 复制文件夹
      const count = copyFolder(targetPath, toPath);
      fileCount += count;
    } else {
      // 拷贝文件
      fs.copyFileSync(targetPath, toPath);
      fileCount++;
    }
  });

  return fileCount;
}

// __dirname 是 datastream-ui/src/utils，需要回到项目根目录，所以用 ../../../
const staticDirectory = p.join(__dirname, '../../../datastream-starter/src/main/resources/static/');
const distDirectory = p.join(__dirname, '../../dist');

console.log('开始文件拷贝...');
console.log(`源目录: ${distDirectory}`);
console.log(`目标目录: ${staticDirectory}`);

// 检查源目录是否存在
if (!fs.existsSync(distDirectory)) {
  console.error(`错误: 源目录不存在: ${distDirectory}`);
  console.error('请先运行 vite build 生成 dist 目录');
  process.exit(1);
}

try {
  // 删除目标目录（如果存在）
  if (fs.existsSync(staticDirectory)) {
    console.log(`删除现有目标目录: ${staticDirectory}`);
    deleteFolder(staticDirectory);
  }

  // 复制整个 dist 目录到 static 目录
  // 这样 dist/static/ 会变成 static/static/，符合 index.html 中 /static/js/... 的引用路径
  const fileCount = copyFolder(distDirectory, staticDirectory);

  if (fileCount > 0) {
    console.log(`文件拷贝成功！共复制 ${fileCount} 个文件`);
    console.log(`文件结构: static/index.html, static/static/js/..., static/static/css/...`);
  } else {
    console.warn('警告: 没有文件被复制');
  }
} catch (error) {
  console.error('文件拷贝失败:', error);
  console.error(error.stack);
  process.exit(1);
}
