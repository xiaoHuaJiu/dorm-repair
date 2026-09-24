# PLAN-6：维修人员主流程

## 一、阶段目标

阶段 6 在阶段 5 自动派单完成后，正式进入维修人员处理工单的核心业务链路。

本阶段完成：

```
待接单
↓
维修人员接单
↓
维修中
↓
添加维修过程
↓
记录材料使用
↓
可中断 / 恢复
↓
提交维修结果
↓
待学生确认
```

本阶段重点建设：

1. 维修人员接单；
2. 维修过程记录；
3. 材料使用记录；
4. 维修中断；
5. 恢复维修；
6. 提交维修结果；
7. 维修人员工单操作权限；
8. 工单状态合法性校验；
9. 工单流转记录与操作日志。

阶段完成后，一张已经自动派单并进入“待接单”的工单，应能够由当前负责人完整处理到“待确认”。

## 二、阶段 6 业务边界

本阶段实现：

```
待接单 → 维修中
维修中 → 已中断
已中断 → 维修中
维修中 / 返工中 → 待确认
```

并写入：

```
repair_process_record
repair_material_usage
repair_order_flow
sys_operation_log
```

本阶段不实现：

```
接单超时自动转派
维修人员主动转派
请假转派
学生确认
学生评价
学生返工申请
多次返工异常处理
管理员人工干预
```

这些放在后续阶段。

## 三、统一业务原则

维修人员对工单的所有操作，都必须同时满足两类条件。

第一类：

```
当前维修人员
=
repair_order.current_assignee_id
```

第二类：

```
当前工单状态
允许执行该操作
```

即：

> “是当前负责人”不代表任何状态下都能修改工单。

也不能只判断状态，而不判断当前负责人。

统一流程：

```
查询工单
↓
校验工单存在
↓
获取当前 workerId
↓
校验 current_assignee_id
↓
校验当前 status
↓
执行业务更新
↓
写业务记录
↓
写 repair_order_flow
↓
写 operation log
```

## 四、当前维修人员身份获取

当前登录上下文提供的是：

```
sys_user.id
```

而：

```
repair_order.current_assignee_id
```

保存的是：

```
repair_worker.id
```

因此维修人员业务操作统一先通过：

```
当前 userId
↓
查询 repair_worker
↓
获取 workerId
```

建议继续复用阶段 3 已建立的当前维修人员解析能力。

禁止前端传：

```
workerId
```

决定当前操作人。

## 五、统一工单负责人校验

建议在阶段 6 正式复用或补充：

```
checkCurrentAssignee(order, currentWorkerId)
```

逻辑：

```
order.current_assignee_id == currentWorkerId
→ 允许继续

否则
→ HTTP 403
```

例如维修人员 B 访问维修人员 A 当前负责的工单：

```
维修人员B
↓
调用添加维修过程
↓
current_assignee_id != B
↓
403
```

不要只依靠维修人员列表的数据范围来保证安全。

详情、修改、接单、材料、提交结果都要再次校验。

## 六、6.1 维修人员接单

### 6.1.1 业务状态

接单前：

```
status = 待接单
```

接单成功：

```
status = 维修中
```

同时：

```
accept_time = now
```

维修人员可以选择填写：

```
expected_complete_time
```

### 6.1.2 接单接口

建议：

```
POST /worker/repair-orders/{orderId}/accept
```

请求：

```
AcceptRepairOrderDTO
```

字段：

```
expectedCompleteTime
```

允许为空。

前端不传：

```
workerId
acceptTime
completeDeadline
status
```

这些全部由服务端确定。

### 6.1.3 接单前置校验

至少：

```
工单存在

当前用户是维修人员

current_assignee_id = 当前workerId

status = 待接单
```

如果已经：

```
维修中
待确认
已完成
已取消
```

均不能再次接单。

### 6.1.4 expected_complete_time 校验

如果维修人员填写：

```
expectedCompleteTime
```

至少要求：

```
expectedCompleteTime > acceptTime
```

不能填写：

```
过去时间
等于接单时间
```

是否限制最大范围，例如不能超过 7 天，可根据业务后续再定，本阶段不提前增加复杂限制。

