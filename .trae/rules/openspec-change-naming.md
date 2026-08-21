# OpenSpec 变更命名规范

## 规则

每个 OpenSpec change 目录名必须使用三位数字前缀，格式为 `NNN-<kebab-case>`。

- 前缀为三位数字序号（如 `001`、`002`、…），用于标识需求编号，便于在 `openspec/changes/` 中按需求排序与追溯。
- 序号从 `001` 递增；创建新 change 时，取 `openspec/changes/` 下已有最大序号 +1。
- 描述部分使用 kebab-case（小写字母与连字符），简明概括该需求。

## 示例

- `001-replace-ui-color-scheme`（第 1 个需求：替换界面配色）
- `002-fix-auth-bypass`（第 2 个需求：修复认证绕过）

## 适用范围

适用于本项目所有通过 `openspec new change` 创建的变更目录命名。

## 注意事项
1:  所有新开发的代码必须保持当前前端或后端代码的命名规范、包结构、类名、方法名等。
（如：`com.itman.datastream.ui.*`、`com.itman.datastream.service.*` 等）

2: 新增页面风格时，必须保持当前页面的布局、颜色、字体等样式。
（如：`data_stream_column_type_map` 页面）

3: 能复用当前系统组件，避免重复开发。
