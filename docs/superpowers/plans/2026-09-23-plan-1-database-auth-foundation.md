# PLAN-1 数据库与权限底座 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 `dorm_repair` 数据库中安全落地 17 张正式表，并建立一一对应的数据层、JWT 登录、三角色授权和可复用的数据范围边界。

**Architecture:** 数据库 DDL 是字段与索引的唯一执行来源，MyBatis Entity/Mapper 严格映射数据库；认证采用 `sys_user + BCrypt + JWT + Spring Security`，身份统一进入 `LoginUser/UserContext`。角色由方法安全控制，数据范围由 Service/Mapper 显式实现，不引入 SQL DataScope AOP。

**Tech Stack:** MySQL 8.4、Docker Compose、Java 17、Spring Boot 3.3.13、Spring Security 6、JJWT 0.12.6、MyBatis 3、JUnit 5、真实 Docker MySQL 集成验证、Vue 3、TypeScript、Pinia、Axios、Vitest。

**Spec:** `docs/superpowers/specs/2026-09-23-plan-1-database-auth-foundation-design.md`

## Global Constraints

- 只使用现有数据库 `dorm_repair`，不创建 `dormrepair`，不删除、重建或迁移旧库。
- 保留既有 `scaffold_persistence_check`；验收对象是 17 张正式表，物理表总数为 18。
- MySQL 使用 InnoDB、`utf8mb4`，不建立数据库外键。
- 同名表结构冲突时停止该表操作并报告，不自动 DROP、ALTER 或覆盖。
- DDL 字段和索引以 `doc/database/数据库DDL及索引设计.md` 为准。
- Entity 不包含 DTO、VO 或临时展示字段；Mapper 不提前实现阶段 2 业务查询。
- JWT 密钥、有效期、数据库密码和开发账号初始密码只能通过未提交的环境配置注入。
- 所有 HTTP 响应保持阶段 0 的数字错误码和统一 `Result`。
- 角色专属接口严格隔离；多角色访问必须显式声明。
- 不实现 SQL DataScope AOP，不信任前端提交的身份字段。
- 不开发配置 CRUD、工单流程、定时任务或业务页面。
- 当前目录不是 Git 仓库，不初始化 Git、不提交、不推送。

## Review Focus

- 数据库已存在部分同名表但结构不同：必须检测并中止，不能用 `IF NOT EXISTS` 掩盖差异。
- DDL 执行到中途失败：再次执行必须安全，已成功表应通过结构校验，未成功表可继续创建。
- JWT 已签发后账号被停用或逻辑删除：下一次请求必须失去认证资格。
- 用户名不存在和密码错误：外部响应必须相同，不能形成账号枚举渠道。
- `Authorization` 为空、非 Bearer、重复 Bearer、篡改或过期：不得产生 500，统一按 401 处理。

---

## 文件结构

### 数据库

- Create: `doc/database/dorm_repair_schema.sql`：17 张正式表的可执行 DDL。
- Create: `scripts/apply-database-schema.ps1`：从 `.env` 安全执行 DDL，不回显密码。
- Create: `scripts/verify-database-schema.ps1`：验证正式表、索引、引擎、字符集和无外键。

### 后端数据层

- Create: `backend/src/main/java/com/dormrepair/domain/entity/` 下 17 个 Entity：`SysUser`、`RepairArea`、`RepairWorker`、`RepairFaultType`、`RepairWorkerFaultType`、`RepairWorkerAreaScope`、`RepairWorkSchedule`、`RepairOrder`、`RepairProcessRecord`、`RepairMaterialUsage`、`RepairTransferRequest`、`RepairLeaveRequest`、`RepairReworkRecord`、`RepairEvaluation`、`RepairOrderFlow`、`RepairReminderRecord`、`SysOperationLog`。
- Create: `backend/src/main/java/com/dormrepair/domain/mapper/` 下对应 17 个 Mapper。
- Create: `backend/src/main/resources/mapper/` 下对应 17 个 Mapper XML。
- Create/Modify: `backend/src/main/java/com/dormrepair/common/enums/` 下已确定状态枚举。

### 后端认证授权

- Create: `backend/src/main/java/com/dormrepair/auth/controller/AuthController.java`
- Create: `backend/src/main/java/com/dormrepair/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/dormrepair/auth/vo/LoginResponse.java`
- Create: `backend/src/main/java/com/dormrepair/auth/service/AuthService.java`
- Create: `backend/src/main/java/com/dormrepair/security/jwt/JwtProperties.java`
- Create: `backend/src/main/java/com/dormrepair/security/jwt/JwtTokenService.java`
- Create: `backend/src/main/java/com/dormrepair/security/jwt/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/dormrepair/security/service/LoginUserService.java`
- Modify: `SecurityConfig.java`、`LoginUser.java`、`UserContext.java`、`ResultCodeEnum.java`、`application.yml`。

