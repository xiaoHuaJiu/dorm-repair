# PLAN-7 学生确认、评价与返工设计

## 1. 目标与范围

本阶段承接 PLAN-6 的“待确认”工单，完成学生侧核心闭环：学生可以确认完成并在完成后评价，也可以在待确认阶段申请原工单返工。返工不创建新工单，原维修人员继续处理。

本阶段实现学生确认、评价、返工、返工次数升级、管理员返工告警、数据权限、事务和并发控制。前端页面、文件上传、主动转派、请假、超时转派、维修超时提醒及管理员异常处置不在本阶段范围内。

## 2. HTTP 接口

新增三个学生接口，统一使用现有 `Result<Void>` 响应：

- `POST /api/student/repair-orders/{id}/confirm`：确认维修完成，请求体可为空；
- `POST /api/student/repair-orders/{id}/evaluation`：提交评价；
- `POST /api/student/repair-orders/{id}/rework`：申请返工。

评价请求包含 `score` 和可选 `content`。`score` 为 1 至 5，`content` 最长 1000 字符。返工请求包含必填 `reason` 和可选 `imageUrls`，原因最长 1000 字符，图片引用最多 9 个。图片只接收已上传对象引用，本阶段不提供上传接口。

所有接口从 `UserContext` 获取学生身份，不接受 `studentUid`、`workerId`、状态、次数或业务时间等服务端字段。

## 3. 权限与错误约定

三个操作均校验 `repair_order.student_uid` 等于当前用户 ID。非本人操作返回 HTTP 403 / 17002，工单不存在或已逻辑删除返回 HTTP 404 / 17001。

参数校验失败返回 HTTP 400 / 10001。非法状态、重复操作、CAS 失败和唯一约束冲突返回 HTTP 409；认证和角色保护继续由 Spring Security 返回 401 或 403。异常响应保持统一 `Result`，不暴露数据库细节。

## 4. 学生确认完成

确认只允许 `3-待确认 → 6-已完成`。条件更新同时包含工单 ID、当前学生、原状态和逻辑删除标记，并使用同一个 `now` 写入 `confirm_time` 与 `complete_time`。`current_assignee_id` 保留最后一任维修人员。

确认事务包含：CAS 更新工单、写入 `repair_order_flow`、写入 `sys_operation_log`。流转类型新增 `STUDENT_CONFIRM`，负责人前后均为当前维修人员，操作人为当前学生。CAS 影响行数为零时重新读取并区分不存在、越权和状态冲突。

## 5. 学生评价

评价只允许学生本人对 `6-已完成` 工单提交。`worker_id` 取工单保留的当前负责人；若负责人为空，则拒绝评价并返回数据冲突，避免产生无法归属的评价。

应用层先查询 `repair_evaluation.order_id`，数据库现有 `UNIQUE(order_id)` 负责并发最终兜底。唯一键异常转换为 HTTP 409。评价事务只包含评价插入和操作日志，不改变工单状态，也不写工单流转。

## 6. 学生申请返工

返工只允许 `3-待确认 → 4-返工中`。单条条件更新完成以下操作：

- `rework_count = rework_count + 1`；
- 清空上一轮 `expected_complete_time`；
- 清空上一轮 `complete_deadline`；
- 当更新后的次数达到 3 时设置 `exception_flag=1`；
- 保留 `current_assignee_id`。

条件更新成功后在同一事务中重新读取工单，使用最新 `rework_count` 作为 `rework_no`，插入 `repair_rework_record`。记录保存申请学生、原因、图片引用、原维修人员，初始 `status=0`。现有 `UNIQUE(order_id,rework_no)` 作为并发兜底。

返工流转类型新增 `REWORK`，记录“第 N 次返工”和原因。返工事务还写操作日志及按次数生成的管理员告警。任何核心写入失败，工单状态和次数一并回滚。

## 7. 返工次数升级

- 第 1 次：正常返工，`exception_flag=0`，不生成管理员告警；
- 第 2 次：`exception_flag=0`，生成 `REWORK_WARNING` 中等级告警；
- 第 3 次及以上：`exception_flag=1`，每轮生成 `REWORK_EXCEPTION` 高等级告警；
- 所有轮次均保留原维修人员，不自动调用派单服务。

学生响应不暴露管理员告警和异常内部处理细节。

