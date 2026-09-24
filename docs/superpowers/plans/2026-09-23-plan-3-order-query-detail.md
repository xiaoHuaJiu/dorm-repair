# PLAN-3 工单查询与详情聚合实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将区域树升级为校区/单位、区域、楼栋、房间四级，并完成管理员、维修人员、学生三角色隔离的工单分页查询与统一详情聚合能力。

**Architecture:** 后端新增 `order` 功能包，Controller 只负责角色入口和参数校验，查询 Service 将公开 DTO 转换为内部 Condition 并强制注入数据范围，MyBatis 以 `repair_order` 为列表主表。详情先查主记录并通过 `RepairOrderAccessService` 校验资源归属，再分别查询过程、材料、返工、评价和流转并聚合，避免一对多 JOIN 造成重复行。

**Tech Stack:** Java 17、Spring Boot 3、Spring MVC、Spring Security、JWT、MyBatis、PageHelper、Hibernate Validator、MySQL 8、JUnit 5、MockMvc。

**Spec:** `doc/requirements/PLAN-3.md`，以及用户确认的“四级区域树，房间挂到楼栋下面”范围变更。

## Global Constraints

- 保持统一 `Result<T>` 响应体、数字业务错误码和 HTTP 200/400/401/403/404/409/500 约定。
- 接口统一使用现有 `/api` 前缀；管理员、维修人员和学生使用独立入口，底层 Service/Mapper 复用。
- 客户端不得提交 `studentUid` 或维修人员数据范围 ID；数据范围只由 `UserContext` 和数据库映射产生。
- 列表 SQL 不 JOIN 过程、材料、返工、评价、流转等一对多表。
- 详情先校验主工单访问权限，再查询关联表；空集合返回 `[]`，无评价返回 `null`。
- 不实现报修创建、重复检测算法、自动派单、接单、维修写入、返工写入或评价写入。
- 每个新增接口同步更新前端联调接口说明；阶段结束维护 `doc/verify/PLAN-3验收记录.md`。
- 不删除或重建现有 `dorm_repair` 数据库，DDL 以幂等迁移方式更新。

## Review Focus

- 维修人员登录身份是 `sys_user.id`，工单负责人是 `repair_worker.id`；映射不存在或停用时不得扩大查询范围。
- DTO 即使出现未知的身份字段，也不能覆盖 Service 注入的 `studentUid/currentAssigneeId`。
- `room_id` 必须关联四级房间节点，房间的父节点必须是同一工单 `building_id` 对应楼栋。
- PageHelper 启动后第一条查询必须是列表 SQL，多条件 JOIN 不得产生重复工单或错误 total。
- 超时判断的状态集合、截止时间为空和边界等于当前时间时必须有确定行为，不得由前端自行计算。

---

### Task 1: 将区域树升级为四级结构

**Files:**
- Modify: `doc/requirements/PLAN-3.md`
- Modify: `doc/database/dorm_repair_schema.sql`
- Modify: `doc/database/数据库DDL及索引设计.md`
- Modify: `doc/database/宿舍报修与维修工单系统核心数据表设计 V1.md`
- Modify: `backend/src/main/java/com/dormrepair/common/enums/AreaTypeEnum.java`
- Modify: `backend/src/main/java/com/dormrepair/area/service/AreaService.java`
- Modify: `backend/src/test/java/com/dormrepair/configuration/FoundationConfigurationServiceTest.java`
- Modify: `backend/src/test/java/com/dormrepair/plan2/Plan2MySqlIntegrationTest.java`
- Modify: `scripts/verify-database-schema.ps1`

**Interfaces:**
- Produces: `AreaTypeEnum.ROOM(4)`；合法父子关系 `ROOT→CAMPUS→AREA→BUILDING→ROOM`；工单 `room_id` 可关联四级区域节点。

- [ ] 写失败测试，覆盖房间只能挂到楼栋、楼栋下可创建房间、其他跨级关系拒绝、公共树包含启用房间、停用祖先时房间不可见。
- [ ] 运行区域模块和真实 MySQL 集成测试，确认因 `ROOM` 和四级关系尚不存在而失败。
- [ ] 扩展枚举、层级校验、树装配和数据库注释；保留既有 `repair_area` 数据，不执行删除或重建。
- [ ] 更新结构验证脚本，验证 `area_type` 注释/约定和 `repair_order.room_id` 使用关系。
- [ ] 重跑区域测试、PLAN-2 集成测试及数据库结构验证，确认通过。