### 前端认证

- Create: `frontend/src/api/auth.ts`、`frontend/src/api/tokenStorage.ts`、对应测试。
- Modify: `frontend/src/api/http.ts`、`frontend/src/stores/app.ts`、`frontend/src/main.ts`。

### 文档

- Modify/Create: 数据库、API、安全、架构和 README 的阶段 1 说明。

---

### Task 1: 固化并静态验证 17 表 DDL

**Interfaces:**
- Consumes: 两份数据库设计文档和现有 `dorm_repair`。
- Produces: `doc/database/dorm_repair_schema.sql`，可安全重复执行的正式 schema。

- [ ] **Step 1: 编写 DDL 静态契约测试**

创建 `backend/src/test/java/com/dormrepair/database/SchemaSqlContractTest.java`，读取 SQL 并断言：恰好 17 个 `CREATE TABLE`；表名集合与规格一致；每表含 `ENGINE=InnoDB` 和 `CHARSET=utf8mb4`；不存在 `FOREIGN KEY`、`DROP TABLE`、`DROP DATABASE`；包含 PLAN-1 全部关键索引名。

- [ ] **Step 2: 运行测试确认 RED**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=SchemaSqlContractTest test`

Expected: FAIL，正式 SQL 文件不存在。

- [ ] **Step 3: 从最终 DDL 提取正式 SQL**

逐块复制 `数据库DDL及索引设计.md` 中 17 个完整 `CREATE TABLE`，在文件头加入 `USE dorm_repair;`。只允许把 `CREATE TABLE name` 改为 `CREATE TABLE IF NOT EXISTS name`，不得自行改变字段、默认值或索引。

- [ ] **Step 4: 增加同名结构预检脚本**

`apply-database-schema.ps1` 先读取 SQL 目标表集合，再查询 `information_schema.tables`。已存在正式表必须调用 `SHOW CREATE TABLE` 与规范化目标结构比较；不一致则以非零退出，全部一致或不存在才执行 SQL。脚本从根目录 `.env` 读取连接配置，通过容器内环境变量调用 mysql，不把密码拼进输出。

- [ ] **Step 5: 静态测试转绿**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=SchemaSqlContractTest test`

Expected: PASS，17 表、引擎、字符集、无外键和索引集合全部满足。

---

### Task 2: 执行 DDL 并验证持久化

**Interfaces:**
- Consumes: Task 1 schema 和脚本。
- Produces: `dorm_repair` 中 17 张正式表及结构验收报告。

- [ ] **Step 1: 执行只读预检**

Run: `docker exec dorm-repair-mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --batch --skip-column-names "$MYSQL_DATABASE" -e "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() ORDER BY table_name;"'`，记录已有正式表和 `scaffold_persistence_check`。Expected: 不修改数据库；若正式同名表冲突则停止并报告。

- [ ] **Step 2: 执行 schema**

Run: `.\scripts\apply-database-schema.ps1`

Expected: 17 张正式表创建成功；既有验证表保持不变。

- [ ] **Step 3: 编写并运行结构验证脚本**

验证表集合、每表主键、关键索引、InnoDB、`utf8mb4`、外键数量为 0；输出只显示对象名和结论。Run: `.\scripts\verify-database-schema.ps1`。Expected: 17/17 通过。

- [ ] **Step 4: 重启 MySQL 并复验**

Run: `docker compose restart mysql`，等待 healthy 后再次运行验证脚本。Expected: 结构仍为 17/17，`scaffold_persistence_check` 仍存在。

---

### Task 3: 17 个 Entity 与字段一致性

**Interfaces:**
- Consumes: Task 2 实际数据库字段。
- Produces: 17 个纯字段映射 Entity。

- [ ] **Step 1: 编写元数据一致性 RED 测试**

创建 `EntitySchemaConsistencyTest`，使用 JDBC 读取 `information_schema.columns`，通过显式的表名→Entity 类映射验证字段数量、蛇形转驼峰名称和类型：BIGINT/INT/TINYINT、字符、DATE/TIME/DATETIME、DECIMAL 分别匹配规格规定的 Java 类型；Entity 不得多出字段。

- [ ] **Step 2: 运行并确认 RED**

Expected: 17 个 Entity 不存在或字段不完整。

- [ ] **Step 3: 按 DDL 创建 Entity**

每个字段提供普通 Java 属性及 getter/setter；时间类型只使用 `java.time`，金额/数量精度字段使用 `BigDecimal`。不使用 JPA 注解，不增加关联对象、显示名称或计算字段。

