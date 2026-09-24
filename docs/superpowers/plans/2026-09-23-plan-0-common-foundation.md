# 阶段 0 公共基础能力 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后续业务模块建立统一响应、HTTP 状态、异常处理、参数校验、PageHelper 分页、登录用户上下文和前端 Axios 公共规范。

**Architecture:** 保留 `com.dormrepair` 根包，按 Spring Boot + MyBatis 常规分层增加小而明确的公共类；认证细节留到阶段 1，业务代码只依赖 `UserContext`。前端将 Axios 协议解析、错误提示和登录失效处理分离，通过类型化 `request<T>` 向业务页面提供统一数据。

**Tech Stack:** Java 17、Spring Boot 3.3.13、Spring MVC、Spring Security、Hibernate Validator、MyBatis 3、PageHelper、H2、JUnit 5、MockMvc；Vue 3、TypeScript、Axios、Pinia、Vue Router、Element Plus、Vitest、Axios Mock Adapter。

**Spec:** `docs/superpowers/specs/2026-09-23-plan-0-common-foundation-design.md`

## Global Constraints

- 不开发任何具体宿舍报修业务，不创建正式业务表、业务 Mapper 或业务状态枚举。
- 保留根包 `com.dormrepair`，不重命名现有启动类和配置类。
- 所有 Controller 与 Spring Security 错误响应均使用统一 `Result` JSON。
- HTTP 状态使用 `200/400/401/403/404/405/409/500`，不得把所有失败伪装成 `200`。
- 系统异常记录完整 Throwable，但响应不得暴露异常消息、SQL、连接信息、路径或堆栈。
- PageHelper 类型不得出现在对外响应协议中。
- 业务身份字段由后端 `UserContext` 获取，不建立接收前端操作人 ID 的公共 DTO。
- 本阶段不实现 JWT 解析、登录接口、真实 RBAC 或数据范围。
- 测试 Controller、测试表和故障触发代码只存在于测试源码。
- 修改公共协议时同步更新 API、架构、安全和项目规则文档。
- 当前目录不是 Git 仓库，不初始化 Git、不提交、不推送。

## Review Focus

- 异常处理器是否对未知异常返回原始 `Exception.message`，导致敏感信息泄露。
- Spring Security 的 401/403 是否绕过统一 `Result`，或错误地返回 HTML。
- `PageHelper.startPage` 是否可能影响错误的下一条 SQL，分页上限是否可绕过。
- `UserContext` 是否接受任意 principal 强制转换，导致空指针或类型转换异常而非 401。
- Axios 是否同时处理 HTTP 非 2xx 的统一 Result、HTTP 2xx 的业务失败、网络失败和重复提示。

---

## 文件结构

### 后端生产源码

- Create: `backend/src/main/java/com/dormrepair/common/result/Result.java`
- Create: `backend/src/main/java/com/dormrepair/common/result/PageQuery.java`
- Create: `backend/src/main/java/com/dormrepair/common/result/PageResult.java`
- Create: `backend/src/main/java/com/dormrepair/common/enums/ResultCodeEnum.java`
- Create: `backend/src/main/java/com/dormrepair/common/enums/UserRoleEnum.java`
- Create: `backend/src/main/java/com/dormrepair/common/exception/BusinessException.java`
- Create: `backend/src/main/java/com/dormrepair/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/dormrepair/security/model/LoginUser.java`
- Create: `backend/src/main/java/com/dormrepair/security/context/UserContext.java`
- Create: `backend/src/main/java/com/dormrepair/security/handler/SecurityErrorResponseWriter.java`
- Create: `backend/src/main/java/com/dormrepair/security/handler/RestAuthenticationEntryPoint.java`
- Create: `backend/src/main/java/com/dormrepair/security/handler/RestAccessDeniedHandler.java`
- Modify: `backend/src/main/java/com/dormrepair/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/dormrepair/health/HealthController.java`
- Create: `backend/src/main/java/com/dormrepair/health/HealthResponse.java`
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`

不为 `controller/service/mapper/entity/dto/vo/task/util` 创建无内容 Java 类型；包会在首个真实类出现时自然建立。

### 后端测试源码

- Create: `backend/src/test/java/com/dormrepair/common/result/ResultTest.java`
- Create: `backend/src/test/java/com/dormrepair/common/exception/BusinessExceptionTest.java`
- Create: `backend/src/test/java/com/dormrepair/common/exception/GlobalExceptionHandlerTest.java`
- Create: `backend/src/test/java/com/dormrepair/common/result/PageQueryTest.java`
- Create: `backend/src/test/java/com/dormrepair/common/result/PageResultIntegrationTest.java`
- Create: `backend/src/test/java/com/dormrepair/security/context/UserContextTest.java`
- Create: `backend/src/test/java/com/dormrepair/security/handler/SecurityErrorHandlerTest.java`
- Create: `backend/src/test/java/com/dormrepair/testsupport/TestExceptionController.java`
- Create: `backend/src/test/java/com/dormrepair/testsupport/PageHelperTestMapper.java`
- Create: `backend/src/test/resources/schema.sql`
- Create: `backend/src/test/resources/data.sql`
- Modify: `backend/src/test/java/com/dormrepair/health/HealthControllerTest.java`
- Modify: `backend/src/test/resources/application-test.yml`

### 前端

- Create: `frontend/src/api/types.ts`
- Create: `frontend/src/api/ApiError.ts`
- Create: `frontend/src/api/errorNotifier.ts`
- Create: `frontend/src/api/unauthorizedHandler.ts`
- Modify: `frontend/src/api/http.ts`
- Create: `frontend/src/api/http.spec.ts`
- Create: `frontend/src/api/pagination.ts`
- Create: `frontend/src/api/pagination.spec.ts`
- Modify: `frontend/src/stores/app.ts`
- Modify: `frontend/src/main.ts`
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`