### 6.1.5 complete_deadline 规则

规则锁定为：

如果维修人员未填写预计完成时间：

```
complete_deadline
=
accept_time + 24h
```

如果填写：

```
complete_deadline
=
expected_complete_time
```

即：

```
expected_complete_time != null
→ complete_deadline = expected_complete_time

expected_complete_time == null
→ complete_deadline = accept_time + 24h
```

这里的 24 小时当前按自然时间处理。

本阶段不自动套用阶段 5 的有效工作时间算法。

因为当前需求只明确：

```
接单 deadline
→ 按有效工作时间累计

维修 complete_deadline
→ 未填写时 accept_time + 24h
```

两者口径不同。

### 6.1.6 接单成功更新

更新：

```
status = 维修中

accept_time = now

expected_complete_time = 用户填写值

complete_deadline =
    expected_complete_time
    或
    accept_time + 24h
```

保留：

```
current_assignee_id
```

不变。

### 6.1.7 接单并发控制

接单属于状态变更操作，建议使用条件更新：

```
UPDATE repair_order
SET
    status = #{repairingStatus},
    accept_time = #{acceptTime},
    expected_complete_time = #{expectedCompleteTime},
    complete_deadline = #{completeDeadline}
WHERE id = #{orderId}
  AND current_assignee_id = #{workerId}
  AND status = #{waitingAcceptStatus}
  AND deleted = 0
```

检查：

```
affectedRows
```

如果：

```
affectedRows = 0
```

说明：

```
工单状态已变化
或
当前负责人已变化
```

返回：

```
HTTP 409
```

不要覆盖最新状态。

### 6.1.8 接单流转记录

接单成功后写：

```
repair_order_flow
```

记录：

```
from_status = 待接单
to_status = 维修中

old_assignee_id = currentWorkerId
new_assignee_id = currentWorkerId

operation_type = ACCEPT
operator_id = 当前用户
operation_time = acceptTime
```

## 七、6.2 添加维修过程

### 7.1 数据表

对应：

```
repair_process_record
```

一张工单允许存在多条过程记录。

### 7.2 支持的记录类型

本阶段至少支持：

```
普通维修记录
维修中断
恢复维修
提交维修结果
```

建议使用：

```
ProcessRecordTypeEnum
```

统一定义。

例如：

```
NORMAL
INTERRUPT
RESUME
SUBMIT_RESULT
```

不要在代码中直接散落：

```
0
1
2
3
```

### 7.3 普通维修过程接口

建议：

```
POST /worker/repair-orders/{orderId}/process-records
```

请求：

```
AddRepairProcessDTO
```

至少：

```
content
```

如果支持过程图片：

```
imageUrls
```

可以同时记录。

### 7.4 普通维修记录允许状态

普通维修过程只允许：

```
维修中
返工中
```

不允许：

```
待接单
待确认
已中断
已完成
已取消
```

特别是：

```
待接单
```

说明维修人员还没有正式接单，因此不能提前写维修过程。

### 7.5 普通过程记录内容

至少记录：

```
order_id
worker_id
record_type = NORMAL
content
record_time
```

当前维修人员来自：

```
UserContext → repair_worker
```

不能由请求指定。

## 八、6.3 材料使用记录

### 8.1 数据表

对应：

```
repair_material_usage
```

当前阶段只解决：

> 本次维修用了什么材料、用了多少。

不建设：

```
材料库存
库存扣减
采购
入库
出库
供应商
成本结算
```

### 8.2 材料记录接口

建议：

```
POST /worker/repair-orders/{orderId}/materials
```

请求：

```
AddMaterialUsageDTO
```

字段根据最终 DDL，例如：

```
materialName
quantity
unit
remark
```

### 8.3 材料允许状态

建议允许：

```
维修中
返工中
```

不允许：

```
待接单
待确认
已中断
已完成
已取消
```

### 8.4 材料记录权限

必须：

```
current_assignee_id
=
currentWorkerId
```

维修人员不能给别人的工单添加材料。

### 8.5 已完成后禁止修改

本阶段锁定：

> 工单已经进入待确认以后，维修过程和材料数据原则上不再允许维修人员任意修改。

尤其：

```
已完成
```

