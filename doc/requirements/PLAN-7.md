# PLAN-7：学生确认、评价与返工

## 一、阶段目标

阶段 7 承接阶段 6。

阶段 6 完成后，维修人员提交维修结果：

```
维修中 / 返工中
↓
待确认
```

此时工单进入学生处理阶段。

学生可以进行两种核心选择：

```
待确认
├─ 确认完成
│   ↓
│ 已完成
│   ↓
│ 评价
│
└─ 申请返工
    ↓
  返工中
    ↓
原维修人员继续处理
```

本阶段主要完成：

1. 学生确认维修完成；
2. 学生维修评价；
3. 学生申请返工；
4. 返工次数累计；
5. 返工记录留痕；
6. 第 2 次返工管理员预警；
7. 第 3 次及以上返工标记异常；
8. 学生资源归属和状态权限；
9. 幂等与并发控制；
10. 工单流转和操作日志。

阶段完成后，工单已经能够完整走完：

```
学生报修
↓
自动派单
↓
维修人员维修
↓
提交维修结果
↓
学生确认
   ↙          ↘
完成           返工
↓              ↓
评价         再次维修
```

## 二、阶段范围与核心业务原则

本阶段实现：

```
待确认 → 已完成

待确认 → 返工中

repair_evaluation

repair_rework_record

rework_count

exception_flag

管理员返工预警
```

本阶段暂不实现：

```
维修人员主动转派
管理员转派审批
请假
接单超时自动转派
维修超时提醒
复杂异常工单处置
```

这些进入后续阶段。

本阶段需要锁定三个核心原则。

### 1. 返工不创建新工单

返工始终使用：

```
原 repair_order
```

重新进入维修流程。

即：

```
待确认
↓
返工中
```

而不是：

```
旧工单结束
+
新建 repair_order
```

因此：

```
duplicate repair
```

和：

```
rework
```

必须明确区分。

重复报修：

```
两张独立 repair_order
```

返工：

```
仍然是同一张 repair_order
```

### 2. 评价只能发生一次

对应：

```
repair_evaluation
```

数据库：

```
UNIQUE(order_id)
```

保证：

> 同一工单最多只能存在一条评价记录。

应用层做前置判断，数据库唯一索引做最终兜底。

### 3. 学生只能操作自己的工单

所有：

```
确认完成
评价
申请返工
```

都必须满足：

```
repair_order.student_uid
=
UserContext.currentUserId
```

不能依赖前端传：

```
studentUid
```

## 三、7.1 学生确认完成

### 3.1 状态流转

确认前：

```
status = 待确认
```

确认成功：

```
status = 已完成
```

同时写入：

```
confirm_time = now
complete_time = now
```

建议两者使用同一个：

```
LocalDateTime now
```

避免时间出现毫秒级差异。

### 3.2 接口设计

建议：

```
POST /student/repair-orders/{orderId}/confirm
```

请求通常不需要复杂 DTO。

如果允许学生填写确认备注，可增加：

```
ConfirmRepairOrderDTO
```

例如：

```
remark
```

第一期也可以不提供。

### 3.3 确认前置条件

必须满足：

```
工单存在

当前用户是学生

student_uid = 当前学生

status = 待确认
```

如果工单已经：

```
已完成
返工中
维修中
已取消
```

都不能再次确认。

### 3.4 确认状态更新

建议条件更新：

```
UPDATE repair_order
SET
    status = #{completedStatus},
    confirm_time = #{now},
    complete_time = #{now}
WHERE id = #{orderId}
  AND student_uid = #{studentUid}
  AND status = #{waitingConfirmStatus}
  AND deleted = 0
```

通过：

```
affectedRows
```

判断是否成功。

这样可以防止：

```
重复点击确认
确认与返工并发
页面状态过期
```

### 3.5 确认完成后的负责人

工单：

```
current_assignee_id
```

建议暂时保留最后一任维修人员。

不要在完成时：

```
current_assignee_id = NULL
```

因为这个字段还可以用于：

```
历史查询
维修人员统计
评价关联展示
```

工单已完成后是否属于“当前负责人”不再由是否为空判断，而由：

```
status = 已完成
```

判断。

### 3.6 工单流转

确认成功后写：

```
repair_order_flow
```

记录：