- [ ] **Step 4: 运行一致性测试**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=EntitySchemaConsistencyTest test`。Expected: 17/17 Entity 与数据库一致。

---

### Task 4: 17 个 Mapper、XML 与 MySQL 链路

**Interfaces:**
- Consumes: Task 3 Entity。
- Produces: 每个 Mapper 的 `T selectById(Long id)`。

- [ ] **Step 1: 编写 Mapper 契约与集成 RED 测试**

`MapperXmlContractTest` 验证17组接口/XML、namespace、resultMap、Base_Column_List 和 selectById；`MapperMySqlIntegrationTest` 在事务中插入每表最小合法测试记录，逐个调用 selectById 并回滚。

- [ ] **Step 2: 运行确认 RED**

Expected: Mapper/XML 不存在。

- [ ] **Step 3: 创建 Mapper 与 XML**

接口统一 `T selectById(@Param("id") Long id)`；XML 使用完整字段 resultMap 和 `SELECT <include refid="Base_Column_List"/> FROM table WHERE id = #{id}`。不添加 insert/update/delete 或复杂查询。

- [ ] **Step 4: 运行契约和真实 MySQL 测试**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" "-Dtest=MapperXmlContractTest,MapperMySqlIntegrationTest" test`。Expected: 17 组静态契约通过，17 个 selectById 均能通过 MyBatis→MySQL 读取并由事务回滚测试数据。

---

### Task 5: 确定状态枚举

**Interfaces:**
- Produces: 所有枚举的 `getCode()`、`getDescription()` 和 `fromCode(Integer)`。

- [ ] **Step 1: 编写枚举 RED 测试**

断言角色 1/2/3、维修人员 0/1/2、工单 0..7 的精确描述；DDL 已明确定义的请假、转派、过程、返工和提醒枚举逐值断言；重复 code 测试失败；未知 code 返回空 Optional 或抛统一参数异常，项目内统一一种行为。

- [ ] **Step 2: 实现最小枚举**

补齐 `UserRoleEnum.description`，创建 `WorkerWorkStatusEnum`、`RepairOrderStatusEnum` 及经 DDL 核实后的其他枚举。不从说明性示例推导未落入 DDL 的状态。

- [ ] **Step 3: 运行枚举测试和后端全套测试**

Expected: 枚举测试和阶段 0 回归测试全部通过。

---

### Task 6: sys_user 查询、BCrypt 登录与 JWT

**Interfaces:**
- Consumes: `SysUserMapper`。
- Produces: `AuthService.login(LoginRequest): LoginResponse`、`JwtTokenService.generate/parse`、扩展后的 `LoginUser`。

- [ ] **Step 1: 登记认证数字错误码并写 RED 测试**

增加登录失败、账号不可用、令牌无效的固定数字码；测试不存在用户名与错误密码返回同一外部 code/message，停用/删除账号返回账号不可用。密码测试使用 BCrypt 哈希，不保存明文到数据库字段断言之外。

- [ ] **Step 2: 扩展用户查询**

为 `SysUserMapper` 新增 `selectByUsername(String username)`，只查询未逻辑删除记录；MySQL 测试覆盖存在、不存在和逻辑删除。

- [ ] **Step 3: 实现 AuthService**

使用 `PasswordEncoder.matches`；成功构造包含 `userId/username/realName/roleType` 的 LoginUser，并由 JWT 服务生成令牌。错误路径不区分用户名和密码。

- [ ] **Step 4: JWT 服务 RED→GREEN**

测试有效签发解析、过期、篡改、错误密钥、缺失 subject/role；实现 `JwtProperties` 环境绑定和 JJWT 0.12.6 服务，日志不记录令牌。

- [ ] **Step 5: 登录 Controller RED→GREEN**

`POST /api/auth/login` 使用 `@Valid LoginRequest`，返回 `Result<LoginResponse>`；Security 放行该路径。MockMvc 测试成功 200、参数 400、失败响应和无敏感字段。

---

### Task 7: JWT 过滤链、UserContext 与三角色权限

**Interfaces:**
- Consumes: JwtTokenService、SysUserMapper、LoginUser。
- Produces: 每个有效请求的 SecurityContext；`@PreAuthorize` 角色语义。

- [ ] **Step 1: 编写过滤器 RED 测试**

覆盖无 Header、非 Bearer、空 Bearer、重复前缀、篡改、过期、有效令牌、签发后账号停用/删除。无令牌继续过滤链交由授权决定；无效令牌不得 500；有效且账号正常才写入认证。