### 文档

- Create: `doc/api/公共接口响应规范.md`
- Create: `doc/architecture/公共能力调用链.md`
- Create: `doc/security/登录用户上下文与安全响应.md`
- Create: `doc/rules/公共开发规范.md`
- Modify: `README.md`

---

### Task 1: Result、通用错误码和健康检查协议

**Files:**
- Create: `backend/src/main/java/com/dormrepair/common/result/Result.java`
- Create: `backend/src/main/java/com/dormrepair/common/enums/ResultCodeEnum.java`
- Create: `backend/src/test/java/com/dormrepair/common/result/ResultTest.java`
- Create: `backend/src/main/java/com/dormrepair/health/HealthResponse.java`
- Modify: `backend/src/main/java/com/dormrepair/health/HealthController.java`
- Modify: `backend/src/test/java/com/dormrepair/health/HealthControllerTest.java`

**Interfaces:**
- Produces: `Result<T>`；`ResultCodeEnum`；`GET /api/health` 返回 `Result<HealthResponse>`。
- Consumes: 无。

- [ ] **Step 1: 编写 Result 失败测试**

`ResultTest` 逐项断言：

```java
assertThat(Result.success().getCode()).isZero();
assertThat(Result.success().getMessage()).isEqualTo("success");
assertThat(Result.success().getData()).isNull();

Result<String> withData = Result.success("value");
assertThat(withData.getData()).isEqualTo("value");

Result<Void> failed = Result.fail(ResultCodeEnum.PARAM_ERROR);
assertThat(failed.getCode()).isEqualTo(10001);
assertThat(failed.getMessage()).isEqualTo("请求参数错误");
```

测试还覆盖 `Result.fail(29999, "测试失败")`，避免工厂方法忽略自定义值。

- [ ] **Step 2: 运行 Result 测试并确认 RED**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=ResultTest test`

Expected: 编译失败，因为 `Result` 和 `ResultCodeEnum` 尚不存在。

- [ ] **Step 3: 实现最小 Result 与错误码**

`ResultCodeEnum` 精确定义：

```java
SUCCESS(0, "success"),
PARAM_ERROR(10001, "请求参数错误"),
UNAUTHORIZED(10002, "未登录或登录状态已失效"),
FORBIDDEN(10003, "无权限访问"),
DATA_NOT_FOUND(10004, "数据不存在"),
DATA_CONFLICT(10005, "数据状态已发生变化"),
OPERATION_NOT_ALLOWED(10006, "当前操作不允许"),
SYSTEM_ERROR(50000, "系统异常，请稍后重试");
```

`Result<T>` 使用私有构造方法、只读 getter 和计划规定的四个静态工厂方法，不增加 builder 或分页快捷方法。

- [ ] **Step 4: 运行 Result 测试并确认 GREEN**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=ResultTest test`

Expected: Result 测试全部通过。

- [ ] **Step 5: 先修改健康检查契约测试**

`HealthControllerTest.healthEndpointIsPublic` 改为断言：

```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.code").value(0))
.andExpect(jsonPath("$.message").value("success"))
.andExpect(jsonPath("$.data.status").value("UP"))
.andExpect(jsonPath("$.data.service").value("dorm-repair-backend"));
```