```
from_status = 待确认
to_status = 已完成

old_assignee_id = 当前维修人员
new_assignee_id = 当前维修人员

operation_type = STUDENT_CONFIRM

operator_id = 当前学生
operation_time = now
```

同时写：

```
sys_operation_log
```

## 四、7.2 学生评价

### 4.1 数据表

评价对应：

```
repair_evaluation
```

一张工单：

```
最多一条评价
```

数据库约束：

```
UNIQUE(order_id)
```

### 4.2 评价前置条件

评价建议只允许：

```
status = 已完成
```

并且：

```
student_uid = 当前学生
```

不能：

```
待确认时提前评价
返工中评价
维修中评价
```

### 4.3 评价接口

建议：

```
POST /student/repair-orders/{orderId}/evaluation
```

请求：

```
RepairEvaluationDTO
```

字段根据最终 DDL，例如：

```
score
comment
```

如果有多个评分维度，再根据数据库实际设计补充。

第一期优先保持简单：

```
总体评分
+
评价内容
```

### 4.4 评价校验

至少：

```
score 非空
score 范围合法

comment 长度合法
```

如果：

```
score = 1~5
```

则明确校验：

```
1 <= score <= 5
```

具体范围以最终表设计为准。

### 4.5 评价幂等

应用层先：

```
SELECT evaluation
WHERE order_id = ?
```

如果已经存在：

```
拒绝重复评价
```

返回：

```
HTTP 409
```

同时数据库：

```
UNIQUE(order_id)
```

作为最终并发兜底。

即：

```
Service前置判断
+
数据库唯一索引
```

双层保证。

不需要 Redis 锁。

### 4.6 并发评价

例如学生快速连续点击两次：

```
请求A
请求B
```

两次都可能在：

```
SELECT
```

阶段查询不到评价。

最终：

```
UNIQUE(order_id)
```

确保只有一个 INSERT 成功。

另一个唯一键异常转换为：

```
HTTP 409
```

例如：

```
该工单已经评价，请勿重复提交。
```

### 4.7 评价和返工的关系

一旦学生：

```
确认完成
↓
已完成
```

才能评价。

返工必须发生在：

```
待确认
```

阶段。

因此正常流程不会出现：

```
已经评价
↓
又申请返工
```

状态自然将两种行为隔离。

### 4.8 评价是否写 flow

评价：

```
不会改变工单 status
不会改变负责人
```

因此：

```
不写 repair_order_flow
```

只写：

```
repair_evaluation
+
sys_operation_log
```

避免把 flow 变成普通行为日志。

## 五、7.3 学生申请返工

### 5.1 状态流转

返工前：

```
status = 待确认
```

返工成功：

```
status = 返工中
```

同时：

```
rework_count = rework_count + 1
```

并插入：

```
repair_rework_record
```

### 5.2 返工接口

建议：

```
POST /student/repair-orders/{orderId}/rework
```

请求：

```
CreateReworkDTO
```

字段至少：

```
reason
```

如果支持返工图片：

```
imageUrls
```

也可以一并记录。

### 5.3 返工前置校验

必须：

```
工单存在

student_uid = 当前学生

status = 待确认
```

非待确认状态：

```
不能返工
```

例如：

```
维修中
返工中
已完成
已取消
已中断
```

全部拒绝。

### 5.4 为什么已完成不能直接返工

当前业务定义：

```
待确认
→ 学生选择：
   确认完成
   或
   返工
```

一旦：

```
确认完成
→ 已完成
```

本轮学生已经确认维修结果。

因此第一期：

> 已完成工单不再开放直接返工。

如果以后需要“完成后再次发现故障”，应该：

```
重新报修
```

而不是修改历史已完成工单。

### 5.5 返工次数更新

必须避免：

```
Java先查 rework_count
↓
Java + 1
↓
普通 UPDATE
```

产生并发覆盖。

建议数据库直接：

```
UPDATE repair_order
SET
    status = #{reworkingStatus},
    rework_count = rework_count + 1
WHERE id = #{orderId}
  AND student_uid = #{studentUid}
  AND status = #{waitingConfirmStatus}
  AND deleted = 0
```

由于状态从：

```
待确认
→ 返工中
```

第一次成功后，第二个并发请求条件已经不成立。

因此：

```
同一次待确认阶段
```

只能成功创建一次返工。

### 5.6 获取本次返工序号

更新成功后需要得到：

```
当前 rework_count
```

作为：

