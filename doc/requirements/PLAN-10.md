# PLAN-10：超时、提醒与定时任务

## 一、阶段目标

阶段 10 建立统一的定时任务和提醒能力，补齐工单运行过程中的超时处理。

本阶段主要包括：

```
接单剩余10分钟提醒
接单剩余5分钟提醒
接单到期超时

维修完成剩余10分钟提醒
维修完成剩余5分钟提醒
维修完成到期超时

请假开始
请假结束
```

其中：

```
接单超时
→ 自动重新派单
```

而：

```
维修完成超时
→ 不修改工单 status
```

因为：

> 超时是工单当前状态下的附加业务事实，不是新的业务状态。

## 二、阶段范围

本阶段实现：

1. 接单提醒定时任务；
2. 接单超时处理；
3. 维修完成提醒；
4. 维修完成超时提醒；
5. `repair_reminder_record` 防重复；
6. 提醒接收人控制；
7. 接单超时调用 `RepairDispatchService`；
8. 转派失败管理员兜底；
9. 请假开始定时任务正式纳入统一调度体系；
10. 请假结束定时任务正式纳入统一调度体系；
11. 定时任务重复执行安全；
12. 构造时间数据完成自动化验收。

本阶段不重新实现：

```
自动派单算法
请假审批
维修人员转派审批
工单主流程
评价 / 返工
```

全部复用前面阶段已经完成的 Service。

## 三、统一定时任务设计原则

第一期不建设复杂调度平台。

继续使用：

```
Spring @Scheduled
+
MySQL 条件查询
+
数据库唯一约束
```

即可。

建议把相关任务放在：

```
task
```

包下。

例如：

```
RepairAcceptReminderTask

RepairAcceptTimeoutTask

RepairCompleteReminderTask

RepairCompleteTimeoutTask

RepairLeaveEffectiveTask

RepairLeaveEndTask
```

如果实现时希望减少类数量，也可以合并提醒扫描，但不要把所有逻辑写进一个巨大任务类。

## 四、定时扫描频率

提醒粒度是：

```
10分钟
5分钟
0分钟
```

因此建议：

```
每1分钟扫描一次
```

例如：

```
0 * * * * ?
```

实际 Cron 按 Spring 配置。

不需要：

```
每秒扫描
```

也不要依赖：

```
任务恰好在 deadline 那一秒执行
```

提醒查询应该按照时间窗口判断。

## 五、提醒记录表

所有已经发送或已经生成的提醒统一写：

```
repair_reminder_record
```

核心作用：

> 防止定时任务反复扫描时，向同一个接收人重复发送同一级别提醒。

数据库唯一约束：

```
UNIQUE(
    order_id,
    reminder_type,
    reminder_level,
    deadline_time,
    receiver_id
)
```

这是整个提醒模块的最终幂等保证。

## 六、提醒类型与等级

建议建立：

```
ReminderTypeEnum
```

至少：

```
ACCEPT
COMPLETE
```

分别表示：

```
接单提醒
维修完成提醒
```

提醒等级：

```
ReminderLevelEnum
```

例如：

```
MINUS_10
MINUS_5
TIMEOUT
```

语义分别是：

```
剩余10分钟
剩余5分钟
已到截止时间
```

不要直接在代码中散落：

```
10
5
0
```

## 七、10.1 接单提醒

接单提醒只针对：

```
status = 待接单
```

并且：

```
accept_deadline IS NOT NULL
```

### 7.1 剩余10分钟提醒

目标：

```
accept_deadline - now
≈ 10分钟
```

由于任务每分钟扫描一次，不建议使用：

```
accept_deadline = now + 10min
```

精确相等。

建议按时间窗口查询。

例如当前扫描时间：

```
scanTime
```

查询：

```
accept_deadline > scanTime + 9min
AND
accept_deadline <= scanTime + 10min
```

或者统一使用：

```
[9分钟, 10分钟]
```

窗口。

