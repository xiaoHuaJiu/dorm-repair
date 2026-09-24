# PLAN-2 基础配置 CRUD 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成学生注册、故障类型、位置树、维修人员、维修技能、负责区域和工作时间方案的后端能力，为阶段 3 报修与自动派单提供可靠基础数据。

**Architecture:** 沿用现有 Spring Boot 单体的功能分包方式，每个模块采用 Controller、Service、DTO/VO 和 MyBatis Mapper/XML。写操作在 Service 层校验业务约束并控制事务；管理接口使用 `@PreAuthorize("hasRole('ADMIN')")`，公共选择接口只要求登录；所有身份字段来自 `UserContext`。

**Tech Stack:** Java 17、Spring Boot 3、Spring MVC、Spring Security、Hibernate Validator、MyBatis、PageHelper、MySQL 8、JUnit 5、MockMvc。

**Spec:** `doc/requirements/PLAN-2.md`

## Global Constraints

- 保持统一 `Result` 响应体和数字错误码；HTTP 使用 200、400、401、403、404、409、500。
- 密码只通过统一 `PasswordEncoder` 进行 BCrypt 哈希与校验，不记录明文或完整哈希。
- 不物理删除基础配置；使用状态字段启停。
- 前端管理页面本阶段暂不实现。
- 不实现工单、报修、派单、维修过程及定时任务。
- 不引入 SQL DataScope AOP；角色、列表范围和资源归属显式实现。
- 不删除或重建现有数据库，不修改 17 张表的既有 DDL。

## Review Focus

- 并发创建相同用户名或业务编码时，数据库唯一约束必须转换为 HTTP 409，而不是 500。
- 位置树遇到停用祖先、错误层级或孤立节点时，不得把该节点作为公共有效位置返回。
- 批量技能与范围保存必须是幂等事务，重复提交不能产生重复行或半完成状态。
- 工作时间日期边界相接视为重叠，修改时必须排除自身记录。
- 管理端身份字段和 `createBy` 必须来自 JWT/UserContext，客户端同名字段不得覆盖。

---

### Task 1: 公共契约和测试基线

**Files:**
- Modify: `backend/src/main/java/com/dormrepair/common/enums/ResultCodeEnum.java`
- Modify: `backend/src/main/java/com/dormrepair/common/exception/BusinessException.java`
- Modify: `backend/src/main/java/com/dormrepair/common/result/PageResult.java`
- Test: `backend/src/test/java/com/dormrepair/common/Plan2CommonContractTest.java`

**Interfaces:**
- Produces: PLAN-2 模块数字错误码、可指定 HTTP 状态的业务异常、响应字段为 `records` 的 `PageResult<T>`。

- [ ] 写失败测试，固定 USER、FAULT、AREA、WORKER、SCHEDULE 错误码、异常 HTTP 状态以及分页 JSON 字段。
- [ ] 运行 `mvn -Dtest=Plan2CommonContractTest test`，确认测试失败。
- [ ] 扩展公共契约，保持阶段 0/1 兼容。
- [ ] 重跑目标测试并确认通过。

### Task 2: 学生公开注册