### Task 2: 建立工单查询契约和数字错误码

**Files:**
- Create: `backend/src/main/java/com/dormrepair/order/dto/RepairOrderQueryRequest.java`
- Create: `backend/src/main/java/com/dormrepair/order/model/RepairOrderQueryCondition.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/RepairOrderListItem.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/AdminRepairOrderListItem.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/WorkerRepairOrderListItem.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/StudentRepairOrderListItem.java`
- Modify: `backend/src/main/java/com/dormrepair/common/enums/ResultCodeEnum.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderContractTest.java`

**Interfaces:**
- Produces: 公开筛选字段、内部数据范围字段、三角色列表 VO；`ORDER_NOT_FOUND` 数字错误码对应 404，`ORDER_ACCESS_DENIED` 数字错误码对应 403。

- [ ] 写失败测试固定分页、状态集合、位置四级、时间范围、异常/重复/超时字段，并确认公开 DTO 不含 `studentUid/currentAssigneeId`。
- [ ] 写失败测试固定订单错误码及 HTTP 状态映射。
- [ ] 实现最小 DTO、Condition、VO 和错误码，时间参数使用完整 `LocalDateTime` 且开始时间不得晚于结束时间。
- [ ] 运行契约测试并确认通过。

### Task 3: 实现角色数据范围与统一访问校验

**Files:**
- Create: `backend/src/main/java/com/dormrepair/order/service/CurrentWorkerResolver.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderAccessService.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderQueryScopeFactory.java`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairWorkerMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairWorkerMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderAccessServiceTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderQueryScopeFactoryTest.java`

**Interfaces:**
- Produces: `resolveCurrentWorkerId()`；`applyScope(request)`；`checkViewPermission(order)`。

- [ ] 写失败测试覆盖管理员无归属过滤、学生强制当前 userId、维修人员 userId→workerId、映射不存在拒绝、三角色详情允许/拒绝。
- [ ] 运行目标测试，确认因服务不存在而失败。
- [ ] 实现 Resolver、范围工厂和访问服务；禁止从请求复制身份字段。
- [ ] 重跑目标测试并确认通过。

### Task 4: 实现三角色工单分页列表

**Files:**
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderQueryService.java`
- Create: `backend/src/main/java/com/dormrepair/order/controller/AdminRepairOrderController.java`
- Create: `backend/src/main/java/com/dormrepair/order/controller/WorkerRepairOrderController.java`
- Create: `backend/src/main/java/com/dormrepair/order/controller/StudentRepairOrderController.java`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairOrderMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderQueryServiceTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderListAuthorizationTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderMapperMySqlTest.java`

**Interfaces:**
- Produces: `GET /api/admin/repair-orders`、`GET /api/worker/repair-orders`、`GET /api/student/repair-orders`。

- [ ] 写失败测试覆盖三角色路径和 401/403、学生/维修人员数据隔离以及客户端不能扩大范围。
- [ ] 写真实 MySQL 失败测试覆盖状态列表、故障类型、四级位置、负责人、时间、异常、重复、接单超时、处理超时及 PLAN-3 指定组合条件。
- [ ] 实现以 `repair_order` 为主表的一对一列表 SQL，固定 `deleted=0`，默认 `report_time DESC,id DESC`。
- [ ] 实现 Service 的 PageHelper→Mapper→PageInfo→PageResult 流程和后端超时字段计算。
- [ ] 运行单元、权限和真实 MySQL 测试，确认 total、records 和数据范围正确。

### Task 5: 实现工单详情关联查询与聚合

**Files:**
- Create: `backend/src/main/java/com/dormrepair/order/vo/RepairOrderDetailData.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/AdminRepairOrderDetailResponse.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/WorkerRepairOrderDetailResponse.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/StudentRepairOrderDetailResponse.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderDetailService.java`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairOrderMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairProcessRecordMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairProcessRecordMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairMaterialUsageMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairMaterialUsageMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairReworkRecordMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairReworkRecordMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairEvaluationMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairEvaluationMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderFlowMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairOrderFlowMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderDetailServiceTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderDetailMySqlTest.java`