- [ ] **Step 6: 运行健康检查测试并确认 RED**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=HealthControllerTest test`

Expected: FAIL，当前响应没有 `code/message/data` 外层。

- [ ] **Step 7: 实现健康检查统一响应**

创建不可变 `HealthResponse(String status, String service)`，控制器返回：

```java
return Result.success(new HealthResponse("UP", "dorm-repair-backend"));
```

- [ ] **Step 8: 运行 Task 1 测试**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=ResultTest,HealthControllerTest test`

Expected: 全部通过。

---

### Task 2: BusinessException、全局异常和安全错误响应

**Files:**
- Create: `backend/src/main/java/com/dormrepair/common/exception/BusinessException.java`
- Create: `backend/src/main/java/com/dormrepair/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/dormrepair/security/handler/SecurityErrorResponseWriter.java`
- Create: `backend/src/main/java/com/dormrepair/security/handler/RestAuthenticationEntryPoint.java`
- Create: `backend/src/main/java/com/dormrepair/security/handler/RestAccessDeniedHandler.java`
- Modify: `backend/src/main/java/com/dormrepair/config/SecurityConfig.java`
- Create: `backend/src/test/java/com/dormrepair/common/exception/BusinessExceptionTest.java`
- Create: `backend/src/test/java/com/dormrepair/common/exception/GlobalExceptionHandlerTest.java`
- Create: `backend/src/test/java/com/dormrepair/security/handler/SecurityErrorHandlerTest.java`
- Create: `backend/src/test/java/com/dormrepair/testsupport/TestExceptionController.java`

**Interfaces:**
- Consumes: `Result<T>`、`ResultCodeEnum`。
- Produces: `BusinessException#getCode()`；统一异常 HTTP 响应；401/403 JSON handler。

- [ ] **Step 1: 编写 BusinessException RED 测试**

断言枚举构造保存 code/message，自定义消息构造只覆盖 message、不改变 code。

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=BusinessExceptionTest test`

Expected: 编译失败，因为类不存在。

- [ ] **Step 2: 实现 BusinessException 并转绿**

只保留 `private final Integer code`、两个构造方法和 getter。

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=BusinessExceptionTest test`

Expected: PASS。

- [ ] **Step 3: 编写 GlobalExceptionHandler MockMvc RED 测试**

使用 `@WebMvcTest(TestExceptionController.class)`、`@Import(GlobalExceptionHandler.class)`、`@AutoConfigureMockMvc(addFilters = false)`。测试 Controller 提供测试源码专用接口，覆盖：

- `BusinessException(DATA_NOT_FOUND)` → HTTP 404、code 10004。
- 无效 `@Valid @RequestBody` → HTTP 400、code 10001、字段消息。
- `@RequestParam Long id` 接收 `abc` → HTTP 400。
- 非法 JSON → HTTP 400。
- GET 调用仅支持 POST 的路径 → HTTP 405、code 10006。
- `RuntimeException("jdbc:mysql://secret/path")` → HTTP 500、code 50000，响应不包含原消息。

日志测试为 `GlobalExceptionHandler` 的 Logback logger 临时挂载 `ListAppender`，断言系统异常事件级别为 ERROR 且 `IThrowableProxy` 非空。

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=GlobalExceptionHandlerTest test`

Expected: 编译失败，因为 handler 尚不存在。

- [ ] **Step 4: 实现 GlobalExceptionHandler**

返回 `ResponseEntity<Result<Void>>`。HTTP 映射：参数类 400、未登录 401、无权限 403、数据不存在 404、冲突 409、方法错误 405、其他业务异常 400、未知异常 500。

字段错误消息从 `BindingResult.getFieldErrors()` 的第一项读取；没有字段错误时回退到“请求参数错误”。未知异常只响应 `ResultCodeEnum.SYSTEM_ERROR`。

- [ ] **Step 5: 运行异常测试并转绿**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=BusinessExceptionTest,GlobalExceptionHandlerTest test`

Expected: 全部通过，系统异常响应不含敏感测试字符串，日志包含 Throwable。

- [ ] **Step 6: 编写 Security handler RED 测试**

直接使用 `MockHttpServletRequest`、`MockHttpServletResponse` 和真实 Jackson `ObjectMapper` 调用两个 handler：

