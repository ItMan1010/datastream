## 1. 权限服务扩展

- [x] 1.1 在 `datastream-security` 的 `PermissionService` 增加只读方法 `isAdmin()`（当前认证含 `ROLE_SYSTEM_ADMIN` 或兼容 `ROLE_TASK_ALL`）与 `getCurrentUserCode()`（从 `DsJwtUser.getSystemUserInfo().getSystemUserCode()` 获取，不抛异常）
- [x] 1.2 确认 JWT 认证链路重建的 `DsJwtUser` 主体携带 `systemUserCode`，必要时补充缺失字段的装载

## 2. 任务列表与 count 查询透传过滤

- [x] 2.1 `DataStreamMapper.java` 为任务查询相关方法（`getMoveTaskCount*` 与 `queryDataMoveTaskBy*` 及其 Oracle 分页变体）增加可空 `systemUserCode` 参数
- [x] 2.2 `DataStreamMapper.xml` 在对应查询的 WHERE 中追加 `<if test="systemUserCode != null and systemUserCode != ''">and system_user_code = #{systemUserCode}</if>`
- [x] 2.3 `DataStreamDao` 相应方法透传 `systemUserCode`
- [x] 2.4 `IMetaService` / `MetaServiceImpl` 相应方法透传 `systemUserCode`
- [x] 2.5 `DataMoveHandler.queryDataMoveTaskCount` 与 `queryDataMoveTaskList` 计算过滤值（管理员传 `null`，普通用户传当前编码）并逐 queryFlag 分支透传；`queryFlag=1` 按 taskId 查询走归属校验
- [x] 2.6 `StartExecutor` 等引擎内部调用 `queryDataMoveTaskByState` 显式传 `null`，保持返回全部任务

## 3. 详情与明细接口归属校验

- [x] 3.1 在 `queryTaskProgress`、`queryDataMoveInfoList`、`queryTableMoveList` 中，先按 taskId 取任务并校验归属，非管理员且非本人时返回无权限错误且不返回数据
- [x] 3.2 复用/补充中文无权限错误提示，包含被拒绝访问的任务标识

## 4. 验证

- [x] 4.1 后端编译通过（`mvn -q -pl datastream-admin -am compile`）
- [x] 4.2 启动应用，验证普通用户仅见本人创建任务、系统管理员可见全部任务、普通用户凭 taskId 访问他人任务详情被拒绝
