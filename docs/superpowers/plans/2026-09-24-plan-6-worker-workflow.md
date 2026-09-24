# PLAN-6 维修人员主流程 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现维修人员从待接单到待确认的完整、安全、可追溯业务链路。

**Architecture:** Controller 仅处理参数和统一响应，`WorkerRepairOrderCommandService` 负责权限与状态编排，独立事务服务执行 CAS、业务记录、flow 和日志的原子写入。查询继续复用阶段 3，学生详情在服务端过滤材料。

**Tech Stack:** Java 17、Spring Boot 3、Spring Security、MyBatis、MySQL 8、JUnit 5、MockMvc。

**Spec:** `docs/superpowers/specs/2026-09-24-plan-6-worker-workflow-design.md`

## Global Constraints

- 不实现前端页面和文件上传。
- 不新增数据库表。
- 所有写接口使用统一 Result，401/403/404/409/500 遵循既有 HTTP 语义。
- 身份必须来自 UserContext，不接受 workerId。
- 每个新增接口同步更新前端联调文档。
- 验收截图使用 `PLAN-6-测试内容.png`。

## Review Focus

- CAS 失败后不能把负责人变化误报为普通状态冲突。
- 接单截止时间恰好等于当前时间的边界应允许，严格晚于截止时间才拒绝。
- 中断和提交结果并发时只能有一组 process/flow/log 落库。
- 事务中后续插入失败时 CAS 状态更新必须回滚。
- 学生详情不能泄露材料名称、数量和备注。

---

### Task 1: DTO、枚举与接口契约

**Files:** 新增 `order/dto` 写请求、`common/enums` 过程/操作/中断枚举；修改 Controller 契约测试。

- [ ] 编写六个路由、校验和角色限制失败测试并确认 RED。
- [ ] 实现 DTO、枚举和 Controller 最小结构。
- [ ] 运行契约测试确认 GREEN。

### Task 2: 接单事务

**Files:** 修改 RepairOrderMapper/XML；新增命令服务与事务服务；新增接单单元/集成测试。

- [ ] 编写负责人、状态、超时、预计完成时间、默认 24 小时、CAS 和回滚测试并确认 RED。
- [ ] 实现接单 CAS、flow 和操作日志事务。
- [ ] 运行测试确认 GREEN。

### Task 3: 普通过程与材料

**Files:** 扩充 Process/Material Mapper；实现新增业务方法；新增状态和越权测试。

- [ ] 编写允许/禁止状态、字段校验、多条记录和越权测试并确认 RED。
- [ ] 实现 process/material 与操作日志事务。
- [ ] 运行测试确认 GREEN。

### Task 4: 中断、恢复与提交结果

**Files:** 扩充 CAS Mapper 和事务服务；新增状态链路与并发测试。

- [ ] 编写中断、恢复、提交、非法状态、并发竞争和回滚测试并确认 RED。
- [ ] 实现 order/process/flow/log 原子事务。
- [ ] 运行测试确认 GREEN。

### Task 5: 详情数据边界、文档和验收

**Files:** 修改详情服务、接口/安全/架构文档；新增 `doc/verify/PLAN-6验收记录.md` 和截图。

- [ ] 编写学生材料隐藏测试并确认 RED，再实现过滤。
- [ ] 执行专项、真实 MySQL、全量、打包、HTTP 冒烟和并发验证。
- [ ] 重启最新版后端，保存 `PLAN-6-*.png` 截图并完成验收记录。
