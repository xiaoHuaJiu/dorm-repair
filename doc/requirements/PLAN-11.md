# PLAN-11：工单流转与操作日志补齐

## 一、阶段目标

阶段 11 不新增核心业务流程，而是对阶段 4～10 已完成的所有工单操作进行一次完整审计。

重点确认两套记录体系：

```
repair_order_flow
→ 工单生命周期轨迹

sys_operation_log
→ 用户 / 系统操作审计
```

两者职责必须明确分离。

最终要求任意选择一张完整生命周期工单，都能够通过：

```
repair_order
+
repair_order_flow
+
repair_process_record
+
repair_rework_record
+
repair_transfer_request
+
sys_operation_log
```

还原整个处理过程。

## 二、两类日志最终职责

### repair_order_flow

回答：

> 这张工单在业务生命周期中发生了什么？

包括：

```
工单创建
负责人变化
状态变化
转派申请
转派失败
超时重新派单
中断 / 恢复
返工
管理员异常介入
取消
最终完成
```

重点关注：

```
什么时候发生
发生了什么
之前什么状态
之后什么状态
原负责人是谁
新负责人是谁
为什么发生
由什么业务来源触发
```

### sys_operation_log

回答：

> 谁在系统中执行了什么操作？

例如：

```
学生提交报修
维修人员点击接单
维修人员新增材料
维修人员申请转派
管理员审批转派
管理员审批请假
学生提交评价
管理员人工派单
管理员取消工单
```

重点关注：

```
操作人
角色
操作模块
操作类型
业务ID
请求结果
操作时间
```

## 三、Flow 最终记录原则

阶段 11 将 `repair_order_flow` 口径统一为：

> **凡是对工单生命周期有重要业务含义的事件，都进入 Flow。**

因此不再限定为：

```
只有 status 改变
或 current_assignee_id 改变
才能写 flow
```

对于：

```
申请转派
转派失败
管理员异常介入
```

这种虽然可能暂时没有状态变化，但会影响后续理解工单历史的重要事件，也允许写 Flow。

此时可以：

```
from_status = 当前状态
to_status   = 当前状态

old_assignee_id = 当前负责人
new_assignee_id = 当前负责人
```

通过：

```
operation_type
source_type
remark
```

表达具体生命周期事件。

但普通：

```
添加维修过程备注
添加材料
查看详情
提交评价
```

如果不影响生命周期，则不进入 Flow。

## 四、统一 Flow 字段规范

每条 Flow 至少应能够表达：

```
order_id

from_status
to_status

old_assignee_id
new_assignee_id

operation_type
source_type

operator_id
operator_role

remark
operation_time
```

具体字段以最终 DDL 为准。

原则：

```
状态未变化
→ from_status = to_status

负责人未变化
→ old_assignee_id = new_assignee_id
```

不要为了表示事件而制造虚假的状态变化。

## 五、需要补齐的生命周期事件

阶段 11 按照下面清单逐项检查。

### 1. 创建工单

阶段：

```
学生报修
```

Flow：

```
from_status = NULL
to_status = 待派单

old_assignee_id = NULL
new_assignee_id = NULL

operation_type = CREATE_ORDER
```

能够回答：

```
工单什么时候创建？
谁创建的？
```

### 2. 自动派单

```
待派单
→ 待接单
```

Flow：

```
old_assignee_id = NULL
new_assignee_id = workerA

operation_type = AUTO_DISPATCH
source_type = INITIAL_REPORT
```

能够回答：

```
最初派给了谁？
什么时候派的？
```

### 3. 人工派单

管理员人工指定维修人员时：

```
待派单
→ 待接单
```

记录：

```
operation_type = MANUAL_DISPATCH
operator_role = ADMIN
new_assignee_id = worker
```

人工派单必须和自动派单区分。

### 4. 维修人员接单

```
待接单
→ 维修中
```

记录：

```
operation_type = ACCEPT
```

用于回答：

```
什么时候接单？
```

### 5. 接单超时

接单超时本身不是新的 status。

建议先写一条生命周期事件：

