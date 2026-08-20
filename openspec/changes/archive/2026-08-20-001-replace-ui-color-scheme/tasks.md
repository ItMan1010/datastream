## 1. 全局设计令牌（App.vue）

- [x] 1.1 更新 `:root` 浅色模式变量：`--bg-primary` 改为纯色 `#F8FAFC`，`--header-bg` 改为 `#1E3A5F`，新增 `--sidebar-bg: #1E3A5F`、`--primary-color: #2563EB`、`--text-primary: #0F172A`、`--text-secondary: #475569`、`--border-color: #E2E8F0`
- [x] 1.2 更新 `.dark-mode` 变量：背景 `#0F172A`、卡片 `#1E293B`、边框 `#334155`、文字 `#E2E8F0`、侧边栏/顶部 `#0F172A`
- [x] 1.3 移除 `.app-container::before` 的紫色径向光晕叠加层

## 2. Element Plus 主题变量（variables.scss）

- [x] 2.1 将 `--primary-color` 改为 `#2563EB`，`--primary-hover-color` 改为 `#60A5FA`
- [x] 2.2 将 `--background-color` 改为 `#F8FAFC`，`--border-color` 改为 `#E2E8F0`
- [x] 2.3 核对 `--el-color-primary-*` 映射仍指向新主色与悬停色

## 3. 登录页（LoginView.vue）

- [x] 3.1 替换登录页背景 `linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)` 为深海军蓝+浅灰的协调背景
- [x] 3.2 替换登录按钮 `linear-gradient(135deg, #667eea 0%, #764ba2 100%)` 为信任蓝 `#2563EB`
- [x] 3.3 清理登录页文字色 `#667eea`

## 4. 主布局（layout/index.vue）

- [x] 4.1 替换侧边栏/顶部 logo 区域 `linear-gradient(90deg, #667eea 0%, #764ba2 100%)` 为深海军蓝 `#1E3A5F`
- [x] 4.2 替换紫色元素 `#8b7dd8` 为深海军蓝/信任蓝
- [x] 4.3 核对暗黑模式下的布局配色（`#2c3e50` 系列渐变保留或统一到新令牌）

## 5. 硬编码色值收敛

- [x] 5.1 用 grep 生成 `#409EFF` / `#409eff` 全部出现位置清单
- [x] 5.2 将 CSS 与内联样式中的 `#409EFF`/`#409eff` 替换为 `var(--primary-color)`
- [x] 5.3 将 JS/ECharts 配置中的 `'#409EFF'` 字符串替换为 `'#2563EB'`
- [x] 5.4 清理残留的 `#667eea`、`#764ba2`、`#f093fb`、`#8b7dd8` 紫色色值

## 6. 验证

- [x] 6.1 grep 复查紫色（`#667eea|#764ba2|#f093fb|#8b7dd8`）与旧蓝（`#27BEF3`）残留归零
- [x] 6.2 浅色模式视觉检查：登录页、主布局、任务管理、数据源管理等页面背景与主色一致
- [x] 6.3 暗黑模式视觉检查与正文/次要文字对比度校验（不低于 4.5:1）