```
rework_no
```

建议更新成功后重新查询工单。

例如：

```
rework_count = 1
→ rework_no = 1

rework_count = 2
→ rework_no = 2
```

然后写：

```
repair_rework_record
```

### 5.7 repair_rework_record

至少记录：

```
order_id

rework_no

student_uid

reason

image_urls

status

create_time
```

具体以最终 DDL 为准。

数据库如果已有：

```
UNIQUE(order_id, rework_no)
```

则继续作为并发兜底。

### 5.8 返工事务

必须同事务：

```
CAS更新 repair_order
↓
rework_count + 1
↓
INSERT repair_rework_record
↓
INSERT repair_order_flow
↓
INSERT operation log
↓
根据次数执行预警 / 异常处理
```

任何核心操作失败：

```
全部回滚
```

不能出现：

```
rework_count 已经 +1
但没有返工记录
```

## 六、返工次数策略

返工次数直接决定后续处理策略。

### 第 1 次返工

```
rework_count = 1
```

处理：

```
status = 返工中

current_assignee_id
保持原维修人员
```

不做异常标记。

不生成管理员异常预警。

即：

```
第一次
→ 原维修人员继续处理
```

### 第 2 次返工

```
rework_count = 2
```

仍然：

```
status = 返工中

current_assignee_id
保持原维修人员
```

但需要：

```
生成管理员预警
```

提醒：

> 同一工单已经发生第二次返工，需要关注维修质量。

此时：

```
exception_flag
```

仍然可以保持：

```
0
```

因为业务明确：

```
第二次
→ 预警

第三次及以上
→ 异常
```

### 第 3 次及以上返工

```
rework_count >= 3
```

必须：

```
exception_flag = 1
```

并：

```
生成管理员高优先级待办 / 异常提醒
```

表示：

> 工单必须进入管理员人工介入范围。

### 返工策略汇总

| rework_count | 状态   | 负责人             | 管理员提醒 | exception_flag |
| ------------ | ------ | ------------------ | ---------- | -------------- |
| 1            | 返工中 | 原维修人员         | 否         | 0              |
| 2            | 返工中 | 原维修人员         | 预警       | 0              |
| >=3          | 返工中 | 原维修人员暂时保留 | 必须介入   | 1              |

第 3 次以后：

```
current_assignee_id
```

暂时仍保留原维修人员。

管理员后续决定：

```
更换维修人员
更换方案
转第三方
更换设备
```

属于后续异常工单管理阶段。

阶段 7 不直接自动换人。

## 七、返工后如何重新进入维修流程

学生返工成功后：

```
status = 返工中
```

维修人员仍然是：

```
原 current_assignee_id
```

阶段 6 已允许：

```
返工中
→ 添加维修过程

返工中
→ 添加材料

返工中
→ 提交维修结果
```

因此不需要再创建新的维修流程。

原维修人员直接：

```
返工中
↓
继续维修
↓
添加过程 / 材料
↓
再次提交维修结果
↓
待确认
```

然后学生又可以：

```
确认完成
或
再次返工
```

## 八、返工与 repair_submit_time

维修人员每次提交维修结果：

```
repair_submit_time = 当前提交时间
```

因此第二次维修后：

```
repair_submit_time
```

会覆盖为最新一次提交时间。

历史每次提交时间仍然可以从：

```
repair_process_record
record_type = SUBMIT_RESULT
```

中查询。

这符合：

```
repair_order
保存当前态

process / flow
保存历史
```

的设计原则。

## 九、返工与 complete_deadline

返工发生后：

```
原来的 complete_deadline
```

是否需要重新生成，需要明确。

当前第一期建议：

> 学生返工本身不重新计算 `complete_deadline`。

原因：

```
complete_deadline
```

原本对应维修人员上一轮维修时限。

进入返工后，如果继续沿用旧 deadline，可能已经过期。

更合理的做法是：

```
返工发生
↓
complete_deadline = NULL
expected_complete_time = NULL
```

随后返工维修如何设置新的维修截止时间，需要结合业务规则。

为了保持当前阶段简单，可以采用：

```
返工进入返工中
↓
清空 expected_complete_time
清空 complete_deadline
```

后续如果需要对返工维修重新设置预计完成时间，可以在维修人员返工处理中增加专门能力。

当前阶段不要继续沿用已经失去意义的上一轮维修 deadline。

