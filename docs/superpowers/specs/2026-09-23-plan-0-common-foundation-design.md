# 阶段 0 公共基础能力设计规格

## 1. 目标

在现有可启动的前后端骨架上，建立后续业务模块共同依赖的工程规范、统一响应、异常处理、参数校验、分页、登录用户上下文和前端请求处理能力。

阶段 0 不开发具体宿舍报修业务。完成后，后续模块不再重复决定接口响应格式、异常返回方式、分页字段、当前用户来源和操作人来源。

## 2. 范围

### 2.1 包含

- 统一后端包结构和职责边界。
- 统一 `Result<T>` 和 `PageResult<T>`。
- 通用错误码、业务异常和全局异常处理。
- Jakarta Validation 参数校验异常转换。
- PageHelper 分页基础能力和分页参数限制。
- `UserRoleEnum`、`LoginUser` 和 `UserContext`。
- Spring Security 未登录、无权限的统一 JSON 响应。
- 前端 Axios 统一响应解析、错误提示入口和登录失效入口。
- 前端统一分页请求、响应类型。
- 自动化测试、联调验证和受影响文档更新。

### 2.2 不包含

- JWT 解析、登录接口、令牌签发和刷新。
- 真实角色授权规则和数据范围控制。
- 故障类型、维修人员、区域、报修、工单、派单、转派、请假、评价、返工等业务。
- 正式业务表、业务 Mapper 和业务状态枚举。
- 复杂 RBAC、DataScope AOP、审计 AOP、工作流、消息队列、分布式锁框架或 DDD 分层。
- 完整原型页面迁移。

## 3. 后端包结构

保留现有根包 `com.dormrepair`，不迁移到文档中的示例包名。

```text
com.dormrepair
├─ common
│  ├─ constant
│  ├─ enums
│  ├─ exception
│  └─ result
├─ config
├─ security
│  ├─ context
│  └─ model
├─ controller
├─ service
│  └─ impl
├─ mapper
├─ entity
├─ dto
├─ vo
├─ task
├─ util
└─ DormRepairApplication
```

只创建后续已有明确职责的包和类，不为保持目录外观创建空接口、空抽象类或虚假业务对象。

## 4. 统一响应结构

`Result<T>` 包含：

```java
private Integer code;
private String message;
private T data;
```

提供以下最小工厂方法：

```java
Result.success()
Result.success(data)
Result.fail(code, message)
Result.fail(ResultCodeEnum resultCode)
```

成功响应统一为：

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

分页使用 `Result<PageResult<T>>`，不增加分页专用响应包装方法。

现有 `/api/health` 也改为返回 `Result<HealthResponse>`，使公共接口遵守同一契约。

## 5. HTTP 状态与错误码

HTTP 状态表达请求在协议层面的处理结果，`Result.code` 表达系统中的具体结果。所有状态均返回统一 `Result` 响应体。

| 场景 | HTTP 状态 | Result.code |
|---|---:|---:|
| 成功 | 200 | `0` |
| 参数校验、参数绑定、参数类型错误 | 400 | `10001` |
| 未登录或登录失效 | 401 | `10002` |
| 无权限 | 403 | `10003` |
| 请求资源或业务数据不存在 | 404 | `10004` 或后续业务错误码 |
| 数据冲突 | 409 | `10005` |
| 请求方法错误 | 405 | `10006` |
| 未知异常、空指针、数据库连接或写入失败 | 500 | `50000` |

查询没有结果但没有违反业务约束时，不视为系统异常：集合查询返回空集合，分页返回 `total = 0` 和空 `records`。只有意外空指针等程序故障返回 `500`。

`ResultCodeEnum` 第一阶段只定义：

- `SUCCESS`
- `PARAM_ERROR`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `DATA_NOT_FOUND`
- `DATA_CONFLICT`
- `OPERATION_NOT_ALLOWED`
- `SYSTEM_ERROR`

请求方法错误复用 `OPERATION_NOT_ALLOWED`，不提前扩展业务错误码。后续模块使用独立编号段增加业务错误码。

## 6. 业务异常与全局异常处理

`BusinessException` 继承 `RuntimeException`，保存整数错误码，支持：

- 通过 `ResultCodeEnum` 构造。
- 通过 `ResultCodeEnum` 加自定义消息构造。

`GlobalExceptionHandler` 使用 `@RestControllerAdvice`，负责转换：

- `BusinessException`。
- `MethodArgumentNotValidException`。
- `ConstraintViolationException`。
- `BindException`。
- `MethodArgumentTypeMismatchException`。
- `HttpMessageNotReadableException`。
- `HttpRequestMethodNotSupportedException`。
- 其他 `Exception`。

业务异常记录 `WARN` 且不打印无意义的完整堆栈。未知系统异常以 `ERROR` 记录完整 `Throwable`，客户端只收到“系统异常，请稍后重试”，不得包含 SQL、连接信息、服务器路径或 Java 堆栈。

业务异常的 HTTP 状态根据错误码映射；未知业务错误默认使用 `400`，系统异常使用 `500`。

Spring Security 的 `AuthenticationEntryPoint` 和 `AccessDeniedHandler` 使用同一个 JSON 序列化契约，分别返回 `401 + UNAUTHORIZED` 和 `403 + FORBIDDEN`。

## 7. 参数校验

业务 DTO 使用 Jakarta Validation。阶段 0 使用测试 DTO 验证：

- Body 缺少字段。
- 字段为空。
- 字段长度非法。
- 路径参数类型错误。
- 查询参数类型错误。
- JSON 格式错误。

