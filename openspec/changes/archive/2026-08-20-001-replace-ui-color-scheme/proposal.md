## Why

当前前端界面使用紫色渐变背景（`#667eea → #764ba2`，登录页还叠加粉色 `#f093fb`），是典型的「AI 生成风格」，与企业级数据迁移工具的定位不符，显得不够专业。同时系统内存在三套互不协调的颜色——紫色渐变背景、青色主色 `#27BEF3`、蓝色 header/按钮 `#409EFF`，视觉上割裂，削弱了产品的可信度与专业感。

## What Changes

- 移除全部紫色/粉色渐变背景（`#667eea`、`#764ba2`、`#f093fb`、`#8b7dd8` 等）。
- 采用专业的企业级配色：**深海军蓝侧边栏/顶部 + 浅灰内容背景 + 信任蓝主色**。
- 统一散落的主色：将青色 `#27BEF3` 与蓝色 `#409EFF` 收敛为单一品牌主色 `#2563EB`。
- 收敛配色为语义化 CSS 变量（设计令牌），消除各组件硬编码的十六进制颜色值。
- 保持现有暗黑模式可用，并同步更新暗黑模式配色以保持一致。

## Capabilities

### New Capabilities

- `ui-theme`: 定义数据迁移系统的专业 UI 配色规范——背景、侧边栏/顶部、主色、语义色与暗黑模式的统一设计要求。

### Modified Capabilities

<!-- 无既有 capability 需要修改 -->

## Impact

- **前端 `datastream-ui`**（主要影响）：
  - `src/App.vue`（全局 CSS 变量与暗黑模式变量）
  - `src/views/LoginView.vue`（登录页背景与按钮渐变）
  - `src/views/layout/index.vue`（侧边栏/顶部/logo 渐变）
  - `src/assets/css/variables.scss`（Element Plus 主题变量）
  - `src/assets/css/public.css`（表格/消息等公共样式）
- 大量组件中硬编码的 `#409EFF` / `#409eff`（约 50 处）需统一为语义令牌。
- 后端无改动。