**Interfaces:**
- Produces: 主记录查询、五类 `selectByOrderId`、按角色裁剪后的聚合详情。

- [ ] 写失败测试覆盖工单不存在 404、先鉴权后查子表、越权时不执行关联查询、空集合和空评价。
- [ ] 写真实 MySQL 失败测试固定过程/材料/返工/流转排序、单评价、四级位置文本和关联人员名称。
- [ ] 实现主记录及关联查询，每个一对多表单独查询，不创建超级 JOIN。
- [ ] 实现聚合及角色字段裁剪：管理员可见异常/内部字段，维修人员可见维修所需联系方式，学生不见内部调度字段。
- [ ] 重跑详情单元与 MySQL 集成测试并确认通过。

### Task 6: 接入三角色详情接口

**Files:**
- Modify: `backend/src/main/java/com/dormrepair/order/controller/AdminRepairOrderController.java`
- Modify: `backend/src/main/java/com/dormrepair/order/controller/WorkerRepairOrderController.java`
- Modify: `backend/src/main/java/com/dormrepair/order/controller/StudentRepairOrderController.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderDetailAuthorizationTest.java`

**Interfaces:**
- Produces: `GET /api/admin/repair-orders/{id}`、`GET /api/worker/repair-orders/{id}`、`GET /api/student/repair-orders/{id}`。

- [ ] 写 MockMvc 失败测试覆盖角色路径、401、跨角色 403、他人工单 403、工单不存在 404 和统一 Result。
- [ ] 实现 Controller 调用同一 Detail Service，不复制聚合逻辑。
- [ ] 重跑接口权限测试并确认通过。

### Task 7: 核心 SQL EXPLAIN 与索引核验

**Files:**
- Modify when evidence requires: `doc/database/dorm_repair_schema.sql`
- Modify when evidence requires: `scripts/verify-database-schema.ps1`
- Create: `backend/src/test/java/com/dormrepair/order/RepairOrderExplainMySqlTest.java`

**Interfaces:**
- Consumes: Task 4 列表 SQL和既有 `idx_order_*` 索引。
- Produces: 管理员常用筛选、维修人员范围、学生范围、接单超时、处理超时查询的执行计划证据。

- [ ] 写真实 MySQL 测试执行核心查询 `EXPLAIN`，断言不出现意外笛卡尔积并记录候选索引。
- [ ] 若现有索引可满足查询，记录“不新增索引”；若不能满足，只补充由 EXPLAIN 证明需要的最小索引。
- [ ] 运行结构验证和 EXPLAIN 测试并确认通过。

### Task 8: 接口文档、全量联调与阶段验收

**Files:**
- Create: `doc/api/前端联调接口说明.md`
- Modify: `doc/api/README.md`
- Modify: `doc/security/登录用户上下文与安全响应.md`
- Modify: `README.md`
- Create: `doc/verify/PLAN-3验收记录.md`
- Test: `backend/src/test/java/com/dormrepair/order/Plan3MySqlIntegrationTest.java`

**Interfaces:**
- Consumes: Tasks 1–7 的全部接口和数据规则。
- Produces: 前端可直接联调的中文接口契约及可复现验收记录。

- [ ] 用真实 MySQL 创建隔离测试数据，验证管理员组合筛选、学生本人列表/详情、维修人员当前指派列表/详情和跨用户越权 403，并在事务后清理测试数据。
- [ ] 按每个接口记录用途、方法路径、鉴权、参数、响应字段、响应示例、错误场景、枚举和数据范围。
- [ ] 更新安全和启动文档，明确 `sys_user.id→repair_worker.id`、四级位置及 PLAN-3 未实现的写业务。
- [ ] 加载 `.env` 后执行 `mvn "-Dmaven.repo.local=..\.m2\repository" test package`；执行前端 `npm run test -- --run` 和 `npm run build`。
- [ ] 执行 `scripts/verify-database-schema.ps1`、`docker compose ps`、后端健康检查及三角色只读烟雾测试。
- [ ] 在 `doc/verify/PLAN-3验收记录.md` 记录范围、逐项结果、命令证据、边界和遗留项。
