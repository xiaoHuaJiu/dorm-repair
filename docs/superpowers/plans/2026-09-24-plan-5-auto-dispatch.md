# PLAN-5 自动派单 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现事务提交后异步自动派单、工作时间截止计算、失败告警和定时补偿。

**Architecture:** 以统一派单服务编排批量候选查询、工作时间计算和事务性 CAS 更新；以事务提交监听器连接工单创建和有界线程池，以待派单状态扫描提供持久化补偿。

**Tech Stack:** Java 17、Spring Boot 3、Spring MVC、Spring Scheduling、Spring Async、MyBatis、MySQL 8、JUnit 5、Mockito。

**Spec:** `docs/superpowers/specs/2026-09-24-plan-5-auto-dispatch-design.md`

## Global Constraints

- 不新增 HTTP 接口，不修改前端页面。
- 不使用 Redis 全局派单锁，不引入消息队列。
- 派单更新与流转记录必须处于同一事务。
- 工单创建成功不得因派单失败回滚。
- 数据库结构和数据通过可重复执行 SQL 管理，不执行附件中的 DROP TABLE。
- 所有文档使用中文，并同步维护数据库、架构、联调和阶段验收文档。

## Review Focus

- 应用在事务提交后、异步任务执行前退出时，补偿扫描可以重新发现工单。
- 同一工单被异步任务和补偿任务同时处理时，只允许一个 CAS 成功。
- 节假日与周末冲突时以节假日配置为准，调休周末可以计入工作时间。
- 跨日、跨周和跨工作时间方案累计 30 分钟时，截止时间准确。
- 无工作方案、无候选人、CAS 状态变化和系统异常均不得留下无告警的待派单工单。

---

### Task 1: 数据库结构与领域契约

**Files:**
- Modify: `doc/database/dorm_repair_schema.sql`
- Create: `doc/database/PLAN-5自动派单数据库变更.sql`
- Create: `backend/src/main/java/com/dormrepair/domain/entity/RepairHolidayCalendar.java`
- Create: `backend/src/main/java/com/dormrepair/domain/entity/RepairDispatchAlert.java`
- Create/Modify: 对应 Mapper Java/XML 与数据库契约测试

**Interfaces:**
- Produces: 节假日查询、失败告警去重写入、待补偿工单和派单 CAS 所需持久化操作。

- [ ] 先新增数据库契约失败测试并确认 RED。
- [ ] 增加两张表、39 条节假日数据、实体和 Mapper。
- [ ] 运行数据库契约测试确认 GREEN。

### Task 2: 有效工作时间计算

**Files:**
- Create: `backend/src/main/java/com/dormrepair/dispatch/service/WorkingTimeCalculator.java`
- Test: `backend/src/test/java/com/dormrepair/dispatch/WorkingTimeCalculatorTest.java`

**Interfaces:**
- Consumes: 启用工作时间方案和节假日日期类型。
- Produces: `LocalDateTime calculateDeadline(LocalDateTime start, Duration duration)`。

- [ ] 编写普通日、非工作时段、跨日、周末、节假日、调休、跨方案和 366 天失败测试并确认 RED。
- [ ] 实现最小计算逻辑。
- [ ] 运行测试确认 GREEN。

### Task 3: 候选筛选与事务派单

**Files:**
- Create: `backend/src/main/java/com/dormrepair/dispatch/service/RepairDispatchService.java`
- Create: `backend/src/main/java/com/dormrepair/dispatch/service/RepairDispatchTransactionService.java`
- Create: `backend/src/main/java/com/dormrepair/dispatch/model/*`
- Modify: 工单、维修人员、关联表、请假和流转 Mapper。
- Test: `backend/src/test/java/com/dormrepair/dispatch/RepairDispatchServiceTest.java`

**Interfaces:**
- Produces: `dispatch(Long, Collection<Long>, DispatchSourceType)` 和基于 CAS 的原子更新。

- [ ] 编写技能、区域、状态、请假、排除列表、工作量排序、无候选和 CAS 冲突测试并确认 RED。
- [ ] 实现批量查询、排序和事务更新。
- [ ] 运行测试确认 GREEN。

### Task 4: 失败告警、事务后异步和定时补偿

**Files:**
- Create: `backend/src/main/java/com/dormrepair/config/DispatchAsyncConfig.java`
- Create: `backend/src/main/java/com/dormrepair/dispatch/event/*`
- Create: `backend/src/main/java/com/dormrepair/dispatch/job/RepairDispatchRecoveryJob.java`
- Modify: `StudentRepairOrderCreationService.java`
- Remove: `NoOpRepairDispatchGateway.java`
- Test: 异步、告警和补偿测试。

**Interfaces:**
- Consumes: Task 3 统一派单入口。
- Produces: 提交后异步触发、有界线程池拒绝记录和固定延迟补偿扫描。

- [ ] 编写提交前不执行、提交后执行、拒绝告警、任务异常告警和补偿扫描测试并确认 RED。
- [ ] 实现事件、线程池、监听器和补偿任务。
- [ ] 运行测试确认 GREEN。

### Task 5: 集成验证与文档

**Files:**
- Modify: `doc/database/数据库DDL及索引设计.md`
- Modify: `doc/database/宿舍报修与维修工单系统核心数据表设计 V1.md`
- Modify: `doc/api/前端联调接口说明.md`
- Create: `doc/verify/PLAN-5验收记录.md`
- Create: `doc/verify/images/PLAN-5-*.png`

**Interfaces:**
- Produces: 可复现的数据库、构建、测试、接口冒烟和截图证据。

- [ ] 应用数据库变更并核对 20 张表、39 条节假日数据和索引。
- [ ] 运行全量 Maven 测试和打包。
- [ ] 重启最新版后端并执行健康检查与真实派单冒烟测试。
- [ ] 保存以 `PLAN-5-测试内容.png` 命名的终端结果截图。
- [ ] 更新全部文档和验收记录。
