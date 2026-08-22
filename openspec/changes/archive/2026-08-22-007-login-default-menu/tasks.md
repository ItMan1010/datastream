## 1. 路由默认落地页与权限控制（router/index.js）

- [x] 1.1 新增 `MENU_ORDER` 常量（左侧菜单展示顺序）与 `getFirstAvailableMenuName()` 函数
- [x] 1.2 将 `homePage` 空子路径的静态重定向改为动态重定向 `() => ({ name: getFirstAvailableMenuName() })`
- [x] 1.3 将 `overview` 加入 `PROTECTED_ROUTE_NAMES`
- [x] 1.4 修复守卫无权限回退逻辑：重定向到首个有权限菜单，无任何菜单权限时回登录页，避免死循环

## 2. Tab 与菜单状态初始化（useTabManage.js）

- [x] 2.1 `activeMenuIndex` 初值按 `route.name` 计算，不再写死 `'5'`
- [x] 2.2 `checkInitialSync` 移除硬编码 `overview` 回退，改为按 `route.name` 同步
- [x] 2.3 `forceSyncTabWithRoute` 在创建 Tab 后调用 `updateMenuState` 同步菜单高亮与面包屑

## 3. 布局初始化（layout/index.vue）

- [x] 3.1 `onMounted` 移除硬编码 `initTabs('overview', ...)`，改为 `tabManage.checkInitialSync()`

## 4. 构建与验证

- [x] 4.1 重新构建打包前端并重启后端（端口 9199）
- [ ] 4.2 验证无「系统概览」权限工号登录后默认落地到左侧首个有权限菜单，菜单高亮、Tab、面包屑正确
- [x] 4.3 验证有「系统概览」权限工号/管理员登录后仍默认落地到系统概览，行为不变
- [ ] 4.4 验证无「系统概览」权限工号访问 `/datastream/overview` 被拦截并提示、跳转至首个有权限菜单