配合唯一索引，即使相邻扫描窗口存在轻微重叠也不会重复发送。

### 7.2 剩余5分钟提醒

同理：

```
accept_deadline > scanTime + 4min
AND
accept_deadline <= scanTime + 5min
```

生成：

```
reminder_type = ACCEPT
reminder_level = MINUS_5
```

### 7.3 接单提醒接收人

主要接收：

```
current_assignee_id 对应维修人员
```

如业务需要，管理员也可以同时收到。

```
10分钟 → 维修人员
5分钟 → 维修人员
超时 → 维修人员 + 管理员
```

管理员超时提醒可单独写一条：

```
receiver_id = adminId
```

因此唯一约束仍然能够区分接收人。

### 7.4 已经接单后不再提醒

如果维修人员已经：

```
待接单 → 维修中
```

则：

```
status != 待接单
```

下一轮扫描自然不会命中。

因此不需要额外删除历史 reminder。

## 八、10.2 接单超时

接单超时查询条件：

```
status = 待接单
AND
accept_deadline <= NOW()
```

使用阶段前面已经设计好的：

```
idx_order_accept_timeout
```

### 8.1 超时不是新状态

不能：

```
status = 接单超时
```

接单超时只是：

```
待接单状态
+
accept_deadline 已经过期
```

真正后续动作是：

```
自动重新派单
```

### 8.2 接单超时处理流程

完整流程：

```
扫描到接单超时工单
↓
获取 current_assignee_id
↓
记录原维修人员
↓
调用 RepairDispatchService
↓
excludeWorkerIds 包含原维修人员
↓
sourceType = ACCEPT_TIMEOUT
↓
成功
→ 新维修人员
→ status = 待接单
→ 生成新的 accept_deadline

失败
→ status = 待派单
→ current_assignee_id = NULL
→ 管理员异常待办
```

这里不重新写任何候选算法。

统一复用：

```
RepairDispatchService
```

### 8.3 为什么必须排除原负责人

如果：

```
张师傅已经接单超时
```

重新匹配时仍然允许张师傅进入候选池，就可能出现：

```
超时
↓
重新派单
↓
又派给张师傅
```

因此：

```
excludeWorkerIds
```

必须包含：

```
oldWorkerId
```

### 8.4 接单超时并发保护

可能同时存在：

```
定时任务判断超时
```

以及：

```
维修人员此刻点击接单
```

因此不能：

```
查到超时
↓
无条件转派
```

必须在执行超时转派前再次校验：

```
status = 待接单

current_assignee_id = 原负责人

accept_deadline <= now
```

如果此时维修人员已经成功接单：

```
status = 维修中
```

则当前超时任务：

```
跳过
```

避免误转派。

## 九、接单超时提醒

接单真正超时时，可以先生成：

```
reminder_type = ACCEPT
reminder_level = TIMEOUT
```

接收人至少：

```
原维修人员
管理员
```

然后进行自动重新派单。

如果重新派单成功：

```
新的维修人员
```

后续会围绕新的：

```
accept_deadline
```

重新产生一套：

```
10分钟
5分钟
0分钟
```

提醒。

由于：

```
deadline_time
```

已经变化，因此唯一约束不会误认为是旧一轮提醒。

## 十、10.3 维修完成提醒

维修完成提醒依据：

```
complete_deadline
```

主要针对仍处于维修处理阶段的工单。

建议状态范围：

```
维修中
返工中
已中断
```

是否把：

```
已中断
```

纳入超时，需要按业务规则统一。

当前如果：

```
complete_deadline
```

在中断期间仍然继续计算自然时间，那么：

```
已中断
```

仍应该参与完成超时判断。

第一期建议：

> 只要 `complete_deadline` 仍存在且工单没有进入待确认/已完成/已取消，就继续判断维修超时。

这样最简单。

### 10.1 剩余10分钟

查询：

```
complete_deadline
落在未来10分钟提醒窗口
```

生成：