绝对不能新增或修改材料。

如果未来管理员需要纠错：

```
单独设计管理员修正能力
```

不要开放给维修人员直接改历史。

### 8.6 学生端展示规则

材料属于维修内部记录。

第一阶段：

```
管理员
→ 可查看

维修人员
→ 可查看自己负责工单材料

学生
→ 默认不展示材料详细信息
```

阶段 3 的通用详情聚合内部可以仍然查材料，但学生 DetailVO 不返回或不赋值。

## 九、6.4 中断维修

### 9.1 业务状态

中断前：

```
维修中
```

中断成功：

```
已中断
```

即：

```
维修中
↓
已中断
```

### 9.2 中断接口

建议：

```
POST /worker/repair-orders/{orderId}/interrupt
```

请求：

```
InterruptRepairDTO
```

字段：

```
interruptReasonType
content
```

例如原因：

```
等待材料
现场条件不允许
需要协调其他人员
学生暂时不在
设备需停机
其他
```

具体枚举以后按业务确定。

### 9.3 中断数据存储原则

不单独建立：

```
repair_interrupt
```

表。

中断记录直接写：

```
repair_process_record
```

例如：

```
record_type = INTERRUPT

interrupt_reason_type = xxx

content = xxx

record_time = now
```

### 9.4 为什么不单独建中断表

当前需求只需要：

```
什么时候中断
为什么中断
什么时候恢复
```

这些信息通过：

```
repair_process_record
```

时间线已经能够完整表达。

没有必要再增加一张一对一/一对多中断业务表。

### 9.5 中断工单更新

同事务：

```
UPDATE repair_order
维修中 → 已中断
```

并：

```
INSERT repair_process_record
record_type = INTERRUPT
```

再写：

```
repair_order_flow
```

### 9.6 中断 flow

记录：

```
from_status = 维修中
to_status = 已中断

old_assignee_id = workerId
new_assignee_id = workerId

operation_type = INTERRUPT
```

负责人不发生变化。

### 9.7 中断后限制

工单处于：

```
已中断
```

时：

```
不能添加普通维修过程
不能添加材料
不能直接提交维修结果
```

必须先：

```
恢复维修
```

## 十、6.5 恢复维修

### 10.1 状态

```
已中断
↓
维修中
```

### 10.2 恢复接口

建议：

```
POST /worker/repair-orders/{orderId}/resume
```

可以允许：

```
remark
```

作为恢复说明。

### 10.3 恢复前置条件

必须：

```
status = 已中断

current_assignee_id = 当前workerId
```

其他状态不能调用。

### 10.4 恢复过程记录

写：

```
repair_process_record
```

记录：

```
record_type = RESUME

content = 恢复维修说明

record_time = now
```

### 10.5 恢复 flow

```
from_status = 已中断
to_status = 维修中

operation_type = RESUME
```

### 10.6 中断时间如何计算

不在 `repair_order` 增加：

```
interrupt_time
resume_time
```

也不增加额外中断表。

需要查看时：

```
repair_process_record
```

按照：

```
INTERRUPT
RESUME
```

记录顺序即可得到：

```
中断时间
中断原因
恢复时间
```

## 十一、6.6 提交维修结果

### 11.1 业务状态

允许：

```
维修中
返工中
```

提交成功：

```
待确认
```

即：

```
维修中
   ↓
待确认
```

或者：

```
返工中
   ↓
待确认
```

### 11.2 提交结果接口

建议：

```
POST /worker/repair-orders/{orderId}/submit-result
```

请求：

```
SubmitRepairResultDTO
```

字段可以包括：

```
resultDescription
resultImageUrls
remark
```

以最终 DDL 和页面为准。

### 11.3 提交前置校验

必须同时满足：

```
current_assignee_id = 当前workerId
```

以及：

```
status IN (
    维修中,
    返工中
)
```

### 11.4 已中断不能直接提交结果

如果：

```
status = 已中断
```

调用：

```
submit-result
```

必须拒绝。

正确流程：

```
已中断
↓
恢复维修
↓
维修中
↓
提交维修结果
```

### 11.5 提交维修结果的过程记录

提交成功时写：

```
repair_process_record
```

例如：

