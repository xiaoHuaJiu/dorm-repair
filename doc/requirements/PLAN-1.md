# PLAN-1：数据库与权限底座建设

# 一、阶段目标

阶段 1 的目标不是开发具体业务功能，而是完成整个宿舍报修与维修工单系统后续开发所依赖的基础数据结构、基础代码结构和访问控制体系。

本阶段只完成以下五部分：

```
1. 数据库完整建表
2. Entity / Mapper / Enum 等基础代码
3. 登录认证
4. 角色权限
5. 数据范围
```

阶段 1 完成后，系统应具备：

```
数据库结构完整
+
Java 基础数据访问层可用
+
用户可以正常登录
+
系统能够识别当前用户角色
+
接口可以按角色进行权限控制
+
后端已经具备统一的数据访问边界规范
```

本阶段不开发具体业务 CRUD。

# 二、阶段 1 与阶段 0 的边界

阶段 0 已完成或计划完成：

```
前后端基础工程
Docker 基础环境
MySQL / Redis / MinIO / Nginx
统一响应规范
错误码规范
统一异常处理
Bean Validation
PageHelper 基础配置
LoginUser / UserContext 基础结构
当前操作人获取规范
Axios 基础响应处理
```

阶段 1 在阶段 0 基础上继续完成：

```
数据库正式落地
↓
Java 数据层基础代码
↓
真实登录认证
↓
角色识别
↓
接口角色权限
↓
数据访问范围
```

# 三、阶段 1 明确不做的内容

以下内容全部移动至阶段 2 或后续阶段：

```
位置 CRUD
故障类型 CRUD
维修人员 CRUD
维修技能配置 CRUD
维修人员负责区域 CRUD
工作时间方案 CRUD
分页业务接口
条件查询接口
管理端配置页面
学生报修
重复报修识别
自动派单
接单
维修过程
材料记录
转派
请假
返工
评价
超时提醒
管理员异常中心
```

阶段 1 不追求“页面能管理数据”。

阶段 1 的完成标准是：

> 后续业务模块已经拥有稳定的数据库、基础数据层和权限底座，可以直接开始编写 CRUD 和业务功能。

# 四、阶段 1 总体依赖关系

开发顺序：

```
最终确认数据库结构
        ↓
完整执行 DDL
        ↓
检查索引 / 唯一约束
        ↓
建立 Entity
        ↓
建立 Mapper / Mapper XML
        ↓
建立基础 Enum
        ↓
登录认证正式接入
        ↓
角色权限
        ↓
数据范围规范
        ↓
权限与数据范围测试
        ↓
阶段 1 验收
```

其中：

```
Entity
Mapper
Enum
```

可以在数据库正式落地后并行开发。

登录认证也可以与 Entity / Mapper 基础代码并行推进。

# 五、任务 1.1：数据库结构最终确认

## 5.1 目标

在开始 Java 业务代码前，将当前已经确定的数据库设计进行最后一次确认。

重点确认：

```
表是否完整
字段是否完整
字段类型是否合理
状态字段是否统一
逻辑删除字段是否统一
唯一索引是否正确
普通索引是否覆盖实际业务查询
表之间关联关系是否明确
```

本阶段数据库结构确认后，原则上：

> 后续 CRUD 和业务功能不再因为基础表设计缺失而频繁调整数据库结构。

# 六、任务 1.2：数据库完整建表

根据最终 DDL 一次性建立系统当前已经确定的全部表。

## 6.1 用户与维修人员

```
sys_user
repair_worker
```

职责：

```
sys_user
→ 用户身份、账号、角色、账号状态

repair_worker
→ 维修人员业务身份、维修人员编号、工作状态
```

## 6.2 故障类型与维修技能

```
repair_fault_type
repair_worker_fault_type
```

用于后续表示：

```
系统有哪些故障类型
+
维修人员能够处理哪些故障类型
```

## 6.3 位置体系

根据最终确定的位置模型建立repair_area：

```
单位
 └─ 区域
     └─ 楼栋
```

同时建立：

```
repair_worker_area_scope
```

用于表示维修人员负责范围。

## 6.4 工作时间

```
repair_work_schedule
```

后续用于：

```
维修人员工作时间判断
接单截止时间计算
```

## 6.5 工单主业务

```
repair_order
```

作为整个系统核心表。

负责保存：

```
当前工单状态
当前负责人
当前关键时间节点
异常标记
重复报修关系
返工次数
```

