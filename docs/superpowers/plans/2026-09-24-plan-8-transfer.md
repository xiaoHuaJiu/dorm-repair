# PLAN-8 维修人员转派实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成维修人员转派申请、管理员审批、排除原负责人重新派单以及失败待办的后端闭环。

**Architecture:** 转派申请模块记录申请、审批与执行结果；真正的工单负责人和状态变化继续由现有 `RepairDispatchService` 负责。申请使用 MySQL `bizNo` 幂等记录和待审批查询，不使用行锁或 Redis 业务锁；审批使用 MySQL 幂等记录与审批状态 CAS。

**Tech Stack:** Java 17、Spring Boot 3、Spring Security、MyBatis、MySQL 8、JUnit 5、Mockito。

**Spec:** `doc/requirements/PLAN-8.md`

## 全局约束

- 只实现后端；前端要求写入联调文档。
- 申请人必须是当前负责人，申请成功不改变工单状态或负责人。
- 原负责人必须加入重新派单排除集合。
- 复用 `RepairDispatchService`，不实现第二套派单算法。
- 所有接口使用统一 `Result`，遵循现有 HTTP 状态映射。
- 每新增接口同步更新前端联调文档和 PLAN-8 验收记录。
- 测试截图必须来自真实终端窗口直接截屏，禁止脚本生成或重绘。

## Review Focus

- 同一 `bizNo` 重放返回相同申请/审批结果且不重复写数据。
- 不同 `bizNo` 对同一工单重复申请返回 409。
- 申请到审批期间负责人或工单状态变化时，旧申请不得继续执行。
- 两个管理员并发审批只能一个 CAS 成功，不能重复派单。
- 派单失败必须保持审批通过、记录执行失败，并将工单转为待派单和生成去重待办。

---

### Task 1：转派契约与持久化

**Files:**
- Modify: `backend/src/main/java/com/dormrepair/domain/entity/RepairTransferRequest.java`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairTransferRequestMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairTransferRequestMapper.xml`
- Modify: `doc/database/dorm_repair_schema.sql`
- Test: `backend/src/test/java/com/dormrepair/transfer/TransferContractTest.java`

- [ ] 先写失败测试，固定枚举、DTO 校验、Mapper 的新增/查询/CAS/分页契约。
- [ ] 运行测试并确认因缺少转派契约失败。
- [ ] 实现枚举、DTO、VO、实体字段和 Mapper SQL。
- [ ] 运行契约和数据库结构测试至通过。

### Task 2：维修人员申请与本人查询

**Files:**
- Create: `backend/src/main/java/com/dormrepair/transfer/service/WorkerTransferRequestService.java`
- Create: `backend/src/main/java/com/dormrepair/transfer/controller/WorkerTransferRequestController.java`
- Test: `backend/src/test/java/com/dormrepair/transfer/WorkerTransferRequestServiceTest.java`

- [ ] 先写失败测试：本人允许状态、他人工单 403、非法状态 409、bizNo 重放、已有待审批 409、申请不改工单。
- [ ] 实现 MySQL 幂等协调、申请事务、本人分页和详情数据权限。
- [ ] 运行测试至通过。

### Task 3：管理员查询与审批驳回

**Files:**
- Create: `backend/src/main/java/com/dormrepair/transfer/service/AdminTransferReviewService.java`
- Create: `backend/src/main/java/com/dormrepair/transfer/controller/AdminTransferRequestController.java`
- Test: `backend/src/test/java/com/dormrepair/transfer/AdminTransferReviewServiceTest.java`

- [ ] 先写失败测试：管理员分页/详情、驳回只改申请、审批 CAS、bizNo 重放。
- [ ] 实现管理员查询、详情、审批幂等与驳回事务。
- [ ] 运行测试至通过。

### Task 4：审批通过与重新派单

**Files:**
- Modify: `backend/src/main/java/com/dormrepair/transfer/service/AdminTransferReviewService.java`
- Modify: `backend/src/main/java/com/dormrepair/dispatch/service/RepairDispatchService.java`（仅在现有契约不足时）
- Test: `backend/src/test/java/com/dormrepair/transfer/TransferRedispatchTest.java`

- [ ] 先写失败测试：重新校验负责人/状态、排除原负责人、成功回写、失败回写与待办、重复审批不派单。
- [ ] 实现通过审批后的统一事务协调并复用现有派单服务。
- [ ] 运行测试至通过。

### Task 5：并发、MySQL 集成与文档验收

**Files:**
- Create: `backend/src/test/java/com/dormrepair/transfer/Plan8MySqlIntegrationTest.java`
- Modify: `doc/api/前端联调接口说明.md`
- Create: `doc/verify/PLAN-8验收记录.md`

- [ ] 编写两个管理员并发审批、重复申请、成功/失败派单的 MySQL 集成测试。
- [ ] 运行完整 Maven 测试、数据库结构验证和真实接口验收。
- [ ] 在真实可见终端执行测试并直接截屏，保存为 `doc/verify/images/PLAN-8-*.png`。
- [ ] 更新接口文档、验收记录并确认最新版后端在 8811 启动。
- [ ] 检查差异并提交本地 Git，不推送远程。