```
record_type = SUBMIT_RESULT

content = resultDescription

record_time = now
```

如果结果图片记录在过程表 JSON 字段，也一并保存。

### 11.6 更新 repair_order

更新：

```
status = 待确认

repair_submit_time = now
```

负责人：

```
current_assignee_id
```

暂时保留。

因为：

> 工单还没有最终结束，只是在等待学生确认。

### 11.7 不要设置 complete_time

维修人员提交维修结果时：

```
repair_submit_time = now
```

但：

```
complete_time
```

此时不能设置。

因为真正完成还需要：

```
学生确认
```

最终：

```
已完成
```

时再写：

```
complete_time
```

### 11.8 提交结果 flow

记录：

```
from_status = 维修中 / 返工中

to_status = 待确认

operation_type = SUBMIT_RESULT

old_assignee_id = workerId
new_assignee_id = workerId
```

## 十二、维修过程与工单状态关系

建议阶段 6 正式锁定以下矩阵：

| 操作         | 待接单 | 维修中 | 已中断 | 返工中         | 待确认 | 已完成 | 已取消 |
| ------------ | ------ | ------ | ------ | -------------- | ------ | ------ | ------ |
| 接单         | ✅      | ❌      | ❌      | ❌              | ❌      | ❌      | ❌      |
| 普通维修记录 | ❌      | ✅      | ❌      | ✅              | ❌      | ❌      | ❌      |
| 材料记录     | ❌      | ✅      | ❌      | ✅              | ❌      | ❌      | ❌      |
| 中断维修     | ❌      | ✅      | ❌      | 视业务后续决定 | ❌      | ❌      | ❌      |
| 恢复维修     | ❌      | ❌      | ✅      | ❌              | ❌      | ❌      | ❌      |
| 提交维修结果 | ❌      | ✅      | ❌      | ✅              | ❌      | ❌      | ❌      |

对于：

```
返工中 → 中断
```

如果当前业务希望返工维修也可以中断，可以允许：

```
返工中 → 已中断
```

但这样恢复后需要知道恢复到：

```
维修中
还是
返工中
```

会增加额外状态恢复问题。

因此第一期建议：

> 中断功能先只支持普通 `维修中` 状态。

返工阶段如果确实需要中断，再结合返工流程补充。

这样结构更简单。

## 十三、状态校验统一实现

不要在每个 Service 方法中散落：

```
if (status != 2) ...
```

建议至少统一使用：

```
RepairOrderStatusEnum
```

并通过：

```
order.getStatus().equals(
    RepairOrderStatusEnum.REPAIRING.getCode()
)
```

判断。

也可以定义轻量辅助方法：

```
canAddProcess()
canAddMaterial()
canInterrupt()
canResume()
canSubmitResult()
```

但第一期不用专门建设复杂状态机框架。

## 十四、状态更新统一采用条件 UPDATE

本阶段所有状态变化建议使用 CAS 风格条件更新。

例如：

```
待接单 → 维修中
```

SQL 条件必须包含：

```
orderId
current_assignee_id
当前 status
```

中断：

```
WHERE id = ?
AND current_assignee_id = ?
AND status = 维修中
```

恢复：

```
WHERE id = ?
AND current_assignee_id = ?
AND status = 已中断
```

提交结果：

```
WHERE id = ?
AND current_assignee_id = ?
AND status IN (维修中, 返工中)
```

这样可以避免：

```
前端页面状态过期
↓
重复点击
↓
并发操作
↓
覆盖最新工单状态
```

## 十五、affectedRows = 0 的统一处理

条件更新：

```
affectedRows = 0
```

不要直接认为：

```
数据库异常
```

通常表示：

```
状态已经变化
或
负责人已经变化
```

建议重新读取工单。

如果：

```
负责人已变化
→ 403 / 409

状态已变化
→ 409
```

userTip：

```
工单状态已发生变化，请刷新后重新操作。
```

## 十六、事务边界

以下操作必须同事务。

### 接单

```
UPDATE repair_order
+
INSERT repair_order_flow
+
operation log
```

### 添加普通过程

```
INSERT repair_process_record
+
operation log
```

普通过程不改变工单状态，因此通常不需要写 flow。