```
from_status = 待接单
to_status = 待接单

old_assignee_id = workerA
new_assignee_id = workerA

operation_type = ACCEPT_TIMEOUT
```

随后重新派单产生另一条：

```
AUTO_REASSIGN
```

这样可以明确回答：

```
为什么这张工单突然换维修人员？
```

### 6. 自动转派

例如：

```
接单超时
请假
转派审批通过
```

重新派单成功。

Flow：

```
old_assignee_id = workerA
new_assignee_id = workerB

to_status = 待接单

operation_type = AUTO_REASSIGN
```

同时通过：

```
source_type
```

区分：

```
ACCEPT_TIMEOUT
LEAVE_REASSIGN
TRANSFER_REASSIGN
```

### 7. 维修中断

```
维修中
→ 已中断
```

记录：

```
operation_type = INTERRUPT
```

中断详细原因继续保存在：

```
repair_process_record
```

Flow 只保留：

```
状态变化
操作类型
摘要
```

### 8. 恢复维修

```
已中断
→ 维修中
```

记录：

```
operation_type = RESUME
```

能够回答：

```
什么时候恢复？
```

### 9. 申请转派

即使此时：

```
status 不变
负责人不变
```

仍建议产生生命周期 Flow：

```
operation_type = TRANSFER_REQUEST
```

例如：

```
from_status = 维修中
to_status = 维修中

old_assignee_id = workerA
new_assignee_id = workerA
```

remark：

```
维修人员申请转派：当前无法处理
```

审批详细过程仍然保存：

```
repair_transfer_request
```

### 10. 转派成功

真正重新分配完成时：

```
old_assignee_id = workerA
new_assignee_id = workerB

to_status = 待接单
```

Flow：

```
operation_type = TRANSFER_SUCCESS
```

或者统一：

```
AUTO_REASSIGN
source_type = TRANSFER_REASSIGN
```

建议不要同时生成两条表达同一件事的 Flow。

优先采用：

```
AUTO_REASSIGN
+
TRANSFER_REASSIGN
```

即可。

### 11. 转派失败

管理员已经批准，但重新派单失败：

```
原负责人
→ NULL

status
→ 待派单
```

应记录：

```
operation_type = TRANSFER_FAILED
```

remark：

```
转派审批通过，自动重新派单失败：无可用维修人员
```

这样可以回答：

```
为什么工单进入了待人工派单？
```

### 12. 提交维修结果

```
维修中 / 返工中
→ 待确认
```

记录：

```
operation_type = SUBMIT_RESULT
```

详细维修结果继续由：

```
repair_process_record
```

保存。

### 13. 学生确认完成

```
待确认
→ 已完成
```

记录：

```
operation_type = STUDENT_CONFIRM
```

能够回答：

```
谁确认完成？
什么时候确认？
```

### 14. 学生返工

```
待确认
→ 返工中
```

记录：

```
operation_type = REWORK
```

remark 建议：

```
第N次返工
```

详细原因由：

```
repair_rework_record
```

保存。

### 15. 管理员异常介入

例如：

```
第三次返工
exception_flag = 1
```

管理员开始处理异常工单。

即使暂时：

```
status 不变
```

也应写：

```
operation_type = ADMIN_INTERVENTION
```

记录：

```
operator_id
管理员处理说明
```

能够回答：

```
谁进行了管理员干预？
什么时候介入？
```

### 16. 取消工单

如果支持取消：

```
当前状态
→ 已取消
```

记录：

```
operation_type = CANCEL
```

remark 保存：

```
取消原因摘要
```

完整取消原因仍以：

```
repair_order.cancel_reason
```

为准。

### 17. 最终完成

最终完成实际就是：

```
学生确认
待确认 → 已完成
```

原则上：

```
STUDENT_CONFIRM
```

这一条已经表达最终完成。

不要额外再生成：

```
FINAL_COMPLETE
```

第二条重复 Flow。

如果未来存在：

```
管理员强制完成
```

再使用：

```
ADMIN_COMPLETE
```

单独区分。

## 六、推荐 OperationType 统一枚举

