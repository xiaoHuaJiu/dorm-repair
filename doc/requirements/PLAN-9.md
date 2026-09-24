# PLAN-9：维修人员请假

## 一、阶段目标

阶段 9 完成维修人员请假申请、管理员审批、请假正式生效、未完成工单自动转派以及请假结束后的状态恢复。

请假必须拆成两个独立过程：

```
第一部分：请假审批
维修人员申请
↓
管理员审批

第二部分：请假生效
到达 start_time
↓
定时任务触发
↓
维修人员进入请假中
↓
已有未完成工单重新派单
```

核心原则：

> **审批通过 ≠ 请假已经开始。**

例如：

```
9月24日审批通过
请假时间：
9月26日08:00
~
9月28日18:00
```

在：

```
9月24日
~
9月26日08:00之前
```

维修人员仍然：

```
正常工作
可以继续接收新工单
```

只有到达：

```
start_time
```

之后才正式进入请假状态。

## 二、阶段范围

本阶段实现：

```
维修人员请假申请
重复时间段校验
管理员审批
审批幂等
请假生效定时任务
维修人员 work_status 切换
请假期间退出自动派单候选池
查询当前人员未完成工单
逐单调用 RepairDispatchService
reassign_status 状态管理
部分转派失败处理
管理员异常提醒
请假结束自动恢复
定时任务幂等
请假历史查询
操作日志
```

本阶段暂不实现：

```
复杂排班系统
按小时请假工资计算
销假审批
请假撤回
请假期间临时返岗
```

如后续有需求再扩展。

## 三、请假状态与转派状态分开

建议 `repair_leave_request` 至少保留两个维度。

### 3.1 审批状态

例如：

```
approval_status

0 待审批
1 已通过
2 已驳回
```

表示：

> 管理员是否同意这次请假。

### 3.2 转派处理状态

例如：

```
reassign_status

0 未处理
1 处理中
2 已完成
3 部分失败
```

表示：

> 请假正式开始后，该维修人员已有工单是否已经完成重新派单。

两个字段不能混成一个状态。

## 四、9.1 维修人员提交请假申请

### 4.1 接口设计

建议：

```
POST /worker/leave-requests
```

请求：

```
CreateLeaveRequestDTO
```

字段：

```
bizNo
startTime
endTime
reason
```

维修人员身份：

```
workerId
```

必须通过：

```
UserContext
↓
sys_user.id
↓
repair_worker.id
```

获取。

前端不得指定。

### 4.2 时间校验

至少：

```
startTime 非空
endTime 非空

startTime < endTime
```

原则上：

```
endTime > 当前时间
```

如果允许临时申请已经开始的请假，也可以允许：

```
startTime <= now
```

管理员审批通过后立即进入生效任务。

当前第一期无需限制：

```
请假最长多少天
```

除非业务明确要求。

## 五、重复时间段请假校验

同一维修人员不能存在时间重叠的有效请假。

需要检查：

```
worker_id = 当前workerId
```

并且请假记录属于：

```
待审批
或
已通过
```

已驳回申请不参与冲突判断。

时间区间冲突条件：

```
existing.start_time < newEndTime
AND
existing.end_time > newStartTime
```

如果命中：

```
HTTP 409
```

例如：

```
已有：
9月25日08:00
~
9月27日18:00

新申请：
9月26日08:00
~
9月28日18:00
```

必须拒绝。

### 5.1 边界不重叠

例如：

```
旧：
08:00 ~ 12:00

新：
12:00 ~ 18:00
```

如果定义时间区间为：

```
[startTime, endTime)
```

则：

```
允许
```

推荐统一采用这个边界语义，避免两条记录在交界时间被误判重叠。

## 六、创建请假申请的幂等

请假申请属于关键创建操作，建议继续沿用：

```
bizNo
```

解决：

```
重复点击
网络重试
响应丢失
```

导致重复申请。

流程：

```
bizNo 幂等
↓
校验当前维修人员
↓
时间参数校验
↓
时间段冲突检查
↓
INSERT repair_leave_request
```

同一次：

```
bizNo
```

重复请求：

```
不能重复生成请假申请
```

## 七、9.2 管理员审批

### 7.1 管理员查询

建议：

```
GET /admin/leave-requests
```

支持：

```
pageNum
pageSize
workerId
approvalStatus
startTime
endTime
```

管理员详情：

```
GET /admin/leave-requests/{id}
```

### 7.2 审批接口

建议：

```
POST /admin/leave-requests/{id}/review
```

请求：

```
ReviewLeaveRequestDTO
```

字段：

