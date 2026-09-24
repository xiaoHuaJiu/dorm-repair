# PLAN-8：维修人员转派

## 一、阶段目标

阶段 8 负责完成维修人员主动发起转派、管理员审批，以及审批通过后的重新自动派单。

完整流程：

```
维修人员发起转派申请
↓
写 repair_transfer_request
↓
管理员审批
├─ 驳回
│  ↓
│ 原负责人继续处理
│
└─ 通过
   ↓
   调用 RepairDispatchService
   ↓
   排除原负责人
   ↓
   ├─ 派单成功
   │  ↓
   │ 新负责人
   │  ↓
   │ status = 待接单
   │
   └─ 派单失败
      ↓
      status = 待派单
      current_assignee_id = NULL
      ↓
      管理员待办 / 异常提醒
```

本阶段最重要的业务边界：

> **转派申请 ≠ 工单负责人已经改变。**

维修人员提交转派申请时，只产生：

```
repair_transfer_request
```

不修改：

```
repair_order.current_assignee_id
```

只有：

```
管理员审批通过
+
真正重新匹配到新的维修人员
```

之后，工单负责人才能发生变化。

## 二、阶段范围

本阶段实现：

```
维修人员发起转派申请
转派原因记录
管理员待审批列表
管理员审批通过
管理员审批驳回
审批幂等
同工单待审批唯一控制
审批通过后调用 RepairDispatchService
重新派单排除原负责人
重新派单成功后的负责人切换
重新派单失败后的待人工派单
管理员失败提醒
转派申请历史查询
工单流转记录
操作日志
```

本阶段暂不实现：

```
管理员手动指定新维修人员
维修人员请假转派
接单超时转派
批量转派
第三方维修
复杂异常工单处置
```

这些后续分别处理。

## 三、核心数据职责

转派涉及两类核心数据：

```
repair_transfer_request
```

负责：

> 转派申请和审批过程。

而：

```
repair_order
+
repair_order_flow
```

负责：

> 真正发生的工单负责人变化和状态变化。

必须保持职责分离。

### repair_transfer_request

记录：

```
申请人
原负责人
转派原因
审批状态
审批人
审批时间
审批意见
执行结果
新负责人
失败原因
```

### repair_order

只保存当前态：

```
current_assignee_id
status
dispatch_time
accept_deadline
```

### repair_order_flow

只在真正发生：

```
负责人变化
或
状态变化
```

时记录。

## 四、8.1 维修人员发起转派申请

### 4.1 接口设计

建议：

```
POST /worker/repair-orders/{orderId}/transfer-requests
```

请求：

```
CreateTransferRequestDTO
```

字段：

```
reasonType
reason
```

如果 `reasonType = OTHER`，则：

```
reason
```

必须填写。

### 4.2 转派原因

统一枚举：

```
OUT_OF_SCOPE
    不属于本人负责范围

NO_SKILL
    不具备对应技能

UNAVAILABLE
    当前无法处理

NEED_OTHER_TRADE
    需要其他工种

OVERLOAD
    当前工作量过大

OTHER
    其他
```

阶段 8 只记录原因。

不要因为维修人员选择：

```
不具备技能
```

就自动修改技能配置。

### 4.3 谁可以申请

必须满足：

```
当前登录用户是维修人员

current_assignee_id = 当前workerId
```

即：

> 维修人员只能为自己当前负责的工单申请转派。

维修人员 B：

```
不能申请转派维修人员A的工单
```

否则：

```
HTTP 403
```

### 4.4 哪些状态可以申请转派

建议允许：

```
待接单
维修中
返工中
已中断
```

这些都属于当前负责人仍然实际承担责任的状态。

不允许：

```
待派单
待确认
已完成
已取消
```

原因：

```
待派单
→ 当前没有正式负责人

待确认
→ 当前已提交维修结果，等待学生处理

已完成 / 已取消
→ 工单已关闭
```

### 4.5 同工单待审批唯一

核心规则：

> 同一张工单不能同时存在多个待审批转派申请。

例如：

```
orderId = 10001

已有：
approval_status = 待审批
```