## 8. 通用工单告警表

新增 `repair_order_alert`，不复用语义仅限自动派单失败的 `repair_dispatch_alert`。字段如下：

- `id BIGINT`：主键；
- `order_id BIGINT`：工单 ID；
- `rework_no INT`：返工轮次；
- `alert_type VARCHAR(50)`：`REWORK_WARNING` 或 `REWORK_EXCEPTION`；
- `alert_level TINYINT`：1 普通、2 中等、3 高；
- `alert_status TINYINT`：0 待处理、1 已处理；
- `alert_content VARCHAR(500)`：告警说明；
- `handled_by BIGINT`、`handled_time DATETIME`：后续管理员处理字段；
- `create_time DATETIME`、`update_time DATETIME`：维护时间。

建立唯一索引 `UNIQUE(order_id,rework_no,alert_type)`，保证接口重试和事务重试不会重复生成同轮同类告警；建立待处理状态与时间索引供后续管理员列表查询。本阶段只写入告警，不新增管理员查询和处理接口。

该表加入主结构 SQL、数据库设计文档、实体和 MyBatis Mapper。正式业务表数量由 20 张增加为 21 张。

## 9. 返工记录生命周期

PLAN-6 已允许维修人员从 `4-返工中` 再次提交结果。该提交事务需要增加一步：把该工单最新一条 `status=0` 的返工记录更新为 `status=1`，并写入与 `repair_submit_time` 相同的 `finish_time`。

更新必须匹配 `order_id`、当前 `rework_no` 和未完成状态；若返工状态下找不到或无法更新对应记录，整个提交结果事务回滚，避免工单进入待确认但返工记录仍显示处理中。普通维修中提交结果不执行该更新。

## 10. 并发与幂等

确认和返工都以 `status=3` 为条件执行 CAS，因此二者并发时只能一个成功。两个返工请求并发时只有一个可以改变状态，`rework_count` 只增加一次，且只生成一条返工记录。

评价通过应用层预查和 `UNIQUE(order_id)` 保证只有一条。确认、返工和评价第一版均不增加 Redis 锁或 `bizNo`。

前端仍需在请求期间禁用按钮，并在操作完成后重新查询详情；前端防重复不能替代服务端并发控制。

## 11. 数据访问层与服务边界

新增学生工单命令服务和事务服务，控制器只负责参数接收和统一响应。Mapper 增加确认 CAS、返工 CAS、评价插入、返工插入与完成、告警插入等明确方法。

事务服务负责数据库写入和强一致性；命令服务负责当前用户、输入和业务前置检查。所有状态码使用 `RepairOrderStatusEnum`，流转类型集中维护在 `RepairOrderOperationTypeEnum`，不在业务代码中散落魔法数字。

## 12. 文档更新

实现时同步维护：

- `doc/api/前端联调接口说明.md`；
- 学生确认、评价与返工专项接口文档；
- `doc/security/` 的学生工单操作权限说明；
- `doc/architecture/` 的返工生命周期与告警设计；
- 主数据库 SQL、DDL/索引设计及核心表设计；
- `README.md` 当前实现范围和跨电脑首次启动说明；
- `doc/verify/PLAN-7验收记录.md`。

## 13. 验证方案

采用测试驱动方式覆盖：

- 确认成功、非法状态、重复确认和越权；
- 评价成功、未完成评价、重复评价和并发唯一约束；
- 第 1、2、3、4 次返工策略；
- 返工次数、记录、告警、流转和操作日志；
- 返工中再次提交结果时关闭本轮返工记录；
- 两个返工请求并发；
- 确认与返工并发；
- 事务失败不留下部分数据；
- 最终完成后仍可查询全部返工历史。

完成专项测试、真实 MySQL 全链路与并发测试、后端全量测试、Maven 打包、接口冒烟及最新版服务健康检查。截图按 `PLAN-7-测试内容.png` 保存到 `doc/verify/images/`。

## 14. 完成标准

阶段完成时，一张待确认工单只能被学生本人确认或返工；完成后可且仅可评价一次；返工始终复用原工单和原维修人员；次数升级、异常标记和管理员告警正确；确认、返工、评价及维修人员再次提交在并发和失败场景下保持数据一致；接口、数据库、安全、架构和验收文档与代码同步。
