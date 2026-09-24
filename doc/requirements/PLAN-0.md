# 阶段 0：基础工程规范化与公共能力建设

## 0.1 阶段目标

阶段 0 不开发具体宿舍报修业务。

本阶段目标是：

> 在现有前后端空项目和基础运行环境已经可用的前提下，建立后续所有业务模块统一依赖的工程规范、接口返回规范、异常处理机制、分页能力、登录用户上下文和操作人获取能力，为后续权限控制、数据范围、CRUD 和工单业务开发提供稳定基础。

阶段 0 完成以后，后续业务模块开发时原则上不再重复处理：

- Controller 返回结构；
- 分页参数和分页结果；
- 业务异常返回；
- 系统异常返回；
- 参数校验异常；
- 当前登录用户获取；
- 当前用户 ID / 角色获取；
- 创建人、修改人等操作人获取；
- 基础包结构和代码放置规范。

# 一、当前已完成内容

目前基础环境建设已经完成。

## 1.1 前后端工程

已完成：

- 后端空项目建立；
- 前端空项目建立；
- 前后端项目均可正常启动。

## 1.2 Docker 基础环境

四个容器均已正常运行。

基础服务包括：

- MySQL；
- Redis；
- MinIO；
- Nginx。

## 1.3 MySQL

已完成：

- MySQL 容器部署；
- MySQL 数据持久化重新建设；
- MySQL 认证验证；
- 应用能够正常连接 MySQL。

## 1.4 Redis

已完成：

- Redis 容器部署；
- Redis 密码认证；
- 应用能够正常连接 Redis。

## 1.5 MinIO

已完成：

- MinIO 容器部署；
- MinIO 健康状态验证；
- 对象存储服务能够正常访问。

本阶段暂不继续封装具体业务文件上传逻辑，文件上传能力可在报修图片功能开始前补充。

## 1.6 前后端访问链路

已完成：

### 直连链路

```
前端
  ↓
后端
```

访问正常。

### Nginx 代理链路

```
浏览器
  ↓
Nginx
  ├─ 前端静态资源
  └─ 后端 API
```

代理访问正常。

# 二、阶段 0 剩余任务总览

剩余任务按照依赖关系分为：

```
包结构与基础常量
        ↓
统一 Result
        ↓
异常码体系
        ↓
业务异常
        ↓
统一异常处理
        ↓
参数校验处理

与此同时：

PageHelper 分页基础能力
        ↓
统一分页请求/返回规范

然后：

登录用户模型
        ↓
登录用户上下文
        ↓
当前操作人获取

最后：

公共能力联调
        ↓
阶段 0 验收
```

其中：

- Result、异常码、异常处理存在明确前后依赖；
- PageHelper 可以与异常体系并行开发；
- 登录用户上下文可以在 Result 基础完成后并行开发；
- 操作人获取依赖登录用户上下文。

# 三、任务 0.1：统一后端包结构

## 3.1 目标

先统一项目目录，避免后续每个功能模块自行创建不同层级和命名。

当前项目第一阶段不引入复杂 DDD 分层，保持 Spring Boot + MyBatis 的常规简单结构。

建议：

```
com.xxx.repair
│
├─ common
│  ├─ result
│  ├─ exception
│  ├─ constant
│  └─ enums
│
├─ config
│
├─ security
│  ├─ context
│  └─ model
│
├─ controller
│
├─ service
│  └─ impl
│
├─ mapper
│
├─ entity
│
├─ dto
│
├─ vo
│
├─ task
│
├─ util
│
└─ RepairApplication
```

后续具体业务继续按业务模块命名类，例如：

```
RepairOrderController
RepairOrderService
RepairOrderServiceImpl
RepairOrderMapper

RepairWorkerController
RepairWorkerService
RepairWorkerServiceImpl
RepairWorkerMapper
```

暂时不继续拆：

```
domain
application
infrastructure
repository
assembler
facade
command
query
```

避免当前项目结构过重。

## 3.2 各目录职责

### `common/result`

