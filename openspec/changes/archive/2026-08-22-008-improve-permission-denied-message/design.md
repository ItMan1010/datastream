## Context

权限校验链路（现状）：

- 受保护接口通过 `@PreAuthorize("@permissionService.hasPermission('task:create')")` 声明所需数据权限（见 [TaskController.java](../../../datastream-admin/src/main/java/com/itman/datastream/admin/controller/TaskController.java)）。
- [PermissionService](../../../datastream-security/src/main/java/com/itman/datastream/security/service/PermissionService.java) 的 `hasPermission` 仅返回 `boolean`，失败时不携带「缺哪个权限」的信息。
- [SecurityConfig](../../../datastream-security/src/main/java/com/itman/datastream/security/config/SecurityConfig.java) 的 `accessDeniedHandler` 对任何拒绝统一返回固定文案「无权限执行该操作」。
- 前端 [fetch.js](../../../datastream-ui/src/utils/fetch.js) 对 403 与其它非 401 错误一样 `Promise.reject(error)`，业务层（如 [useTaskCreate.js](../../../datastream-ui/src/composables/useTaskCreate.js)）将 AxiosError 字符串化后展示为「生成任务失败：AxiosError: Request failed with status code 403」。

权限中文名称存储在 `data_stream_permission.permission_name`（如 `task:create` → 「创建迁移任务」），由 `datastream-engine` 的 `SystemPermissionDao` 读取。

## Goals / Non-Goals

**Goals:**

- 无权限拒绝时，后端返回可读中文提示，且包含缺失权限的中文名称与编码。
- 前端 403 时展示该中文提示，不再展示原始 AxiosError。
- 无法解析具体权限时安全回退为通用中文提示，不泄露技术异常。

**Non-Goals:**

- 不改变权限判定规则（谁拥有什么权限的判断逻辑不变）。
- 不改变 `@PreAuthorize` 的声明方式与各受保护接口清单。
- 不引入新的权限管理流程或数据模型。

## Decisions

### 1. 通过自定义异常在权限失败时携带具体权限信息

`hasPermission` 失败时由「返回 false」改为「抛出自定义异常 `PermissionDeniedException`（继承 `AccessDeniedException`）」，异常 message 为 `无【{权限名}（{权限编码}）】权限，请联系管理员`。

- **理由**：`@PreAuthorize` 表达式求值抛出的 `AccessDeniedException` 会由 `ExceptionTranslationFilter` 交给 `accessDeniedHandler`，无需改动接口声明，即可把「具体缺哪个权限」传到拒绝处理器。
- **替代方案**：在 `accessDeniedHandler` 内解析请求/注解反推权限 → 复杂且易错，放弃。
- **替代方案**：在 `SecurityContext` 或 `ThreadLocal` 暂存缺失权限 → 需要维护清理逻辑，放弃。

### 2. `hasAnyPermission` 内部先做无异常判断，全部失败再抛组合异常

新增私有 `checkPermission(String code)` 返回 `boolean`（不抛异常）；公开 `hasPermission` 调用 `checkPermission`，失败时抛单权限异常；`hasAnyPermission` 对每个候选调用 `checkPermission`，全部失败时抛「无【A】或【B】权限」的组合异常。

- **理由**：避免 `hasAnyPermission` 第一个候选失败就提前抛出，无法尝试后续候选。

### 3. `accessDeniedHandler` 按异常类型透出文案

`accessDeniedHandler` 判断 `accessDeniedException instanceof PermissionDeniedException`：是则用 `getMessage()`，否则回退通用文案「无权限执行该操作」。

- **理由**：避免把 Spring 默认 `AccessDeniedException("Access is denied")` 这类技术文案暴露给用户。

### 4. 权限名称来源：复用 `SystemPermissionDao` 查询并缓存

在 `SystemPermissionDao` 增加 `selectPermissionNameByCode(String code)`（Mapper 查 `data_stream_permission`），`PermissionService` 注入并做轻量缓存（如 `Map` + 惰性加载）。查询失败或名称为空时，用「该操作」作为名称回退，编码仍保留。

- **理由**：权限名是运行数据，不应硬编码；`security` 模块已依赖 `datastream-engine` 的 `SystemPermissionDao`，复用无新增依赖。
- **风险**：鉴权热点查库 → 用进程内缓存缓解；缓存失效策略为「新增/编辑权限时清缓存」（后续可扩展，本次先做惰性加载缓存）。

### 5. 前端在 `fetch.js` 统一处理 403

响应拦截器 `error` 分支新增 403 处理：关闭已有提示，读取 `error.response.data.resultMsg`；有值则以该字符串 `Promise.reject`，无值则回退通用文案「无权限执行该操作，请联系管理员」。业务层 catch 保持 `生成任务失败：${err}`，因 `err` 已为可读中文，最终展示如「生成任务失败：无【创建迁移任务（task:create）】权限，请联系管理员」。

- **理由**：改动集中在拦截器，业务层无需逐处识别 AxiosError；`reject` 字符串而非 Error 对象，保证 `${err}` 模板字符串直接得到中文。

## Risks / Trade-offs

- [权限名查询失败导致提示降级为「该操作」] → 兜底文案仍包含权限编码，可定位；日志记录查询失败原因。
- [`hasPermission` 改为抛异常，可能影响其它调用方] → 全项目仅 `@PreAuthorize` 通过 `permissionService` 调用，无直接 Java 调用方，风险可控。
- [缓存与数据库权限名不一致] → 权限名极少变更，且本次为惰性加载缓存；后续如权限支持改名，可补缓存失效。
- [业务层前缀「生成任务失败」对权限场景语义略宽] → 属既有文案，不在本次范围内调整；核心目标是「说清缺哪个权限」，已满足。