维修人员再次申请：

```
拒绝
```

返回：

```
HTTP 409
```

当前业务中：

```
一张工单只有一个 current_assignee_id
且只有当前负责人可以发起转派申请
```

因此这里主要防的不是“多个维修人员并发申请”，而是：

```
当前负责人重复点击
网络重试
前端重复提交
```

导致同一工单产生多条待审批转派申请。

采用：

```
bizNo 幂等
+
待审批状态查询
```

即可。

流程：

```
收到转派申请
↓
校验 bizNo 幂等
↓
校验当前负责人
↓
校验工单状态
↓
查询是否已有待审批转派申请
↓
有
→ HTTP 409

无
→ INSERT repair_transfer_request
```

其中：

```
同一个 bizNo
→ 同一次请求重复执行
→ 不重复创建申请
```

而：

```
不同 bizNo
但该工单已有待审批申请
→ 属于业务重复申请
→ HTTP 409
```

第一期不额外使用：

```
SELECT ... FOR UPDATE
Redis业务锁
```

避免为了低概率场景增加不必要的锁复杂度。

对应验收调整为：

```
同一 bizNo 重复请求不会创建多条申请

同工单已有待审批申请时再次申请返回409

不同维修人员无法给不属于自己的工单发起转派

申请提交成功后工单负责人仍保持不变
```

### 4.6 数据库唯一控制

如果 MySQL 当前表结构允许，建议通过应用层查询 + 数据库约束共同保证。

应用层：

```
SELECT
FROM repair_transfer_request
WHERE order_id = ?
AND approval_status = 待审批
AND deleted = 0
```

存在：

```
直接拒绝
```

数据库如已有对应唯一设计，则作为最终兜底。

如果当前 DDL 没有办法通过简单唯一索引表达：

```
同工单仅允许一条待审批
```

则第一期采用：

```
Service 前置查询
+
事务内锁定工单行
```

即可。

不要为了这个单独引入 Redis 锁。

### 4.8 创建转派申请不修改工单

这是阶段 8 最重要的规则。

提交申请成功后：

```
repair_order.status
不变

current_assignee_id
不变
```

例如：

```
原状态 = 维修中
原负责人 = 张师傅
```

申请提交后仍然：

```
status = 维修中
current_assignee_id = 张师傅
```

直到管理员真正审批并执行。

### 4.9 是否写 repair_order_flow

申请转派：

```
没有改变工单状态
没有改变负责人
```

因此：

```
不写 repair_order_flow
```

只写：

```
repair_transfer_request
+
sys_operation_log
```

避免把申请行为误当成已经发生的工单流转。

## 五、8.2 管理员转派审批

### 5.1 待审批列表

建议：

```
GET /admin/transfer-requests
```

支持：

```
pageNum
pageSize
orderNo
applicantWorkerId
approvalStatus
createStartTime
createEndTime
```

默认主要查询：

```
approval_status = 待审批
```

### 5.2 转派详情

建议：

```
GET /admin/transfer-requests/{requestId}
```

返回：

```
转派申请信息
工单基本信息
当前负责人
转派原因
申请时间
审批信息
执行结果
```

工单详情可复用阶段 3 的通用能力。

### 5.3 审批接口

建议：

```
POST /admin/transfer-requests/{requestId}/approve
```

或者统一：

```
POST /admin/transfer-requests/{requestId}/review
```

请求：

```
ReviewTransferRequestDTO
```

字段：

```
action
remark
bizNo
```

其中：

```
action = APPROVE / REJECT
```

建议审批继续使用：

```
bizNo
```

做幂等。

因为管理员审批属于关键写操作，网络重试不能重复执行重新派单。

## 六、审批幂等控制

审批必须避免：

```
第一次审批已经成功
但响应丢失
↓
客户端重试
↓
再次调用 RepairDispatchService
```

因此建议继续复用项目统一幂等机制：

```
Redis SET NX
+
MySQL 幂等记录
```

业务键：

```
biz_type = TRANSFER_REVIEW
user_id = 当前管理员
biz_no = 前端生成
```

数据库业务状态本身也提供一层保护：

