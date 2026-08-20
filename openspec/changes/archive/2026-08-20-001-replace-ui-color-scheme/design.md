## Context

当前前端颜色体系存在三套互不协调的定义，且背景为「AI 风格」的紫色渐变：

- [App.vue](file:///Users/jack2/Documents/soft/datastream/datastream-ui/src/App.vue) 的 `:root` 定义 `--bg-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%)`（紫色渐变背景）、`--header-bg: #409EFF`（蓝色）。
- [variables.scss](file:///Users/jack2/Documents/soft/datastream/datastream-ui/src/assets/css/variables.scss) 定义 `--primary-color: #27BEF3`（青色）并映射到 Element Plus 的 `--el-color-primary`。
- [LoginView.vue](file:///Users/jack2/Documents/soft/datastream/datastream-ui/src/views/LoginView.vue) 与 [layout/index.vue](file:///Users/jack2/Documents/soft/datastream/datastream-ui/src/views/layout/index.vue) 中另有独立的紫色渐变与硬编码 `#409EFF`（约 50 处）。

约束：Vue 3 + Element Plus，通过 CSS 变量覆盖主题；项目已有一套 `--bg-*` / `--header-bg` / `--card-bg` 变量与暗黑模式 `.dark-mode` 覆盖。

## Goals / Non-Goals

**Goals:**

- 以一套统一、专业的企业级配色替换紫色渐变背景。
- 将主色收敛为单一信任蓝，消除青色 `#27BEF3` 与蓝色 `#409EFF` 的混用。
- 将配色沉淀为语义化 CSS 令牌，便于后续维护与暗黑模式扩展。

**Non-Goals:**

- 不改动任何页面布局、组件结构或交互逻辑。
- 不引入新的 UI 组件库或 CSS 框架。
- 不涉及后端改动。

## Decisions

### 1. 配色方向：深海军蓝侧边栏 + 浅灰背景 + 信任蓝主色

采用 ui-ux-pro-max 针对「企业数据工具」推荐的 Navy/Trust Blue 方案，明确规避其标记的反模式「AI purple/pink gradients」。

备选方案（已排除）：
- 保留青色 `#27BEF3`：与现有 Element Plus 主题变量接近，但青色调偏消费级，专业感弱。
- 纯浅色侧边栏：改动最小，但缺乏企业数据工具常有的深色导航区，层次感弱。

### 2. 设计令牌（Light Mode）

| 令牌 | 值 | 用途 |
|------|-----|------|
| `--sidebar-bg` | `#1E3A5F` | 侧边栏/顶部（深海军蓝） |
| `--header-bg` | `#1E3A5F` | 顶部导航（与侧边栏一致） |
| `--bg-primary` | `#F8FAFC` | 主内容背景（浅灰，由渐变改为纯色） |
| `--bg-secondary` / `--card-bg` | `#FFFFFF` | 卡片/面板 |
| `--primary-color` | `#2563EB` | 品牌主色（信任蓝） |
| `--text-primary` | `#0F172A` | 主要文字 |
| `--text-secondary` | `#475569` | 次要文字 |
| `--border-color` | `#E2E8F0` | 边框/分隔线 |
| `--danger-color` | `#DC2626` | 危险/错误 |

侧边栏选用 `#1E3A5F`（海军蓝）而非更接近黑的 `#0F172A`：更贴合「深蓝侧边栏」的直觉，与浅灰背景形成清晰但不过分生硬的对比，且与信任蓝 `#2563EB` 同属蓝系。

### 3. 背景从渐变改为纯色

`--bg-primary` 由紫色 `linear-gradient` 改为纯色 `#F8FAFC`，并移除 [App.vue](file:///Users/jack2/Documents/soft/datastream/datastream-ui/src/App.vue) 中 `.app-container::before` 的径向光晕叠加层。理由：企业工具界面以内容可读性优先，纯色背景更稳定、不易干扰数据展示，也避免重蹈「AI 渐变」覆辙。

### 4. Element Plus 主题变量映射

保留 [variables.scss](file:///Users/jack2/Documents/soft/datastream/datastream-ui/src/assets/css/variables.scss) 的覆盖方式，将 `--primary-color` 改为 `#2563EB`，`--primary-hover-color` 改为 `#60A5FA`，其余 `--el-color-primary-light-*` 继续映射到主色/悬停色。

### 5. 硬编码色值收敛

将散落各组件约 50 处的 `#409EFF` / `#409eff` 统一替换为 `var(--primary-color)`（或直接由 Element Plus 主色承接），并清理 `#667eea`、`#764ba2`、`#f093fb`、`#8b7dd8` 等紫色。用 grep 清单驱动，避免遗漏。

### 6. 暗黑模式

暗黑模式沿用同一令牌体系，仅切换变量值：背景 `#0F172A`、卡片 `#1E293B`、边框 `#334155`、文字 `#E2E8F0`，侧边栏/顶部加深为 `#0F172A`。确保切换后层次清晰、对比协调。

## Risks / Trade-offs

- **[大量硬编码替换易漏]** → 用 `grep -rn '#409EFF\|#409eff\|#667eea\|#764ba2\|#f093fb\|#8b7dd8'` 生成清单，逐文件替换并 grep 复查归零。
- **[暗黑模式对比度未知]** → 按 spec 的 4.5:1 要求，对暗色下的正文/次要文字分别校验对比度。
- **[登录页与布局有独立渐变定义]** → 与全局令牌一并覆盖，避免只改 App.vue 而登录页仍是紫色。
- **[主色切换可能影响语义色]** → 只替换品牌主色相关色值，保留成功/警告/危险等语义色的既有语义。