```
bizNo
action
remark
```

其中：

```
action = APPROVE / REJECT
```

### 7.3 审批幂等

管理员审批继续使用：

```
bizNo 幂等
+
approval_status CAS
```

只有：

```
待审批
```

才能变成：

```
已通过
或
已驳回
```

两个管理员并发审批：

```
只有一个成功
```

另一个返回：

```
HTTP 409
```

## 八、审批驳回

管理员驳回：

```
approval_status = 已驳回
review_admin_id = 当前管理员
review_time = now
review_remark = xxx
```

除此之外：

```
repair_worker.work_status
不变
```

也不处理：

```
已有工单
```

即：

> 驳回请假不能影响维修人员当前工作状态。

## 九、审批通过

管理员通过后：

```
approval_status = 已通过
```

但不要立即：

```
work_status = 请假中
```

除非：

```
当前时间已经 >= start_time
```

普通未来请假：

```
审批通过
↓
等待 start_time
```

此时：

```
repair_worker.work_status
仍然保持正常
```

自动派单仍可以选择该维修人员。

## 十、审批通过且请假已开始

如果管理员审批时：

```
start_time <= now < end_time
```

说明：

> 请假时间已经到达。

此时不需要等待下一次很久后的任务。

可以：

```
审批事务完成
↓
由定时任务下一轮立即识别
```

如果定时任务频率较高，例如：

```
每1分钟
```

即可接受。

第一期不必在审批 Service 里再复制一套生效逻辑。

## 十一、9.3 请假正式生效定时任务

建议独立：

```
RepairLeaveEffectiveTask
```

职责只负责：

```
找到已经审批通过
并且已经到达 start_time
但尚未处理转派的请假
```

### 11.1 扫描条件

核心查询：

```
approval_status = 已通过

reassign_status = 0

start_time <= now

end_time > now
```

使用：

```
idx_leave_start_task
```

### 11.2 为什么要求 end_time > now

避免系统停机较长时间后恢复：

```
请假其实已经结束
```

但任务仍把维修人员改成：

```
请假中
```

如果：

```
end_time <= now
```

说明该请假已经不再需要进入实际请假状态。

这类历史遗漏可以直接标记处理完成或按补偿逻辑处理。

## 十二、请假生效第一步：人员状态变更

正式进入请假区间：

```
repair_worker.work_status
=
请假中
```

从这一刻开始：

```
RepairDispatchService
```

必须把该维修人员从候选池排除。

阶段 5 已经预留：

```
停用 / 请假
```

候选过滤规则。

## 十三、请假生效第二步：reassign_status

开始处理前：

```
reassign_status
0 → 1
```

即：

```
未处理
→
处理中
```

这个状态用于防止：

```
下一轮定时任务
```

再次重复执行同一张请假单。

## 十四、9.4 查询维修人员未完成工单

根据：

```
worker_id
```

查询：

```
repair_order
```

条件：

```
current_assignee_id = 当前workerId
AND
status IN 未完成且需要转派状态
```

当前建议范围：

```
待接单
维修中
返工中
已中断
```

使用：

```
idx_order_assignee_status
```

### 14.1 待确认是否转派

建议：

```
待确认
```

不进行转派。

原因：

> 维修人员已经提交维修结果，目前等待学生确认，不再需要维修人员继续现场处理。

因此请假转派主要针对：

```
待接单
维修中
返工中
已中断
```

## 十五、逐单重新派单

不能把维修人员全部工单一次性：

```
批量 UPDATE 给同一个新人
```

因为每张工单：

```
故障类型不同
位置不同
技能要求不同
负责区域不同
候选人工作量不同
```

所以必须：

```
逐张工单调用 RepairDispatchService
```

例如：

```
dispatch(
    orderId,
    Set.of(leaveWorkerId),
    DispatchSourceType.LEAVE_REASSIGN
);
```

## 十六、原请假人员必须排除

虽然当前维修人员已经：

```
work_status = 请假中
```

理论上自动派单已经会过滤。

但调用时仍然明确：

```
excludeWorkerIds
=
[leaveWorkerId]
```

作为本轮业务规则的第二层保障。

## 十七、单工单转派成功

例如：

```
原负责人 = workerA
```

重新派单选择：

```
workerB
```

则：

```
current_assignee_id = workerB

status = 待接单

dispatch_time = now

accept_deadline = 新一轮接单截止时间
```

并由：

```
RepairDispatchService
```

写：

```
repair_order_flow
```

来源：

```
LEAVE_REASSIGN
```

## 十八、单工单转派失败

如果没有其他候选维修人员：