存放：

```
Result
PageResult
```

### `common/exception`

存放：

```
BusinessException
GlobalExceptionHandler
```

### `common/enums`

存放系统级枚举，例如：

```
ResultCodeEnum
UserRoleEnum
```

具体业务状态枚举以后跟随对应业务模块增加。

### `common/constant`

存放：

```
系统公共常量
Redis Key 前缀
请求头常量
```

不放业务状态。

### `security`

负责：

```
登录用户信息
当前用户上下文
权限相关能力
```

阶段 0 只完成上下文能力。

真正角色权限控制放到后续权限阶段。

## 3.3 验收项

- 包结构创建完成；
- 无无意义空抽象层；
- 后续 Controller / Service / Mapper 有统一放置位置；
- 公共类与业务类边界明确。

### 估时

```
0.25 人日
```

# 四、任务 0.2：统一 Result 返回结构

## 4.1 目标

所有 Controller 接口统一返回格式。

建议：

```
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

}
```

统一格式：

```
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 4.2 建议约定

成功：

```
code = 0
message = success
```

业务失败：

```
code != 0
message = 对应业务错误信息
```

HTTP Status 第一阶段可以保持：

```
200
```

由业务 `code` 表示具体业务结果。

如果后续已有团队统一 HTTP 状态码规范，再统一调整。

## 4.3 Result 提供的基本方法

建议只提供必要方法：

```
Result.success()

Result.success(data)

Result.fail(code, message)

Result.fail(ResultCodeEnum resultCode)
```

不要一开始加入大量：

```
successPage
errorData
warn
unauthorized
forbidden
build
of
```

分页通过 `Result<PageResult<T>>` 表达即可。

## 4.4 示例

普通查询：

```
@GetMapping("/{id}")
public Result<RepairOrderVO> detail(@PathVariable Long id) {
    return Result.success(repairOrderService.detail(id));
}
```

删除：

```
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id) {
    repairOrderService.delete(id);
    return Result.success();
}
```

分页：

```
@GetMapping
public Result<PageResult<RepairOrderVO>> page(RepairOrderQueryDTO query) {
    return Result.success(repairOrderService.page(query));
}
```

## 4.5 验收项

至少验证：

```
无 data 成功
有 data 成功
List 返回
VO 返回
分页返回
业务失败返回
```

接口 JSON 结构保持一致。

### 估时

```
0.25 人日
```

# 五、任务 0.3：异常码体系

## 5.1 目标

统一系统错误码，避免后续出现：

```
throw new RuntimeException("没有权限");

throw new RuntimeException("工单不存在");

return Result.fail(500, "失败");
```

等无统一口径情况。

## 5.2 第一阶段错误码不要设计过重

建议采用：

```
系统通用错误
+
业务模块错误
```

即可。

例如：

```
public enum ResultCodeEnum {

    SUCCESS(0, "成功"),

    PARAM_ERROR(10001, "请求参数错误"),

    UNAUTHORIZED(10002, "未登录或登录状态已失效"),

    FORBIDDEN(10003, "无权限访问"),

    DATA_NOT_FOUND(10004, "数据不存在"),

    DATA_CONFLICT(10005, "数据状态已发生变化"),

    OPERATION_NOT_ALLOWED(10006, "当前状态不允许执行该操作"),

