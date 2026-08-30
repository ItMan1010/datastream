## 1. 公共实体与错误码

- [x] 1.1 新建 StatTaskTypeCountEntity（taskType/taskCount）
- [x] 1.2 新建 StatTaskStateCountEntity（state/taskCount）
- [x] 1.3 StatSystemInfoEntity 增加 taskTypeCountList/taskStateCountList 两个 List 字段
- [x] 1.4 DataStreamErrorCode 增加按类型/按状态统计失败错误码

## 2. 后端统计链路

- [x] 2.1 DataStreamMapper 增加 statMoveTaskCountGroupByType / statMoveTaskCountGroupByState 接口
- [x] 2.2 DataStreamMapper.xml 增加对应分组统计 SQL（兼容 Sharding hint）
- [x] 2.3 DataStreamDao 增加 statMoveTaskCountGroupByType / statMoveTaskCountGroupByState
- [x] 2.4 IMetaService 增加两个分组统计方法声明
- [x] 2.5 MetaServiceImpl 实现两个分组统计方法
- [x] 2.6 DataBaseHandler.statSystemInfo 填充 taskTypeCountList / taskStateCountList

## 3. 前端概览页

- [x] 3.1 useOverview.js 增加 taskTypeCountList/taskStateCountList 状态与两个 ECharts 实例
- [x] 3.2 useOverview.js 增加任务类型/任务状态图表构建与生命周期处理（init/resize/dispose）
- [x] 3.3 overview/index.vue 增加「任务类型分布」「任务状态分布」两个图表面板并接入生命周期

## 4. 验证

- [x] 4.1 后端编译通过（mvn -q compile）
- [x] 4.2 前端构建通过（npm run build）