工单主表只保存当前态，历史变化由流转表记录。

## 6.6 维修过程

```
repair_process_record
repair_material_usage
```

分别用于：

```
维修过程记录
维修材料使用记录
```

## 6.7 转派与请假

```
repair_transfer_request
repair_leave_request
```

## 6.8 返工与评价

```
repair_rework_record
repair_evaluation
```

## 6.9 工单流转与提醒

```
repair_order_flow
repair_reminder_record
```

## 6.10 操作日志

```
sys_operation_log
```

# 七、数据库统一规范

数据库统一：

```
MySQL 8.x
InnoDB
utf8mb4
```

第一阶段：

```
不建立数据库 FK
```

业务关联完整性由 Service 层保证。

# 八、逻辑删除规范

数据库建表时统一检查逻辑删除使用方式。

## 8.1 历史业务数据

例如：

```
repair_order
repair_leave_request
repair_transfer_request
repair_rework_record
repair_order_flow
repair_evaluation
```

原则：

> 正常业务流程中不进行真实物理删除。

历史数据必须可追溯。

## 8.2 配置关系

例如：

```
repair_worker_fault_type
```

不使用：

```
UNIQUE(worker_id, fault_type_id, deleted)
```

而采用：

```
UNIQUE(worker_id, fault_type_id)
```

配置取消时：

```
status = 0
```

重新启用：

```
status = 1
```

避免出现逻辑删除参与唯一索引后产生历史记录冲突的问题。

# 九、数据库核心索引检查

本阶段执行 DDL 后必须检查核心索引。

## 工单

```
idx_order_duplicate_check
idx_order_assignee_status
idx_order_status_report
idx_order_accept_timeout
idx_order_complete_timeout
idx_order_exception
idx_order_duplicate_relation
```

## 维修人员技能

```
uk_worker_fault
idx_fault_worker
```

## 维修人员负责范围

```
idx_scope_worker
idx_scope_location
```

## 工作时间

```
idx_schedule_effective
```

## 请假

```
idx_leave_worker_time
idx_leave_pending_review
idx_leave_start_task
idx_leave_end_task
```

## 转派

按最终 DDL 检查：

```
维修人员转派查询索引
管理员待审批索引
工单转派查询索引
```

## 维修过程

```
idx_process_order_time
idx_process_worker_time
idx_process_order_type
```

## 材料

```
idx_material_order_time
idx_material_worker_time
```

## 返工

```
uk_rework_order_no
idx_rework_order_time
idx_rework_admin
```

## 工单流转

```
idx_flow_order_time
```

## 评价

```
uk_evaluation_order
```

## 提醒

```
uk_reminder_deduplicate
```

# 十、数据库建表验收

至少执行：

```
SHOW TABLES;
```

确认所有正式表存在。

逐表执行：

```
SHOW CREATE TABLE table_name;
```

检查：

```
字段
字段类型
NULL / NOT NULL
默认值
主键
唯一索引
普通索引
字符集
存储引擎
```

同时验证：

```
MySQL 容器重启
↓
数据和表结构仍然存在
```

### 估时

```
0.5 ~ 1 人日
```

# 十一、任务 1.3：Entity 基础代码

数据库正式完成后，按照 DDL 建立 Entity。

至少包括：

```
SysUser
RepairWorker
RepairFaultType
RepairWorkerFaultType
RepairWorkerAreaScope
RepairWorkSchedule
RepairOrder
RepairLeaveRequest
RepairTransferRequest
RepairProcessRecord
RepairMaterialUsage
RepairReworkRecord
RepairOrderFlow
RepairEvaluation
RepairReminderRecord
SysOperationLog
```

以及位置体系对应 Entity。

# 十二、Entity 设计规范

Entity：

> 只负责数据库表字段映射。

不直接承担：

```
Controller 请求参数
Controller 返回数据
复杂业务逻辑
```

后续业务接口仍使用：

```
DTO
VO
```

## Java 类型统一

统一使用：

```
BIGINT
→ Long

TINYINT / INT
→ Integer

VARCHAR / TEXT
→ String

DATE
→ LocalDate

TIME
→ LocalTime

DATETIME
→ LocalDateTime

DECIMAL
→ BigDecimal
```

不混用：

```
java.util.Date
Timestamp
LocalDateTime
```

# 十三、Entity 与数据库一致性验收

检查：