- AuthenticationEntryPoint → 401、JSON、code 10002。
- AccessDeniedHandler → 403、JSON、code 10003。

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=SecurityErrorHandlerTest test`

Expected: 编译失败，因为 handler 不存在。

- [ ] **Step 7: 实现并接入安全错误响应**

`SecurityErrorResponseWriter` 负责设置 UTF-8 JSON Content-Type、HTTP 状态和序列化 `Result.fail`。两个 Spring Security handler 委托它写响应。`SecurityConfig` 注入这两个 handler，替换当前 `sendError`，并配置：

```java
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint(authenticationEntryPoint)
    .accessDeniedHandler(accessDeniedHandler)
)
```

- [ ] **Step 8: 运行 Task 2 测试与现有安全测试**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=BusinessExceptionTest,GlobalExceptionHandlerTest,SecurityErrorHandlerTest,HealthControllerTest test`

Expected: 全部通过；未定义受保护路径返回 401 和统一 Result。

---

### Task 3: PageHelper、PageQuery 与 PageResult

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application-test.yml`
- Create: `backend/src/main/java/com/dormrepair/common/result/PageQuery.java`
- Create: `backend/src/main/java/com/dormrepair/common/result/PageResult.java`
- Create: `backend/src/test/java/com/dormrepair/common/result/PageQueryTest.java`
- Create: `backend/src/test/java/com/dormrepair/common/result/PageResultIntegrationTest.java`
- Create: `backend/src/test/java/com/dormrepair/testsupport/PageHelperTestMapper.java`
- Create: `backend/src/test/resources/schema.sql`
- Create: `backend/src/test/resources/data.sql`

**Interfaces:**
- Consumes: MyBatis、H2 测试数据。
- Produces: `PageQuery#getPageNum/getPageSize`；`PageResult.from(PageInfo<T>)`。

- [ ] **Step 1: 添加 PageHelper 依赖**

在 `pom.xml` 增加属性和依赖：

```xml
<pagehelper-spring-boot.version>2.1.1</pagehelper-spring-boot.version>
<dependency>
  <groupId>com.github.pagehelper</groupId>
  <artifactId>pagehelper-spring-boot-starter</artifactId>
  <version>${pagehelper-spring-boot.version}</version>
</dependency>
```

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" dependency:tree -Dincludes=com.github.pagehelper:*`

Expected: 解析出 starter、PageHelper 和分页插件，构建无依赖冲突。

- [ ] **Step 2: 编写 PageQuery RED 测试**

覆盖默认值、null、0、负数、正常值、100 和大于 100：

```java
PageQuery query = new PageQuery();
assertThat(query.getPageNum()).isEqualTo(1);
assertThat(query.getPageSize()).isEqualTo(10);

query.setPageNum(0);
query.setPageSize(101);
assertThat(query.getPageNum()).isEqualTo(1);
assertThat(query.getPageSize()).isEqualTo(100);
```

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=PageQueryTest test`

Expected: 编译失败，因为 PageQuery 不存在。

- [ ] **Step 3: 实现 PageQuery 并转绿**

使用默认字段值和 setter 归一化；getter 返回已归一化值，不允许 null 传播到 PageHelper。

- [ ] **Step 4: 编写 PageHelper 集成 RED 测试**

测试资源创建 `page_helper_probe(id, category, display_name)` 并插入 25 条固定记录，其中类别 A 15 条、B 10 条。测试 Mapper 使用 `@Mapper` 和 `@Select`，只存在于测试源码。

测试覆盖：第一页、中间页、最后一页、越界页、空筛选、`pageSize=100`、非法 pageNum/pageSize、类别筛选。每个用例严格执行：

```java
PageHelper.startPage(query.getPageNum(), query.getPageSize());
List<ProbeRow> rows = mapper.selectByCategory(category);
PageResult<ProbeRow> result = PageResult.from(new PageInfo<>(rows));
```

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=PageResultIntegrationTest test`

Expected: 编译失败，因为 PageResult 不存在。

- [ ] **Step 5: 实现 PageResult 与测试配置**

`PageResult<T>` 提供只读 getter、私有构造和静态 `from(PageInfo<T>)`，复制 `total/pageNum/pageSize/list`，使用不可变副本或新的 `ArrayList`，不向外暴露 PageInfo。

测试 Profile 开启 SQL 初始化；生产 `application.yml` 增加 PageHelper 合理化配置，但仍由 `PageQuery` 实施项目自己的上限规则。