    SYSTEM_ERROR(50000, "系统异常");

}
```

## 5.3 后续业务错误码

到对应业务阶段再增加。

例如工单：

```
20001 工单不存在
20002 工单状态不允许当前操作
20003 当前用户不是工单负责人
20004 工单已经被其他维修人员处理
```

维修人员：

```
21001 维修人员不存在
21002 维修人员已停用
```

转派：

```
22001 当前存在待审批转派申请
```

不要在阶段 0 一次性预定义全部业务错误码。

## 5.4 错误码原则

错误码必须满足：

```
一个含义对应一个错误码
```

不要：

```
10001 = 所有业务失败
```

也不要把错误码细到：

```
用户名为空一个码
手机号为空一个码
手机号格式错误一个码
```

参数字段级错误统一：

```
PARAM_ERROR
```

具体字段原因写入 message。

## 5.5 验收项

- 成功码统一；
- 参数错误码存在；
- 未登录错误码存在；
- 无权限错误码存在；
- 数据不存在错误码存在；
- 状态冲突错误码存在；
- 系统异常错误码存在。

### 估时

```
0.25 人日
```

# 六、任务 0.4：BusinessException

## 6.1 目标

所有可预期业务异常统一通过：

```
BusinessException
```

抛出。

例如：

```
if (order == null) {
    throw new BusinessException(
        ResultCodeEnum.DATA_NOT_FOUND
    );
}
```

也允许覆盖 message：

```
throw new BusinessException(
    ResultCodeEnum.OPERATION_NOT_ALLOWED,
    "当前工单不是待接单状态"
);
```

## 6.2 建议字段

```
public class BusinessException extends RuntimeException {

    private final Integer code;

}
```

即可。

不要额外引入复杂异常继承树：

```
OrderException
WorkerException
TransferException
LeaveException
```

业务差异用错误码区分。

## 6.3 验收项

- 支持错误码枚举构造；
- 支持自定义 message；
- 能被全局异常处理器正确捕获。

### 估时

```
0.25 人日
```

# 七、任务 0.5：统一异常处理

## 7.1 目标

Controller 不写大量：

```
try {
    ...
} catch (...) {
    ...
}
```

由：

```
@RestControllerAdvice
```

统一处理。

建议建立：

```
GlobalExceptionHandler
```

## 7.2 第一阶段至少处理

### 1. BusinessException

返回业务错误码：

```
{
  "code": 20001,
  "message": "工单不存在",
  "data": null
}
```

### 2. 参数校验异常

包括：

```
MethodArgumentNotValidException
ConstraintViolationException
BindException
```

统一返回：

```
PARAM_ERROR
```

message 返回具体字段原因。

例如：

```
{
  "code": 10001,
  "message": "联系电话不能为空",
  "data": null
}
```

### 3. 请求参数类型错误

例如：

```
id=abc
```

但后端要求 Long。

统一：

```
PARAM_ERROR
```

### 4. HTTP 请求方式错误

例如：

```
POST 接口使用 GET
```

统一处理。

### 5. 未知系统异常

最后兜底：

```
@ExceptionHandler(Exception.class)
```

日志记录完整堆栈：

```
log.error("系统异常", e);
```

对前端只返回：

```
系统异常，请稍后重试
```

不能直接把：

```
SQL
Java 堆栈
Redis 连接信息
服务器路径
```

返回给前端。

## 7.3 日志原则

### BusinessException

属于预期业务异常。

通常：

```
WARN
```

或者部分场景不打印堆栈。

### 系统异常

```
ERROR
+
完整 Throwable
```

例如：

```
log.error("Unhandled system exception", e);
```

不能只写：

```
log.error(e.getMessage());
```

否则没有堆栈。

## 7.4 验收项

人工构造以下场景：

```
业务异常
参数为空
参数格式错误
URL 参数类型错误
请求方法错误
数据库异常
普通 RuntimeException
```

所有接口均返回统一 Result。

### 估时

```
0.5 人日
```

# 八、任务 0.6：参数校验基础能力

## 8.1 目标

Controller DTO 使用：

```
Jakarta Validation / Bean Validation
```

统一参数校验。

例如：

```
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

Controller：

```
@PostMapping("/login")
public Result<LoginVO> login(
        @Valid @RequestBody LoginDTO dto) {
    ...
}
```

## 8.2 当前阶段只建立规范

后续业务 DTO 按具体需求增加：

```
@NotNull
@NotBlank
@Size
@Min
@Max
@Pattern
```

阶段 0 不需要自定义大量 Validator。

## 8.3 验收项

验证：

```
body 缺字段
字段为空
长度不合法
@PathVariable 类型错误
@RequestParam 类型错误
```