```
表字段数量
字段名称
Java 属性
类型
逻辑删除字段
时间字段
主键
```

Entity 不允许出现数据库不存在的临时业务字段。

如果需要组合展示字段：

```
workerName
faultTypeName
areaName
```

后续放 VO，不放基础 Entity。

### 估时

```
0.5 人日
```

# 十四、任务 1.4：Mapper 基础代码

本阶段建立所有表对应的 Mapper。

例如：

```
SysUserMapper
RepairWorkerMapper
RepairFaultTypeMapper
RepairWorkerFaultTypeMapper
RepairWorkerAreaScopeMapper
RepairWorkScheduleMapper
RepairOrderMapper
...
```

# 十五、Mapper 当前阶段职责

阶段 1 只建立：

```
Mapper 接口
Mapper XML
基础映射
必要的 selectById
必要的基础查询
```

不提前开发：

```
自动派单复杂查询
重复报修查询
超时扫描
复杂统计
多表分页
异常工单查询
```

这些 SQL 在对应功能阶段开发。

# 十六、Mapper XML 规范

统一：

```
XxxMapper.java
XxxMapper.xml
```

名称对应。

XML 统一：

```
namespace
resultMap
基础列
SQL
```

复杂多表 SQL 后续统一放 XML。

不在阶段 1 提前建设通用动态 SQL 框架。

# 十七、Mapper 基础验收

每个核心 Mapper 至少完成一次基础数据库访问测试：

```
selectById
```

确认：

```
Spring
↓
Mapper
↓
MyBatis
↓
MySQL
```

链路正常。

### 估时

```
0.5 ~ 1 人日
```

# 十八、任务 1.5：Enum 基础建设

阶段 1 建立数据库中已经明确确定的状态枚举。

原则：

> 只建立已经确定且会被后续代码共同使用的枚举，不提前把所有业务逻辑都枚举化。

# 十九、基础角色枚举

```
UserRoleEnum
```

例如：

```
STUDENT = 1
WORKER = 2
ADMIN = 3
```

# 二十、维修人员状态

```
WorkerWorkStatusEnum
```

例如：

```
NORMAL = 0
ON_LEAVE = 1
DISABLED = 2
```

# 二十一、工单状态

```
RepairOrderStatusEnum
```

当前确定：

```
0 待派单
1 待接单
2 维修中
3 待确认
4 返工中
5 已中断
6 已完成
7 已取消
```

阶段 1 只建立枚举，不实现完整状态机。

# 二十二、其他确定状态枚举

可根据数据库 DDL 建立：

```
LeaveRequestStatusEnum
TransferApprovalStatusEnum
TransferExecuteStatusEnum
ProcessRecordTypeEnum
ReworkStatusEnum
ReminderTypeEnum
ReminderLevelEnum
```

如果某些状态仍可能调整，则暂缓创建。

# 二十三、Enum 规范

所有枚举统一至少提供：

```
code
description
```

例如：

```
public enum UserRoleEnum {

    STUDENT(1, "学生"),
    WORKER(2, "维修人员"),
    ADMIN(3, "管理员");

}
```

业务代码禁止散落：

```
if (roleType == 3)
```

应统一：

```
UserRoleEnum.ADMIN
```

### 估时

```
0.25 ~ 0.5 人日
```

# 二十四、任务 1.6：登录认证正式实现

阶段 0 已建立：

```
LoginUser
UserContext
```

阶段 1 正式让它们接入真实认证体系。

目标：

```
用户名 / 密码
↓
认证
↓
查询 sys_user
↓
校验账号状态
↓
生成登录凭证
↓
请求携带凭证
↓
认证过滤器
↓
LoginUser
↓
SecurityContext
↓
UserContext
```

# 二十五、登录数据来源

登录用户以：

```
sys_user
```

为统一身份来源。

至少读取：

```
id
username
password
real_name
role_type
status
```

# 二十六、登录校验

至少校验：

```
用户是否存在
密码是否正确
账号是否正常
账号是否已逻辑删除
```

# 二十七、登录成功后 LoginUser

至少包含：

```
userId
username
realName
roleType
```

业务层通过：

```
UserContext.getCurrentUser()
```

获取。

# 二十八、未登录处理

未登录或认证信息失效：

```
HTTP 401
```

例如：

```
AUTH-A-00001
```

userTip：

```
登录状态已失效，请重新登录。
```

# 二十九、账号停用

账号：

```
status = 0
```