- [ ] **Step 6: 运行 Task 3 测试**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=PageQueryTest,PageResultIntegrationTest test`

Expected: 所有分页边界和筛选场景通过。

---

### Task 4: UserRoleEnum、LoginUser 与 UserContext

**Files:**
- Create: `backend/src/main/java/com/dormrepair/common/enums/UserRoleEnum.java`
- Create: `backend/src/main/java/com/dormrepair/security/model/LoginUser.java`
- Create: `backend/src/main/java/com/dormrepair/security/context/UserContext.java`
- Create: `backend/src/test/java/com/dormrepair/security/context/UserContextTest.java`

**Interfaces:**
- Produces: `UserContext.getCurrentUser/getCurrentUserId/getCurrentRoleType/isAdmin/isWorker/isStudent`。
- Consumes: `SecurityContextHolder`、`BusinessException`、`ResultCodeEnum.UNAUTHORIZED`。

- [ ] **Step 1: 编写 UserContext RED 测试**

每个测试后执行 `SecurityContextHolder.clearContext()`。覆盖：

- principal 为用户 A 时返回 A 的 userId。
- 替换为用户 B 后返回 B，不能缓存旧用户。
- 三种角色判断互斥且正确。
- Authentication 为 null、未认证、anonymous 字符串、其他 principal 类型时均抛出 code 10002 的 BusinessException。

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=UserContextTest test`

Expected: 编译失败，因为相关类型不存在。

- [ ] **Step 2: 实现枚举、模型与上下文**

`UserRoleEnum` 固定 `STUDENT(1)`、`WORKER(2)`、`ADMIN(3)`。`LoginUser` 是只读普通 Java 类，不实现 `UserDetails`，避免阶段 0 提前绑定认证细节。

`UserContext` 为不可实例化工具类，每次调用实时读取 `SecurityContextHolder.getContext().getAuthentication()`；只有 `isAuthenticated()` 且 principal 为 `LoginUser` 才返回，否则抛出 UNAUTHORIZED。

- [ ] **Step 3: 运行 UserContext 测试**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" -Dtest=UserContextTest test`

Expected: 全部通过，无跨测试上下文污染。

- [ ] **Step 4: 运行后端完整测试**

Run: `mvn "-Dmaven.repo.local=..\.m2\repository" test`

Expected: 新旧测试全部通过。

---

### Task 5: 前端统一请求、错误处理和分页类型

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/src/api/types.ts`
- Create: `frontend/src/api/ApiError.ts`
- Create: `frontend/src/api/errorNotifier.ts`
- Create: `frontend/src/api/unauthorizedHandler.ts`
- Modify: `frontend/src/api/http.ts`
- Create: `frontend/src/api/http.spec.ts`
- Create: `frontend/src/api/pagination.ts`
- Create: `frontend/src/api/pagination.spec.ts`
- Modify: `frontend/src/stores/app.ts`
- Modify: `frontend/src/main.ts`

**Interfaces:**
- Consumes: 后端 `Result<T>`、HTTP 状态、错误码 10002。
- Produces: `request<T>(config): Promise<T>`；`ApiError`；错误提示和未登录回调注册入口；`normalizePageQuery`。

- [ ] **Step 1: 安装请求测试依赖**

Run: `npm install --save-dev axios-mock-adapter --registry=https://registry.npmmirror.com --no-audit --no-fund`

Expected: `package.json` 和 lockfile 仅增加 Axios Mock Adapter 及其必要依赖。

- [ ] **Step 2: 编写 Axios RED 测试**

使用真实 Axios 实例和 Axios Mock Adapter，测试：

- HTTP 200 + code 0：`request<User>` 直接返回 `data`。
- HTTP 200 + code 10004：拒绝 `ApiError`，保存业务码和消息，提示一次。
- HTTP 401 + code 10002：拒绝 `ApiError`，先调用未登录处理入口，再提示一次。
- HTTP 500 + code 50000：保留通用后端消息，不暴露 Axios 内部错误。
- 网络错误：转换为消息“网络请求失败，请稍后重试”，提示一次。
- 非 Result 响应：转换为通用 ApiError，不能把对象序列化内容直接展示给用户。

测试通过 `setErrorNotifier(spy)` 与 `setUnauthorizedHandler(spy)` 注入真实公共边界的测试实现，并在 `afterEach` 恢复默认实现。

Run: `npm run test -- --run src/api/http.spec.ts`

Expected: 编译失败，因为公共类型和处理器不存在。

- [ ] **Step 3: 实现类型、ApiError 和处理入口**

`types.ts` 精确定义 `Result<T>`、`PageQuery`、`PageResult<T>`。`ApiError` 保存 `httpStatus`、`code` 和用户可展示 message。

