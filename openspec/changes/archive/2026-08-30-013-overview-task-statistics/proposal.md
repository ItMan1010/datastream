## Why

概览页当前仅展示迁移任务总数、迁移执行数、表链接任务总数、表链接任务执行数、数据源连接数等聚合指标，以及按天统计的任务趋势图，缺少按「任务类型」与「任务状态」两个维度的分布统计。用户在进入系统概览页时无法一眼掌握不同任务类型（数据迁移、数据清理、迁移清理、结构迁移、数据稽核、增量迁移）的数量占比，也无法快速了解任务当前处于何种状态（等待中、运行中、运行结束、运行失败、运行暂停）。

## What Changes

- 后端统计接口 `/api/stat/statSystemInfo` 在返回体 `statSystemInfoEntity` 中新增两个列表字段：
  - `taskTypeCountList`：按 `data_stream_move_task.task_type` 分组的任务类型统计（`taskType` + `taskCount`）。
  - `taskStateCountList`：按 `data_stream_move_task.state` 分组的任务状态统计（`state` + `taskCount`）。
- 新增公共实体 `StatTaskTypeCountEntity`、`StatTaskStateCountEntity`，并在 `StatSystemInfoEntity` 中登记上述两个列表。
- `DataStreamMapper`/DAO/Service/Handler 链路新增「按类型」「按状态」两个分组统计方法，复用既有元数据库（MySQL/Oracle/H2）兼容策略。
- 概览页新增「任务类型分布」「任务状态分布」两个可视化面板（复用 ECharts），布局与既有统计卡片/趋势图风格保持一致。

## Capabilities

### New Capabilities

- `overview-task-statistics`: 概览页任务维度统计能力——按任务类型、任务状态两个维度统计并可视化 `data_stream_move_task` 中的任务数量分布，与既有概览聚合指标、趋势图共同构成完整的系统概览视图。

## Impact

- 后端代码：`datastream-common`（新增 `StatTaskTypeCountEntity`、`StatTaskStateCountEntity`，扩展 `StatSystemInfoEntity`、`DataStreamErrorCode`）、`datastream-engine`（`DataStreamMapper`、`DataStreamDao`、`DataStreamMapper.xml`）、`datastream-admin`（`IMetaService`、`MetaServiceImpl`、`DataBaseHandler`）。
- 前端代码：`datastream-ui`（`useOverview.js`、`views/overview/index.vue`）。
- 无数据库结构变更（仅新增分组统计 SQL，基于既有 `task_type`/`state` 列）。
