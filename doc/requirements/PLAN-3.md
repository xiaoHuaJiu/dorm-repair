# PLAN-3：工单通用查询骨架与详情聚合

> 范围更新：位置树采用四级结构，依次为单位/校区、区域、楼栋、房间；房间节点挂在楼栋节点下。工单 `room_id` 指向 `repair_area.area_type = 4` 的房间节点。

# 一、阶段目标

阶段 3 在正式开发：

```
学生报修
重复报修检测
工单创建
自动派单
```

之前，先完成 `repair_order` 的通用查询底座。

原因：

> 工单是整个系统的核心业务对象，后续学生端、维修人员端、管理员端都需要围绕工单进行列表查询和详情查看。

本阶段首先建立：

```
1. 工单通用分页查询骨架
2. 三角色数据范围
3. 管理员工单复杂筛选
4. 学生工单查询
5. 维修人员工单查询
6. 工单详情统一聚合接口
7. 工单详情关联数据查询骨架
```

阶段完成后，后续新业务只需要：

```
写入 repair_order
+
写入相关业务表
```

现有工单列表和详情即可直接展示最新数据。

# 二、为什么先做工单查询骨架

后续功能都会依赖工单查询：

```
学生报修
→ 创建工单
→ 学生工单列表立即可查

自动派单
→ 更新 current_assignee_id
→ 维修人员列表立即可查

维修人员接单
→ 更新 status
→ 三端状态立即同步

维修过程
→ 写 repair_process_record
→ 工单详情立即展示

材料使用
→ 写 repair_material_usage
→ 工单详情立即展示

返工
→ 写 repair_rework_record
→ 工单详情立即展示

评价
→ 写 repair_evaluation
→ 工单详情立即展示

状态流转
→ 写 repair_order_flow
→ 工单详情立即展示
```

因此先把：

```
查询
+
数据范围
+
详情聚合
```

做稳定，可以避免后续每个业务阶段重复修改接口。

# 三、阶段 3 查询架构原则

统一采用：

```
Controller
↓
Service
↓
根据角色注入数据范围
↓
Mapper
↓
repair_order 主查询
↓
VO
```

列表查询：

> 以 `repair_order` 为主表。

详情查询：

> 先查 `repair_order`，再批量查询相关业务表，Service 聚合为详情 VO。

禁止使用：

```
一条超大 SQL
JOIN 所有过程 / 材料 / 返工 / 评价 / 流转
```

否则一对多关系会形成笛卡尔积，分页和详情都难维护。

# 四、任务 3.1：统一工单查询对象

建立：

```
RepairOrderQueryDTO
```

用于接收前端合法筛选条件。

以及：

```
RepairOrderQueryCondition
```

作为 Service → Mapper 内部查询条件。

## DTO 和内部 Condition

前端可以控制：

```
工单编号
状态
故障类型
位置
时间范围
异常条件
```

但不能控制：

```
studentUid
currentAssigneeId
当前角色
```

因此：

```
RepairOrderQueryDTO
```

只保存公开查询条件。

Service 根据：

```
UserContext
```

生成：

```
RepairOrderQueryCondition
```

并追加数据范围。

这样从结构上避免：

> 前端伪造 studentUid 或 workerId 越权查询。

## RepairOrderQueryDTO

管理员查询条件建议支持：

```
orderNo
status
faultTypeId

campusId
areaId
buildingId
roomId

workerId

reportStartTime
reportEndTime

exceptionFlag
suspectedDuplicate

acceptTimeout
completeTimeout
```

同时继承或包含：

```
pageNum
pageSize
```

## 管理员筛选条件

### 工单编号

支持：

```
orderNo
```

建议：

```
精确查询
或
前缀 / 模糊查询
```

第一期如果工单号格式统一，可以支持模糊搜索：

```
order_no LIKE CONCAT('%', #{orderNo}, '%')
```

### 工单状态

增加：

```
statusList
```

前端可以做多状态筛选。

### 故障类型

```
faultTypeId
```

对应：

```
repair_order.fault_type_id
```

### 位置筛选

管理员支持：

```
campusId
areaId
buildingId
roomId
```

按照前端级联选择结果组合。

例如：

只选择校区：

```
campus_id = ?
```

选择到区域：

```
campus_id = ?
AND area_id = ?
```

选择楼栋：

```
building_id = ?
```

