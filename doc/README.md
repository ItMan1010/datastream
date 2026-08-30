# DataStream 文档索引

> 本目录集中存放工程文档（代码模块目录除外）。根目录仅保留 `README.md`（项目入口）与 `LICENSE`（开源协议）。

## 目录结构总览

```
datastream/
├── README.md                 # 项目入口文档（功能介绍、部署、演示）
├── LICENSE                   # Apache License 2.0 开源协议
├── doc/                      # 文档中心（本目录）
│   ├── README.md             # ← 本文档索引
│   ├── reviews/              # 评审报告
│   ├── guides/               # 开发指南
│   └── sql/                  # SQL 脚本（DDL / 种子数据 / 测试数据）
├── openspec/                 # 变更规范（specs 规范 + changes 变更档案）
├── image/                    # README 引用的图片资源
└── .trae/                    # Trae IDE 工具配置（rules/commands/skills）
```

## 1. 项目入口

| 文档 | 位置 | 说明 |
|------|------|------|
| 项目 README | [`../README.md`](../README.md) | 功能简介、技术栈、模块结构、安装部署、操作演示 |

## 2. 评审报告（`doc/reviews/`）

| 文档 | 位置 | 说明 |
|------|------|------|
| 代码审查报告 | [`reviews/CODE_REVIEW.md`](reviews/CODE_REVIEW.md) | 静态代码审查，70+ 问题点，按 P0~P3 分级 |
| 架构优化分析 | [`reviews/ARCHITECTURE_REVIEW.md`](reviews/ARCHITECTURE_REVIEW.md) | 架构评审，三大优化点与优先级 |

## 3. 开发指南（`doc/guides/`）

| 文档 | 位置 | 说明 |
|------|------|------|
| 测试约定 | [`guides/TESTING.md`](guides/TESTING.md) | 测试分层、回归测试约定、测试基类 |

## 4. SQL 脚本（`doc/sql/`）

> 注意：本目录路径 `doc/sql/...` 被代码常量与 OpenSpec 规范引用，请勿改动目录名。

| 分类 | 文件 | 说明 |
|------|------|------|
| 建库 DDL | [`sql/datastream-mysql-ddl.sql`](sql/datastream-mysql-ddl.sql) | MySQL 元数据库建表脚本 |
| 建库 DDL | [`sql/datastream-h2-ddl.sql`](sql/datastream-h2-ddl.sql) | H2 元数据库建表脚本 |
| 种子数据 | [`sql/datastream-rbac-seed.sql`](sql/datastream-rbac-seed.sql) | RBAC 权限种子数据 |
| 类型配置 | [`sql/datastream-column-type-map-v2.sql`](sql/datastream-column-type-map-v2.sql) | 字段类型定义与映射初始化 |
| 权限迁移 | [`sql/datastream-rbac-task-type-permission-migration.sql`](sql/datastream-rbac-task-type-permission-migration.sql) | 任务类型权限存量迁移 |
| 测试数据 | [`sql/test-data/`](sql/test-data/) | 各数据库测试数据、校验脚本、Docker Compose |

## 5. 变更规范（`openspec/`）

| 内容 | 位置 | 说明 |
|------|------|------|
| 当前规范 | `openspec/specs/` | 各能力域（权限、数据源、主题等）的当前规格 |
| 变更档案 | `openspec/changes/archive/` | 已归档的变更提案（proposal/design/tasks） |
| 配置 | `openspec/config.yaml` | OpenSpec 配置 |

## 6. 其他

| 内容 | 位置 | 说明 |
|------|------|------|
| 图片资源 | `image/` | 根 README 引用的界面截图、二维码 |
| IDE 工具配置 | `.trae/` | Trae 的 rules / commands / skills |
| 开源协议 | `LICENSE` | Apache License 2.0 |

## 附：非文档目录（运行时 / 构建产物，已 gitignore）

| 目录/文件 | 说明 |
|-----------|------|
| `h2db/` | H2 文件库运行时数据 |
| `logs/` | 运行日志 |
| `target/` | Maven 构建产物 |
| `pom-xml-flattened` | Maven flatten 插件生成的中间文件 |