建议整理：

```
CREATE_ORDER
AUTO_DISPATCH
MANUAL_DISPATCH
ACCEPT

ACCEPT_TIMEOUT
AUTO_REASSIGN

INTERRUPT
RESUME

TRANSFER_REQUEST
TRANSFER_REJECT
TRANSFER_FAILED

SUBMIT_RESULT

REWORK
STUDENT_CONFIRM

ADMIN_INTERVENTION
CANCEL
ADMIN_COMPLETE
```

其中：

```
TRANSFER_REJECT
```

是否进入 Flow 可以根据最终口径决定。

推荐：

> 审批驳回作为重要生命周期事件，也保留 Flow。

状态和负责人保持不变：

```
operation_type = TRANSFER_REJECT
```

这样能够完整展示：

```
申请转派
↓
管理员驳回
↓
原负责人继续处理
```

## 七、SourceType 统一整理

对于重新派单：

```
INITIAL_REPORT
ACCEPT_TIMEOUT
TRANSFER_REASSIGN
LEAVE_REASSIGN
MANUAL
```

推荐 `operation_type` 表示：

```
发生了什么
```

`source_type` 表示：

```
为什么发生
```

例如：

```
operation_type = AUTO_REASSIGN
source_type = ACCEPT_TIMEOUT
```

意思：

```
发生了自动重新派单
原因是接单超时
```

这样不要定义几十个：

```
ACCEPT_TIMEOUT_REASSIGN
LEAVE_REASSIGN
TRANSFER_AUTO_REASSIGN
...
```

避免枚举膨胀。

## 八、sys_operation_log 补齐

阶段 11 同时检查阶段 4～10 的用户操作。

建议至少包括：

| 操作               | operation log |
| ------------------ | ------------- |
| 学生创建报修       | ✅             |
| 学生确认重复仍提交 | ✅             |
| 维修人员接单       | ✅             |
| 添加维修过程       | ✅             |
| 添加材料           | ✅             |
| 中断 / 恢复        | ✅             |
| 提交维修结果       | ✅             |
| 转派申请           | ✅             |
| 管理员审批转派     | ✅             |
| 请假申请           | ✅             |
| 管理员审批请假     | ✅             |
| 学生返工           | ✅             |
| 学生确认完成       | ✅             |
| 学生评价           | ✅             |
| 管理员人工派单     | ✅             |
| 管理员异常介入     | ✅             |
| 取消工单           | ✅             |

定时任务自动行为也可以记录：

```
operator_role = SYSTEM
```

但不要把：

```
每次扫描
每次查询
每次提醒检查
```

都写进 operation log。

只记录真正产生业务影响的自动操作。

## 九、Flow 与其他业务记录不要重复承担职责

### repair_process_record

负责：

```
维修过程
中断原因
恢复说明
维修结果
```

### repair_rework_record

负责：

```
每次返工详细原因
图片
返工序号
```

### repair_transfer_request

负责：

```
转派申请
审批结果
审批人
审批意见
执行结果
```

### repair_order_flow

负责：

```
生命周期时间线
```

因此 Flow 中：

```
remark
```

只保存必要摘要。

不要复制完整：

```
维修过程内容
返工图片
审批对象
```

## 十、统一 Flow 写入入口

建议增加一个轻量：

```
RepairOrderFlowService
```

例如：

```
void recordFlow(RepairOrderFlowCommand command);
```

它只负责：

```
组装 flow
INSERT
```

不要把状态机、派单等复杂逻辑放进该 Service。

后续所有业务 Service：

```
创建
派单
接单
中断
返工
确认
...
```

统一调用。

这样避免每个模块自己拼一套：

```
operator
oldAssignee
newAssignee
operationTime
```

## 十一、统一时间来源

同一次业务操作：

```
repair_order 更新时间
repair_order_flow.operation_time
process record时间
operation log时间
```

尽量共用：

```
LocalDateTime now = LocalDateTime.now();
```

不要一个业务方法内部连续调用多个：

```
LocalDateTime.now()
```

导致时间线出现不必要的毫秒差异。