均能转换成统一参数异常。

### 估时

```
0.25 人日
```

# 九、任务 0.7：PageHelper 分页能力

已经确定：

> 使用 PageHelper。

阶段 0 就将分页口径固定下来。

## 9.1 请求模型

建议统一：

```
public class PageQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
```

业务查询 DTO 继承：

```
public class RepairOrderQueryDTO extends PageQuery {

    private Integer status;

    private Long faultTypeId;

}
```

如果不希望 DTO 继承，也可以直接组合字段。

这个项目规模下继承一个 `PageQuery` 就足够简单。

## 9.2 分页调用

Service：

```
PageHelper.startPage(
    query.getPageNum(),
    query.getPageSize()
);

List<RepairOrderVO> list =
        repairOrderMapper.selectPage(query);

PageInfo<RepairOrderVO> pageInfo =
        new PageInfo<>(list);
```

## 9.3 对外不要直接返回 PageInfo

建议封装：

```
public class PageResult<T> {

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private List<T> records;
}
```

前端统一收到：

```
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 125,
    "pageNum": 1,
    "pageSize": 10,
    "records": []
  }
}
```

这样业务接口不会直接依赖 PageHelper 的返回结构。

## 9.4 分页限制

建议增加：

```
pageNum < 1
→ 1

pageSize < 1
→ 10

pageSize > 100
→ 100
```

避免：

```
pageSize = 100000
```

把数据库一次性拖垮。

## 9.5 注意 PageHelper 使用边界

必须：

```
PageHelper.startPage(...)
```

紧挨着真正需要分页的第一条查询。

不要：

```
PageHelper.startPage();

queryA();

queryB();
```

否则很容易分页到错误 SQL。

后续分页 Service 统一保持：

```
startPage
↓
Mapper分页SQL
↓
PageInfo
↓
PageResult
```

## 9.6 验收项

测试：

```
第一页
中间页
最后一页
超出最大页码
total = 0
pageSize = 100
非法 pageNum
非法 pageSize
带筛选条件分页
```

### 估时

```
0.5 人日
```

# 十、任务 0.8：登录用户模型

## 10.1 目标

为后续业务提供统一的当前登录用户结构。

建议：

```
public class LoginUser {

    private Long userId;

    private String username;

    private String realName;

    private Integer roleType;

}
```

阶段 0 暂时够用。

维修人员对应的：

```
workerId
```

不建议一开始直接塞到 LoginUser。

因为：

```
sys_user.id
```

和：

```
repair_worker.id
```

属于不同业务身份。

真正需要 `workerId` 时可以后续在维修人员上下文中查询或增加专用能力。

其中roleType必要时可以新建一个枚举类。（ADMIN \ WORKER \ STUDENT）

# 十一、任务 0.9：登录用户上下文

## 11.1 目标

业务 Service 不再从 Controller 一层层传：

```
userId
username
roleType
```

而统一获取：

```
UserContext.getCurrentUser()
```

或者：

```
SecurityUtils.getLoginUser()
```

具体名称二选一即可。

建议统一一个：

```
UserContext
```

## 11.2 需要提供的方法

至少：

```
getCurrentUser()

getCurrentUserId()

getCurrentRoleType()

isAdmin()

isWorker()

isStudent()
```

不要建立多个重复工具：

```
SecurityUtils
LoginUtils
UserUtils
CurrentUserUtils
```

一个入口即可。

## 11.3 底层实现

具体取决于后续认证方式。

如果已经使用 Spring Security：

```
SecurityContextHolder
```

如果当前登录机制还没完全实现：

阶段 0 先确定：

```
LoginUser
+
UserContext 接口结构
```

真正 token 解析跟认证阶段完成。

但最终业务 Service 必须只能依赖：

```
UserContext.getCurrentUserId()
```

而不关心 JWT、Session 或 SecurityContext 的具体实现。

## 11.4 未登录处理

如果：

```
当前上下文没有登录用户
```

统一：

```
UNAUTHORIZED
```

