# PLAN-7 学生确认、评价与返工 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现学生确认完成、一次性评价、原工单返工、返工次数升级和管理员返工告警，形成报修到评价的核心闭环。

**Architecture:** 在现有学生工单控制器上增加三个命令接口，由学生命令服务完成身份与输入校验，事务服务通过 MyBatis CAS、唯一索引和行锁保证一致性。新增通用 `repair_order_alert` 表承载返工预警，并扩展 PLAN-6 的返工提交事务以关闭当前返工记录。

**Tech Stack:** Java 17、Spring Boot 3、Spring MVC、Spring Security、Hibernate Validator、MyBatis、MySQL 8、JUnit 5、Mockito、Maven。

**Spec:** `docs/superpowers/specs/2026-09-24-plan-7-student-confirm-evaluation-rework-design.md`

## Global Constraints

- 前端页面和文件上传不在本阶段范围内；图片字段只保存既有对象引用。
- 所有身份来自 `UserContext`，请求不得指定学生或维修人员 ID。
- 统一使用 `Result`；越权 403、缺失 404、状态/并发/重复操作 409、系统异常 500。
- 状态变化、业务记录、工单流转、告警和操作日志按规格置于同一事务。
- 每个新增接口同步更新 `doc/api/前端联调接口说明.md` 和 PLAN-7 验收记录。
- 不使用 Redis 锁或 `bizNo`；依靠 CAS 和数据库唯一约束。
- 不修改或清理用户已有的无关文件，特别是后续 PLAN 文档。

## Review Focus

- 评价工单没有负责人时必须返回 409，不得插入 `worker_id=NULL`。
- 空白评价内容归一为 `null`，长度超过 1000 必须返回 400。
- 返工图片数组为空时存 `NULL`，超过 9 个或含空白引用返回 400。
- 第三次以后每个返工轮次生成独立高等级告警，重试不得重复同轮告警。
- 返工状态提交结果时找不到对应未完成返工记录必须整体回滚。

---

### Task 1: 数据库结构与 MyBatis 基础

**Files:**
- Create: `doc/database/PLAN-7学生确认评价返工数据库变更.sql`
- Create: `backend/src/main/java/com/dormrepair/domain/entity/RepairOrderAlert.java`
- Create: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderAlertMapper.java`
- Create: `backend/src/main/resources/mapper/RepairOrderAlertMapper.xml`
- Modify: `doc/database/dorm_repair_schema.sql`
- Modify: `doc/database/数据库DDL及索引设计.md`
- Modify: `doc/database/宿舍报修与维修工单系统核心数据表设计 V1.md`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairEvaluationMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairEvaluationMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairReworkRecordMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairReworkRecordMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/database/SchemaSqlContractTest.java`
- Test: `backend/src/test/java/com/dormrepair/database/MapperXmlContractTest.java`

**Interfaces:**
- Produces: `RepairOrderAlertMapper.insertIgnore(RepairOrderAlert)`、`RepairEvaluationMapper.insert(RepairEvaluation)`、`RepairReworkRecordMapper.insert(RepairReworkRecord)`、`completeCurrent(orderId,reworkNo,finishTime)`。

- [ ] **Step 1: 写失败的结构契约测试**，断言主 SQL 包含 `repair_order_alert`、`uk_order_alert_rework_type(order_id,rework_no,alert_type)`，并断言三个 Mapper XML 包含新增语句。
- [ ] **Step 2: 运行测试确认失败**：`mvn -Dtest=SchemaSqlContractTest,MapperXmlContractTest test`，预期缺少表和语句。
- [ ] **Step 3: 编写增量 SQL 和主结构**，字段严格采用规格中的 `order_id/rework_no/alert_type/alert_level/alert_status/alert_content/handled_by/handled_time/create_time/update_time`，使用 `CREATE TABLE IF NOT EXISTS`，不包含 DROP 和外键。
- [ ] **Step 4: 实现实体与 Mapper**；告警插入使用 `INSERT IGNORE`，评价和返工使用生成主键，完成返工条件为 `order_id=? AND rework_no=? AND status=0 AND deleted=0`。
- [ ] **Step 5: 更新两份数据库说明并运行契约测试**，预期通过。
- [ ] **Step 6: 在 Docker MySQL 幂等执行增量 SQL并验证正式表为 21 张**。
- [ ] **Step 7: 提交**：`git commit -m "feat: 增加返工告警数据结构"`。