## 十、返工与负责人

第 1、2、3 次返工初始都：

```
current_assignee_id
=
原维修人员
```

区别只在：

```
管理员是否收到提醒
exception_flag
```

不要在第 2 次返工时自动调用：

```
RepairDispatchService
```

因为业务明确：

```
第1次
→ 原维修人员继续

第2次
→ 管理员预警

第3次及以上
→ 必须管理员介入
```

是否换人：

> 由管理员后续决定，而不是学生返工接口自动决定。

## 十一、管理员返工预警

阶段 7 需要复用阶段 5 建立的：

```
管理员异常提醒 / 待办能力
```

### 第二次返工

生成：

```
REWORK_WARNING
```

例如：

```
工单 ROxxx 已发生第2次返工，请关注维修处理情况。
```

### 第三次及以上

生成：

```
REWORK_EXCEPTION
```

例如：

```
工单 ROxxx 已发生第3次返工，已标记为异常工单，请及时介入处理。
```

### 防止重复提醒

提醒需要基于：

```
orderId
+
reworkNo
+
reminderType
```

进行去重。

避免接口重试、事务重试产生重复管理员提醒。

## 十二、确认完成与返工的并发竞争

这是本阶段最重要的并发场景之一。

学生可能：

```
请求A
→ 确认完成

请求B
→ 申请返工
```

几乎同时到达。

两个请求前置查询都可能看到：

```
status = 待确认
```

因此必须依赖：

```
CAS 条件 UPDATE
```

最终只能一个成功。

### 情况 A：确认先成功

```
待确认
→ 已完成
```

返工 UPDATE：

```
WHERE status = 待确认
```

影响：

```
0 行
```

返工失败：

```
HTTP 409
```

### 情况 B：返工先成功

```
待确认
→ 返工中
```

确认 UPDATE 同样：

```
0 行
```

确认失败：

```
HTTP 409
```

最终绝不允许：

```
既已完成
又生成返工记录
```

## 十三、返工幂等与重复提交

用户明确要求：

```
同一次返工不能重复创建
```

这里第一层保护已经来自状态 CAS：

```
待确认
→ 返工中
```

第一次成功后：

```
status != 待确认
```

第二次重复请求无法再成功。

数据库：

```
UNIQUE(order_id, rework_no)
```

如果已经存在，则作为第二层兜底。

因此当前返工接口：

> 不一定需要额外再引入 Redis 分布式锁。

如果前端发生同一次 HTTP 请求网络重试，状态已经变化后可以：

```
重新读取当前工单
```

判断是否已经产生当前轮次返工。

## 十四、是否需要 bizNo

阶段 4 创建报修必须使用：

```
bizNo
```

因为创建接口重复执行可能产生：

```
两张 repair_order
```

返工则不同。

状态机本身已经提供非常强的天然幂等条件：

```
只有待确认才能返工
```

所以阶段 7 第一版建议：

```
不额外引入 bizNo
```

而采用：

```
CAS状态更新
+
rework_no唯一索引
```

即可。

评价同理：

```
UNIQUE(order_id)
```

已经提供数据库幂等保护。

这样保持实现简单。

## 十五、工单流转记录

### 确认完成

写：

```
待确认
→ 已完成
```

operationType：

```
STUDENT_CONFIRM
```

### 返工

写：

```
待确认
→ 返工中
```

operationType：

```
REWORK
```

建议在：

```
remark
```

或者对应原因字段记录：

```
第N次返工
+
返工原因
```

例如：

```
第2次返工：水龙头仍有漏水。
```

### 评价

因为没有状态变化：

```
不写 repair_order_flow
```

## 十六、操作日志

以下全部写：

```
sys_operation_log
```

包括：

```
学生确认完成
学生提交评价
学生申请返工
```

操作人：

```
UserContext.getCurrentUserId()
```

业务对象：

```
orderId
```

## 十七、接口设计汇总

### 确认完成

```
POST /student/repair-orders/{id}/confirm
```

### 提交评价

```
POST /student/repair-orders/{id}/evaluation
```

### 提交返工

```
POST /student/repair-orders/{id}/rework
```

查询继续复用阶段 3：

```
GET /student/repair-orders

GET /student/repair-orders/{id}
```

详情中的：

```
reworkRecords
evaluation
flows
```

随着阶段 7 数据写入自动展示。

## 十八、前端交互