时禁止登录。

错误信息按照阶段 0 错误码规范制定。

# 三十、登录认证验收

至少建立：

```
管理员账号
维修人员账号
学生账号
停用账号
```

验证：

```
正确账号密码 → 登录成功

错误密码 → 登录失败

不存在账号 → 登录失败

停用账号 → 登录失败

无认证访问受保护接口 → 401

合法认证访问接口 → UserContext 正确
```

### 估时

```
1 ~ 1.5 人日
```

# 三十一、任务 1.7：角色权限

角色权限解决：

> 当前用户是否有资格调用某一个接口。

当前系统三个角色：

```
ADMIN
WORKER
STUDENT
```

# 三十二、权限实现原则

优先使用：

```
Spring Security
+
@PreAuthorize
```

或项目当前统一的 Security 权限机制。

例如：

```
@PreAuthorize("hasRole('ADMIN')")
```

# 三十三、阶段 1 权限测试接口

由于阶段 1 尚未开发完整业务 CRUD，可以建立临时测试接口：

```
/api/test/admin
/api/test/worker
/api/test/student
```

分别要求：

```
ADMIN
WORKER
STUDENT
```

用于验证角色权限。

阶段验收完成后删除测试接口。

# 三十四、管理员角色

管理员以后拥有：

```
基础配置管理
全部工单查询
异常工单处理
审批
人工派单
```

阶段 1 先建立：

```
ADMIN 权限识别
```

即可。

# 三十五、维修人员角色

维修人员以后拥有：

```
自己的工单
维修过程
材料
转派
请假
```

阶段 1 只保证：

```
WORKER 角色能够被正确识别和限制
```

# 三十六、学生角色

学生以后拥有：

```
报修
自己的工单
确认
返工
评价
```

阶段 1 只保证：

```
STUDENT 角色能够被正确识别和限制
```

# 三十七、角色权限验收矩阵

| 当前用户 | ADMIN接口 | WORKER接口 | STUDENT接口 |
| -------- | --------- | ---------- | ----------- |
| 管理员   | 允许      | 按设计决定 | 按设计决定  |
| 维修人员 | 拒绝      | 允许       | 拒绝        |
| 学生     | 拒绝      | 拒绝       | 允许        |
| 未登录   | 401       | 401        | 401         |

无权限：

```
HTTP 403
AUTH-A-xxxxx
```

### 估时

```
0.5 人日
```

# 三十八、任务 1.8：数据范围体系

角色权限解决：

```
能不能调用接口
```

数据范围解决：

```
能够访问哪些具体数据
```

两者必须明确区分。

# 三十九、当前数据范围规则

后续业务统一按照以下规则开发。

## 管理员

```
全部业务数据
```

默认不增加身份过滤。

## 维修人员

主要数据范围：

```
本人相关数据
+
当前由本人负责的数据
```

典型字段：

```
repair_order.current_assignee_id
repair_leave_request.worker_id
repair_transfer_request.applicant_worker_id
repair_process_record.worker_id
repair_material_usage.worker_id
```

## 学生

主要数据范围：

```
本人提交的数据
```

典型字段：

```
repair_order.student_uid
repair_evaluation.student_uid
repair_rework_record.applicant_uid
```

# 四十、本阶段不采用 SQL DataScope AOP

阶段 1 明确：

> 暂不实现通过切面统一修改 SQL 的 DataScope 框架。

即暂不开发：

```
@DataScope
↓
AOP
↓
解析角色
↓
修改 Mapper SQL
```

# 四十一、不使用通用 DataScope AOP 的原因

## 原因 1：不同表归属字段不同

例如：

```
工单
→ student_uid
→ current_assignee_id

请假
→ worker_id

转派
→ applicant_worker_id

评价
→ student_uid
```

不存在一个统一字段可以简单追加。

## 原因 2：复杂 SQL 风险

后续存在：

```
JOIN
GROUP BY
ORDER BY
子查询
统计
PageHelper COUNT
```

运行时再自动修改 SQL 会增加排查难度。

## 原因 3：数据范围不等于业务权限

例如：

```
维修人员是不是当前负责人
学生是不是报修人
工单是不是待确认
转派申请是不是待审批
```

最终都需要 Service 层判断。

SQL AOP 无法代替这些业务校验。

# 四十二、阶段 1 数据范围采用三层规范

后续所有业务统一使用：