### Task 2: DTO、枚举与异常契约

**Files:**
- Create: `backend/src/main/java/com/dormrepair/order/dto/CreateRepairEvaluationRequest.java`
- Create: `backend/src/main/java/com/dormrepair/order/dto/CreateReworkRequest.java`
- Create: `backend/src/main/java/com/dormrepair/common/enums/RepairOrderAlertTypeEnum.java`
- Modify: `backend/src/main/java/com/dormrepair/common/enums/RepairOrderOperationTypeEnum.java`
- Modify: `backend/src/main/java/com/dormrepair/common/enums/ResultCodeEnum.java`
- Modify: `backend/src/main/java/com/dormrepair/common/exception/BusinessException.java`
- Test: `backend/src/test/java/com/dormrepair/order/Plan7ContractTest.java`

**Interfaces:**
- Produces: `CreateRepairEvaluationRequest(Integer score,String content)`、`CreateReworkRequest(String reason,List<String> imageUrls)`、`STUDENT_CONFIRM`、`REWORK`、评价重复冲突码。

- [ ] **Step 1: 写失败的 DTO 校验和枚举测试**，覆盖评分 0/6、1001 字评价、空白原因、10 张图片和空白图片引用。
- [ ] **Step 2: 运行 `mvn -Dtest=Plan7ContractTest test` 确认失败**。
- [ ] **Step 3: 实现 DTO 注解**：评分 `@NotNull @Min(1) @Max(5)`；内容 `@Size(max=1000)`；原因 `@NotBlank @Size(max=1000)`；图片最多 9 个且每项非空、最长 500。
- [ ] **Step 4: 扩展集中枚举和 409 映射**，不得散落操作类型数字。
- [ ] **Step 5: 运行测试确认通过并提交**：`git commit -m "feat: 定义学生确认评价返工契约"`。

### Task 3: 学生确认完成事务

**Files:**
- Create: `backend/src/main/java/com/dormrepair/order/service/StudentRepairOrderCommandService.java`
- Create: `backend/src/main/java/com/dormrepair/order/service/StudentRepairOrderTransactionService.java`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairOrderMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/order/controller/StudentRepairOrderController.java`
- Test: `backend/src/test/java/com/dormrepair/order/StudentRepairOrderCommandServiceTest.java`

**Interfaces:**
- Produces: `confirm(Long orderId)`；Mapper `casStudentConfirm(id,studentUid,now)`。

- [ ] **Step 1: 写失败测试**，覆盖本人待确认成功、他人工单 403、维修中/完成状态 409、CAS 零行重新判定。
- [ ] **Step 2: 运行专项测试确认失败**。
- [ ] **Step 3: 实现 SQL**：`SET status=6,confirm_time=#{now},complete_time=#{now} WHERE id=#{id} AND student_uid=#{studentUid} AND status=3 AND deleted=0`。
- [ ] **Step 4: 在 `@Transactional` 方法中写 CAS、`STUDENT_CONFIRM` flow 和操作日志**，负责人前后保持不变。
- [ ] **Step 5: 暴露 `POST /api/student/repair-orders/{id}/confirm` 并运行测试通过**。
- [ ] **Step 6: 提交**：`git commit -m "feat: 实现学生确认维修完成"`。

### Task 4: 一次性维修评价

**Files:**
- Modify: `StudentRepairOrderCommandService.java`
- Modify: `StudentRepairOrderTransactionService.java`
- Modify: `StudentRepairOrderController.java`
- Test: `StudentRepairOrderCommandServiceTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/Plan7EvaluationMySqlTest.java`