学生打开：

```
待确认
```

工单详情。

展示维修人员：

```
维修结果
维修过程
维修结果图片
```

底部操作：

```
确认完成
申请返工
```

### 确认完成

建议二次确认：

```
确认该维修问题已经解决？
```

点击确认后：

```
待确认
→ 已完成
```

随后展示：

```
评价入口
```

### 申请返工

弹窗填写：

```
返工原因
返工图片（可选）
```

提交成功：

```
返工中
```

提示：

```
返工申请已提交，维修人员将继续处理。
```

### 第 2、3 次返工

学生端不需要展示：

```
管理员预警
exception_flag内部处理
```

仍然只展示正常：

```
返工已提交
```

后台自动执行预警。

如果产品希望异常状态展示给学生，后续再增加。

## 十九、状态操作矩阵

建议锁定：

| 学生操作 | 待确认 | 已完成 | 返工中 | 维修中 | 已取消 |
| -------- | ------ | ------ | ------ | ------ | ------ |
| 确认完成 | ✅      | ❌      | ❌      | ❌      | ❌      |
| 申请返工 | ✅      | ❌      | ❌      | ❌      | ❌      |
| 提交评价 | ❌      | ✅      | ❌      | ❌      | ❌      |

这样：

```
待确认
```

只有两个互斥业务出口：

```
确认
或
返工
```

状态模型比较干净。

## 二十、重点验收场景

### 确认完成

正常：

```
待确认
↓
确认
↓
已完成
```

验证：

```
confirm_time != null
complete_time != null
status = 已完成
flow 正确
```

### 非待确认确认

例如：

```
维修中
返工中
已完成
```

调用确认接口：

```
HTTP 409
```

### 其他学生确认

学生 B 操作学生 A 工单：

```
HTTP 403
```

### 正常评价

```
status = 已完成
student_uid = 当前学生
```

评价成功。

数据库：

```
repair_evaluation
```

新增一条。

### 重复评价

第一次成功。

第二次提交：

```
HTTP 409
```

数据库始终：

```
只有1条评价
```

### 未完成提前评价

```
status = 待确认
```

评价：

```
HTTP 409
```

### 第一次返工

```
待确认
rework_count = 0
↓
返工
```

结果：

```
status = 返工中
rework_count = 1
exception_flag = 0
```

生成：

```
rework_no = 1
```

不产生管理员异常预警。

### 第二次返工

前一轮重新维修后：

```
返工中
↓
提交维修结果
↓
待确认
```

学生再次返工。

结果：

```
rework_count = 2
status = 返工中
exception_flag = 0
```

生成：

```
rework_no = 2
```

同时：

```
生成管理员预警
```

### 第三次返工

结果：

```
rework_count = 3
status = 返工中
exception_flag = 1
```

同时：

```
管理员异常待办
```

必须产生。

### 第四次及以上

继续：

```
rework_count + 1
```

且：

```
exception_flag
持续保持 1
```

管理员仍然属于必须介入状态。

### 非待确认返工

例如：

```
维修中
返工中
已完成
已中断
```

调用：

```
/rework
```

全部：

```
HTTP 409
```

### 同一次返工重复请求

两个请求同时：

```
status = 待确认
```

最终：

```
只有一个 CAS UPDATE 成功
```

数据库：

```
rework_count 只 +1
repair_rework_record 只有一条
```

### 确认与返工同时提交

并发：

```
请求A：confirm
请求B：rework
```

最终：

```
只能一个成功
```

不得产生：

```
status = 已完成
+
repair_rework_record 又增加
```

这种矛盾结果。

### 完成后的返工历史

工单最终：

```
已完成
```

详情仍然必须可以查看：

```
第1次返工
第2次返工
第3次返工
```

`repair_rework_record` 不因完成状态而删除。

## 二十一、事务设计

### 确认完成

同事务：

```
UPDATE repair_order
↓
INSERT repair_order_flow
↓
INSERT operation log
```

### 评价

同事务：

```
INSERT repair_evaluation
↓
INSERT operation log
```

### 返工

同事务：

```
CAS UPDATE repair_order

status = 返工中
rework_count + 1
必要时 exception_flag = 1

↓
INSERT repair_rework_record
↓
INSERT repair_order_flow
↓
生成管理员提醒
↓
INSERT operation log
```

管理员提醒如果作为核心业务记录要求强一致，则同事务。