选择房间：

```
room_id = ?
```

### 维修人员筛选

支持：

```
workerId
```

对应：

```
current_assignee_id = #{workerId}
```

管理员可按当前负责人查询。

### 报修时间

支持：

```
reportStartTime
reportEndTime
```

查询：

```
report_time >= #{reportStartTime}
AND report_time <= #{reportEndTime}
```

如果数据库字段最终为：

```
create_time
```

则统一按最终 DDL 字段处理。

前端建议：

```
开始时间
结束时间
```

成对提交。

### 异常工单

支持：

```
exceptionFlag
```

例如：

```
1 = 只看异常工单
0 / null = 不限制
```

对应：

```
exception_flag = 1
```

异常具体原因不要在列表查询层重新计算。

应以：

```
repair_order.exception_flag
```

当前态为准。

### 疑似重复工单

管理员筛选：

```
suspectedDuplicate
```

根据最终工单表字段设计：

```
duplicate_flag
```

具体以最终 DDL 为准。

**注意**：查询层只使用数据库已经记录的重复判断结果，不重新运行重复检测算法。

### 超时筛选

管理员需要查询接单超时、处理超时。

建议 QueryDTO：

```
acceptTimeout
completeTimeout
```

#### 超时查询原则

“超时”不是独立工单状态。

工单状态仍然可能是：待接单 或 维修中

超时属于：状态 + 截止时间 的衍生属性。

因此不要为了查询增加：

```
status = 接单超时
status = 维修超时
```

#### 接单超时判断

例如：

```
status = 待接单
AND accept_deadline < 当前时间
```

管理员选择：

```
acceptTimeout = true
```

时追加该条件。

#### 处理超时判断

例如：

```
status IN (维修中, 返工中等需要处理的状态)
AND complete_deadline < 当前时间
```

具体有效状态以后根据最终状态机确定。

#### 列表中的超时展示

列表 VO 直接返回：

```
acceptTimeout
completeTimeout
```

由后端根据：

```
status
deadline
当前时间
```

计算。

前端只负责展示：

```
接单超时
处理超时
```

标签。

避免前端自己计算业务状态。

# 五、任务 3.2：角色数据范围

工单查询必须严格遵守阶段 1 数据范围规范。

## 管理员数据范围

管理员：

```
不增加用户归属条件
```

即：

```
全部 repair_order
```

再叠加管理员主动传入的筛选条件。

## 维修人员数据范围

维修人员：

```
current_assignee_id = 当前维修人员ID
```

注意：

当前登录用户得到的是：

```
sys_user.id
```

而工单保存的是：

```
repair_worker.id
```

因此先：

```
sys_user.id
↓
查询 repair_worker
↓
repair_worker.id
```

再作为：

```
current_assignee_id
```

查询条件。

### 维修人员不能通过参数扩大范围

例如当前维修人员：

```
workerId = 1001
```

即使请求：

```
?workerId=1002
```

后端也不能使用。

维修人员查询必须强制：

```
current_assignee_id = 1001
```

## 学生数据范围

学生：

```
student_uid = 当前用户ID
```

直接使用：

```
UserContext.getCurrentUserId()
```

### 学生不能通过参数扩大范围

同样，学生接口不接收：

```
studentUid
```

如果内部 QueryCondition 有：

```
studentUid
```

也必须由 Service 设置。

## 角色查询流程

统一：

```
RepairOrderQueryDTO
↓
Service
↓
LoginUser
↓
判断 roleType
↓
转换 RepairOrderQueryCondition
↓
追加数据范围
↓
Mapper
```

例如：

```
ADMIN
→ 不追加归属条件

WORKER
→ currentAssigneeId = currentWorkerId

STUDENT
→ studentUid = currentUserId
```

# 六、任务 3.3：工单分页列表

统一业务 Service：

```
pageOrders()
```

如果管理员、维修人员、学生的展示字段差异很大，可以 Controller 分入口：

```
/admin/orders
/worker/orders
/student/orders
```

但是底层查询 Service 和 Mapper 尽量复用。

推荐：

```
三个角色接口
+
一个共享 RepairOrderQueryService
```

这样前端 API 语义清楚，底层又不重复 SQL。

## 管理员工单列表

管理员列表建议返回：

