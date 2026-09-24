# PLAN-9 验收记录

## 验收范围

- 维修人员请假申请（bizNo 幂等、时间与重叠校验）。
- 管理员请假审批（通过/驳回、并发 CAS、幂等）。
- 请假生效定时任务：维修人员置为请假中，未完成工单逐单转派，部分失败汇总提醒。
- 请假结束定时任务：恢复正常工作，连续请假判断。
- 权限控制、操作日志、系统配置与定时补偿。

## 验收结果（暂未测试）

> 项目当前无法启动（缺少运行环境），本阶段**未执行任何运行时测试**。以下仅为开发完成情况与编译级验证，不构成测试结论。

| 项目 | 状态 | 说明 |
|---|---|---|
| 后端主代码开发 | 完成 | `com.dormrepair.leave` 包 + 扩展 Mapper/XML + 配置，共 17 个新类 |
| 编译验证 | 通过 | `mvn compile` 与 `mvn test-compile` 均为 `BUILD SUCCESS`（IDEA JBR + 内置 Maven 3.9） |
| 测试代码开发 | 完成，暂未运行 | 契约测试 1 个类、单元测试 3 个类（Mockito，可运行）、MySQL 集成测试 2 个类（需真实 MySQL，暂未运行） |
| 接口冒烟 | 未执行 | 后端未启动 |
| MySQL 集成测试 | 未执行 | 需真实 MySQL 环境 |
| 并发竞争测试 | 未执行 | 需真实 MySQL 环境 |
| 前端页面接入 | 不适用 | 本阶段不实现前端，占位页面保持原样 |

测试代码清单：

- `Plan9ContractTest`：DTO 校验注解与枚举码契约（纯 Validator，无外部依赖）。
- `LeaveRequestServiceTest` / `LeaveEffectiveServiceTest` / `LeaveEndServiceTest`：Mockito 单元测试。
- `Plan9MySqlIntegrationTest`：创建/重叠/驳回重提/生效转派/跨扫描幂等/部分失败汇总提醒/结束恢复/连续请假 8 个场景。
- `Plan9ConcurrencyMySqlTest`：并发审批 CAS 与并发生效任务 CAS 抢占。

## 待运行时验证清单（后端可启动后执行）

1. 运行测试：`Plan9ContractTest`、三个单元测试类 → `mvn -Dtest=Plan9ContractTest,LeaveRequestServiceTest,LeaveEffectiveServiceTest,LeaveEndServiceTest test`。
2. 应用数据库变更：本阶段无增量 DDL（`repair_leave_request` 表及索引在既有 schema 中已存在）。
3. 启动后端后运行 MySQL 集成与并发测试：`mvn -Dtest=Plan9MySqlIntegrationTest,Plan9ConcurrencyMySqlTest test`。
4. 接口冒烟：登录维修人员/管理员，创建请假、审批、观察生效与结束任务、核对转派与恢复。
5. 全量回归测试，确认 PLAN-1 ~ PLAN-8 测试不受影响。

## 代码逻辑审查结论（review）

开发完成后对全部新增/修改代码进行了逻辑审查，结论：

**已确认的设计**：bizNo 幂等（Redis 门闩 + 唯一约束 + 快照重放）、审批 CAS、生效任务 CAS 抢占与处理中超时补偿、逐单转派异常隔离、无巨型事务、连续请假判断、允许开始时间早于当前（PLAN-9 允许临时申请已开始的请假）。

**审查发现并已修复的问题**：

1. 并发双处理时可能重复转派：处理中超时补偿与首个进程并发执行时，工单可能被重复转派并产生误报失败 → 在 `RepairDispatchService` 中为 `LEAVE_REASSIGN` 来源增加防线：派单前校验工单仍属于请假人员，CAS 未命中时重查，已不属于请假人员的工单视为已处理成功（仅影响 `LEAVE_REASSIGN`，不影响既有派单来源）。
2. 汇总提醒未与终态 CAS 绑定：并发场景下即使终态 CAS 未命中仍会写提醒 → 改为仅在本轮 CAS 成功闭环时写入。
3. 结束任务 LIMIT 饥饿：已结束请假的维修人员永远留在扫描结果中，超过批量上限后其余人员永远不会被恢复 → 扫描条件增加 `JOIN repair_worker work_status=1`，只扫描当前请假中的人员。
4. 停用人员被请假覆盖：生效任务无条件 `updateStatus` 会把停用人员置为请假中，结束任务又会将其恢复为正常 → 改为 CAS `正常→请假中`，停用人员不被覆盖。

**已知可接受项**：结束恢复与生效任务在请假时间边界上的毫秒级竞态（连续请假判断已覆盖实际场景）；请假审批不校验维修人员工作状态（停用人员仍可被审批通过，但不影响工单数据正确性）。

**同步更新**：修复涉及的单元测试同步调整（`LeaveEffectiveServiceTest` 新增 CAS 闭环用例、`RepairDispatchServiceTest` 新增 2 个 LEAVE_REASSIGN 用例），架构文档《请假生效与转派设计.md》已同步。

## 未完成项

- 前端请假页面与请假审批页面未接入（保持占位）。
- 维修人员消息中心接口不属于 PLAN-9 范围，仍未实现。