## 十二、历史数据一致性检查

阶段 11 不只看代码，还需要检查已经存在的开发测试数据。

重点排查：

```
repair_order 已经发生状态变化
但没有对应 flow

current_assignee_id 已经变化
但 flow 没有记录原负责人

rework_count > 0
但没有对应 REWORK flow

存在 transfer_request 已通过
但没有重新派单 flow

status = 已完成
但没有 STUDENT_CONFIRM flow
```

如果目前仍是开发环境，可以：

```
清理测试数据
重新走完整流程
```

优先于编写复杂历史补数据脚本。

## 十三、完整生命周期验收用例

建议专门构造一张复杂工单：

```
学生创建报修
↓
自动派给维修人员A
↓
A接单
↓
A添加维修记录
↓
A中断维修
↓
A恢复维修
↓
A申请转派
↓
管理员审批通过
↓
重新派给维修人员B
↓
B接单
↓
B提交维修结果
↓
学生第一次返工
↓
B再次维修
↓
B提交结果
↓
学生第二次返工
↓
管理员收到预警
↓
B再次提交
↓
学生确认完成
↓
学生评价
```

然后按照：

```
operation_time ASC
id ASC
```

查询：

```
repair_order_flow
```

必须能够清楚还原整个工单生命周期。

## 十四、核心问题验收

针对上述工单，仅通过业务数据必须能够回答以下问题。

### 最初是谁负责？

通过：

```
AUTO_DISPATCH
```

找到：

```
new_assignee_id
```

### 什么时候接单？

通过：

```
ACCEPT
```

找到：

```
operation_time
```

并可与：

```
repair_order.accept_time
```

交叉验证。

### 为什么转派？

通过：

```
TRANSFER_REQUEST
```

查看：

```
remark
```

详细原因再关联：

```
repair_transfer_request
```

### 转给了谁？

通过：

```
AUTO_REASSIGN
```

读取：

```
old_assignee_id
new_assignee_id
```

### 什么时候中断？

通过：

```
INTERRUPT
```

查看时间。

详细原因：

```
repair_process_record
```

### 什么时候恢复？

通过：

```
RESUME
```

查看时间。

### 返工了几次？

当前：

```
repair_order.rework_count
```

给出累计次数。

历史：

```
repair_rework_record
+
REWORK flow
```

能够逐次核对。

### 谁进行了管理员干预？

通过：

```
ADMIN_INTERVENTION
```

查看：

```
operator_id
operation_time
remark
```

## 十五、数据一致性校验

阶段 11 建议增加开发阶段核对 SQL。

例如：

```
已完成工单
但不存在完成 flow
rework_count
与 repair_rework_record COUNT 不一致
当前负责人
与最后一次负责人变化 flow 不一致
存在接单时间
但不存在 ACCEPT flow
```

这些 SQL 主要用于：

```
开发验收
数据排查
```

不需要建设成正式线上定时任务。

## 十六、前端工单时间线

阶段 3 已经存在：

```
flows
```

阶段 11 可以统一时间线展示。

例如：

```
09:00 学生提交报修

09:01 系统自动派单
      张师傅

09:10 张师傅接单

10:30 维修中断
      等待配件

14:00 恢复维修

15:00 张师傅申请转派
      需要其他工种

15:10 管理员审批通过

15:10 系统重新派单
      张师傅 → 李师傅

16:30 李师傅提交维修结果

17:00 学生申请第1次返工

次日10:00 再次提交维修结果

10:30 学生确认完成
```

学生端是否展示：

```
内部管理员操作
转派失败技术原因
```

可以通过 VO 做角色裁剪。

数据库 Flow 本身保存完整历史。

## 十七、阶段 11 不做的内容

本阶段不要继续扩展成：

```
事件总线
Event Sourcing
审计中心
日志大数据平台
MQ异步日志
统一行为埋点系统
```

当前继续使用：

```
业务 Service
↓
同事务写 repair_order_flow
+
sys_operation_log
```

即可。

## 十八、推荐开发顺序