```
status = 待派单

current_assignee_id = NULL
```

然后：

```
管理员待办 / 异常提醒
```

不能重新留给已经请假的原负责人。

## 十九、9.5 reassign_status 最终状态

所有目标工单执行结束后统计结果。

### 全部成功

```
reassign_status = 2
```

表示：

```
已完成
```

例如：

```
4张未完成工单
4张全部重新派单成功
```

### 没有未完成工单

也应：

```
reassign_status = 2
```

因为：

> 不存在需要转派的数据，本次处理已经完成。

### 部分失败

例如：

```
5张工单

3张成功
2张失败
```

结果：

```
reassign_status = 3
```

同时：

```
生成管理员提醒
```

失败的两张工单已经：

```
status = 待派单
current_assignee_id = NULL
```

管理员后续人工处理。

### 全部失败

同样：

```
reassign_status = 3
```

不需要再增加：

```
4 = 全部失败
```

第一期保持简单。

## 二十、部分失败记录

为了管理员知道哪些工单失败，可以依赖：

```
repair_order 当前状态
+
管理员提醒记录
+
repair_order_flow
```

如果 `repair_leave_request` 当前已有：

```
reassign_remark
```

或结果字段，也可以记录摘要：

```
总计5张
成功3张
失败2张
```

第一期不需要专门增加：

```
leave_reassign_detail
```

明细表。

因为单工单本身已经有完整流转历史。

## 二十一、定时任务幂等

这是阶段 9 最重要的技术点之一。

同一请假单：

```
不能因为定时任务重复扫描
导致工单重复转派
```

主要依赖：

```
reassign_status
```

状态控制。

扫描只处理：

```
reassign_status = 0
```

开始执行时首先：

```
0 → 1
```

成功抢到：

```
才执行后续转派
```

### 21.1 CAS 抢任务

建议：

```
UPDATE repair_leave_request
SET reassign_status = 1
WHERE id = #{leaveId}
  AND approval_status = #{approved}
  AND reassign_status = 0
```

检查：

```
affectedRows
```

如果：

```
1
```

说明：

```
当前任务获得执行权
```

如果：

```
0
```

说明：

```
已经被其他线程处理
```

直接跳过。

不需要额外：

```
Redis分布式锁
```

## 二十二、处理中任务异常怎么办

需要避免：

```
reassign_status = 1
```

永久卡死。

例如：

```
任务执行中应用宕机
```

此时重启后：

```
status 永远不是0
```

普通扫描不会再处理。

第一期建议：

```
增加处理开始时间
或复用 update_time
```

定时任务同时补偿：

```
reassign_status = 1
AND
update_time < now - 超时阈值
```

认为：

```
上次任务异常中断
```

允许重新恢复执行。

## 二十三、恢复执行不会重复转派

恢复任务时不能假设：

```
所有工单都没处理
```

应该重新查询：

```
current_assignee_id = leaveWorkerId
AND
status IN 需要转派状态
```

已经成功转走的工单：

```
current_assignee_id
已经不是 leaveWorkerId
```

自然不会再次匹配。

因此：

```
任务重试
```

仍然可以做到幂等。

## 二十四、单工单派单异常隔离

请假人员可能有：

```
5张未完成工单
```

如果第2张派单失败：

> 不应停止后面3张。

正确流程：

```
工单1 → 成功
工单2 → 失败，记录
工单3 → 继续
工单4 → 继续
工单5 → 继续
```

最后统一统计：

```
成功数
失败数
```

再决定：

```
reassign_status = 2 / 3
```

## 二十五、事务边界

不建议：

```
一个请假单
+
全部N张工单重新派单
```

放进一个巨大事务。

否则：

```
最后一张失败
```

可能导致前面所有成功转派全部回滚。

更合理：

```
请假任务领取
↓
逐张工单独立调用 RepairDispatchService
↓
各工单独立完成派单事务
↓
最后更新 reassign_status
```

符合：

> 部分成功 / 部分失败允许存在。

## 二十六、9.6 请假结束恢复

还需要独立定时处理：

```
end_time <= now
```

的已批准请假。

建议：

```
RepairLeaveEndTask
```

或者和生效任务放在同一个：

```
RepairLeaveScheduleTask
```

中两个步骤执行。

### 26.1 恢复条件

查找：

```
approval_status = 已通过
AND
end_time <= now
```

并且当前维修人员：

```
work_status = 请假中
```

恢复：

```
work_status = 正常
```

### 26.2 恢复后效果

从恢复时刻开始：

```
RepairDispatchService
```

再次允许：

```
把新工单派给该维修人员
```