不能返回：

```
NullPointerException
```

## 11.5 验收项

登录用户 A：

```
getCurrentUserId() = A
```

登录用户 B：

```
getCurrentUserId() = B
```

退出登录：

```
getCurrentUser()
→ UNAUTHORIZED
```

### 估时

如果认证机制已经存在：

```
0.5 人日
```

如果认证体系尚未开始：

```
阶段0只完成结构
实际认证放阶段1
```

# 十二、任务 0.10：当前操作人获取

## 12.1 目标

后续创建、审批、流转、日志等业务统一通过登录上下文获取操作者。

例如：

```
repair_work_schedule.create_by
repair_leave_request.review_admin_id
repair_transfer_request.review_admin_id
repair_order_flow.operator_id
sys_operation_log.operator_id
```

不能让前端传：

```
{
  "reviewAdminId": 10001
}
```

后端必须自己获取。

## 12.2 统一调用

例如：

```
Long operatorId =
        UserContext.getCurrentUserId();
```

需要角色：

```
Integer roleType =
        UserContext.getCurrentRoleType();
```

## 12.3 后续业务示例

管理员审批：

```
leaveRequest.setReviewAdminId(
    UserContext.getCurrentUserId()
);
```

工单流转：

```
flow.setOperatorId(
    UserContext.getCurrentUserId()
);
```

学生提交报修：

```
order.setStudentUid(
    UserContext.getCurrentUserId()
);
```

这是后续数据范围安全的重要基础：

> 身份字段尽量由后端根据登录上下文生成，而不是信任前端提交。

## 12.4 验收项

确认：

```
学生不能伪造 student_uid
维修人员不能伪造 worker userId
管理员审批人不能由前端指定
流转操作人统一来自当前登录用户
```

### 估时

```
0.25 人日
```

# 十三、任务 0.11：基础枚举规范

阶段 0 只建立枚举使用规范。

至少建立：

```
UserRoleEnum
ResultCodeEnum
```

例如：

```
public enum UserRoleEnum {

    STUDENT(1, "学生"),
    WORKER(2, "维修人员"),
    ADMIN(3, "管理员");

}
```

后续：

```
RepairOrderStatusEnum
TransferApprovalStatusEnum
LeaveStatusEnum
```

跟对应功能模块一起增加。

不要在阶段 0 一次性把全部业务枚举写完。

### 估时

```
0.25 人日
```

# 十四、任务 0.12：基础前端请求规范

后端完成统一 Result 后，前端同步统一 Axios 处理。

## 14.1 Axios 响应处理

统一判断：

```
code = 0
→ 正常返回 data

code != 0
→ 弹出 message
```

例如：

```
ElMessage.error(message)
```

## 14.2 登录失效

遇到：

```
UNAUTHORIZED
```

统一：

```
清除登录状态
→ 跳转登录页
```

业务页面不要各自处理。

## 14.3 分页字段统一

前端统一使用：

```
pageNum
pageSize
```

后端统一返回：

```
total
pageNum
pageSize
records
```

这样后面所有：

```
工单列表
维修人员列表
请假列表
转派列表
异常列表
```

直接复用同一分页组件。

### 估时

```
0.5 人日
```

# 十五、阶段 0 并行边界

可以并行开发：

```
A：
Result
异常码
BusinessException
GlobalExceptionHandler

B：
PageHelper
PageQuery
PageResult

C：
前端 Axios
前端分页公共能力

D：
LoginUser
UserContext 结构
```

依赖关系：

```
Result
 ↓
异常处理

LoginUser
 ↓
UserContext
 ↓
操作人获取

PageHelper
 ↓
PageResult

Result + PageResult
 ↓
前端统一接口解析
```

# 十六、阶段 0 建议实施顺序

建议实际开发顺序：