```
11-01 整理 RepairOrderFlowOperationTypeEnum

11-02 整理 DispatchSourceType

11-03 实现轻量 RepairOrderFlowService

11-04 核对学生报修 Flow

11-05 核对自动 / 人工派单 Flow

11-06 核对接单 / 接单超时 Flow

11-07 核对自动转派 Flow

11-08 核对中断 / 恢复 Flow

11-09 核对转派申请 / 驳回 / 失败

11-10 核对维修结果提交

11-11 核对返工 / 确认完成

11-12 核对管理员异常介入

11-13 核对取消工单

11-14 核对 sys_operation_log

11-15 全生命周期测试数据

11-16 Flow 时间线查询验证

11-17 数据一致性核对

11-18 前端时间线联调
```

## 十九、估时

| 工作项                   | 估时              |
| ------------------------ | ----------------- |
| Flow 枚举整理            | 0.2 人日          |
| RepairOrderFlowService   | 0.25              |
| 阶段4～10 Flow 核对补齐  | 0.5~0.75          |
| operation log 核对       | 0.25~0.5          |
| 全生命周期联调           | 0.5               |
| 一致性 / 越权 / 历史测试 | 0.25~0.5          |
| **合计**                 | **约 2~2.5 人日** |

基本符合：

```
阶段11预计2人日
```

## 二十、阶段验收清单

### Flow 完整性

- 

  创建工单有 Flow；

- 

  自动派单有 Flow；

- 

  人工派单有 Flow；

- 

  接单有 Flow；

- 

  接单超时有 Flow；

- 

  自动转派有 Flow；

- 

  中断有 Flow；

- 

  恢复有 Flow；

- 

  转派申请有 Flow；

- 

  转派审批结果可追踪；

- 

  转派成功有负责人变化 Flow；

- 

  转派失败有 Flow；

- 

  提交维修结果有 Flow；

- 

  学生返工有 Flow；

- 

  学生确认完成有 Flow；

- 

  管理员异常介入有 Flow；

- 

  取消工单有 Flow；

- 

  最终完成可以明确追溯。

### Flow 数据正确性

- 

  from_status 正确；

- 

  to_status 正确；

- 

  old_assignee_id 正确；

- 

  new_assignee_id 正确；

- 

  operation_type 正确；

- 

  source_type 正确；

- 

  operator_id 正确；

- 

  operation_time 正确。

### Operation Log

- 

  关键用户写操作都有操作日志；

- 

  自动系统业务操作能够识别 SYSTEM；

- 

  普通查询不产生大量无意义日志；

- 

  operation log 不替代业务 Flow。

### 生命周期还原

选择一张完整工单必须能够回答：

```
谁创建？
最初派给谁？
什么时候派单？
什么时候接单？
什么时候中断？
为什么中断？
什么时候恢复？
什么时候申请转派？
为什么转派？
管理员如何审批？
原负责人是谁？
新负责人是谁？
为什么自动重新派单？
返工多少次？
什么时候最终完成？
谁进行了管理员介入？
```

## 二十一、Definition of Done

阶段 11 完成后：

```
repair_order
```

负责描述：

> 工单现在是什么状态、当前负责人是谁。

```
repair_order_flow
```

负责描述：

> 工单从创建到结束经历了哪些关键生命周期事件。

```
sys_operation_log
```

负责描述：

> 谁在系统中执行了什么操作。

三者职责清晰：

```
当前态
→ repair_order

生命周期
→ repair_order_flow

用户 / 系统审计
→ sys_operation_log
```

任意一张完整生命周期工单都可以通过 Flow 清晰回答：

```
最初是谁负责？
什么时候接单？
为什么转派？
转给了谁？
什么时候中断？
什么时候恢复？
返工了几次？
谁进行了管理员干预？
最后什么时候完成？
```

并能够通过其他业务记录继续下钻查看：

```
具体维修过程
具体返工原因
具体转派审批
具体材料使用
具体评价内容
```

至此，整个工单系统不仅能够“正确运行”，还能够做到：

> **每一次重要状态变化、负责人变化和生命周期事件都有据可查。**