```
第一层：Controller
控制角色能否访问接口

第二层：Service / Mapper
控制列表能够查询哪些数据

第三层：Service
控制单条资源归属
```

# 四十三、第一层：Controller 角色权限

例如：

```
管理员接口
→ ADMIN

维修接口
→ WORKER

学生接口
→ STUDENT
```

由：

```
@PreAuthorize
```

控制。

# 四十四、第二层：列表数据范围

后续例如工单列表：

管理员：

```
无身份过滤
```

维修人员：

```
current_assignee_id = 当前维修人员ID
```

学生：

```
student_uid = 当前用户ID
```

这些条件：

> 必须由 Service 根据当前登录用户写入查询条件。

# 四十五、禁止信任前端身份参数

以下字段后续都不能直接相信前端：

```
studentUid
workerId
operatorId
reviewAdminId
applicantUid
```

例如当前登录学生：

```
userId = 10001
```

即使前端发送：

```
studentUid = 10002
```

服务端仍然必须使用：

```
10001
```

# 四十六、身份信息统一来源

统一：

```
UserContext
```

例如：

```
Long currentUserId =
    UserContext.getCurrentUserId();
```

后续当前维修人员业务 ID：

```
sys_user.id
↓
repair_worker.user_id
↓
repair_worker.id
```

通过统一 Service 查询。

不允许前端自己提交当前 `workerId` 作为身份依据。

# 四十七、第三层：单条资源归属校验

即使列表已经增加数据范围，详情、修改、操作接口仍然必须单独校验。

例如：

```
GET /orders/{id}
```

不能：

```
selectById
→ 直接返回
```

必须：

```
查询资源
↓
判断是否存在
↓
判断当前用户是否拥有访问权限
↓
允许 / 403
```

# 四十八、阶段 1 暂不实现具体业务 DataScope

因为当前没有开始：

```
工单 CRUD
请假 CRUD
转派 CRUD
```

所以阶段 1 不需要把每张业务表的数据范围 SQL 全部提前写出来。

阶段 1 要完成的是：

```
规则确定
+
UserContext 可用
+
角色信息可用
+
Service 权限校验模式可验证
```

# 四十九、数据范围验证方案

可以建立一套简单临时测试数据：

```
资源A → 属于学生A
资源B → 属于学生B
```

模拟：

```
学生A访问资源A
→ 允许

学生A访问资源B
→ 403

管理员访问资源A/B
→ 允许
```

维修人员同理。

也可以直接使用测试表或测试接口验证模式。

验收完成后删除临时代码。

# 五十、数据范围开发规范

以后所有业务开发统一遵循：

```
列表：
Mapper 显式数据范围条件

详情：
Service 资源归属检查

修改：
Service 资源归属 + 状态检查

删除：
Service 资源归属 + 是否允许删除

身份字段：
UserContext 获取
```

# 五十一、阶段 1 错误码补充

阶段 1 至少补登录和权限相关错误码。

例如：

```
AUTH-A-00001
HTTP 401
未登录 / 登录失效
AUTH-A-00002
HTTP 403
无访问权限
USER-B-00001
HTTP 404
用户不存在
USER-A-00001
HTTP 403 / 409
账号不可用
```

具体编号按照项目错误码表最终登记。

# 五十二、阶段 1 并行边界

数据库 DDL 完成以后可以并行：

## 工作包 A

```
Entity
Mapper
Mapper XML
Enum
```

## 工作包 B

```
登录认证
Security
LoginUser
UserContext 接入
```

工作包 A 和 B 基本可以并行。

角色权限依赖：

```
登录认证
```

数据范围规范依赖：

```
UserContext
+
角色识别
```

# 五十三、阶段 1 推荐执行顺序

```
1-01 最终核对数据库 DDL

1-02 执行全部建表 SQL

1-03 检查表结构

1-04 检查索引 / 唯一约束

1-05 建立 Entity

1-06 建立 Mapper

1-07 建立 Mapper XML

1-08 建立基础 Enum

1-09 完成 sys_user 登录查询

1-10 登录认证实现

1-11 认证过滤链路接入

1-12 LoginUser / UserContext 正式接入

1-13 三角色识别

1-14 Controller 角色权限

1-15 数据范围规则实现规范

1-16 资源归属校验模式验证

1-17 越权测试

1-18 清理临时测试接口

1-19 阶段 1 验收
```

# 五十四、阶段 1 验收清单

## 数据库