### 添加材料

```
INSERT repair_material_usage
+
operation log
```

也不改变状态，不必写 flow。

### 中断

```
UPDATE repair_order
+
INSERT process(INTERRUPT)
+
INSERT flow
+
operation log
```

### 恢复

```
UPDATE repair_order
+
INSERT process(RESUME)
+
INSERT flow
+
operation log
```

### 提交维修结果

```
UPDATE repair_order
+
INSERT process(SUBMIT_RESULT)
+
INSERT flow
+
operation log
```

任何核心步骤失败：

```
事务回滚
```

## 十七、什么时候写 repair_order_flow

`repair_order_flow` 记录：

> 状态变化或负责人变化。

因此阶段 6：

需要写 flow：

```
接单
中断
恢复
提交维修结果
```

普通：

```
添加维修说明
添加材料
```

不改变状态，因此：

```
不写 flow
```

只写：

```
process/material
+
operation log
```

这样避免 flow 变成普通操作日志。

## 十八、operation log 规则

维修人员以下操作都应记录系统操作日志：

```
接单
添加维修过程
添加材料
中断
恢复
提交维修结果
```

至少记录：

```
operator_id
operator_role
business_id = orderId
operation_type
operation_result
operation_time
```

具体按照现有操作日志规范。

## 十九、阶段 3 工单详情自动复用

阶段 3 已经完成工单详情聚合：

```
工单基本信息
+
维修过程
+
材料
+
返工
+
评价
+
流转
```

因此阶段 6 每开发一个操作：

```
process INSERT
material INSERT
flow INSERT
repair_order UPDATE
```

无需重新建立详情接口。

执行成功后：

```
重新请求阶段3详情
```

即可看到最新结果。

## 二十、维修过程列表排序

统一按照：

```
record_time ASC
id ASC
```

展示。

这样详情页自然形成：

```
开始处理
↓
普通维修记录
↓
中断
↓
恢复
↓
维修记录
↓
提交结果
```

的时间线。

## 二十一、材料列表排序

建议：

```
use_time ASC
id ASC
```

如果表中没有：

```
use_time
```

则使用：

```
create_time ASC
id ASC
```

保持稳定排序。

## 二十二、接单截止时间与接单动作

维修人员接单时，正常情况下：

```
now <= accept_deadline
```

但是：

> 接单超时后的自动转派属于后续阶段。

阶段 6 本身不要重新实现超时转派。

如果维修人员点击接单时已经：

```
now > accept_deadline
```

建议仍然在 Service 中做一次截止时间检查。

返回：

```
HTTP 409
```

提示：

```
该工单接单时限已过，请刷新后查看最新状态。
```

防止：

```
定时任务还没来得及转派
```

的短暂窗口内，维修人员继续接下已经超时的工单。

## 二十三、维修完成时限本阶段职责

阶段 6 负责：

```
生成 complete_deadline
```

但不负责：

```
扫描 complete_deadline
超时提醒
标记维修超时
通知管理员
```

这些进入后续超时提醒阶段。

## 二十四、维修人员前端页面

本阶段对应维修人员端至少完成：

### 待接单详情

操作：

```
接单
```

可填写：

```
预计完成时间
```

### 维修中详情

支持：

```
添加维修记录
添加材料
中断维修
提交维修结果
```

### 已中断详情

只允许：

```
查看历史
恢复维修
```

不显示：

```
提交完成
新增材料
普通维修记录
```

### 待确认详情

全部维修编辑按钮关闭。

只展示：

```
已提交维修结果
等待学生确认
```

## 二十五、前端按钮控制不是权限控制

前端根据：

```
status
```

隐藏或显示操作按钮只是用户体验。

例如：

```
status = 已中断
```

前端不显示：

```
提交维修结果
```

但用户仍可能手动调用 API。

所以后端必须再次执行：

```
负责人校验
+
状态校验
```

不能依赖前端。

## 二十六、核心接口清单

阶段 6 建议提供：

```
POST /worker/repair-orders/{id}/accept
```

接单。

```
POST /worker/repair-orders/{id}/process-records
```

添加普通维修过程。

```
POST /worker/repair-orders/{id}/materials
```