已经在请假期间成功转给别人的旧工单：

```
不会自动转回来
```

## 二十七、连续请假的边界

需要注意：

```
请假A结束
↓
紧接着请假B开始
```

例如：

```
A：
9月24日08:00
~
9月25日08:00

B：
9月25日08:00
~
9月26日08:00
```

如果时间段冲突校验允许首尾相接：

```
A.end = B.start
```

那么恢复任务不能简单：

```
A结束
→ work_status = 正常
```

还需要检查：

> 当前时刻是否还有另一张已审批且正在生效的请假。

如果存在：

```
仍然保持 请假中
```

只有不存在任何当前生效请假时：

```
恢复正常
```

## 二十八、自动派单候选过滤

阶段 5 的候选过滤最终应满足：

```
repair_worker.work_status = 正常
```

并且必要时继续检查：

```
当前是否存在有效请假
```

这样即使：

```
定时任务延迟几十秒
```

也不会错误派给当前已进入请假时间的维修人员。

即：

```
work_status
+
有效请假记录
```

构成双保险。

## 二十九、管理员提醒

以下情况必须提醒管理员：

```
部分工单转派失败
全部工单转派失败
请假转派出现可识别业务异常
```

提醒至少包含：

```
维修人员
请假开始时间
请假结束时间
需转派工单数量
成功数量
失败数量
失败工单
```

例如：

```
张师傅请假已生效，共需转派5张工单，
其中3张自动转派成功，2张失败，
失败工单已进入待人工派单，请及时处理。
```

## 三十、操作日志与 Flow

### 提交请假

```
operation log
```

不写：

```
repair_order_flow
```

### 审批

```
operation log
```

不涉及单张工单状态时不写 flow。

### 请假生效

维修人员状态变化写：

```
operation log
```

### 已有工单转派

每张工单的：

```
repair_order_flow
```

统一由：

```
RepairDispatchService
```

写入。

来源：

```
LEAVE_REASSIGN
```

### 请假结束

恢复人员状态：

```
operation log
```

## 三十一、接口与任务清单

维修人员：

```
POST /worker/leave-requests

GET /worker/leave-requests

GET /worker/leave-requests/{id}
```

管理员：

```
GET /admin/leave-requests

GET /admin/leave-requests/{id}

POST /admin/leave-requests/{id}/review
```

定时任务：

```
RepairLeaveEffectiveTask

RepairLeaveEndTask
```

或合并：

```
RepairLeaveScheduleTask
```

内部两个处理步骤。

## 三十二、重点验收场景

### 重复时间段

已有：

```
待审批 / 已通过
9月25日 ~ 9月27日
```

再次申请重叠区间：

```
HTTP 409
```

### 已驳回历史不影响新申请

旧申请：

```
已驳回
```

即使时间重叠：

```
允许重新申请
```

### 审批驳回

结果：

```
approval_status = 已驳回
```

维修人员：

```
work_status 不变
```

已有工单：

```
不转派
```

### 未来请假审批通过

当前：

```
now < start_time
```

管理员审批通过。

结果：

```
approval_status = 已通过

work_status = 正常
```

维修人员仍然可以：

```
接单
参与自动派单
```

### 请假正式开始

到达：

```
start_time
```

定时任务执行：

```
work_status = 请假中
reassign_status = 处理中
```

之后：

```
不能再进入自动派单候选池
```

### 没有未完成工单

请假生效时：

```
查询未完成工单 = 0
```

结果：

```
reassign_status = 已完成
```

### 已有未完成工单

例如：

```
4张
```

逐张调用：

```
RepairDispatchService
```

全部成功：

```
reassign_status = 已完成
```

### 部分失败

例如：

```
5张
成功3
失败2
```

结果：

```
reassign_status = 部分失败
```

失败工单：

```
status = 待派单
current_assignee_id = NULL
```

并生成管理员提醒。

### 请假人员必须被排除

即使其工作量：

```
0
```

技能和区域：

```
全部匹配
```

只要：

```
当前处于请假区间
```

就不能被 RepairDispatchService 选中。

### 定时任务重复执行

同一 leaveId：

```
第一次扫描
→ 0 → 1
→ 开始处理

第二个任务线程再次扫描
```

不能再次获取执行权。

最终：

```
每张工单只发生一次有效请假转派
```

### 任务处理中应用异常

部分工单已经成功转派后：

```
应用宕机
```

恢复任务再次执行：

```
只处理仍然 current_assignee_id = 请假人员
```

的工单。

已经成功转走的工单：

```
不会重复转派
```

### 请假结束