```
reminder_type = COMPLETE
reminder_level = MINUS_10
```

接收：

```
当前维修人员
```

### 10.2 剩余5分钟

生成：

```
reminder_type = COMPLETE
reminder_level = MINUS_5
```

### 10.3 到期提醒

```
complete_deadline <= now
```

生成：

```
reminder_type = COMPLETE
reminder_level = TIMEOUT
```

接收建议：

```
维修人员
+
管理员
```

## 十一、10.4 维修完成超时

这是本阶段最重要的状态边界之一。

维修超时：

```
NOW() > complete_deadline
```

但是：

> **不修改 repair_order.status。**

例如：

```
status = 维修中
complete_deadline 已过
```

仍然：

```
status = 维修中
```

只是：

```
当前已经维修超时
```

### 11.1 不新增“维修超时”状态

禁止增加：

```
status = 8
维修超时
```

否则会破坏已有业务状态机：

```
维修中
已中断
返工中
```

真实状态。

### 11.2 超时动态判定

阶段 3 管理员列表已经支持：

```
completeTimeout
```

筛选。

动态规则：

```
complete_deadline IS NOT NULL
AND
complete_deadline < now
AND
status 属于未关闭且仍在处理的状态
```

前端需要展示时：

```
completeTimeout = true
```

即可。

### 11.3 超时后仍然可以正常继续维修

例如：

```
维修中
↓
维修超时
```

维修人员仍然可以：

```
添加维修过程
添加材料
提交维修结果
```

不能因为：

```
deadline 已过
```

就把正常维修流程锁死。

## 十二、维修超时管理员处理

完成超时后：

```
不自动换维修人员
```

只做：

```
提醒维修人员
提醒管理员
```

管理员根据情况决定：

```
继续等待
主动联系
后续转派
其他人工处理
```

这和：

```
接单超时
```

不同。

接单超时：

```
自动重新派单
```

完成超时：

```
仅提醒
```

必须区分。

## 十三、防重复提醒

提醒处理统一流程：

```
扫描命中
↓
尝试 INSERT repair_reminder_record
↓
插入成功
→ 发送站内提醒 / 消息

唯一键冲突
→ 说明已经发送
→ 直接跳过
```

这里：

> 不需要在 INSERT 前先 SELECT 判断是否存在。

因为：

```
SELECT
↓
不存在
↓
两个任务同时 INSERT
```

仍然会产生并发竞争。

直接依赖：

```
UNIQUE
```

做最终原子防重复更可靠。

## 十四、提醒记录关键字段

至少：

```
order_id
reminder_type
reminder_level
deadline_time
receiver_id
send_status
send_time
create_time
```

如果消息实际通过站内信发送，可以同时保存：

```
message_id
```

用于关联。

## 十五、为什么 deadline_time 必须参与唯一约束

同一个工单可能多次被重新派单。

例如：

```
第一次：
accept_deadline = 10:30

接单超时重新派单

第二次：
accept_deadline = 11:15
```

如果唯一键只有：

```
order_id
reminder_type
reminder_level
receiver_id
```

那么第二轮：

```
10分钟提醒
```

可能被第一轮记录阻止。

因此必须包含：

```
deadline_time
```

表示：

> 某一次具体截止时间对应的一轮提醒。

## 十六、提醒发送失败

需要区分：

```
提醒记录已创建
```

和：

```
提醒真正发送成功
```

建议：

```
send_status

0 待发送
1 已发送
2 发送失败
```

如果当前只是站内待办，可以在创建记录后直接同步写入消息。

第一期不要因为消息发送失败：

```
回滚工单超时业务处理
```

例如：

```
接单超时自动转派
```

不能因为提醒接口失败而停止转派。

## 十七、定时任务之间的职责分离

建议最终职责：

### RepairAcceptReminderTask

负责：

```
接单10分钟提醒
接单5分钟提醒
```

### RepairAcceptTimeoutTask

负责：

```
接单0分钟超时
+
自动转派
```