添加材料记录。

```
POST /worker/repair-orders/{id}/interrupt
```

中断维修。

```
POST /worker/repair-orders/{id}/resume
```

恢复维修。

```
POST /worker/repair-orders/{id}/submit-result
```

提交维修结果。

查询继续复用阶段 3：

```
GET /worker/repair-orders

GET /worker/repair-orders/{id}
```

## 二十七、错误码建议

本阶段主要增加：

```
ORDER-A
```

类业务错误。

例如：

```
ORDER-A-xxxxx
当前工单状态不允许接单
HTTP 409
ORDER-A-xxxxx
当前工单状态不允许添加维修记录
HTTP 409
ORDER-A-xxxxx
当前工单状态不允许中断
HTTP 409
ORDER-A-xxxxx
当前工单状态不允许恢复
HTTP 409
ORDER-A-xxxxx
当前工单状态不允许提交维修结果
HTTP 409
```

负责人不匹配继续统一使用：

```
AUTH-A-xxxxx
HTTP 403
```

## 二十八、重点验收场景

### 接单

正常：

```
待接单
+
当前负责人
↓
接单成功
↓
维修中
```

验证：

```
accept_time
expected_complete_time
complete_deadline
flow
```

全部正确。

### 未填写预计完成时间

```
expected_complete_time = null
```

必须：

```
complete_deadline
=
accept_time + 24h
```

### 填写预计完成时间

例如：

```
expected_complete_time
=
2026-09-25 18:00
```

必须：

```
complete_deadline
=
2026-09-25 18:00
```

### 待接单添加维修记录

```
status = 待接单
```

调用：

```
process-records
```

结果：

```
HTTP 409
```

数据库：

```
repair_process_record
不新增
```

### 待接单添加材料

结果：

```
HTTP 409
```

### 正常添加多条过程

```
维修中
```

连续添加：

```
记录1
记录2
记录3
```

全部保留。

### 正常添加多条材料

允许：

```
材料A
材料B
材料C
```

分别记录。

### 中断维修

```
维修中
↓
中断
↓
已中断
```

验证：

```
process_record = INTERRUPT

interrupt_reason_type 正确

flow 正确
```

### 已中断添加普通过程

结果：

```
HTTP 409
```

### 已中断添加材料

结果：

```
HTTP 409
```

### 已中断直接完成

调用：

```
submit-result
```

必须：

```
HTTP 409
```

不能出现：

```
已中断 → 待确认
```

### 恢复维修

```
已中断
↓
恢复
↓
维修中
```

验证：

```
process_record = RESUME
flow 正确
```

### 非中断状态调用恢复

例如：

```
维修中
```

调用：

```
resume
```

返回：

```
409
```

### 提交维修结果

```
维修中
↓
提交维修结果
↓
待确认
```

同时：

```
repair_submit_time = now
```

并写：

```
SUBMIT_RESULT process
flow
```

### 待确认再次提交结果

```
status = 待确认
```

再次调用：

```
submit-result
```

必须：

```
HTTP 409
```

### 待确认添加维修记录

必须：

```
HTTP 409
```

### 已完成修改材料

必须：

```
HTTP 409
```

### 维修人员 B 操作 A 的工单

工单：

```
current_assignee_id = workerA
```

当前登录：

```
workerB
```

调用：

```
接单
添加过程
材料
中断
恢复
提交结果
```

全部：

```
HTTP 403
```

### 并发重复接单

两个相同接单请求同时执行：

```
线程A
线程B
```

最终：

```
只有一个 UPDATE 成功
```

另一请求：

```
409
```

只能生成一条有效接单 flow。

### 并发中断 / 提交

例如：

```
线程A → 中断
线程B → 提交结果
```

同一时刻操作维修中工单。

最终只能一个状态更新成功。

不能出现：

```
status = 待确认
但又存在同一时刻的已中断状态
```

## 二十九、推荐开发顺序