`errorNotifier.ts` 提供 `setErrorNotifier` 与 `notifyError`，默认实现调用 `ElMessage.error`。`unauthorizedHandler.ts` 提供同样模式的注册和调用入口，默认无副作用。

- [ ] **Step 4: 实现 Axios 解析**

保留 Axios 实例的 `/api` baseURL 和 10 秒超时。响应成功和失败分支都尝试读取统一 Result，通过同一个内部函数转换，确保 401 不会提示两次。

导出：

```ts
export function request<T>(config: AxiosRequestConfig): Promise<T>
```

不向业务调用者暴露 `AxiosResponse<Result<T>>`。

- [ ] **Step 5: 接入 Pinia 登录状态清理**

`useAppStore` 增加最小 `currentUser` 状态和 `clearLoginState()`。`main.ts` 在 Pinia 与 Router 创建后注册未登录处理：清空状态；仅当 `router.hasRoute('login')` 时跳转名为 `login` 的路由。本阶段不创建虚假登录页。

- [ ] **Step 6: 运行 Axios 测试并转绿**

Run: `npm run test -- --run src/api/http.spec.ts`

Expected: 所有成功、业务错误、401、500、网络错误和非标准响应测试通过。

- [ ] **Step 7: 编写并实现分页 RED→GREEN**

`pagination.spec.ts` 断言：默认值 1/10，非法 pageNum 归一为 1，非法 pageSize 归一为 10，大于 100 限制为 100，原对象不被修改。

Run RED: `npm run test -- --run src/api/pagination.spec.ts`

实现：

```ts
export function normalizePageQuery(query?: Partial<PageQuery>): PageQuery
```

Run GREEN: `npm run test -- --run src/api/pagination.spec.ts`

Expected: 全部通过。

- [ ] **Step 8: 运行前端完整测试与构建**

Run:

```powershell
npm run test -- --run
npm run build
```

Expected: 所有测试通过，TypeScript 和 Vite 构建成功。

---

### Task 6: 文档、全链路验收与阶段 0 收尾

**Files:**
- Create: `doc/api/公共接口响应规范.md`
- Create: `doc/architecture/公共能力调用链.md`
- Create: `doc/security/登录用户上下文与安全响应.md`
- Create: `doc/rules/公共开发规范.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: Tasks 1–5 的最终接口。
- Produces: 后续业务模块的中文开发契约与阶段 0 验收证据。

- [ ] **Step 1: 编写 API 规范**

记录统一 Result 示例、HTTP 状态表、通用错误码、分页请求/响应 JSON、空集合语义，以及前后端字段类型。示例不得包含真实凭据或生产数据。

- [ ] **Step 2: 编写架构与安全规范**

架构文档记录 Controller→DTO→Service→UserContext→Mapper→PageHelper→VO→Result 和异常反向链路。安全文档记录 LoginUser 字段、UserContext 前置条件、401/403 Result、操作人不得由前端传入，以及 JWT 留到阶段 1。

- [ ] **Step 3: 编写公共开发规则并更新 README**

规则文档明确包职责、异常使用、PageHelper 紧邻查询、PageResult 边界和禁止重复工具类。README 增加阶段 0 测试命令和当前未实现范围。

- [ ] **Step 4: 执行后端全量验证**

Run:

```powershell
mvn "-Dmaven.repo.local=..\.m2\repository" test
mvn "-Dmaven.repo.local=..\.m2\repository" package
```

Expected: 全部测试通过，生成可执行 JAR。

- [ ] **Step 5: 执行前端全量验证**

Run:

```powershell
npm run test -- --run
npm run build
```

Expected: 全部测试和 TypeScript/Vite 构建通过。

- [ ] **Step 6: 启动后端并验证统一健康响应**

使用根目录 `scripts/start-backend.ps1` 启动后端。直连和 Nginx 请求均断言：HTTP 200、code 0、message `success`、data.status `UP`。

- [ ] **Step 7: 验证安全和基础设施边界**

请求未定义受保护路径，断言 HTTP 401、code 10002、JSON Content-Type。检查 `docker compose ps`，MySQL、Redis、MinIO 保持 healthy，Nginx running；不得重建或删除持久化目录。

- [ ] **Step 8: 对照 PLAN-0 验收清单**

逐项核对工程结构、Result、异常、参数校验、分页、用户上下文和前端公共规范。业务验收项若依赖阶段 1 或具体业务模块，只能报告“结构已建立、业务场景未实现”，不得用测试桩冒充业务完成。