- [ ] **Step 2: 实现 LoginUserService 与 JwtAuthenticationFilter**

每次认证根据 token 用户 ID 查询账号必要状态，构造 Spring authorities：`ROLE_ADMIN`、`ROLE_WORKER`、`ROLE_STUDENT`。更新 LoginUser/UserContext 测试，确认用户名、姓名和角色均正确。

- [ ] **Step 3: 接入 SecurityConfig**

启用 `@EnableMethodSecurity`，过滤器置于 UsernamePasswordAuthenticationFilter 前，保持无状态和统一 401/403 handler。

- [ ] **Step 4: 权限矩阵测试**

只在测试源码创建三个 `@PreAuthorize` Controller。对三角色和未登录执行完整 4×3 矩阵：同角色 200，其他已登录角色 403，未登录 401；管理员不自动访问学生/维修专属接口。

---

### Task 8: 数据范围模式验证

**Interfaces:**
- Consumes: UserContext 和三角色认证。
- Produces: 后续 Service 可复用的显式资源归属校验模式和书面规范。

- [ ] **Step 1: 编写测试源码资源归属 RED 测试**

测试资源包含 ownerUserId 和 assigneeUserId。断言学生 A 可访问 A、访问 B 为 403；维修人员 A 可访问自己负责资源、访问 B 为 403；管理员访问全部；前端传入伪造 ownerId 不改变 UserContext 身份。

- [ ] **Step 2: 实现最小测试服务模式**

测试源码服务按角色从 UserContext 读取身份并显式判断归属，失败抛 FORBIDDEN。不得创建生产 DataScope AOP、注解或通用 SQL 改写器。

- [ ] **Step 3: 固化生产开发规范**

更新安全/架构文档，提供列表 Mapper 显式条件和详情 Service 归属校验的示例签名，但不增加正式业务 SQL。

---

### Task 9: 前端认证公共能力

**Interfaces:**
- Consumes: `/api/auth/login` 和 Bearer JWT。
- Produces: `login()`、`tokenStorage`、Axios 请求头和 Pinia 登录态。

- [ ] **Step 1: 编写前端 RED 测试**

覆盖登录请求类型、保存/读取/清除 token、带 token 请求自动添加单个 Bearer、无 token 不添加、401 先清 token/用户再提示；重复请求不得形成 `Bearer Bearer`。

- [ ] **Step 2: 实现 tokenStorage 与 auth API**

存储键集中定义；`login(request)` 调用现有 `request<LoginResponse>`；不在组件中直接操作 localStorage。

- [ ] **Step 3: 接入 Axios 和 Pinia**

请求拦截器从 tokenStorage 获取令牌；未登录处理清除 token 和 currentUser。保留“只有存在 login 命名路由才跳转”的阶段 0 规则，本阶段不制作业务页面。

- [ ] **Step 4: 运行前端全套测试和构建**

Run: `npm run test -- --run`、`npm run build`。Expected: 全部通过，只有已记录的包体积警告可接受。

---

### Task 10: 开发账号、文档与全链路验收

**Interfaces:**
- Consumes: Tasks 1–9。
- Produces: 可复现的本地初始化、中文契约和 PLAN-1 验收证据。

- [ ] **Step 1: 创建本地开发账号初始化脚本**

脚本从忽略提交的环境变量读取四个账号及初始密码，生成 BCrypt 后按 username 幂等插入管理员、维修人员、学生和停用账号；不覆盖既有账号，不输出密码或哈希。

- [ ] **Step 2: 更新中文文档**

数据库文档登记实际 17 表和既有验证表；API 文档登记登录请求/响应及新数字错误码；安全文档登记 JWT、角色矩阵、令牌与数据范围；README 添加初始化与验证命令。

- [ ] **Step 3: 后端全量验证**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" test package`。Expected: 所有单元、MockMvc、权限、数据范围和 MySQL 集成测试通过并生成 JAR。

- [ ] **Step 4: 前端全量验证**

Run: `npm run test -- --run`、`npm run build`。Expected: 全部测试与生产构建通过。

- [ ] **Step 5: 数据库与运行态验收**

运行 schema 验证脚本；登录四类账号；携带 JWT 验证 UserContext、三角色 200/403 和无认证 401；重启 MySQL 后再次验证 17 表及测试记录；`docker compose ps` 确认 MySQL/Redis/MinIO healthy、Nginx running。

- [ ] **Step 6: 对照 PLAN-1 Definition of Done**

逐项记录数据库、Entity、Mapper、Enum、认证、角色和数据范围结论。CRUD、业务流程和真实数据范围 SQL明确标为后续阶段未实现，不用测试桩冒充业务完成。