参数问题统一返回 `400 + PARAM_ERROR`。字段错误消息优先返回第一个可稳定定位的校验消息，不暴露内部类名或字段绑定实现细节。

用于验收异常转换的测试 Controller 只存在于测试源码，生产源码不保留演示或故障触发接口。

## 8. PageHelper 分页

引入 PageHelper 的 Spring Boot 3 兼容版本。

`PageQuery` 默认：

```text
pageNum = 1
pageSize = 10
```

归一规则：

```text
pageNum < 1  → 1
pageSize < 1 → 10
pageSize > 100 → 100
```

`PageResult<T>` 包含：

```java
private Long total;
private Integer pageNum;
private Integer pageSize;
private List<T> records;
```

提供从 `PageInfo<T>` 转换到 `PageResult<T>` 的唯一入口，业务接口不直接返回 PageHelper 类型。

分页调用必须严格遵守：

```text
PageHelper.startPage
→ 紧邻的 Mapper 查询
→ PageInfo
→ PageResult
```

使用测试源码中的 H2 表、测试 Mapper 和固定数据验证第一页、中间页、最后一页、越界页、空数据、最大页大小、非法参数及条件筛选。生产源码不创建虚假业务表或业务 Mapper。

## 9. 登录用户模型与上下文

`UserRoleEnum`：

```text
STUDENT = 1
WORKER = 2
ADMIN = 3
```

`LoginUser` 只包含：

```java
Long userId
String username
String realName
Integer roleType
```

不加入 `workerId`，因为系统用户 ID 与维修人员业务实体 ID 属于不同身份。

`UserContext` 是当前用户的唯一公共入口，底层读取 `SecurityContextHolder`，提供：

```java
getCurrentUser()
getCurrentUserId()
getCurrentRoleType()
isAdmin()
isWorker()
isStudent()
```

无认证、匿名认证或 principal 不是 `LoginUser` 时，统一抛出 `BusinessException(UNAUTHORIZED)`。本阶段测试通过显式设置和清理 SecurityContext 验证不同用户、角色判断与未登录行为，不实现 JWT 解析。

后续创建人、审批人、学生用户 ID 和流转操作人必须由 `UserContext` 获取，不接受前端传入的身份字段。

## 10. 前端请求规范

前端定义与后端一致的 TypeScript 类型：

```ts
interface Result<T> {
  code: number
  message: string
  data: T
}

interface PageQuery {
  pageNum: number
  pageSize: number
}

interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}
```

Axios 客户端区分两类失败：

- 收到 HTTP 响应时，读取统一 `Result`；`code = 0` 返回 `data`，否则生成包含 HTTP 状态、业务码和消息的 `ApiError`。
- 网络错误或非统一响应时，转换为不泄露底层实现信息的通用错误。

错误提示通过单一适配入口触发，避免业务页面重复调用提示组件。阶段 0 可以使用 Element Plus 的 `ElMessage` 作为默认实现；后续学生端和维修人员端接入 Vant 页面时，只替换提示适配器，不修改 Axios 解析逻辑。

收到 `401` 或业务码 `UNAUTHORIZED` 时：

1. 清理 Pinia 中的登录状态。
2. 调用统一的未登录处理入口。
3. 当前尚未实现 Vue 登录页，因此不创建虚假登录页面；阶段 1 注册真实登录路由后，由该入口完成跳转。

前端使用 Vitest 和 Axios Mock Adapter 或等价的请求适配测试，覆盖成功、业务错误、401、网络错误和分页类型约定。测试断言真实请求封装的返回和副作用，不只断言 Mock 本身。

## 11. 测试与验收

### 11.1 后端

- `Result`：无数据成功、有数据成功、列表、VO、分页和失败。
- 错误码：编号、消息和 HTTP 映射。
- `BusinessException`：枚举构造和覆盖消息。
- MockMvc：参数校验、参数类型、JSON 格式、方法错误、业务异常、未登录、无权限和系统异常。
- 日志：系统异常由日志测试或测试 Appender 确认保留 Throwable，响应确认不泄露异常细节。
- PageHelper：完整分页边界和筛选场景。
- `UserContext`：用户 A、用户 B、三类角色和未登录。

### 11.2 前端

- 统一响应解包。
- 业务失败提示。
- 401 清理登录状态并调用登录失效入口。
- 网络异常转换。
- 分页请求和响应类型。
- 现有首页测试继续通过。

### 11.3 集成

- 后端测试与 Maven 打包通过。
- 前端测试与 Vite 构建通过。
- `/api/health` 直连返回统一 `Result`。
- Nginx `/api/health` 代理返回相同结构。
- Docker 基础设施状态不因本阶段代码变更而受损。

## 12. 文档

实现时同步更新：

- `doc/api/`：统一 Result、HTTP 状态、错误码和分页协议。
- `doc/architecture/`：公共调用链和 UserContext 边界。
- `doc/security/`：401/403 响应、上下文模型和阶段 1 边界。
- `doc/rules/`：包结构、异常、分页和操作人规则。

## 13. 完成标准

一个新业务模块可以直接按照以下链路开发：

```text
Controller
→ DTO 校验
→ Service
→ UserContext
→ Mapper
→ PageHelper（分页时）
→ VO
→ Result
```

异常链路统一为：

```text
BusinessException 或框架异常
→ GlobalExceptionHandler / Spring Security Handler
→ HTTP 状态 + Result.fail
→ Axios
→ 统一提示或登录失效入口
```

阶段 0 完成后进入阶段 1，实现 JWT 登录认证、角色权限和数据范围。
