## Context

概览页数据来自 `/api/stat/statSystemInfo`，由 `DataBaseHandler.statSystemInfo` 聚合 `IMetaService` 的统计结果后通过 `StatSystemInfoEntity` 返回。既有统计已覆盖聚合计数（`statMoveTaskCount`/`statLinkTaskCount`）与按天趋势（`statMoveTaskCountGroupByDay`），但未覆盖按任务类型/任务状态的分组维度。

任务类型（`task_type`：1 数据迁移、2 数据清理、3 迁移清理、4 结构迁移、5 数据稽核、6 增量迁移）与任务状态（`state`：0 等待中、1 运行中、2 运行结束、3 运行失败、4 运行暂停）均落在 `data_stream_move_task` 表；链表任务（`data_stream_link_task`）无 `task_type` 维度，故本变更仅针对迁移任务表统计。

## Goals / Non-Goals

**Goals:**
- 概览页新增任务类型分布与任务状态分布两个可视化面板，与既有卡片/趋势图风格统一。
- 复用既有统计链路（Mapper/DAO/Service/Handler）与前端 ECharts 集成方式，不引入新依赖。

**Non-Goals:**
- 不统计链表任务（`data_stream_link_task`）的类型维度（该表无 task_type）。
- 不引入独立的统计接口或数据库表（沿用现有 `statSystemInfo` 聚合接口）。
- 不改变既有概览卡片与趋势图的行为。

## Decisions

### 决策 1：沿用 statSystemInfo 聚合接口，而非新增接口
在 `StatSystemInfoEntity` 中新增两个列表字段，由既有 `statSystemInfo` 接口一并返回。

- 理由：概览页已整体依赖该接口一次性渲染全部指标，新增接口会增加请求与首屏复杂度；复用接口也符合现有前端 `useOverview` 的单一数据源模式。

### 决策 2：分组统计 SQL 不区分 Oracle/MySQL（无需 LikeOracle 变体）
按 `task_type`/`state` 分组的 SQL 不涉及日期函数，在 MySQL/Oracle/H2 上语义一致，仅保留 Sharding hint 分支（`dbType` 参数）。

- 理由：与既有 `statMoveTaskCount` 的处理一致（该接口同样仅依赖 `dbType` 做 hint），避免不必要的重复 SQL。

### 决策 3：前端复用 ECharts 渲染，左侧条形图 + 右侧环形图
任务类型分布用横向条形图（6 类中文标签便于阅读），任务状态分布用环形图（5 种状态 + 语义色），两者并排各占半宽。

- 理由：与既有趋势图同栈（ECharts），视觉与交互一致；类型维度类别多、标签长，条形图更清晰；状态维度为占比语义，环形图更直观。

### 决策 4：前端标签复用既有常量映射
任务类型名称、任务状态名称与语义色复用 `@/constants/taskConstants` 的 `TASK_TYPE_MAP`/`TASK_STATE_MAP`，避免前端重复维护中文映射。

- 理由：遵循项目「复用当前系统组件/常量、避免重复开发」的约定；后端仅返回编码（taskType/state），名称与颜色由前端统一映射。

## Risks / Trade-offs

- [某类型/状态无数据] → 分组查询不会返回该维度行，前端在渲染前按完整枚举补齐 0 值，保证图例完整、布局稳定。
- [元数据库为 Oracle/H2] → 分组 SQL 不含日期函数，已规避日期方言差异；Sharding hint 沿用既有写法。
- [状态「等待中」与「运行结束」在表格常量中同色] → 图表使用独立语义色数组，保证环形图区分度，不影响表格标签颜色。

## Migration Plan

1. 后端：重新编译打包 `datastream-admin`/`datastream-engine`/`datastream-common` 并部署。
2. 前端：重新构建 `datastream-ui` 使新面板生效。
3. 回滚：回退上述前后端改动即可，无数据库变更，不影响既有数据。