```
approval_status
只能从 待审批 → 已通过 / 已驳回
```

所以最终是：

```
业务状态 CAS
+
统一幂等
```

双层保证。

## 七、8.3 审批驳回

### 7.1 业务行为

管理员选择：

```
REJECT
```

只更新：

```
repair_transfer_request
```

例如：

```
approval_status = 已驳回
review_admin_id = 当前管理员
review_time = now
review_remark = xxx
```

### 7.2 工单保持不变

审批驳回：

```
repair_order.status
不变

repair_order.current_assignee_id
不变
```

即：

> 原负责人继续处理。

例如申请前：

```
维修中
张师傅
```

驳回后仍然：

```
维修中
张师傅
```

### 7.3 驳回不写 flow

因为：

```
没有状态变化
没有负责人变化
```

所以：

```
不写 repair_order_flow
```

只写：

```
transfer_request审批结果
+
operation log
```

## 八、8.4 审批通过

审批通过并不等于：

```
立刻改负责人
```

正确流程：

```
审批通过
↓
记录审批状态
↓
获取当前原负责人
↓
调用 RepairDispatchService
↓
excludeWorkerIds 包含原负责人
↓
重新匹配
```

### 8.1 原负责人必须加入排除名单

例如：

```
current_assignee_id = 1001
```

调用：

```
dispatch(
    orderId,
    Set.of(1001L),
    DispatchSourceType.TRANSFER_REASSIGN
);
```

防止：

```
张师傅申请转派
↓
算法认为张师傅工作量最少
↓
又派回张师傅
```

### 8.2 审批通过前再次校验工单

从申请到审批之间可能已经发生：

```
工单完成
负责人变化
管理员其他操作
工单取消
```

因此审批通过执行重新派单前必须重新读取：

```
repair_order
```

至少校验：

```
工单仍存在
当前负责人仍然是申请时的原负责人
当前状态仍然允许转派
```

如果已经发生变化：

```
不能继续执行旧申请
```

建议将申请标记为：

```
执行失败 / 已失效
```

并返回管理员明确提示。

## 九、8.5 重新派单成功

RepairDispatchService 成功：

```
selectedWorker = 新维修人员
```

自动派单核心已经负责：

```
status = 待接单

current_assignee_id = 新workerId

dispatch_time = now

accept_deadline = 新一轮有效工作时间30分钟
```

同时：

```
repair_order_flow
```

记录真正的负责人变化。

### 9.1 Flow 内容

例如：

```
old_assignee_id = 张师傅
new_assignee_id = 李师傅

source_type = TRANSFER_REASSIGN

operation_type = AUTO_REASSIGN
```

状态可能：

```
维修中
→ 待接单
```

或者：

```
已中断
→ 待接单
```

具体 fromStatus 使用审批执行前真实状态。

### 9.2 转派申请执行结果

派单成功后更新：

```
repair_transfer_request
```

例如：

```
approval_status = 已通过
execute_status = 成功
new_worker_id = 李师傅
execute_time = now
```

如果表结构已经拆：

```
approval_status
execute_status
```

则严格区分：

```
审批通过
≠
执行成功
```

这是很重要的。

## 十、8.6 重新派单失败

如果：

```
RepairDispatchService
```

没有找到新维修人员：

```
status = 待派单

current_assignee_id = NULL
```

同时：

```
生成管理员异常提醒 / 待办
```

这是阶段 5 已经确定的统一失败策略。

### 10.1 转派申请状态

管理员已经审批：

```
通过
```

所以审批状态不能改回：

```
待审批
```

而应该记录：

```
approval_status = 已通过

execute_status = 失败
```

并保存：

```
failure_reason
```

例如：

```
NO_SKILL_WORKER
NO_AREA_WORKER
NO_AVAILABLE_WORKER
NO_WORK_SCHEDULE
```

### 10.2 为什么要区分审批失败和执行失败

两个概念不同。

#### 审批驳回

```
管理员不同意转派
```

结果：

```
原负责人继续
```

#### 审批通过但重新派单失败

```
管理员同意转派
但系统当前找不到其他合适人员
```

结果：