```
orderId
orderNo
status
statusName

studentUid
studentName

faultTypeId
faultTypeName

campusName
areaName
buildingName
roomName

currentAssigneeId
workerName

reportTime

acceptDeadline
completeDeadline

exceptionFlag
duplicateFlag

acceptTimeout
completeTimeout
```



## 维修人员工单列表

维修人员只查：

```
current_assignee_id = 当前维修人员
```

返回：

```
orderId
orderNo
status
faultTypeName
locationText
studentName
reportTime
acceptDeadline
completeDeadline
acceptTimeout
completeTimeout
exceptionFlag
```

## 学生工单列表

学生只查：

```
student_uid = 当前用户
```

建议返回：

```
orderId
orderNo
status
faultTypeName
locationText
reportTime
workerName
completeTime
exceptionFlag
```

学生端不需要看到内部：

```
exceptionReason
调度错误
管理员内部备注
```

等后台字段。

##  SQL 设计

以`repair_order ro`为主表。

可以 LEFT JOIN：

```
repair_fault_type
repair_worker
sys_user
位置基础表
```

获取一对一展示字段。

允许：一条工单 → 一行 的 JOIN。

### 列表禁止 JOIN 一对多表

禁止列表直接 JOIN：

```
repair_process_record
repair_material_usage
repair_rework_record
repair_order_flow
```

因为：

```
1工单
×
N过程
×
M材料
×
K流转
```

会导致：

```
重复行
COUNT 错误
PageHelper total 错误
分页结果错误
```

**这些表只在详情聚合接口查询。**

### 列表排序

管理员默认：

```
report_time DESC
id DESC
```

如果没有明确 `report_time`，使用：

```
create_time DESC
id DESC
```

保证排序稳定。

## PageHelper

流程固定：

```
PageHelper.startPage
↓
selectOrderPage
↓
PageInfo
↓
PageResult
```

PageHelper 调用后不要插入其他查询。

# 七、任务 3.4：管理员筛选查询

管理员列表重点完成以下组合条件：

```
工单编号
工单状态
故障类型
位置
维修人员
报修时间
异常工单
疑似重复
接单超时
处理超时
```

所有查询条件：

> 可以任意组合。

Mapper XML 使用：

```
<if>
```

动态拼接。

## 组合筛选验收

至少测试：

```
状态 + 故障类型

校区 + 区域 + 楼栋

维修人员 + 状态

时间范围 + 状态

异常工单 + 故障类型

疑似重复 + 区域

接单超时 + 待接单

处理超时 + 维修人员

工单号 + 其他条件
```

确认：

```
PageHelper total
records
```

均正确。

# 八、任务 3.5：通用工单详情

详情接口不要按管理员详情、维修人员详情、学生详情分别复制三套业务聚合逻辑。
建立：

```
RepairOrderDetailService
```

统一聚合。

角色差异通过：**访问权限+返回字段裁剪** 处理。

## 详情查询整体结构

详情返回：

```
工单基本信息

维修过程

材料记录

返工历史

评价

工单流转
```

形成：

```
RepairOrderDetailVO
```

## RepairOrderDetailVO

建议结构：

```
RepairOrderDetailVO
│
├─ baseInfo
│
├─ processRecords
│
├─ materialRecords
│
├─ reworkRecords
│
├─ evaluation
└─ flows
```

对应：

```
RepairOrderBaseVO

RepairProcessVO

RepairMaterialVO

RepairReworkVO

RepairEvaluationVO

RepairOrderFlowVO
```

保持结构清楚。

## 详情查询执行流程

建议：

```
根据 orderId 查询 repair_order
        ↓
资源不存在 → 404
        ↓
进行当前用户资源访问权限校验
        ↓
查询基础信息
        ↓
查询维修过程
        ↓
查询材料记录
        ↓
查询返工历史
        ↓
查询评价
        ↓
查询工单流转
        ↓
Service 聚合
        ↓
RepairOrderDetailVO
```

### 先做详情权限校验

详情不能：

```
先查所有子表
↓
最后判断没权限
```

应：

```
先查 repair_order
↓
检查访问权限
↓
再查子表
```

避免无权限请求造成不必要的数据访问。

### 管理员详情权限

管理员：

```
任意工单均可查看
```

只需要：

```
order 是否存在
```

### 维修人员详情权限

维修人员当前规则：

```
current_assignee_id = 当前维修人员
```

符合：

```
允许查看
```

