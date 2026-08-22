## Context

登录后落地页目前在前端被硬编码为「系统概览」：路由 `homePage` 的空子路径固定 `redirect: '/datastream/overview'`，布局 `onMounted` 也写死 `initTabs('overview', ...)`，`activeMenuIndex` 初始值写死为 `'5'`（系统概览）。因此无论工号是否具备「系统概览」权限，登录后都会展示概览菜单/Tab 并落地到概览页，不符合按菜单权限动态渲染的要求。

## Goals / Non-Goals

**Goals:**

- 登录后默认落地到「左侧菜单展示顺序中第一个有权限的菜单」。
- 默认落地时正确同步菜单高亮、面包屑与 Tab。
- 「系统概览」与其他菜单一致纳入权限控制（无权限不显示、路由不可访问并提示）。

**Non-Goals:**

- 不改变菜单权限的授予/回收逻辑（已在 `006-rbac-permission-management` 中定义）。
- 不引入后端权限计算，落地页选择纯前端基于登录后 `user.menus` 完成。

## Decisions

### 1. 菜单展示顺序集中为单一常量

在 `router/index.js` 定义 `MENU_ORDER`（左侧菜单展示顺序的路由名称数组），用于确定「第一个有权限的菜单」。顺序与 `layout/index.vue` 侧边栏模板中的渲染顺序保持一致。

- 备选：从 `useMenuConfig` 的 `menuNameArr` 派生顺序。但 `menuNameArr` 是按索引的稀疏数组（含父菜单空串、遗留项 `auditLog`/`batchMoveTask`），不适合直接表达「展示顺序」，故单独维护常量。

### 2. 路由默认重定向动态化

将 `homePage` 空子路径的静态重定向改为函数 `redirect: () => ({ name: getFirstAvailableMenuName() })`，其中 `getFirstAvailableMenuName()` 按 `MENU_ORDER` 顺序返回 `store.getLoginMenus` 中第一个命中的菜单名（管理员直接返回 `overview`，无任何菜单时回退 `overview`）。

- 备选：在布局 `onMounted` 里用 `router.replace` 手动跳转。缺点是无法保证在 `checkInitialSync`/菜单渲染前完成跳转，存在时序竞态，故采用路由层重定向。

### 3. `overview` 纳入路由保护并修复守卫回退

将 `overview` 加入 `PROTECTED_ROUTE_NAMES`，使无权限用户访问 `/datastream/overview` 被拦截。守卫回退由写死 `next({ name: 'overview' })` 改为 `next({ name: getFirstAvailableMenuName() })`；当该用户无任何菜单权限（回退结果仍无权限）时跳转登录页，避免死循环。

### 4. Tab/菜单状态按当前路由初始化

- `layout/index.vue` 的 `onMounted` 移除硬编码 `initTabs('overview', ...)`，改为 `tabManage.checkInitialSync()`，按当前（已被重定向的）路由创建默认 Tab。
- `useTabManage.js` 中 `activeMenuIndex` 初始值由写死 `'5'` 改为按 `route.name` 计算，确保 `el-menu` 的 `:default-active` 在首次渲染时即为正确菜单索引（`default-active` 非响应式，须在渲染前给对初值）。
- `checkInitialSync` 移除硬编码 `overview` 回退，改为按 `route.name` 同步；`forceSyncTabWithRoute` 在创建 Tab 后调用 `updateMenuState` 同步高亮与面包屑。

### 5. 复用既有 `hasMenu` 权限判断

侧边栏菜单项的显隐与默认落地页的权限判断均复用 `usePermission().hasMenu()`，保持与现有菜单过滤逻辑一致。

## Risks / Trade-offs

- **`default-active` 非响应式** → 通过「在 setup 阶段按 `route.name` 计算初值」规避，确保首次渲染即正确。
- **无任何菜单权限的用户** → 守卫与重定向均需回退到登录页，避免空内容页或重定向死循环。
- **`MENU_ORDER` 与侧边栏模板顺序不一致** → 新增/调整菜单时需同步维护该常量；在 tasks 中加入「验证顺序一致」检查。
- **`overview` 纳入保护后** → 管理员（`SYSTEM_ADMIN`）仍通过 `getIsAdmin` 绕过校验，行为不变。

## Migration Plan

前端改动，随 `datastream-ui` 重新构建打包生效，无数据迁移。回滚策略：恢复静态重定向与硬编码初始化即可。