```
原负责人解除
↓
待派单
↓
管理员人工处理
```

不能混成一个：

```
转派失败
```

状态。

## 十一、审批通过后的事务边界

这里需要特别注意事务设计。

推荐：

```
审批状态更新
↓
提交
↓
调用 RepairDispatchService
```

还是一个大事务，并不是最合适。

更推荐把：

```
审批决定
```

和：

```
重新派单执行
```

逻辑上区分。

但为了保证当前第一期一致性，可以采用 Service 层统一事务协调：

```
锁定 transfer_request
↓
校验待审批
↓
更新 approval_status = 已通过
↓
调用 RepairDispatchService
↓
根据返回更新 execute_status
↓
提交事务
```

前提：

```
RepairDispatchService
```

本身不要使用冲突的独立事务传播。

### 11.1 为什么不能先改 current_assignee_id 再派单

错误流程：

```
审批通过
↓
current_assignee_id = NULL
↓
再找候选人
```

如果中间发生异常：

```
工单会提前失去负责人
```

正确方式应让：

```
RepairDispatchService
```

统一负责：

```
负责人变化
状态变化
flow
失败兜底
```

审批模块不要自己提前改工单负责人。

## 十二、转派申请状态模型

建议至少区分：

### approval_status

```
0 待审批
1 已通过
2 已驳回
```

### execute_status

```
0 未执行
1 执行成功
2 执行失败
```

这样可以表达：

```
待审批 / 未执行

已驳回 / 未执行

已通过 / 执行成功

已通过 / 执行失败
```

比单一：

```
status
```

更准确。

## 十三、转派申请历史

维修人员应能查看：

```
自己发起的转派申请
```

建议：

```
GET /worker/transfer-requests
```

数据范围：

```
applicant_worker_id
=
currentWorkerId
```

支持：

```
审批状态
时间范围
工单编号
```

### 13.1 维修人员详情

建议：

```
GET /worker/transfer-requests/{id}
```

只能查看：

```
自己的申请
```

不能查看其他维修人员申请。

## 十四、操作日志与流转规则

### 申请

```
写 operation log
不写 flow
```

### 审批驳回

```
写 operation log
不写 flow
```

### 审批通过且派单成功

```
写 operation log
+
RepairDispatchService 写 flow
```

### 审批通过但派单失败

```
写 operation log
+
管理员异常提醒
```

是否额外写一条失败 flow：

第一期建议：

> 只有真正状态/负责人变化才写 flow。

如果失败处理把工单：

```
维修中 / 已中断
→ 待派单
```

那么由于状态确实发生变化：

```
应该写 flow
```

记录：

```
原负责人
→ NULL
```

和：

```
sourceType = TRANSFER_REASSIGN
failure
```

## 十五、并发与状态保护

阶段 8 重点防以下情况。

### 1. 同工单重复申请

通过：

```
工单行锁
+
待审批检查
```

保证只有一个待审批申请。

### 2. 同一申请重复审批

通过：

```
approval_status = 待审批
```

条件 UPDATE。

例如：

```
UPDATE repair_transfer_request
SET approval_status = #{result},
    review_admin_id = #{adminId},
    review_time = #{now}
WHERE id = #{requestId}
  AND approval_status = #{pending}
  AND deleted = 0
```

`affectedRows = 0`：

```
说明已经被其他管理员处理
```

返回：

```
HTTP 409
```

### 3. 审批期间工单负责人发生变化

审批执行前重新检查：

```
current_assignee_id
```

必须仍然等于：

```
申请时 original_worker_id
```

否则该申请已经失效。

### 4. 重新派单排除原负责人

强制：

```
excludeWorkerIds
```

包含：

```
original_worker_id
```

不能依赖调用方“记得传”。

审批 Service 自己负责组装。

## 十六、管理员提醒

重新派单失败必须生成管理员待办。

提醒内容至少：

```
工单号
原维修人员
转派申请人
转派原因
审批管理员
审批时间
重新派单失败原因
```

例如：

```
工单 RO202609240001 转派审批已通过，
但重新派单失败：当前无符合技能及负责区域条件的可用维修人员，
请人工处理。
```