否则：

```
HTTP 403
```

后续如果业务要求：

> 历史处理过该工单的维修人员仍可查看

再扩展权限规则。

### 学生详情权限

学生：

```
repair_order.student_uid = 当前用户ID
```

否则：

```
HTTP 403
```

## 详情基础信息

基础信息至少包括：

```
orderId
orderNo
status
statusName

故障类型
故障描述
报修图片

学生信息

校区
区域
楼栋
房间
完整位置文本

当前维修人员

报修时间
派单时间
接单截止时间
接单时间
预计完成截止时间
完成时间

异常标记
重复标记
返工次数
```

具体字段按最终 `repair_order` DDL 映射。

## 维修过程

来自：

```
repair_process_record
```

查询：

```
WHERE order_id = #{orderId}
ORDER BY create_time ASC, id ASC
```

或者按照 DDL 实际业务时间字段排序。

详情返回：

```
processId
workerId
workerName
processType
content
createTime
```

以后新增：

```
开始维修
中断
恢复
完成
备注
```

等过程记录后，详情接口无需改结构。

## 材料记录

来自：

```
repair_material_usage
```

查询：

```
order_id = 当前工单
```

返回：

```
materialId
materialName
quantity
unit
workerName
useTime
remark
```

具体以 DDL 字段为准。

## 返工历史

来自：

```
repair_rework_record
```

按照：

```
rework_no ASC
```

或时间排序。

返回：

```
reworkId
reworkNo
reason
status
applicant
createTime
处理时间
```

后续返工业务上线后直接展示。

## 评价

来自：

```
repair_evaluation
```

当前数据库规则：

```
一个工单最多一条评价
```

因此详情：

```
evaluation
```

是对象：

```
null
或
RepairEvaluationVO
```

而不是 List。

## 工单流转

来自：

```
repair_order_flow
```

按照：

```
flow_time ASC
id ASC
```

返回完整状态流转时间线。

例如后续可以形成：

```
学生提交报修
↓
系统自动派单
↓
张师傅接单
↓
开始维修
↓
提交完成
↓
学生确认
↓
评价
```

## 流转记录和操作日志不要混用

详情展示：

```
repair_order_flow
```

表示：

> 工单生命周期发生了什么。

后台系统审计：

```
sys_operation_log
```

表示：

> 谁调用了什么系统操作。

工单详情默认展示：

```
repair_order_flow
```

不直接把系统操作日志暴露给学生或维修人员。

## 详情查询 SQL 原则

详情不要写：

```
一个 Mapper + 一个超级 JOIN SQL
```

推荐：

```
1次 repair_order / 基本信息查询

1次 process 查询

1次 material 查询

1次 rework 查询

1次 evaluation 查询

1次 flow 查询
```

最多约：

```
6 次数据库查询
```

对于单工单详情，这个量完全可接受。

换来的好处是：

```
SQL 简单
数据不会重复
结构清楚
后续容易扩展
```

## 详情关联数据为空

必须允许：

```
processRecords = []

materialRecords = []

reworkRecords = []

evaluation = null

flows = []
```

不能因为**工单刚创建**，还没有维修过程或评价，就导致详情接口失败。

# 九、任务 3.6：详情数据裁剪

虽然聚合逻辑通用，但不同角色看到的信息可以不同。

## 管理员

可以看到：

```
全部工单业务信息
异常信息
内部调度信息
完整流转
```

## 维修人员

可以看到：

```
维修所需工单信息
学生联系方式
维修过程
材料
返工
业务流转
```

但管理员内部操作字段按需要隐藏。

## 学生

主要看到：

```
自己填写的报修信息
当前状态
当前维修人员
维修过程中的可公开信息
维修结果
评价
关键流转时间线
```

不建议暴露：

```
内部派单失败原因
管理员内部备注
其他候选维修人员
调度内部参数
```

## 字段裁剪

选择：

```
共享内部 Detail 数据
↓
根据角色转换对应 VO
```

例如：

```
AdminRepairOrderDetailVO
WorkerRepairOrderDetailVO
StudentRepairOrderDetailVO
```

如果三者差异不大，也可以使用同一个 VO，敏感字段：

```
只有管理员赋值
其他角色为 null
```



# 十、任务 3.7：工单访问校验统一封装

在本阶段正式建立：

```
RepairOrderAccessService
```

