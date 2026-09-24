# PLAN-4 学生报修创建实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现学生报修、24 小时疑似重复提示、Redis + MySQL 双层幂等以及工单创建事务，不开发前端页面。

**Architecture:** Controller 只接收并校验学生请求；重复检测、幂等协调和事务创建拆为独立服务。Redis 仅作为短期并发门闩，MySQL 幂等记录及唯一约束提供最终保证；工单、首条流转、操作日志和幂等成功结果在同一事务提交，提交后只调用空派单边界。

**Tech Stack:** Java 17、Spring Boot 3、Spring Security、Hibernate Validator、MyBatis、MySQL 8、Redis、JUnit 5、MockMvc。

**Spec:** `doc/requirements/PLAN-4.md`，以及用户确认的“本阶段不负责前端开发，但接口文档必须明确 bizNo 与防重复点击要求”和“Redis 键由 RedisConstant.java 统一管理”。

## Global Constraints

- 不修改 `frontend/`。
- 使用统一 `Result<T>`、数字错误码和既有 HTTP 状态约定。
- Redis Key 只能从 `RedisConstant.java` 构造，业务代码不得硬编码前缀。
- 学生身份只来源于 `UserContext`，请求 DTO 不包含 `studentUid` 或 `duplicateFlag`。
- `request_hash` 不包含 `confirmDuplicate`，同一次确认流程复用同一个 `bizNo`。
- 疑似重复是 HTTP 200 业务提示，不是强制去重；同一请求并发处理中或复用 bizNo 改内容才返回 HTTP 409。
- 不实现自动派单算法，只保留事务提交后的调用边界。
- 新增接口同步更新 `doc/api/前端联调接口说明.md`，阶段完成维护 `doc/verify/PLAN-4验收记录.md`。

## Review Focus

- Redis 异常或 Key 过期时，MySQL 唯一键仍必须阻止同一学生、同一 bizNo 创建两张工单。
- 疑似重复未确认时必须释放 Redis Key，且不得留下 MySQL SUCCESS 记录。
- 同一 bizNo 从 `confirmDuplicate=false` 改为 `true` 时 Hash 必须相同；其他业务字段变化必须 409。
- 位置 ID 各自存在但父子链不一致时必须拒绝创建。
- 工单、流转、日志或幂等更新任一步失败时，数据库必须全部回滚并允许原 bizNo 重试。

---

### Task 1: 固定公开契约、枚举和 Redis Key

**Files:**
- Create: `backend/src/main/java/com/dormrepair/common/constant/RedisConstant.java`
- Create: `backend/src/main/java/com/dormrepair/order/dto/RepairOrderDuplicateCheckRequest.java`
- Create: `backend/src/main/java/com/dormrepair/order/dto/CreateRepairOrderRequest.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/SuspectedRepairOrder.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/RepairOrderDuplicateCheckResponse.java`
- Create: `backend/src/main/java/com/dormrepair/order/vo/CreateRepairOrderResponse.java`
- Modify: `backend/src/main/java/com/dormrepair/common/enums/RepairOrderStatusEnum.java`
- Modify: `backend/src/main/java/com/dormrepair/common/enums/ResultCodeEnum.java`
- Test: `backend/src/test/java/com/dormrepair/order/Plan4ContractTest.java`

- [ ] 写失败测试固定 DTO 禁止身份字段、开放状态集合、Redis Key 格式、请求校验和数字错误码。
- [ ] 运行目标测试，确认因契约尚不存在而失败。
- [ ] 实现最小契约并映射 400/404/409。
- [ ] 重跑测试确认通过。

### Task 2: 新增 MySQL 幂等记录表及 Mapper

**Files:**
- Modify: `doc/database/dorm_repair_schema.sql`
- Modify: `doc/database/数据库DDL及索引设计.md`
- Modify: `doc/database/宿舍报修与维修工单系统核心数据表设计 V1.md`
- Modify: `scripts/verify-database-schema.ps1`
- Create: `backend/src/main/java/com/dormrepair/domain/entity/SysIdempotentRecord.java`
- Create: `backend/src/main/java/com/dormrepair/domain/mapper/SysIdempotentRecordMapper.java`
- Create: `backend/src/main/resources/mapper/SysIdempotentRecordMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/order/Plan4SchemaContractTest.java`