### 16.1 防重复提醒

建议业务去重维度：

```
transferRequestId
+
reminderType
```

一个转派申请执行失败：

```
只生成一条核心管理员异常待办
```

## 十七、前端交互

### 维修人员端

允许在符合状态的工单详情中点击：

```
申请转派
```

填写：

```
转派原因类型
详细说明
```

提交后显示：

```
转派申请已提交，等待管理员审批。
```

但工单仍然显示：

```
当前负责人 = 自己
```

不能前端提前显示“已转派”。

### 管理员端

待办中心增加：

```
转派待审批
```

列表展示：

```
工单号
当前负责人
故障类型
位置
转派原因
申请时间
```

详情操作：

```
驳回
通过
```

### 审批通过成功

前端提示：

```
转派成功，已重新分配给李师傅。
```

### 审批通过但自动派单失败

提示：

```
转派申请已审批通过，但当前未找到可用维修人员，
工单已进入待人工派单列表。
```

这是：

```
审批成功
+
执行失败
```

不能提示成：

```
审批失败
```

## 十八、接口清单

维修人员：

```
POST /worker/repair-orders/{orderId}/transfer-requests

GET /worker/transfer-requests

GET /worker/transfer-requests/{id}
```

管理员：

```
GET /admin/transfer-requests

GET /admin/transfer-requests/{id}

POST /admin/transfer-requests/{id}/review
```

后端复用：

```
RepairDispatchService.dispatch(...)
```

不新增第二套：

```
TransferDispatchService
```

## 十九、重点验收场景

### 正常发起

维修人员 A：

```
current_assignee_id = A
status = 维修中
```

提交转派。

结果：

```
repair_transfer_request 新增
```

但：

```
repair_order.current_assignee_id
仍然 = A
```

### 维修人员 B 申请 A 的工单

结果：

```
HTTP 403
```

数据库不新增申请。

### 同工单重复待审批申请

第一条：

```
待审批
```

再次申请：

```
HTTP 409
```

只能存在一条待审批申请。

### 非法状态申请

例如：

```
待确认
已完成
已取消
```

不能申请转派。

返回：

```
HTTP 409
```

### 审批驳回

结果：

```
approval_status = 已驳回

current_assignee_id
仍然 = 原负责人

status
不变
```

不写负责人变化 flow。

### 审批通过且重新派单成功

原：

```
workerA
```

调用：

```
dispatch(
    orderId,
    [workerA],
    TRANSFER_REASSIGN
)
```

选中：

```
workerB
```

结果：

```
current_assignee_id = workerB

status = 待接单

dispatch_time = now

accept_deadline = 新截止时间

execute_status = 成功
```

flow：

```
workerA
→ workerB
```

### 原负责人工作量最低

即使：

```
workerA = 0单
workerB = 5单
```

但 A 在：

```
excludeWorkerIds
```

仍然：

```
不能重新派给A
```

### 重新派单失败

如果没有其他候选人：

```
status = 待派单

current_assignee_id = NULL

approval_status = 已通过

execute_status = 失败
```

同时：

```
管理员异常待办
```

必须生成。

### 两个管理员同时审批

管理员 A：

```
通过
```

管理员 B：

```
驳回
```

并发执行。

最终：

```
只能一个审批成功
```

另一请求：

```
HTTP 409
```

不得出现：

```
审批记录和派单结果互相矛盾
```

### 审批前工单负责人已经变化

申请时：

```
original_worker_id = A
```

审批时：

```
current_assignee_id = B
```

则：

```
旧转派申请不能继续执行
```

应返回：

```
HTTP 409
```

并将申请标记为失效或执行失败，具体按表状态设计。

### 网络重试审批

同一：

```
bizNo
```

重复调用审批接口。

结果：

```
不重复执行派单
不生成重复 flow
不生成重复提醒
```

## 二十、错误码建议

建议增加：

```
TRANSFER
```

模块。

例如：