### RepairCompleteReminderTask

负责：

```
完成10分钟
完成5分钟
```

### RepairCompleteTimeoutTask

负责：

```
完成0分钟超时提醒
```

### RepairLeaveEffectiveTask

负责：

```
请假开始
```

### RepairLeaveEndTask

负责：

```
请假结束
```

这样职责比一个巨型：

```
RepairScheduleTask
```

更清楚。

## 十八、任务失败隔离

一次扫描可能命中：

```
100张工单
```

其中某一张出现异常：

```
不能导致整个批次停止
```

正确方式：

```
for each order:

    try:
        处理当前工单

    catch:
        记录日志
        形成必要异常提醒
        继续下一张
```

尤其：

```
接单超时自动转派
```

要逐单处理。

## 十九、任务批次大小

第一期业务量不大，可以：

```
每次查询100条
```

例如：

```
LIMIT 100
```

循环分页或下一轮继续扫描。

不要一次：

```
SELECT 全部超时工单
```

避免后期数据量增长后一次任务处理时间过长。

## 二十、接单超时处理的幂等

接单超时任务即使一分钟跑一次，也不能重复转派。

第一层：

```
扫描只命中：
status = 待接单
AND accept_deadline <= now
```

第一次成功重新派单后：

```
dispatch_time
accept_deadline
current_assignee_id
```

都会改变。

如果新 deadline 仍在未来：

```
下一轮不会继续命中超时
```

第二层：

```
RepairDispatchService
```

自身已有：

```
原状态
原负责人
CAS
```

并发保护。

所以不需要额外 Redis 超时锁。

## 二十一、请假任务纳入统一调度

阶段 9 已经完成：

```
RepairLeaveEffectiveTask
RepairLeaveEndTask
```

阶段 10 只需要正式统一定时配置：

```
扫描周期
异常日志
任务开关
批处理大小
```

不要重新实现请假业务。

## 二十二、定时任务配置

建议统一配置：

```
repair:
  task:
    reminder-scan-cron: "0 * * * * ?"
    timeout-scan-cron: "0 * * * * ?"
    leave-scan-cron: "0 * * * * ?"
    batch-size: 100
```

具体命名可以按项目规范调整。

不要把：

```
1分钟
100条
```

散落硬编码在 Java 中。

## 二十三、服务器时间统一

所有：

```
now
```

应由后端统一产生。

不要使用 **前端时间** 判断超时。

建议单次扫描：

```
LocalDateTime scanTime = LocalDateTime.now();
```

本轮任务都使用同一个：

```
scanTime
```

避免循环过程中时间不断变化导致边界结果不一致。

## 二十四、索引使用

### 接单提醒 / 超时

依赖：

```
idx_order_accept_timeout
```

核心：

```
status
+
accept_deadline
```

### 完成提醒 / 超时

依赖：

```
idx_order_complete_timeout
```

核心：

```
status
+
complete_deadline
```

### 请假开始

依赖：

```
idx_leave_start_task
```

### 请假工单重新派单

依赖：

```
idx_order_assignee_status
```

开发完成必须执行：

```
EXPLAIN
```

确认目标查询真正命中索引。

## 二十五、提醒与 operation log / flow 的关系

提醒：

```
不写 repair_order_flow
```

因为提醒本身：

```
没有改变工单状态
没有改变负责人
```

普通提醒也不需要每一条都写：

```
sys_operation_log
```

`repair_reminder_record` 本身已经是提醒业务记录。

### 接单超时自动转派

真正：

```
负责人变化
状态变化
```

则：

```
repair_order_flow
```

由：

```
RepairDispatchService
```

统一写入。

来源：

```
ACCEPT_TIMEOUT
```

### 完成超时

不改变：

```
status
负责人
```

所以：

```
不写 flow
```

只写：

```
repair_reminder_record
```

和管理员/维修人员消息。

## 二十六、提醒消息建议

### 接单剩余10分钟

