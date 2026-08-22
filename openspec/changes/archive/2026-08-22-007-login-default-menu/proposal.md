## Why

当前系统登录后固定落地到「系统概览」页，未根据工号的菜单权限动态选择默认落地页。当工号未配置「系统概览」权限时，登录后仍可能展示无权限的概览菜单/Tab，甚至直接进入无权限页面；需要让系统在登录后默认展示该工号在左侧菜单顺序中第一个有权限的菜单。

## What Changes

- 登录后默认落地页由固定「系统概览」改为动态选择：按左侧菜单展示顺序，选取当前用户有权限访问的第一个菜单。
- 默认落地菜单同步初始化对应的 Tab、菜单高亮与面包屑。
- 「系统概览」菜单、Tab 与路由与其他菜单保持一致，统一纳入菜单权限控制（无权限不显示、路由不可访问并提示）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `system-permission/menu-permission`: 新增需求——登录后默认展示左侧菜单顺序中第一个有权限的菜单；「系统概览」与其他菜单同等受权限控制。

## Impact

- 前端路由：`datastream-ui/src/router/index.js`（默认重定向、受保护路由列表、路由守卫回退逻辑）。
- 前端布局与 Tab：`datastream-ui/src/views/layout/index.vue`、`datastream-ui/src/composables/useTabManage.js`。
- 菜单配置：`datastream-ui/src/composables/useMenuConfig.js`（菜单展示顺序）。