```
6-01 接单 DTO / VO

6-02 当前维修人员与负责人校验

6-03 接单 CAS 更新

6-04 complete_deadline 计算

6-05 接单 flow / log

6-06 普通维修过程记录

6-07 材料使用记录

6-08 中断原因枚举

6-09 中断维修

6-10 中断 process / flow

6-11 恢复维修

6-12 恢复 process / flow

6-13 提交维修结果 DTO

6-14 提交结果 process

6-15 提交结果 CAS 状态更新

6-16 repair_submit_time

6-17 提交结果 flow / log

6-18 阶段3详情联调

6-19 维修人员前端操作页面

6-20 非法状态测试

6-21 越权测试

6-22 并发状态测试

6-23 全链路验收
```

## 三十、并行开发边界

可以拆成三个工作包。

### 工作包 A：接单

```
接单接口
complete_deadline
CAS
flow
```

### 工作包 B：维修过程

```
普通记录
中断
恢复
process record
```

### 工作包 C：材料与提交结果

```
材料记录
提交维修结果
repair_submit_time
```

三者最终统一接入：

```
维修人员详情页
+
repair_order_flow
+
operation log
```

## 三十一、估时

| 工作项                     | 估时           |
| -------------------------- | -------------- |
| 接单                       | 0.5~0.75 人日  |
| 完成截止时间计算           | 0.25           |
| 接单 CAS / flow            | 0.25~0.5       |
| 普通维修过程               | 0.5            |
| 材料记录                   | 0.5            |
| 中断维修                   | 0.5            |
| 恢复维修                   | 0.25~0.5       |
| 提交维修结果               | 0.5~0.75       |
| 操作日志整合               | 0.25           |
| 前端维修操作页面           | 1~1.5          |
| 非法状态 / 越权 / 并发测试 | 1~1.5          |
| **合计**                   | **5.5~7 人日** |

## 三十二、阶段 6 验收清单

### 接单

- 只有待接单状态可以接单；
- 只有当前负责人可以接单；
- `accept_time` 正确；
- 未填写预计完成时间时 `complete_deadline = accept_time + 24h`；
- 填写后 `complete_deadline = expected_complete_time`；
- 接单超时后不能继续接单；
- 并发接单只有一次成功。

### 维修过程

- 维修中可以添加普通记录；
- 返工中可以添加普通记录；
- 支持多条过程记录；
- 待接单不能添加；
- 已中断不能添加；
- 待确认不能添加；
- 已完成不能添加。

### 材料

- 维修中可以添加；
- 返工中可以添加；
- 支持多条；
- 已中断不能添加；
- 待确认不能添加；
- 已完成不能修改；
- 学生默认不展示详细材料数据。

### 中断

- 维修中可以中断；
- 状态变为已中断；
- 中断原因写入过程表；
- 中断时间从过程表获得；
- 不新增中断专表；
- 已中断不能直接提交结果。

### 恢复

- 只有已中断可以恢复；
- 恢复后进入维修中；
- 恢复时间写 process；
- flow 正确。

### 提交维修结果

- 维修中可以提交；
- 返工中可以提交；
- 已中断不能提交；
- 待确认不能重复提交；
- 提交后状态为待确认；
- `repair_submit_time` 正确；
- 不提前设置 `complete_time`。

### 权限

- 维修人员A可以操作自己的工单；
- 维修人员B不能操作A的工单；
- 身份来自 UserContext；
- 前端传 workerId 无法绕过权限。

### 数据一致性

- 状态变化均使用条件 UPDATE；
- flow 与状态更新同事务；
- process 与状态变化保持一致；
- 操作失败不会留下半条业务数据。

## 三十三、Definition of Done

阶段 6 完成后，一张已经派单成功的工单可以完整走完维修人员侧主流程：

```
待接单
↓
维修人员接单
↓
accept_time
complete_deadline
↓
维修中
↓
添加多条维修过程
↓
添加材料记录
↓
可中断
↓
已中断
↓
恢复
↓
维修中
↓
提交维修结果
↓
repair_submit_time
↓
待确认
```

同时满足：

```
负责人权限正确
状态变化正确
非法状态无法操作
维修过程可追溯
材料使用可追溯
中断/恢复可追溯
工单流转可追溯
系统操作可审计
```

阶段 6 完成后，工单已经走到：

```
待确认
```

下一阶段即可进入：

> **阶段 7：学生确认、返工申请、维修评价与多次返工处理。**