```
工单 ROxxx 距离接单截止时间还有10分钟，请及时处理。
```

### 接单剩余5分钟

```
工单 ROxxx 距离接单截止时间还有5分钟，请尽快接单。
```

### 接单超时

维修人员：

```
工单 ROxxx 已超过接单时限，系统正在重新派单。
```

管理员：

```
工单 ROxxx 接单超时，系统已启动自动重新派单。
```

### 完成剩余10分钟

```
工单 ROxxx 距离维修截止时间还有10分钟。
```

### 完成剩余5分钟

```
工单 ROxxx 距离维修截止时间还有5分钟，请及时处理。
```

### 维修超时

维修人员：

```
工单 ROxxx 已超过维修截止时间，请尽快完成处理。
```

管理员：

```
工单 ROxxx 已维修超时，请关注处理进度。
```

## 二十七、测试不要等待真实时间

阶段 10 验收不应该真的：

```
等待30分钟
```

测试数据直接构造：

```
accept_deadline = now + 10min
accept_deadline = now + 5min
accept_deadline = now
accept_deadline = now - 1min
```

完成时间同样：

```
complete_deadline = now + 10min
complete_deadline = now + 5min
complete_deadline = now
complete_deadline = now - 1min
```

然后：

```
手动执行 task 方法
```

验证结果即可。

## 二十八、重点验收场景

### 接单10分钟提醒

构造：

```
status = 待接单
accept_deadline = 提醒窗口内
```

执行任务。

结果：

```
生成一条 MINUS_10 reminder
```

再次执行：

```
不生成第二条
```

### 接单5分钟提醒

同理：

```
只生成一次 MINUS_5
```

### 已经接单

如果：

```
status = 维修中
```

即使旧：

```
accept_deadline
```

仍然存在。

也：

```
不能再次发送接单提醒
```

### 接单超时

构造：

```
status = 待接单
accept_deadline < now
```

原负责人：

```
workerA
```

执行：

```
RepairDispatchService(
    orderId,
    [workerA],
    ACCEPT_TIMEOUT
)
```

重新分配成功：

```
workerB
status = 待接单
```

并产生新的：

```
accept_deadline
```

### 接单超时重新派单失败

没有候选人。

结果：

```
status = 待派单
current_assignee_id = NULL
```

同时：

```
管理员异常待办
```

必须存在。

### 超时任务与接单并发

任务查询到：

```
待接单且已超时
```

但维修人员同时完成接单：

```
status → 维修中
```

超时任务再次校验失败：

```
不得继续转派
```

### 完成10分钟提醒

构造：

```
complete_deadline = now + 10min窗口
```

只发送一次。

### 完成5分钟提醒

只发送一次。

### 维修超时

构造：

```
status = 维修中
complete_deadline < now
```

结果：

```
生成超时提醒
```

但：

```
status
仍然 = 维修中
```

### 返工中维修超时

```
status = 返工中
complete_deadline < now
```

如果该轮存在有效 deadline，同样：

```
只提醒
不改变status
```

### 已完成工单

```
status = 已完成
complete_deadline < now
```

不能再产生：

```
维修超时提醒
```

### 防重复

同一任务连续执行：

```
10次
```

唯一约束保证：

```
同一 orderId
+
reminderType
+
reminderLevel
+
deadlineTime
+
receiverId
```

始终只有：

```
1条 reminder
```

### 新一轮派单提醒

第一次：

```
deadline = 10:30
```

已经产生10分钟提醒。

超时转派后：

```
deadline = 11:30
```

新维修人员仍然能够收到新一轮：

```
MINUS_10
```

验证：

```
deadline_time
```

参与唯一键的必要性。

## 二十九、任务异常与恢复

如果某轮定时任务执行中异常：

```
下一分钟继续扫描
```

即可。

因为：

```
提醒
→ 唯一键幂等

接单超时
→ status + deadline + RepairDispatchService CAS

完成超时
→ reminder 唯一键
```

都具备重复执行安全性。