- [ ] 写失败测试固定字段、唯一键 `(biz_type,user_id,biz_no)` 和 Mapper 行为。
- [ ] 运行测试确认失败。
- [ ] 添加第 18 张表、实体与 Mapper，并以幂等 DDL 应用到现有 `dorm_repair`。
- [ ] 运行结构验证，确认 18 张表和唯一索引存在。

### Task 3: 实现位置、故障类型与疑似重复检测

**Files:**
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairOrderMapper.xml`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderSubmissionValidator.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderDuplicateService.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderDuplicateServiceTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderDuplicateMySqlTest.java`

- [ ] 写失败测试覆盖四级位置链、停用节点、停用故障类型、开放状态、24 小时边界和最小公开字段。
- [ ] 运行测试确认失败。
- [ ] 实现后端时间计算和 `selectSuspectedDuplicateOrders`，按 `report_time DESC,id DESC` 排序。
- [ ] 运行单元和真实 MySQL 测试确认通过，并执行 EXPLAIN 验证 `idx_order_duplicate_check`。

### Task 4: 实现请求 Hash 与 Redis 幂等门闩

**Files:**
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderRequestHasher.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderIdempotencyService.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderRequestHasherTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderIdempotencyServiceTest.java`

- [ ] 写失败测试证明 Hash 稳定、忽略 confirmDuplicate、包含服务端 studentUid 和全部业务字段。
- [ ] 写失败测试覆盖 Redis PROCESSING/SUCCESS、Key 释放、MySQL SUCCESS 重放和 Hash 冲突。
- [ ] 实现 SHA-256 标准化 Hash 与 `RedisConstant.repairOrderCreateKey(studentUid,bizNo)`。
- [ ] 实现 10 分钟 Redis 门闩；Redis 故障时降级到 MySQL，不把 Redis 当权威数据源。
- [ ] 重跑测试确认通过。

### Task 5: 实现工单事务创建及派单边界

**Files:**
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairOrderMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderFlowMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairOrderFlowMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/SysOperationLogMapper.java`
- Modify: `backend/src/main/resources/mapper/SysOperationLogMapper.xml`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderNumberGenerator.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairOrderCreationTransactionService.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/RepairDispatchGateway.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/NoOpRepairDispatchGateway.java`
- Test: `backend/src/test/java/com/dormrepair/order/RepairOrderCreationTransactionServiceTest.java`

- [ ] 写失败测试覆盖工单初始值、可读唯一编号、首条 flow、操作日志、SUCCESS 快照和任一步失败整体回滚。
- [ ] 运行测试确认失败。
- [ ] 实现 Mapper INSERT 与 `@Transactional` 创建服务。
- [ ] 实现事务提交后调用空派单网关，派单失败不得回滚已创建工单。
- [ ] 重跑测试确认通过。

### Task 6: 编排正式创建接口与权限

**Files:**
- Modify: `backend/src/main/java/com/dormrepair/order/controller/StudentRepairOrderController.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/StudentRepairOrderCreationService.java`
- Test: `backend/src/test/java/com/dormrepair/order/Plan4AuthorizationIntegrationTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/Plan4MySqlRedisIntegrationTest.java`

- [ ] 写失败测试覆盖学生角色、401/403、预检查、未确认提示、确认创建、重复响应重放、并发同 bizNo 和内容冲突。
- [ ] 运行测试确认失败。
- [ ] 实现两个 POST 接口及编排服务，未确认时释放 Redis 占用且不写 MySQL 幂等成功记录。
- [ ] 真实 MySQL + Redis 验证同一 bizNo 最终只产生一张工单，事务失败后可重试。
- [ ] 重跑目标测试确认通过。

### Task 7: 文档与阶段验收

**Files:**
- Modify: `doc/api/前端联调接口说明.md`
- Modify: `doc/api/README.md`
- Modify: `doc/security/登录用户上下文与安全响应.md`
- Modify: `README.md`
- Create: `doc/verify/PLAN-4验收记录.md`

- [ ] 记录两个接口的鉴权、字段、响应、错误、数据范围和幂等语义。
- [ ] 明确前端生成/保持 bizNo、提交按钮 loading、超时重试不得换 bizNo、确认重复复用原 bizNo。
- [ ] 执行后端全量测试和 Maven 打包；不运行或修改前端。
- [ ] 执行 18 表结构验证、Docker 状态、MySQL EXPLAIN、Redis 和接口冒烟测试。
- [ ] 将命令、数量、结果、边界与未实现项写入 PLAN-4 验收记录。