不要在：

```
详情
修改
接单
返工
评价
```

每个接口重新写一遍用户归属判断。

## 统一查看权限逻辑

逻辑：

```
ADMIN
→ 允许

STUDENT
→ order.studentUid == currentUserId

WORKER
→ order.currentAssigneeId == currentWorkerId

其他
→ 403
```

## 后续可以扩展动作权限

以后可继续增加：

```
checkViewPermission

checkWorkerOperatePermission

checkStudentConfirmPermission

checkReworkPermission

checkAdminPermission
```

但是阶段 3 只先抽：

```
查看权限
```

不要提前做复杂权限框架。

# 十一、任务 3.8：工单查询 Mapper

建议建立或完善：

```
RepairOrderMapper
```

本阶段至少实现：

```
selectPage(condition)

selectDetailBase(orderId)
```

## 关联 Mapper

同时准备：

```
RepairProcessRecordMapper
→ selectByOrderId

RepairMaterialUsageMapper
→ selectByOrderId

RepairReworkRecordMapper
→ selectByOrderId

RepairEvaluationMapper
→ selectByOrderId

RepairOrderFlowMapper
→ selectByOrderId
```

这些方法以后整个项目持续复用。

## 列表索引关注点

管理员筛选较多，不建议为了每一个条件都建立单列索引。

优先利用已经按照核心业务建立的索引。

重点关注：

```
order_no

status + report_time

fault_type_id

current_assignee_id + status

location

exception_flag

accept_deadline

complete_deadline
```

真正需要新增索引的条件：

> 必须结合 EXPLAIN 和实际数据量决定。

不要阶段 3 因为查询条件多，就一次性建十几个索引。

## 超时查询索引

接单超时：

```
status
+
accept_deadline
```

处理超时：

```
status
+
complete_deadline
```

属于高频定时任务和管理员查询共同使用条件。

这些索引优先级较高。

# 阶段 3 接口建议

可以形成三个列表入口：

```
GET /admin/repair-orders

GET /worker/repair-orders

GET /student/repair-orders
```

底层复用：

```
RepairOrderQueryService
```

详情也可以三个入口：

```
GET /admin/repair-orders/{id}

GET /worker/repair-orders/{id}

GET /student/repair-orders/{id}
```

底层仍然复用：

```
RepairOrderDetailService
```

这样：

```
接口权限清楚
前端调用清楚
Service 不重复
```

## 管理员列表接口

```
GET /admin/repair-orders
```

参数：

```
pageNum
pageSize

orderNo
status
faultTypeId

campusId
areaId
buildingId
roomId

workerId

reportStartTime
reportEndTime

exceptionFlag
suspectedDuplicate

acceptTimeout
completeTimeout
```

权限：

```
ADMIN
```

## 维修人员列表接口

```
GET /worker/repair-orders
```

允许的业务筛选：

```
pageNum
pageSize

orderNo
status
faultTypeId
reportStartTime
reportEndTime
```

服务端强制：

```
current_assignee_id
=
currentWorkerId
```

不接受：

```
workerId
```

作为数据范围参数。

## 学生列表接口

```
GET /student/repair-orders
```

允许：

```
pageNum
pageSize
status
faultTypeId
reportStartTime
reportEndTime
```

服务端强制：

```
student_uid
=
currentUserId
```

## 详情接口

三个入口底层统一：

```
RepairOrderDetailService.getDetail(orderId)
```

执行：

```
查工单
↓
校验角色归属
↓
查关联表
↓
聚合
↓
按角色转换 VO
```

# 阶段 3 错误码

本阶段正式增加：

```
ORDER
```

错误码模块。

至少：

```
ORDER-B-00001
HTTP 404
工单不存在
ORDER-A-00001
HTTP 403
无权访问该工单
```

建议：

```
资源不存在
→ ORDER-B

权限不足
→ AUTH-A
```

职责更清楚。

# 时间查询边界

时间区间必须统一定义。

建议：

```
reportStartTime
→ >=

reportEndTime
→ <=
```

如果前端按“日期”传：

```
2026-09-23
```

则前端或后端统一转换为：

```
2026-09-23 00:00:00
~
2026-09-23 23:59:59.999...
```

前端直接传完整时间范围，避免后端猜测语义。

# 管理员查询的异常组合

需要提前明确：