```
TRANSFER-A-00001
当前工单状态不允许申请转派
HTTP 409
TRANSFER-A-00002
该工单已有待审批转派申请
HTTP 409
TRANSFER-A-00003
该转派申请已经处理
HTTP 409
TRANSFER-A-00004
工单负责人已发生变化，该转派申请已失效
HTTP 409
TRANSFER-B-00001
转派申请不存在
HTTP 404
```

无权访问：

```
AUTH-A-xxxxx
HTTP 403
```

重新派单失败：

> 不作为审批接口异常直接抛出。

因为审批本身已经成功。

应返回一个明确业务结果，例如：

```
reviewSuccess = true
dispatchSuccess = false
```

并说明：

```
已进入管理员人工处理。
```

## 二十一、推荐开发顺序

```
8-01 TransferReasonTypeEnum

8-02 TransferApprovalStatusEnum

8-03 TransferExecuteStatusEnum

8-04 CreateTransferRequestDTO

8-05 维修人员申请权限校验

8-06 bizNo 幂等 + 待审批唯一业务校验

8-07 创建 repair_transfer_request

8-08 维修人员申请列表 / 详情

8-09 管理员待审批列表 / 详情

8-10 ReviewTransferRequestDTO

8-11 审批 bizNo 幂等

8-12 审批 CAS

8-13 审批驳回

8-14 审批通过状态校验

8-15 原负责人加入 excludeWorkerIds

8-16 调用 RepairDispatchService

8-17 派单成功回写 execute_status

8-18 派单失败回写 execute_status

8-19 失败管理员待办

8-20 operation log

8-21 工单 flow 联调

8-22 维修人员前端申请页面

8-23 管理员审批页面

8-24 并发申请测试

8-25 并发审批测试

8-26 全链路验收
```



## 二十三、阶段验收清单

### 申请

- 

  维修人员只能申请自己的工单；

- 

  只有允许状态可以申请；

- 

  同工单只能有一条待审批申请；

- 

  创建申请不改变工单负责人；

- 

  创建申请不改变工单状态；

- 

  申请不写负责人变化 flow。

### 审批驳回

- 

  只有待审批可以处理；

- 

  驳回后原负责人不变；

- 

  工单状态不变；

- 

  不调用 RepairDispatchService；

- 

  不写负责人变化 flow。

### 审批通过

- 

  审批前重新校验工单；

- 

  原负责人加入排除名单；

- 

  统一调用 RepairDispatchService；

- 

  不存在独立转派匹配算法；

- 

  新负责人不能是原负责人。

### 派单成功

- 

  新负责人正确；

- 

  状态变为待接单；

- 

  新 dispatch_time 正确；

- 

  新 accept_deadline 正确；

- 

  flow 记录原负责人和新负责人；

- 

  execute_status = 成功。

### 派单失败

- 

  状态进入待派单；

- 

  current_assignee_id = NULL；

- 

  approval_status 仍然是已通过；

- 

  execute_status = 失败；

- 

  失败原因有记录；

- 

  管理员收到异常待办。

### 并发与幂等

- 

  同工单不能并发产生多个待审批申请；

- 

  同一申请只能审批一次；

- 

  两个管理员并发审批只有一个成功；

- 

  审批网络重试不重复派单；

- 

  不生成重复 flow；

- 

  不生成重复异常提醒。

## 二十四、Definition of Done

阶段 8 完成后，维修人员能够：

```
自己的工单
↓
发起转派申请
↓
等待管理员审批
```

此时：

```
原负责人仍然负责
```

管理员审批：

```
驳回
↓
原负责人继续
```

或者：

```
通过
↓
排除原负责人
↓
统一调用 RepairDispatchService
```

成功：

```
新负责人
↓
待接单
↓
重新生成 accept_deadline
↓
记录负责人变化
```

失败：

```
待派单
↓
current_assignee_id = NULL
↓
管理员异常待办
```

最终确保：

> **转派申请记录审批过程，**`**repair_order**` **只记录真正发生后的当前负责人，**`**repair_order_flow**` **只记录真正发生的负责人和状态变化。**

阶段 8 完成后，下一阶段可以进入：

> **阶段 9：维修人员请假申请、管理员审批、请假生效与未完成工单自动转派。**