到达：

```
end_time
```

且没有其他生效请假：

```
work_status
请假中 → 正常
```

维修人员重新进入候选池。

### 连续请假

A：

```
结束时间 = 9月26日08:00
```

B：

```
开始时间 = 9月26日08:00
```

A结束任务执行时发现 B 已生效：

```
不恢复正常
```

继续保持：

```
请假中
```

## 三十三、索引验证

本阶段核心 SQL 必须检查：

### 请假正式开始扫描

使用：

```
idx_leave_start_task
```

目标条件：

```
approval_status
reassign_status
start_time
```

### 查询维修人员未完成工单

使用：

```
idx_order_assignee_status
```

目标：

```
current_assignee_id
+
status
```

使用：

```
EXPLAIN
```

确认执行计划。

## 三十四、推荐开发顺序

```
9-01 LeaveApprovalStatusEnum

9-02 LeaveReassignStatusEnum

9-03 CreateLeaveRequestDTO

9-04 请假时间校验

9-05 重复时间段查询

9-06 创建请假 bizNo 幂等

9-07 维修人员请假申请

9-08 维修人员请假列表 / 详情

9-09 管理员请假列表 / 详情

9-10 审批 bizNo 幂等

9-11 审批 CAS

9-12 审批驳回

9-13 审批通过

9-14 RepairLeaveEffectiveTask

9-15 reassign_status CAS 抢任务

9-16 work_status → 请假中

9-17 查询未完成工单

9-18 逐单调用 RepairDispatchService

9-19 汇总转派结果

9-20 reassign_status = 完成 / 部分失败

9-21 管理员失败提醒

9-22 处理中异常补偿

9-23 RepairLeaveEndTask

9-24 work_status 恢复正常

9-25 连续请假判断

9-26 自动派单请假过滤联调

9-27 EXPLAIN

9-28 定时任务幂等测试

9-29 全链路验收
```

## 阶段验收清单

### 请假申请

- 

  维修人员只能给自己申请；

- 

  时间合法；

- 

  待审批/已通过的重叠区间被拦截；

- 

  已驳回记录不阻止重新申请；

- 

  同一 bizNo 不重复创建。

### 审批

- 

  只有待审批可以处理；

- 

  审批幂等；

- 

  驳回不改变人员状态；

- 

  驳回不转派工单；

- 

  未来请假审批通过后仍保持正常状态。

### 请假生效

- 

  到达 start_time 后进入请假中；

- 

  `reassign_status 0 → 1`；

- 

  请假人员退出自动派单候选池；

- 

  查询该人员需要转派的未完成工单。

### 自动转派

- 

  每张工单独立调用 RepairDispatchService；

- 

  原请假人员必须排除；

- 

  成功后新负责人进入待接单；

- 

  新 accept_deadline 正确；

- 

  flow 来源为 LEAVE_REASSIGN；

- 

  失败工单进入待派单。

### 转派结果

- 

  全部成功 → reassign_status=2；

- 

  无工单需要处理 → reassign_status=2；

- 

  部分失败 → reassign_status=3；

- 

  全部失败 → reassign_status=3；

- 

  失败产生管理员提醒。

### 定时任务

- 

  同一请假不会被两个任务重复执行；

- 

  reassign_status CAS 生效；

- 

  任务异常后能够恢复；

- 

  已经成功转走的工单不会再次转派。

### 请假结束

- 

  end_time 到达后恢复正常；

- 

  连续请假时不会错误恢复；

- 

  恢复后可以重新参与自动派单；

- 

  请假期间转出的旧工单不会自动转回来。

## Definition of Done

阶段 9 完成后，一次请假能够完整走完：

```
维修人员申请
↓
时间冲突检查
↓
管理员审批
↓
审批通过
↓
未来时间尚未开始
→ 维修人员继续正常工作
↓
到达 start_time
↓
work_status = 请假中
↓
reassign_status = 处理中
↓
查未完成工单
↓
逐张调用 RepairDispatchService
↓
成功的工单重新分配
↓
失败的工单进入待派单并通知管理员
↓
reassign_status = 已完成 / 部分失败
↓
到达 end_time
↓
如果没有其他生效请假
→ work_status = 正常
↓
重新进入自动派单候选池
```

并确保：

```
审批通过不等于立即请假
请假开始后一定退出候选池
已有工单不会遗留给请假人员
自动转派失败一定有人知道
任务重复执行不会重复转派
请假结束能够正确恢复
```

阶段 9 完成后，可以进入：

> **阶段 10：接单提醒、接单超时、维修超时与统一超时任务。**