```
exceptionFlag
duplicateFlag
timeout
```

可以同时出现。

例如：

```
异常工单+接单超时
```

表示：

```
既标记异常
又已经接单超时
```

所有筛选默认：

```
AND
```

组合。

# 超时字段不建议落重复状态

列表返回：

```
acceptTimeout
completeTimeout
```

可以实时计算。

而：

```
exceptionFlag
duplicateFlag
```

属于业务已经识别的当前事实，应该落库。

这样区分：

```
超时
→ 时间衍生

异常
→ 业务标记

重复
→ 业务识别结果
```

# 阶段 3 并行边界

可以拆成：

## 工作包 A：管理员列表

```
RepairOrderQueryDTO
Condition
Mapper XML
分页
复杂条件
```

## 工作包 B：角色数据范围

```
currentWorkerId
studentUid
角色条件注入
越权测试
```

## 工作包 C：工单详情

```
DetailVO
关联 Mapper
聚合 Service
访问校验
```

A、B、C 可以大部分并行。

但：

```
详情访问校验
```

依赖当前 workerId 获取能力。

# 推荐开发顺序

```
3-01 RepairOrderQueryDTO

3-02 RepairOrderQueryCondition

3-03 RepairOrderListVO

3-04 管理员 selectPage SQL

3-05 管理员复杂筛选

3-06 学生数据范围

3-07 维修人员 currentWorkerId 获取

3-08 维修人员数据范围

3-09 学生列表

3-10 维修人员列表

3-11 RepairOrderDetailVO

3-12 基础信息详情 SQL

3-13 过程记录查询

3-14 材料查询

3-15 返工历史查询

3-16 评价查询

3-17 流转查询

3-18 详情 Service 聚合

3-19 RepairOrderAccess 校验

3-20 三角色详情接口

3-21 超时字段计算

3-22 组合条件测试

3-23 数据越权测试

3-24 EXPLAIN 核心 SQL

3-25 阶段验收
```



# 阶段 3 验收清单

## 管理员工单列表

- 

  支持分页；

- 

  工单编号筛选；

- 

  状态筛选；

- 

  故障类型筛选；

- 

  校区筛选；

- 

  区域筛选；

- 

  楼栋筛选；

- 

  房间筛选；

- 

  维修人员筛选；

- 

  报修时间范围；

- 

  异常工单筛选；

- 

  疑似重复筛选；

- 

  接单超时筛选；

- 

  处理超时筛选；

- 

  多条件组合正确；

- 

  PageHelper total 正确。

## 维修人员

- 

  只能查询 `current_assignee_id = 当前维修人员`；

- 

  前端不能指定其他 workerId 扩大范围；

- 

  列表分页正确；

- 

  条件查询正确；

- 

  详情不能访问其他维修人员工单。

## 学生

- 

  只能查询 `student_uid = 当前用户`；

- 

  前端无法伪造 studentUid；

- 

  列表分页正确；

- 

  详情不能访问其他学生工单。

## 工单详情

- 

  工单基本信息；

- 

  维修过程；

- 

  材料记录；

- 

  返工历史；

- 

  评价；

- 

  工单流转；

- 

  无过程数据返回空数组；

- 

  无评价返回 null；

- 

  工单不存在返回 404；

- 

  越权返回 403。

## SQL

- 

  列表没有 JOIN 一对多表；

- 

  分页不会产生重复工单；

- 

  详情不存在笛卡尔积；

- 

  核心查询完成 EXPLAIN；

- 

  超时条件能够利用目标索引。

# 阶段 3 Definition of Done

阶段 3 完成后，即使数据库里只有测试工单，三类用户已经能够按照自己的身份正确查看工单。

管理员：

```
全部工单
+
复杂筛选
```

维修人员：

```
只看当前分配给自己的工单
```

学生：

```
只看自己的报修工单
```

任何一张工单都可以通过统一详情能力得到：

```
基本信息
+
维修过程
+
材料记录
+
返工历史
+
评价
+
工单流转
```

并且权限边界正确。

此时后续业务只需要不断向：

```
repair_order
repair_process_record
repair_material_usage
repair_rework_record
repair_evaluation
repair_order_flow
```

写入数据，列表与详情体系即可持续复用。

完成这一查询骨架后，再进入下一部分：

> **学生提交报修 → 重复报修检测 → 创建工单 → 自动派单。**