```
0-01 包结构创建

0-02 Result

0-03 ResultCodeEnum

0-04 BusinessException

0-05 GlobalExceptionHandler

0-06 参数校验异常处理

0-07 PageHelper 配置

0-08 PageQuery / PageResult

0-09 UserRoleEnum

0-10 LoginUser

0-11 UserContext

0-12 当前操作人获取

0-13 Axios 统一 Result 解析

0-14 前端分页统一

0-15 编写测试接口

0-16 全链路验收

0-17 删除测试接口
```

# 十七、阶段 0 预计工作量

| 任务                   | 估时           |
| ---------------------- | -------------- |
| 包结构整理             | 0.25 人日      |
| Result                 | 0.25           |
| 异常码                 | 0.25           |
| BusinessException      | 0.25           |
| GlobalExceptionHandler | 0.5            |
| 参数校验               | 0.25           |
| PageHelper             | 0.5            |
| 登录用户模型           | 0.25           |
| UserContext            | 0.5            |
| 操作人获取             | 0.25           |
| 基础枚举               | 0.25           |
| 前端 Axios / 分页规范  | 0.5            |
| 联调与验收             | 0.5            |
| **合计**               | **4~4.5 人日** |

如果登录认证本身尚未实现，则：

> 阶段 0 只建立 `LoginUser + UserContext` 的结构和调用规范，真正 JWT / Spring Security 登录认证进入阶段 1。

这种情况下阶段 0 可以控制在：

```
3~4 人日
```

左右。

# 十八、阶段 0 验收清单

阶段 0 完成必须全部通过以下验收。

## 工程结构

- 

  后端包结构统一；

- 

  公共类与业务类目录明确；

- 

  不存在重复公共工具类。

## Result

- 

  所有测试接口统一返回 `Result<T>`；

- 

  成功返回格式统一；

- 

  失败返回格式统一。

## 异常处理

- 

  BusinessException 正确转换；

- 

  Bean Validation 错误正确转换；

- 

  参数类型错误正确转换；

- 

  系统异常不会把堆栈返回前端；

- 

  系统异常日志保留完整堆栈。

## PageHelper

- 

  PageHelper 配置完成；

- 

  pageNum/pageSize 正常；

- 

  查询条件 + 分页正常；

- 

  返回统一 `PageResult`；

- 

  最大 pageSize 有限制。

## 用户上下文

- 

  可以统一获取当前用户；

- 

  可以获取 userId；

- 

  可以获取 roleType；

- 

  未登录能够统一抛出异常；

- 

  操作人 ID 不依赖前端传入。

## 前端

- 

  Axios 可以统一解析 Result；

- 

  业务错误统一提示；

- 

  登录失效存在统一处理入口；

- 

  分页参数名称统一；

- 

  分页响应结构统一。

# 十九、阶段 0 明确不做内容

本阶段不开发：

```
故障类型 CRUD
维修人员 CRUD
区域树业务
维修人员技能
负责范围
学生报修
自动派单
工单状态机
转派
请假
超时任务
提醒
评价
返工
```

也暂不提前开发：

```
复杂 RBAC
复杂 DataScope AOP
分布式锁框架
消息队列
统一审计 AOP
复杂缓存框架
DDD 分层
通用工作流引擎
```

这些能力只有业务真正需要时再进入对应阶段。

# 二十、阶段 0 完成定义 Definition of Done

阶段 0 最终完成标准：

> 前后端开发人员可以直接开始写一个新的业务模块，而不再需要考虑接口应该返回什么格式、异常如何返回、分页怎么处理、当前用户从哪里获取、操作人由谁传入等公共问题。

具体表现为一个标准业务接口能够直接按照：

```
Controller
   ↓
DTO 参数校验
   ↓
Service
   ↓
UserContext 获取当前用户
   ↓
Mapper
   ↓
PageHelper（分页场景）
   ↓
VO
   ↓
Result
```

运行。

异常时：

```
Service
   ↓
throw BusinessException
   ↓
GlobalExceptionHandler
   ↓
Result.fail
   ↓
Axios
   ↓
统一错误提示
```

至此，阶段 0 结束，进入：

> **阶段 1：登录认证、角色权限与数据范围建设。**