**Interfaces:**
- Produces: `evaluate(Long orderId,CreateRepairEvaluationRequest request)`。

- [ ] **Step 1: 写失败测试**，覆盖已完成本人评价、未完成 409、越权 403、空负责人 409、空白内容转 null、重复评价 409。
- [ ] **Step 2: 写 MySQL 并发测试**，用两个线程对同一 `order_id` 插入，断言只有一条且另一请求为冲突。
- [ ] **Step 3: 运行测试确认失败**。
- [ ] **Step 4: 实现评价事务**：锁定并校验工单、前置查询、插入评价和日志；捕获 `DuplicateKeyException` 转为评价冲突，不写 flow。
- [ ] **Step 5: 暴露 `POST /api/student/repair-orders/{id}/evaluation`，运行单元和 MySQL 测试通过**。
- [ ] **Step 6: 提交**：`git commit -m "feat: 实现学生一次性维修评价"`。

### Task 5: 返工事务与次数升级

**Files:**
- Modify: `StudentRepairOrderCommandService.java`
- Modify: `StudentRepairOrderTransactionService.java`
- Modify: `StudentRepairOrderController.java`
- Modify: `RepairOrderMapper.java`
- Modify: `RepairOrderMapper.xml`
- Test: `StudentRepairOrderCommandServiceTest.java`
- Test: `backend/src/test/java/com/dormrepair/order/Plan7ReworkMySqlTest.java`

**Interfaces:**
- Produces: `rework(Long orderId,CreateReworkRequest request)`；Mapper `casStudentRework(id,studentUid)`。

- [ ] **Step 1: 写失败单元测试**，覆盖本人待确认、越权、非法状态、图片 JSON、第一至第四次策略。
- [ ] **Step 2: 写 MySQL 集成测试**，循环“返工→维修再次提交→待确认”并验证次数 1/2/3/4、异常标记、返工记录和告警数量。
- [ ] **Step 3: 运行测试确认失败**。
- [ ] **Step 4: 实现 CAS SQL**，原子增加次数、进入状态 4、清空预计/截止时间，并以 `CASE WHEN rework_count+1>=3 THEN 1 ELSE exception_flag END` 标记异常。
- [ ] **Step 5: 实现事务写入**：重读最新次数、插入返工记录、REWORK flow、按次数 `insertIgnore` 告警、操作日志；第一次不告警、第二次 WARNING、第三次以后 EXCEPTION。
- [ ] **Step 6: 暴露返工接口并运行测试通过**。
- [ ] **Step 7: 提交**：`git commit -m "feat: 实现学生返工和次数升级"`。

### Task 6: 关闭返工记录并保证维修提交一致性

**Files:**
- Modify: `backend/src/main/java/com/dormrepair/order/service/WorkerRepairOrderCommandService.java`
- Modify: `RepairReworkRecordMapper.java`
- Modify: `RepairReworkRecordMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/order/WorkerRepairOrderCommandServiceTest.java`
- Test: `Plan7ReworkMySqlTest.java`

**Interfaces:**
- Consumes: `completeCurrent(orderId,reworkNo,finishTime)`。
- Produces: 返工中提交结果时原子关闭当前返工记录。

- [ ] **Step 1: 写失败测试**，断言状态 4 提交调用 `completeCurrent`，状态 2 不调用；返工记录零行时抛出 409 并回滚。
- [ ] **Step 2: 运行测试确认失败**。
- [ ] **Step 3: 在提交结果事务中以更新前 `rework_count` 关闭记录**，使用与 `repair_submit_time` 相同的时间。
- [ ] **Step 4: 运行单元与 MySQL 全链路测试，验证 `status=1` 和 `finish_time`**。
- [ ] **Step 5: 提交**：`git commit -m "feat: 完善返工维修提交生命周期"`。

### Task 7: 并发、回滚与完整闭环验收