**Files:**
- Create: `backend/src/main/java/com/dormrepair/user/controller/UserRegistrationController.java`
- Create: `backend/src/main/java/com/dormrepair/user/dto/StudentRegisterRequest.java`
- Create: `backend/src/main/java/com/dormrepair/user/service/UserRegistrationService.java`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/SysUserMapper.java`
- Modify: `backend/src/main/resources/mapper/SysUserMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/dormrepair/user/UserRegistrationServiceTest.java`
- Test: `backend/src/test/java/com/dormrepair/user/UserRegistrationControllerTest.java`

**Interfaces:**
- Produces: `POST /api/auth/register`；请求包含 username、password、confirmPassword、realName、phone，服务端固定 STUDENT。

- [ ] 写 Service 和 MockMvc 失败测试，覆盖正常注册、密码不一致、重复用户名、客户端角色注入无效、注册后可登录。
- [ ] 运行注册模块测试并确认失败。
- [ ] 实现校验、BCrypt、唯一冲突转换和匿名放行。
- [ ] 重跑测试，额外查询数据库确认密码不是明文。

### Task 3: 故障类型 CRUD 与公共选择接口

**Files:**
- Create: `backend/src/main/java/com/dormrepair/fault/**`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairFaultTypeMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairFaultTypeMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/fault/**`

**Interfaces:**
- Produces: `/api/admin/fault-types` 管理 CRUD/分页/启停；`GET /api/fault-types/enabled` 已登录用户选择接口。

- [ ] 写失败测试，覆盖编码唯一、详情 404、排序、启停、分页条件和非管理员 403。
- [ ] 实现 DTO/VO、Service、Mapper SQL 和 Controller。
- [ ] 使用数据库唯一索引兜底并转换并发冲突。
- [ ] 运行模块测试和 Mapper 集成测试。

### Task 4: 位置树 CRUD 与公共级联

**Files:**
- Create: `backend/src/main/java/com/dormrepair/area/**`
- Create: `backend/src/main/java/com/dormrepair/common/enums/AreaTypeEnum.java`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairAreaMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairAreaMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/area/**`

**Interfaces:**
- Produces: `/api/admin/areas` 管理接口；`GET /api/areas/tree` 和 `GET /api/areas/{parentId}/children` 公共有效位置接口。

- [ ] 写失败测试，固定 DDL 中的区域类型编码及合法父子关系。
- [ ] 覆盖根节点、父节点不存在/停用、非法跨级、同父同类型同名、禁止改 parentId、停用祖先过滤。
- [ ] 实现树装配、管理查询和公共有效树/子节点查询。
- [ ] 运行模块与权限测试。

### Task 5: 工作时间方案 CRUD

**Files:**
- Create: `backend/src/main/java/com/dormrepair/schedule/**`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairWorkScheduleMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairWorkScheduleMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/schedule/**`

**Interfaces:**
- Produces: `/api/admin/work-schedules` 的新增、修改、详情、分页、按日期查询和启停。

- [ ] 写失败测试，覆盖日期倒置、时间倒置、边界重叠、修改排除自身、启用时重新检查冲突。
- [ ] 实现 `createBy = UserContext.getCurrentUserId()` 和区间重叠 SQL。
- [ ] 实现分页和条件查询。
- [ ] 运行模块、事务和权限测试。

### Task 6: 维修人员 CRUD 与账号绑定

**Files:**
- Create: `backend/src/main/java/com/dormrepair/worker/**`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairWorkerMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairWorkerMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/SysUserMapper.java`
- Modify: `backend/src/main/resources/mapper/SysUserMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/worker/**`

**Interfaces:**
- Produces: `/api/admin/workers` 管理接口；支持创建账号并绑定或绑定已有用户。

- [ ] 写失败测试，覆盖两种创建模式、用户角色、userId/workerNo 唯一、事务回滚和密码哈希。
- [ ] 实现组合 VO、分页条件查询和独立技能/范围查询入口。
- [ ] 明确账号状态与业务工作状态的独立更新。
- [ ] 运行模块、事务和角色权限测试。

### Task 7: 技能与负责区域批量配置

**Files:**
- Create: `backend/src/main/java/com/dormrepair/worker/config/**`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairWorkerFaultTypeMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairWorkerFaultTypeMapper.xml`
- Modify: `backend/src/main/java/com/dormrepair/domain/mapper/RepairWorkerAreaScopeMapper.java`
- Modify: `backend/src/main/resources/mapper/RepairWorkerAreaScopeMapper.xml`
- Test: `backend/src/test/java/com/dormrepair/worker/config/**`

**Interfaces:**
- Produces: `GET/PUT /api/admin/workers/{workerId}/fault-types` 和 `GET/PUT /api/admin/workers/{workerId}/area-scopes`。

- [ ] 写失败测试，覆盖技能新增/停用/恢复/重复提交及停用依赖拒绝。
- [ ] 写失败测试，覆盖校区、区域、楼栋归属、重复范围、上级覆盖和自动停用冗余下级。
- [ ] 实现两个事务化差集保存服务及批量 Mapper SQL。
- [ ] 运行模块和真实 MySQL 集成测试，验证重复提交幂等。

### Task 8: 契约文档、全量联调与验收

**Files:**
- Create: `doc/api/基础配置接口.md`
- Modify: `doc/api/学生注册与区域树接口约定.md`
- Modify: `doc/security/登录用户上下文与安全响应.md`
- Modify: `README.md`
- Test: `backend/src/test/java/com/dormrepair/plan2/Plan2AuthorizationIntegrationTest.java`

**Interfaces:**
- Consumes: Tasks 1–7 的全部 REST 接口。
- Produces: 可供前端阶段直接对接的中文接口契约和验收证据。

- [ ] 用 MockMvc 和真实 MySQL 验证管理员完整配置链路、学生注册后登录、公共选择接口和跨角色 403。
- [ ] 验证事务失败无半成品数据、数据库无明文密码、所有响应为统一 `Result`。
- [ ] 更新中文 API、安全及启动文档。
- [ ] 执行 `mvn test package`、前端既有 `npm test -- --run` 和 `npm run build`。
- [ ] 重启后端，执行健康检查及核心接口烟雾测试，不向数据库写入不可清理的验收垃圾数据。