如果提醒系统后续采用异步消息，可以按提醒模块最终设计调整。

## 二十二、错误码建议

继续使用：

```
ORDER
REWORK
EVALUATION
```

模块。

例如：

```
ORDER-A-xxxxx
当前工单状态不允许确认
HTTP 409
REWORK-A-xxxxx
当前状态不允许申请返工
HTTP 409
EVALUATION-A-xxxxx
该工单已经评价
HTTP 409
EVALUATION-A-xxxxx
当前工单尚未完成，不能评价
HTTP 409
```

资源归属不符：

```
AUTH-A-xxxxx
HTTP 403
```

## 二十三、推荐开发顺序

```
7-01 学生确认完成接口

7-02 confirm_time / complete_time

7-03 确认 CAS

7-04 确认 flow / log

7-05 评价 DTO / VO

7-06 评价新增

7-07 UNIQUE(order_id) 异常处理

7-08 返工 DTO

7-09 返工 CAS

7-10 rework_count + 1

7-11 repair_rework_record

7-12 第1次返工策略

7-13 第2次管理员预警

7-14 第3次 exception_flag

7-15 返工 flow / log

7-16 complete_deadline 等旧维修时间清理

7-17 阶段3详情联调

7-18 学生端确认页面

7-19 学生评价页面

7-20 学生返工弹窗

7-21 状态非法测试

7-22 越权测试

7-23 并发确认 / 返工测试

7-24 重复评价 / 重复返工测试

7-25 阶段验收
```

## 阶段验收清单

### 确认完成

- 

  只有待确认可以确认；

- 

  只有报修学生本人可以确认；

- 

  状态更新为已完成；

- 

  `confirm_time` 正确；

- 

  `complete_time` 正确；

- 

  flow 正确；

- 

  重复确认不会重复执行。

### 评价

- 

  只有已完成工单可以评价；

- 

  只有本人可以评价；

- 

  一个工单只能一条评价；

- 

  `UNIQUE(order_id)` 生效；

- 

  并发评价只有一次成功；

- 

  评价不修改工单状态；

- 

  评价不写业务 flow。

### 返工

- 

  只有待确认可以返工；

- 

  返工不创建新工单；

- 

  `rework_count + 1` 正确；

- 

  每次产生一条 `repair_rework_record`；

- 

  `rework_no` 正确；

- 

  原负责人继续处理；

- 

  同一次返工不会重复创建；

- 

  非本人不能申请返工。

### 返工升级

- 

  第1次返工不标记异常；

- 

  第1次不产生管理员异常预警；

- 

  第2次产生管理员预警；

- 

  第2次 `exception_flag = 0`；

- 

  第3次 `exception_flag = 1`；

- 

  第3次产生管理员介入待办；

- 

  第4次及以上保持异常状态。

### 并发

- 

  两个返工请求只有一个成功；

- 

  确认与返工并发只有一个成功；

- 

  `rework_count` 不会重复累加；

- 

  不会同时出现已完成和新增返工记录。

### 历史

- 

  最终完成后返工历史仍然可查；

- 

  多次维修提交过程仍然可查；

- 

  工单 flow 可以完整还原维修与返工过程。

## Definition of Done

阶段 7 完成后，一张待确认工单能够完整处理两种学生选择。

第一种：

```
待确认
↓
学生确认完成
↓
confirm_time
complete_time
↓
已完成
↓
学生评价
↓
repair_evaluation
```

第二种：

```
待确认
↓
学生申请返工
↓
rework_count + 1
↓
repair_rework_record
↓
返工中
↓
原维修人员继续处理
↓
再次提交维修结果
↓
待确认
```

并根据返工次数自动处理：

```
第1次
→ 正常返工

第2次
→ 管理员预警

第3次及以上
→ exception_flag = 1
→ 管理员必须介入
```

同时确保：

```
返工始终属于原工单
评价只能一次
状态转换合法
学生只能操作自己的工单
返工次数不会并发重复累加
历史维修和返工记录永久可追溯
```

阶段 7 完成后，系统的核心正向业务闭环已经形成：

```
报修
↓
派单
↓
接单
↓
维修
↓
提交结果
↓
学生确认 / 返工
↓
完成
↓
评价
```

下一阶段可以进入：

> **阶段 8：维修人员转派申请、管理员审批与重新自动派单。**