- 

  所有正式表已建立；

- 

  表数量与最终设计一致；

- 

  字段与 DDL 一致；

- 

  主键正确；

- 

  唯一索引正确；

- 

  查询索引正确；

- 

  字符集正确；

- 

  InnoDB 正确；

- 

  未建立不需要的 FK；

- 

  MySQL 重启后结构正常。

## Entity

- 

  所有正式表都有对应 Entity；

- 

  Java 类型正确；

- 

  时间类型统一；

- 

  Entity 不混入 VO 字段；

- 

  Entity 与数据库结构一致。

## Mapper

- 

  所有核心表已有 Mapper；

- 

  Mapper XML 命名正确；

- 

  namespace 正确；

- 

  基础查询可运行；

- 

  MyBatis → MySQL 链路正常。

## Enum

- 

  用户角色枚举完成；

- 

  维修人员状态枚举完成；

- 

  工单状态枚举完成；

- 

  已确定状态字段不再散落 Magic Number；

- 

  未确定业务不提前过度枚举。

## 登录认证

- 

  管理员可登录；

- 

  维修人员可登录；

- 

  学生可登录；

- 

  密码错误不能登录；

- 

  不存在用户不能登录；

- 

  停用账号不能登录；

- 

  未登录访问保护接口返回 401；

- 

  登录后 UserContext 能取得正确用户。

## 角色权限

- 

  ADMIN 正确识别；

- 

  WORKER 正确识别；

- 

  STUDENT 正确识别；

- 

  无权限接口返回 403；

- 

  后端权限独立于前端页面控制。

## 数据范围

- 

  管理员定义为全量数据；

- 

  维修人员定义为本人/本人负责数据；

- 

  学生定义为本人数据；

- 

  身份字段以后统一来源于 UserContext；

- 

  前端不能决定 studentUid；

- 

  前端不能决定当前 workerId；

- 

  前端不能决定 operatorId；

- 

  列表数据范围规范已确定；

- 

  单条资源归属校验规范已确定；

- 

  越权访问测试通过；

- 

  当前阶段未引入 SQL DataScope AOP。

# 五十五、阶段 1 估时

| 工作项             | 估时            |
| ------------------ | --------------- |
| 最终 DDL 核对      | 0.5             |
| 完整建表与索引检查 | 0.5~1           |
| Entity             | 0.5             |
| Mapper / XML       | 0.5~1           |
| Enum               | 0.25~0.5        |
| 登录认证           | 1~1.5           |
| 角色权限           | 0.5             |
| 数据范围规范与验证 | 0.5~1           |
| 联调 / 越权测试    | 0.5~1           |
| **总计**           | **4.75~7 人日** |

如果数据库 DDL 已基本完全锁定，且 Spring Security 基础依赖已经存在，整体可以更接近：

```
5 个工作日左右
```

# 五十六、阶段 1 Definition of Done

阶段 1 完成后，系统虽然还没有正式的基础配置 CRUD 页面，但底层应已经能够回答：

```
数据库有哪些正式业务表？

每张表对应哪个 Entity？

每张表通过哪个 Mapper 访问？

核心状态使用哪些 Enum？

当前登录用户是谁？

当前用户是什么角色？

这个角色能否访问某个接口？

这个用户以后应该能够访问哪些数据？

身份字段应该从哪里获取？

越权访问如何被拦截？
```

并且后续阶段可以直接基于：

```
数据库
+
Entity
+
Mapper
+
Enum
+
UserContext
+
角色权限
+
数据范围规则
```

继续开发。

阶段 1 完成后进入：

> **阶段 2：基础配置 CRUD、分页查询与条件查询。**

阶段 2 再正式开发：

```
位置管理
故障类型管理
维修人员管理
维修技能配置
维修人员负责区域配置
工作时间方案管理
分页
条件查询
前端配置页面
```

# 五十七、阶段 1 核心原则

```
阶段 1
只搭底座
不做业务 CRUD
数据库先落地
再生成数据层代码
角色权限
解决“能不能调用”
数据范围
解决“能访问哪些数据”
Controller
控制角色
Service
控制资源归属
Mapper
显式控制列表范围
身份数据
统一来自 UserContext
当前阶段
不使用通用 SQL DataScope AOP
```

最终目标：

> **阶段 1 只把数据库、数据层、认证、角色和数据边界做稳定；所有基础配置 CRUD 和查询功能从阶段 2 开始。**