**Files:**
- Create: `backend/src/test/java/com/dormrepair/order/Plan7ConcurrencyMySqlTest.java`
- Create: `backend/src/test/java/com/dormrepair/order/Plan7WorkflowMySqlTest.java`

**Interfaces:**
- Consumes: 三个学生命令和维修人员提交结果命令。

- [ ] **Step 1: 写确认与返工双线程测试**，断言仅一个成功，最终不能同时出现完成状态和返工记录。
- [ ] **Step 2: 写双返工并发测试**，断言次数只加一、记录一条、告警不重复。
- [ ] **Step 3: 写完整链路测试**：待确认→返工→维修提交→待确认→确认→评价，断言状态、时间、历史、flow、日志和评价。
- [ ] **Step 4: 增加核心插入失败的事务回滚测试**，断言状态、次数和关联记录全部回滚。
- [ ] **Step 5: 运行 PLAN-7 全部专项测试并提交**：`git commit -m "test: 覆盖PLAN-7并发与完整闭环"`。

### Task 8: 接口、安全、架构和验收文档

**Files:**
- Create: `doc/api/学生确认评价与返工接口.md`
- Create: `doc/security/学生工单操作权限.md`
- Create: `doc/architecture/返工生命周期与工单告警设计.md`
- Create: `doc/verify/PLAN-7验收记录.md`
- Modify: `doc/api/前端联调接口说明.md`
- Modify: `README.md`

**Interfaces:**
- Documents: 三个接口、状态矩阵、错误场景、按钮防重复、图片上传前置条件、21 张正式表和跨电脑首次启动步骤。

- [ ] **Step 1: 编写三个接口的请求、响应、权限、状态和错误示例**。
- [ ] **Step 2: 记录学生数据权限、返工生命周期、告警去重和事务边界**。
- [ ] **Step 3: 更新 README 已实现范围，删除“接单与维修尚未实现”等过期描述**。
- [ ] **Step 4: 创建验收记录，先写已确定的验收范围、命令和截图命名，最终结果由 Task 9 回填**。
- [ ] **Step 5: 提交**：`git commit -m "docs: 更新PLAN-7接口架构与验收说明"`。

### Task 9: 最终验证、截图与服务替换

**Files:**
- Create: `doc/verify/images/PLAN-7-学生确认评价返工专项测试.png`
- Create: `doc/verify/images/PLAN-7-MySQL全链路集成测试.png`
- Create: `doc/verify/images/PLAN-7-重复评价并发测试.png`
- Create: `doc/verify/images/PLAN-7-确认与返工并发测试.png`
- Create: `doc/verify/images/PLAN-7-Maven全量测试.png`
- Create: `doc/verify/images/PLAN-7-Maven打包.png`
- Create: `doc/verify/images/PLAN-7-接口冒烟测试.png`
- Create: `doc/verify/images/PLAN-7-后端启动与健康检查.png`
- Modify: `doc/verify/PLAN-7验收记录.md`

**Interfaces:**
- Produces: 可复核的测试证据和运行中的最新版 8811 后端。

- [ ] **Step 1: 运行 PLAN-7 专项、MySQL 全链路与并发测试并保存日志和截图**。
- [ ] **Step 2: 加载本地测试环境变量并执行 `mvn test`，要求全部通过**。
- [ ] **Step 3: 执行 `mvn -DskipTests package`，要求 BUILD SUCCESS**。
- [ ] **Step 4: 停止旧 8811 进程并以隐藏窗口启动最新 JAR**。
- [ ] **Step 5: 使用真实学生 Token 冒烟三个接口，验证成功、401、403、404 和 409，同时核对数据库关联记录**。
- [ ] **Step 6: 请求 `/api/health`，记录 HTTP 200、`code=0`、`status=UP` 和监听 PID**。
- [ ] **Step 7: 将实际测试数量、结果、截图和未完成项写入验收记录**。
- [ ] **Step 8: 检查 `git status`，确认未提交 `.env`、日志、构建产物或测试数据，提交：`git commit -m "test: 完成PLAN-7阶段验收"`。