因此阶段 10 不需要额外建设：

```
定时任务执行记录中心
分布式任务锁平台
任务补偿框架
```

第一期保持简单。

## 三十、推荐开发顺序

```
10-01 ReminderTypeEnum

10-02 ReminderLevelEnum

10-03 repair_reminder_record Mapper

10-04 提醒唯一键异常处理

10-05 接单10分钟查询

10-06 接单5分钟查询

10-07 RepairAcceptReminderTask

10-08 接单超时查询

10-09 RepairAcceptTimeoutTask

10-10 ACCEPT_TIMEOUT 接入 RepairDispatchService

10-11 超时重新派单并发校验

10-12 完成10分钟查询

10-13 完成5分钟查询

10-14 RepairCompleteReminderTask

10-15 完成超时查询

10-16 RepairCompleteTimeoutTask

10-17 维修人员提醒

10-18 管理员超时提醒

10-19 请假开始 / 结束任务统一配置

10-20 定时参数配置化

10-21 EXPLAIN

10-22 构造时间测试数据

10-23 防重复提醒测试

10-24 接单超时转派测试

10-25 定时任务重复执行测试

10-26 全链路验收
```

## 阶段验收清单

### 接单提醒

- 

  剩余10分钟提醒正确；

- 

  剩余5分钟提醒正确；

- 

  已接单工单不再提醒；

- 

  相同提醒不会重复发送；

- 

  新一轮派单可以重新提醒。

### 接单超时

- 

  `status=待接单 AND accept_deadline<=now` 正确命中；

- 

  原负责人被排除；

- 

  统一调用 RepairDispatchService；

- 

  成功后生成新负责人；

- 

  新 accept_deadline 正确；

- 

  派单失败进入待派单；

- 

  失败生成管理员待办；

- 

  与接单并发时不会误转派。

### 完成提醒

- 

  剩余10分钟提醒正确；

- 

  剩余5分钟提醒正确；

- 

  完成后不再提醒；

- 

  重复扫描不重复生成提醒。

### 完成超时

- 

  `complete_deadline < now` 可以正确识别；

- 

  超时不修改工单 status；

- 

  维修人员收到超时提醒；

- 

  管理员收到超时提醒；

- 

  超时后仍可继续正常维修。

### 提醒幂等

- 

  UNIQUE 约束生效；

- 

  同一分钟重复执行不重复提醒；

- 

  多实例同时扫描不重复提醒；

- 

  deadline 变化后允许新一轮提醒。

### 定时任务

- 

  一条任务失败不影响其他工单；

- 

  批次大小可配置；

- 

  Cron 可配置；

- 

  任务重复执行安全；

- 

  请假开始任务正常；

- 

  请假结束任务正常。

## Definition of Done

阶段 10 完成后，系统能够自动围绕：

```
accept_deadline
complete_deadline
leave.start_time
leave.end_time
```

持续运行。

接单流程：

```
待接单
↓
剩余10分钟提醒
↓
剩余5分钟提醒
↓
到期
↓
接单超时
↓
排除原维修人员
↓
RepairDispatchService
↓
成功 → 新负责人重新待接单
失败 → 待人工派单 + 管理员兜底
```

维修完成流程：

```
维修中 / 返工中
↓
剩余10分钟提醒
↓
剩余5分钟提醒
↓
到期
↓
标识为动态维修超时
↓
提醒维修人员和管理员
↓
status 保持原业务状态
```

请假：

```
到达 start_time
↓
执行阶段9请假生效逻辑

到达 end_time
↓
执行阶段9恢复逻辑
```

最终保证：

```
提醒不会重复
接单超时一定触发重新派单
重新派单失败一定进入管理员处理
维修超时不污染业务状态机
定时任务可以安全重复执行
```

阶段 10 完成后，整个系统的：

> **报修 → 派单 → 接单 → 维修 → 确认/返工 → 超时提醒 → 异常兜底**

主业务链路基本完整。