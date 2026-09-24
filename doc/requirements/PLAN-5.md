# PLAN-5：自动派单核心

# 一、阶段目标

阶段 5 开发整个维修工单系统最核心的自动派单能力。

统一建立：

```
RepairDispatchService
```

所有需要系统自动选择维修人员的业务场景统一调用：

```
dispatch(orderId, excludeWorkerIds, sourceType)
```

后续禁止在：

```
学生报修
接单超时
维修人员请假
维修人员转派
```

四套业务代码中分别实现一套维修人员匹配算法。

统一流程：

```
业务场景
↓
确定需要排除的维修人员
↓
调用 RepairDispatchService
↓
统一候选人匹配
↓
统一工作量比较
↓
统一接单截止时间计算
↓
统一更新 repair_order
↓
统一写 repair_order_flow
↓
成功 / 失败结果返回调用方
```

需求确定自动派单的主要依据为：

```
故障类型
+
负责区域
+
当前工作量
```

第一期不引入复杂权重评分算法。

# 二、阶段 5 核心原则

整个自动派单模块遵循以下原则：

```
一个派单入口
一套候选算法
一套截止时间算法
一套派单结果处理
```

不得出现：

```
新报修写一套
超时写一套
请假写一套
转派再写一套
```

否则后期非常容易出现：

```
某个流程过滤请假
另一个流程没有过滤

某个流程按工作量排序
另一个流程随机派单

某个流程计算工作时间
另一个流程直接 now + 30min
```

# 三、统一 Service

定义：

```
public interface RepairDispatchService {

    DispatchResult dispatch(
        Long orderId,
        Set<Long> excludeWorkerIds,
        DispatchSourceType sourceType
    );

}
```

其中：

```
orderId
→ 当前需要派单的工单

excludeWorkerIds
→ 本轮不能再次派给的维修人员

sourceType
→ 本次派单发生的业务来源
```

## 派单来源 DispatchSourceType

```
INITIAL_REPORT
    新报修首次自动派单

ACCEPT_TIMEOUT
    接单超时重新派单

LEAVE_REASSIGN
    请假触发重新派单

TRANSFER_REASSIGN
    转派审批通过后重新派单
```

后续如有需要再增加。

不要一开始把：

```
人工派单
返工
第三方维修
```

全部塞入自动派单来源。

管理员人工指定维修人员属于另一类业务，不需要运行自动匹配算法。

###  **sourceType 必须保留**

虽然最终都是：

```
current_assignee_id
发生变化
```

但是业务原因不同。

需求中已经明确需要区分：

```
维修人员主动转派
维修人员请假自动转派
接单超时自动转派
管理员人工转派
```

这样以后查看工单流转时才能知道“为什么换人”。

因此：

```
RepairDispatchService
```

只负责统一算法，

而：

```
sourceType
```

负责保留业务来源。

# 四、自动派单完整算法

统一算法：

```
读取 repair_order
↓
校验工单允许执行派单
↓
① 根据 fault_type 找技能匹配维修人员
↓
② 根据工单位置过滤负责范围
↓
③ 过滤停用 / 请假 / 不可接单人员
↓
④ 排除 excludeWorkerIds
↓
⑤ 统计所有候选人的未完成工单数量
↓
⑥ 按未完成工单数量升序
↓
⑦ 工作量相同时执行稳定排序
↓
⑧ 选择第一名候选人
↓
⑨ 根据有效工作时间计算 accept_deadline
↓
⑩ 更新 repair_order
↓
⑪ 写 repair_order_flow
↓
返回派单成功

如果任意候选阶段最终无可用人员
↓
保持待派单
↓
清空当前负责人
↓
形成管理员异常提醒 / 待办
↓
返回派单失败
```

# 第一步：读取工单

首先：

```
SELECT repair_order
WHERE id = ?
AND deleted = 0
```

不存在：

```
HTTP 404
ORDER-B-xxxxx
```

## 允许派单的工单状态

首次派单通常：

```
status = 待派单
```

未来重新派单可能来自：

```
待接单
维修中
返工中
已中断
```

因此：

> 是否允许派单不能简单写死为 `status = 0`。

应结合：

```
sourceType
```

判断。

例如：

### INITIAL_REPORT

要求：

```
status = 待派单
```

### ACCEPT_TIMEOUT

要求：

```
status = 待接单
```

### LEAVE_REASSIGN

根据请假业务允许：

```
待接单
维修中
返工中
已中断
```

需求中对请假需要转出的未完成工单就是上述范围。

阶段 5 可以先把结构预留好，具体后续阶段调用时再补相应 source 状态规则。

# 第二步：技能匹配

根据：

```
repair_order.fault_type_id
```

查询：

```
repair_worker_fault_type
```

要求：

```
fault_type_id = 当前故障类型
AND
status = 启用
```

得到：

```
具备该故障维修能力的 worker_id
```

## 技能匹配索引

依赖：

```
idx_fault_worker
```

DDL 已按：

```
故障类型
→ 找具备技能维修人员
```

设计该索引。

### 技能匹配结果为空

如果：

```
没有任何维修人员具备该技能
```

无需继续执行：

```
区域匹配
工作量统计
```

直接进入：

```
派单失败
```

失败原因建议：

```
NO_SKILL_WORKER
```

方便管理员异常中心展示具体原因。

# 第三步：负责范围过滤

根据工单：

```
campus_id
area_id
building_id
```

匹配：

```
repair_worker_area_scope
```

当前业务支持三种负责粒度：

```
整个校区
某个区域
指定楼栋
```

## 区域匹配优先规则

只要维修人员存在任意一条有效范围满足：

```
指定楼栋
OR
整个区域
OR
整个校区
```

即认为：

```
区域匹配成功
```

### 指定楼栋

例如：

```
campus_id = 1
area_id = 10
building_id = 100
```

要求：

```
工单 campus_id = 1
AND
工单 area_id = 10
AND
工单 building_id = 100
```

### 整个区域

例如配置：

```
campus_id = 1
area_id = 10
building_id = NULL
```

表示：

```
负责整个区域
```

工单：

```
campus_id = 1
AND
area_id = 10
```

即匹配。

### 整个校区

例如：

```
campus_id = 1
area_id = NULL
building_id = NULL
```

表示：

```
负责整个校区
```

工单只需要：

```
campus_id = 1
```

即可匹配。

## 区域匹配 SQL 逻辑

整体语义：

```
scope.campus_id = order.campus_id

AND

(
    scope.building_id = order.building_id

    OR

    (
        scope.area_id = order.area_id
        AND scope.building_id IS NULL
    )

    OR

    (
        scope.area_id IS NULL
        AND scope.building_id IS NULL
    )
)
```

同时只考虑：

```
有效 / 启用
```

的负责范围。

## 区域索引

依赖：

```
idx_scope_location
```

DDL 中已经明确该索引用于自动派单的负责区域过滤。

### 区域匹配结果为空

存在技能维修人员，但：

```
没有任何人负责当前区域
```

派单失败原因：

```
NO_AREA_WORKER
```

进入管理员待办。

# 第四步：维修人员状态过滤

技能和区域均匹配后，继续过滤：

```
停用人员
请假人员
当前不可接单人员
```

需求明确要求自动派单过滤：

```
停用
请假
不可接单
```

维修人员。

## 停用过滤

例如：

```
repair_worker.work_status = DISABLED
```

或者实际最终状态字段表示：

```
停用
```

则：

```
不能进入候选池
```

## 请假过滤

请假判断不能只看：

```
有一张审批通过的请假单
```

而应该判断：

```
请假已审批通过
AND
leave_start <= 当前时间
AND
leave_end >= 当前时间
```

即：

> 只有当前确实处于生效请假区间，才从候选池排除。

需求也明确区分了“请假审批通过”和“当前是否处于请假时间”；请假开始前人员仍然可以正常工作。

## 请假状态实现兼容

如果后续定时任务已经把：

```
repair_worker.work_status
```

同步成：

```
请假中
```

派单可以直接过滤。

但是为了防止定时任务延迟、状态同步失败

最终派单候选检查仍然以：

```
有效请假记录
```

作为可靠判断之一。

第一版可以根据最终请假阶段实现方式确定是否需要两层判断。

## 人员状态索引

依赖：

```
idx_worker_work_status
```

DDL 已明确用于：

```
过滤请假 / 停用
```

人员。

# 第五步：排除本轮不能再次派给的人

统一参数：

```
Set<Long> excludeWorkerIds
```

## 首次派单

新报修：

```
excludeWorkerIds = empty
```

## 接单超时重新派单

例如：

```
原负责人 = workerA
```

则：

```
excludeWorkerIds = [workerA]
```

防止：

```
A接单超时
↓
自动转派
↓
算法又把工单派给A
```

## 转派重新匹配

原维修人员提出转派并审批通过：

```
excludeWorkerIds
至少包含原维修人员
```

避免立即重新派回原负责人。

## 请假重新派单

当前请假人员：

```
必须排除
```

通常：

```
excludeWorkerIds
包含原负责人
```

即使请假过滤已经能排除，也建议调用方明确传入。

这样业务含义更稳定。

## excludeWorkerIds 只代表本轮排除

不要把：

```
excludeWorkerIds
```

永久保存到维修人员配置。

它表示：

> 当前这一轮重新派单不能选择的人。

# 第六步：统计候选维修人员工作量

剩余候选维修人员需要统计：

```
当前未完成工单数量
```

然后：

```
未完成工单最少
→ 优先派单
```

这是需求明确的核心排序规则。

## 未完成工单

根据当前工单状态：

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

工作量统计时应统计：

> 当前确实由该维修人员负责、且尚未关闭的工单。

由于：

```
待派单
```

通常：

```
current_assignee_id = NULL
```

不会计入具体维修人员。

因此实际候选人工作量通常来源：

```
待接单
维修中
待确认
返工中
已中断
```

是否将：

```
待确认
```

继续计入维修人员工作量，需要按照业务口径固定。

> 只要工单尚未完成/取消，且 `current_assignee_id` 仍然指向该维修人员，就计入未完成工作量。

这样口径最稳定。

## 工作量 SQL

核心：

```
SELECT current_assignee_id,
       COUNT(*) AS unfinished_count
FROM repair_order
WHERE current_assignee_id IN (...)
  AND status IN (...)
  AND deleted = 0
GROUP BY current_assignee_id
```

没有查询结果的候选人：

```
unfinished_count = 0
```

## 工作量索引

依赖：

```
idx_order_assignee_status
```

现有 DDL：

```
(current_assignee_id, status, deleted)
```

正好用于：

```
负责人 + 工单状态
```

查询。

DDL 设计说明也明确该索引用于统计未完成工单。

## 不要 N+1 统计工作量

错误方式：

```
候选人A → COUNT
候选人B → COUNT
候选人C → COUNT
候选人D → COUNT
```

候选人越多 SQL 越多。

正确方式：

```
一次 GROUP BY
```

批量统计所有候选人工作量。

# 第七步：最少工作量优先

候选结果：

```
workerA → 2单
workerB → 0单
workerC → 4单
```

选择：

```
workerB
```

## 工作量相同怎么办

必须提前制定稳定规则。

例如：

```
workerA → 1
workerB → 1
workerC → 1
```

如果没有第二排序条件，数据库返回顺序可能不稳定。

建议：

```
unfinished_count ASC
worker_id ASC
```

作为稳定排序。

即：

```
工作量最少优先
↓
工作量相同
↓
worker_id 较小者优先
```

以后确实需要更公平，可以再增加：

```
last_dispatch_time
```

等轮转机制。

当前阶段不引入。

## 并发下的工作量不是绝对强一致

例如：

```
工单A
工单B
```

同时派单。

都查询发现：

```
张师傅 = 0
李师傅 = 1
```

可能两个工单都选择张师傅。

这属于：

```
并发瞬间工作量竞争
```

第一期不需要为了绝对平均引入：

```
全局派单锁
维修人员悲观锁
复杂队列
```

原因：

> 当前规则目标是尽量选择工作量最少者，不要求并发下实现数学上的绝对均分。

如果后续真实业务并发非常高，再升级。



# 同一工单并发保护

## 防止重复派单

虽然不需要锁整个派单系统，但：

> 同一张工单绝不能被两个线程同时派给两个维修人员。

例如：

```
报修创建触发派单
+
定时任务同时扫描待派单
```

都处理同一 orderId。

必须防止：

```
线程A → 张师傅
线程B → 李师傅
```

后写覆盖前写。

推荐：

```
数据库条件更新
```

而不是一开始就给整个派单过程加 Redis 锁。

例如首次派单：

```
UPDATE repair_order
SET
    status = 1,
    current_assignee_id = #{workerId},
    dispatch_time = #{dispatchTime},
    accept_deadline = #{acceptDeadline}
WHERE id = #{orderId}
  AND status = 0
  AND current_assignee_id IS NULL
  AND deleted = 0
```

检查：

```
affectedRows
```

### 更新成功

```
affectedRows = 1
```

说明：

```
当前线程成功取得该工单派单权
```

继续写：

```
repair_order_flow
```

### 更新失败

```
affectedRows = 0
```

说明：

```
工单状态已经发生变化
或
已经被其他线程派单
```

此时：

```
重新读取 repair_order
```

如果已经：

```
status = 待接单
current_assignee_id != null
```

说明其他线程已经完成派单。

当前调用不应再次覆盖。

## 重新派单的 CAS 条件

未来：

```
ACCEPT_TIMEOUT
LEAVE_REASSIGN
TRANSFER_REASSIGN
```

不能全部使用：

```
status=0 AND assignee IS NULL
```

而应根据：

```
sourceType
+
原状态
+
原负责人
```

进行条件更新。

例如：

```
WHERE id = ?
AND status = 待接单
AND current_assignee_id = 原负责人
```

保证：

> 只修改自己读取到的那一版业务状态。

# 第八步：计算接单截止时间

这是阶段 5 的重点。

需求明确：

```
接单时限 = 30分钟有效工作时间
```

不能直接：

```
dispatch_time + 30min
```

需求文档明确说明：

```
工作时间 08:00 - 18:00

17:50派单
→ 当天只累计10分钟
→ 剩余20分钟
→ 次日08:00继续
→ accept_deadline = 次日08:20
```

如果：

```
20:00派单
```

则：

```
次日08:00开始计时
→ 08:30截止
```

并且要求派单成功时直接把结果保存为 `accept_deadline`，后续提醒任务无需反复计算。

## 独立截止时间计算服务

不要把跨工作时间计算全部塞在：

```
RepairDispatchServiceImpl
```

建立一个非常轻量的：

```
RepairWorkTimeService
```

例如：

```
LocalDateTime calculateAcceptDeadline(
    LocalDateTime dispatchTime,
    int requiredMinutes
);
```

当前：

```
requiredMinutes = 30
```

## 独立 WorkTimeService

后续：

```
接单提醒
接单超时
其他有效工作时间计算
```

都可能复用。

但是保持简单：

```
RepairWorkTimeService
```

只负责：

> 给一个起始时间和有效分钟数，计算实际截止时间。

不要进一步建设复杂的：

```
CalendarEngine
BusinessTimeFramework
ScheduleRuleEngine
```

## 工作时间方案查询

### 有效工作时间最终定义

接单时限固定为：

```
30分钟有效工作时间
```

这里的“有效工作时间”必须同时满足：

```
1. 当前日期不是周六、周日

2. 当前日期不是国家法定节假日

3. 当前日期存在启用中的工作时间方案

4. 当前时刻位于：
   work_start_time
   ~
   work_end_time
```

因此：

```
自然时间
≠
接单有效时间
```

### 最终 accept_deadline 计算原则

禁止：

```
accept_deadline
=
dispatch_time + 30分钟
```

统一改为：

```
从 dispatch_time 开始
↓
寻找第一个有效工作时间点
↓
累计30分钟有效工作时间
↓
期间自动跳过：

下班时间
周六
周日
国家法定节假日

↓
得到 accept_deadline
```

### RepairWorkTimeService 职责调整

原：

```
LocalDateTime calculateAcceptDeadline(
    LocalDateTime dispatchTime,
    int requiredMinutes
);
```

接口本身可以保持不变。

但内部职责调整为：

```
RepairWorkTimeService
│
├─ 判断是否工作日
├─ 判断是否法定节假日
├─ 查询当天工作时间方案
├─ 寻找下一有效工作时间点
└─ 累计有效工作分钟
```

仍然不需要建设复杂日历引擎。

### 有效工作日判断

建议统一内部方法：

```
boolean isEffectiveWorkday(LocalDate date);
```

逻辑：

```
如果是周六
→ false

如果是周日
→ false

如果是国家法定节假日
→ false

否则
→ true
```

### 周末判断

使用：

```
date.getDayOfWeek()
```

判断：

```
SATURDAY
SUNDAY
```

直接跳过。

例如：

```
星期五 17:50派单
工作时间 08:00-18:00
```

星期五：

```
17:50 → 18:00
累计10分钟
```

剩余：

```
20分钟
```

如果：

```
星期六
星期日
```

均为非工作日，则跳过。

下一有效工作时间：

```
星期一 08:00
```

继续20分钟。

最终：

```
星期一 08:20
```

### 法定节假日判断

需要有统一的数据来源判断：

```
某个 LocalDate
是否属于国家法定节假日
```

建议不要：

```
硬编码春节
国庆节
劳动节
```

到 Java 代码。

应该由数据库配置维护。

### 建议增加法定节假日表

如果当前数据库尚未存在节假日配置表，建议增加：

```
repair_holiday_calendar
```

或采用项目统一命名。

字段建议：

```
id

holiday_date
holiday_name

holiday_type

status

remark

create_time
update_time
```

其中：

```
holiday_date
```

建立唯一索引。

### holiday_type

如果本系统明确规定：

> 只排除周末和国家法定节假日

第一期实际上可以只维护：

```
holiday_date
```

即可。

但考虑国家节假日通常存在：

```
调休上班
```

建议最好预留：

```
holiday_type
```

例如：

```
1 = 法定节假日
2 = 调休工作日
```

这样可以正确处理：

```
星期六
但因为国庆调休需要上班
```

这种情况。

### 周末与调休的优先级

这里建议直接锁死：

> 国家发布的调休工作日优先级高于普通周末规则。

即：

```
星期六 / 星期日
```

默认：

```
非工作日
```

但是如果节假日日历明确配置：

```
holiday_type = 调休工作日
```

则：

```
视为工作日
```

反之：

普通周一至周五，如果配置：

```
holiday_type = 法定节假日
```

则：

```
视为非工作日
```

最终判断顺序：

```
先查询节假日日历
↓
如果明确标记“调休工作日”
→ 工作日

如果明确标记“法定节假日”
→ 非工作日

否则
↓
判断是否周六/周日
```

### 推荐工作日判断逻辑

```
查询 holiday_calendar(date)
        ↓
存在记录？
   ┌──────────────┐
   │              │
  是              否
   │              │
   ↓              ↓
调休工作日？      判断星期
 │      │          │
是      否         ├─ 周六/周日 → 非工作日
│        │         └─ 周一~周五 → 工作日
↓        ↓
工作日   法定节假日
         ↓
       非工作日
```

### 为什么不能简单“周末全部跳过”

中国法定节假日存在：

```
调休工作
```

例如：

```
某个星期六
```

可能实际上属于：

```
正常工作日
```

如果简单写：

```
if (SATURDAY || SUNDAY) {
    skip;
}
```

会导致：

```
accept_deadline
```

计算错误。

因此更准确的规则应该是：

```
法定节假日日历
优先
+
普通星期规则兜底
```

### 节假日查询 Service

可以建立：

```
HolidayCalendarService
```

提供：

```
DayType getDayType(LocalDate date);
```

返回：

```
WORKDAY

HOLIDAY
```

或者更明确：

```
NORMAL_WORKDAY
NORMAL_WEEKEND
LEGAL_HOLIDAY
ADJUSTED_WORKDAY
```

第一期也可以保持简单：

```
boolean isWorkday(LocalDate date);
```

### RepairWorkTimeService 与节假日服务关系

结构：

```
RepairWorkTimeService
        ↓
HolidayCalendarService
        ↓
节假日表
```

同时：

```
RepairWorkTimeService
        ↓
RepairWorkScheduleMapper
        ↓
工作时间方案
```

即：

```
日期是不是工作日
```

由：

```
HolidayCalendarService
```

负责。

```
这一天几点上班、几点下班
```

由：

```
repair_work_schedule
```

负责。

职责清晰。

### 最终寻找有效工作区间流程

给定：

```
cursor
```

执行：

```
取 cursor 日期
↓
判断是否有效工作日
↓
否
→ 跳到下一天
→ 重新判断

是
↓
查询当天工作时间方案
↓
不存在
→ 配置异常
↓
存在
→ 得到 workStart / workEnd
↓
判断 cursor 所在时间
```

### 当天工作时间判断

如果：

```
cursor < workStart
```

则：

```
cursor = 当天 workStart
```

如果：

```
workStart <= cursor < workEnd
```

则：

```
从 cursor 开始累计
```

如果：

```
cursor >= workEnd
```

则：

```
cursor = 下一天00:00
```

重新寻找有效工作日。

### 最终累计算法

伪流程：

```
remainingMinutes = 30
cursor = dispatchTime

while remainingMinutes > 0:

    date = cursor.toLocalDate()

    if date 不是有效工作日:
        cursor = 下一天00:00
        continue

    schedule = 查询当天工作时间方案

    if schedule不存在:
        抛出工作时间配置异常

    workStart = date + schedule.workStartTime
    workEnd   = date + schedule.workEndTime

    if cursor < workStart:
        cursor = workStart

    if cursor >= workEnd:
        cursor = 下一天00:00
        continue

    availableMinutes =
        workEnd - cursor

    if availableMinutes >= remainingMinutes:
        return cursor + remainingMinutes

    remainingMinutes -= availableMinutes

    cursor = 下一天00:00
```

下一次循环：

```
重新判断周末
重新判断节假日
重新查询工作时间方案
```

### 普通跨下班场景

工作时间：

```
08:00 - 18:00
```

周三：

```
17:50派单
```

当天：

```
17:50 - 18:00
= 10分钟
```

周四不是节假日：

```
08:00开始
```

剩余：

```
20分钟
```

结果：

```
周四08:20
```

### 跨周末场景

工作时间：

```
08:00 - 18:00
```

星期五：

```
17:50派单
```

当天：

```
累计10分钟
```

剩余：

```
20分钟
```

星期六：

```
跳过
```

星期日：

```
跳过
```

星期一：

```
08:00继续
↓
08:20
```

最终：

```
accept_deadline
=
星期一08:20
```

### 周五下班后派单

星期五：

```
20:00派单
```

当天：

```
已经下班
```

星期六：

```
跳过
```

星期日：

```
跳过
```

星期一：

```
08:00开始
↓
累计30分钟
```

结果：

```
星期一08:30
```

### 跨法定节假日

例如：

```
9月30日17:50派单
```

当天累计：

```
10分钟
```

假设：

```
10月1日
~ 
10月7日
```

均配置为法定节假日。

则全部跳过。

如果：

```
10月8日
```

为有效工作日，工作时间：

```
08:30 - 17:30
```

则：

```
10月8日08:30
↓
继续20分钟
```

最终：

```
accept_deadline
=
10月8日08:50
```

### 跨周末 + 法定节假日

算法不需要分别写两套。

统一：

```
寻找下一个有效工作日
```

即可。

例如：

```
周五派单
↓
周六
↓
周日
↓
周一法定节假日
↓
周二法定节假日
↓
周三工作日
```

系统自动：

```
跳过周六
跳过周日
跳过周一
跳过周二
↓
周三继续累计
```

### 调休工作日

例如：

```
星期六
```

通常应该跳过。

但节假日日历配置：

```
date = 2026-XX-XX
type = ADJUSTED_WORKDAY
```

则：

```
当天有效
```

继续读取当天：

```
repair_work_schedule
```

并累计有效时间。

### 跨夏冬方案 + 节假日

每进入新的日期都必须重新：

```
判断工作日
+
查询工作时间方案
```

因此可以自然处理：

```
9月30日
夏季作息

↓
国庆假期

↓
10月8日
冬季作息
```

不能把9月30日查到的：

```
08:00 - 18:00
```

一直沿用到10月。

### 节假日表索引

建议：

```
UNIQUE(holiday_date)
```

如果支持：

```
status
```

查询主要还是：

```
holiday_date = ?
```

不需要复杂索引。

### 是否每循环一天都查数据库

30分钟响应时限通常最多跨：

```
数天
```

即使国庆期间：

```
约7天
```

逐日期查询本身压力很低。

但可以简单优化：

> 一次查询一定时间范围内的节假日日历。

例如：

```
dispatchDate
~
dispatchDate + 30天
```

加载成：

```
Map<LocalDate, DayType>
```

然后 Java 内判断。

第一期如果实现成本更低，也可以逐日查询。

不要为此提前引入 Redis 缓存。

### 搜索上限

原有规则继续保留：

```
最多向未来搜索366天
```

如果连续：

```
366天
```

都找不到有效工作时间：

```
配置异常
```

不能无限循环。

### 没有节假日数据怎么办

这个情况必须区分。

普通某一天：

```
节假日表没有记录
```

并不代表异常。

规则是：

```
没有特殊日历记录
↓
按照星期判断
```

即：

```
周一~周五
→ 正常工作日

周六~周日
→ 普通周末
```

### 工作时间方案缺失仍然属于异常

即使判断某一天：

```
属于有效工作日
```

但查询：

```
repair_work_schedule
```

没有有效方案。

则：

```
NO_WORK_SCHEDULE
```

不能默认：

```
08:00-18:00
```

更不能：

```
now + 30分钟
```

### 阶段 5 派单成功条件调整

在确定候选维修人员后：

```
selectedWorker
↓
dispatchTime = now
↓
RepairWorkTimeService.calculateAcceptDeadline(
    dispatchTime,
    30
)
```

内部依次排除：

```
下班时间
周末
法定节假日
```

计算成功后才能写：

```
status = 待接单

current_assignee_id = worker

dispatch_time = dispatchTime

accept_deadline = deadline
```

### accept_deadline 必须落库

仍然坚持：

```
派单成功时
一次性算好
```

后续：

```
接单提醒
接单超时扫描
```

直接：

```
WHERE status = 待接单
AND accept_deadline <= ...
```

不需要重新解析：

```
周末
节假日
夏冬作息
```

### 节假日修改后的历史 deadline

已经生成的：

```
accept_deadline
```

原则上：

> 不因为管理员后来修改工作时间或节假日日历而自动重新计算。

因为它代表：

```
当次派单发生时生成的业务截止时间
```

只有：

```
重新派单
```

时才重新生成新的：

```
dispatch_time
accept_deadline
```

这样历史行为可追溯。

## 新增验收矩阵

除了原有时间场景，再增加：

| 场景             | 派单时间    | 预期结果                |
| ---------------- | ----------- | ----------------------- |
| 普通工作日       | 周三10:00   | 周三10:30               |
| 周五下班前10分钟 | 周五17:50   | 周一08:20               |
| 周五下班后       | 周五20:00   | 周一08:30               |
| 周六派单         | 周六10:00   | 周一08:30               |
| 周日派单         | 周日10:00   | 周一08:30               |
| 周一法定节假日   | 周一10:00   | 下一工作日08:30         |
| 节日前剩10分钟   | 节日前17:50 | 节后首个工作日08:20     |
| 周末后仍是假期   | 周五17:50   | 假期后首工作日继续累计  |
| 周六调休上班     | 周六10:00   | 周六10:30               |
| 假期后切冬季作息 | 节前17:50   | 节后冬季上班时间+20分钟 |

## 新增单元测试

`RepairWorkTimeService` 至少覆盖：

```
普通工作日
上班前
上班中
下班后
恰好下班

跨一天

跨星期五到星期一

星期六派单

星期日派单

普通周末

法定节假日

连续法定节假日

周末 + 节假日组合

调休工作日

夏季 → 冬季方案

冬季 → 夏季方案

工作日无时间方案

连续未来无方案
```

工作时间计算属于纯业务算法：

> 非常适合做独立单元测试。

## 最终有效时间模型

阶段 5 最终将：

```
RepairWorkTimeService
```

定义为：

```
日期层：
法定节假日日历
+
周末判断

        ↓

确定是否有效工作日

        ↓

时间层：
repair_work_schedule

        ↓

确定当天有效工作时间段

        ↓

累计30分钟

        ↓

accept_deadline
```

最终：

```
accept_deadline
```

同时正确排除：

```
非工作时段
周末
国家法定节假日
```

并兼容：

```
调休工作日
夏季作息
冬季作息
跨日
跨周
跨节假日
```



根据某一天：

```
date
```

从：

```
repair_work_schedule
```

查询：

```
status = 启用
AND
start_date <= date
AND
end_date >= date
```

得到：

```
work_start_time
work_end_time
```

### 工作时间索引

依赖：

```
idx_schedule_effective
```

现有数据库索引设计明确该索引用于自动派单计算工作时间。

### 工作时间方案唯一性前提

阶段 2 已经要求：

> 同一天只能命中唯一有效工作时间方案。

所以：

```
RepairWorkTimeService
```

正常情况下：

```
查询某日
→ 0条或1条
```

如果查询出：

```
>1条有效方案
```

属于配置异常。

不能随机选一条。

### 派单发生在工作时间内

假设：

```
工作时间：
08:00 - 18:00

dispatchTime：
10:00
```

剩余工作时间：

```
8小时
```

大于：

```
30分钟
```

因此：

```
accept_deadline = 10:30
```

### 距离下班不足30分钟

例如：

```
工作时间：
08:00 - 18:00

派单：
17:50
```

当天：

```
17:50 → 18:00
= 10分钟
```

剩余：

```
20分钟
```

下一有效工作区间：

```
08:00
```

继续累计：

```
20分钟
```

最终：

```
accept_deadline = 次日08:20
```

### 恰好剩余30分钟

例如：

```
派单：
17:30

下班：
18:00
```

刚好：

```
30分钟
```

则：

```
accept_deadline = 当天18:00
```

不需要跨天。

### 派单发生在上班前

例如：

```
工作时间：
08:00 - 18:00

派单：
07:00
```

不能从：

```
07:00
```

开始累计。

应该：

```
08:00
↓
累计30分钟
↓
08:30
```

### 派单发生在下班后

例如：

```
派单：
20:00
```

应该：

```
寻找下一有效工作区间
↓
次日08:00
↓
08:30
```

### 派单恰好发生在上班时间

```
dispatchTime = 08:00
```

直接：

```
accept_deadline = 08:30
```

### 派单恰好发生在下班时间

```
dispatchTime = 18:00
```

当天已经没有有效工作分钟。

因此：

```
从下一有效工作日08:00开始
↓
08:30截止
```

### 跨工作方案日期

需要考虑：

```
9月30日
→ 夏季作息最后一天

10月1日
→ 冬季作息第一天
```

例如：

```
9月30日 17:50派单

夏季：
08:00 - 18:00

冬季：
08:30 - 17:30
```

当天累计：

```
10分钟
```

剩余：

```
20分钟
```

进入次日时：

> 必须重新查询 10月1日对应的工作时间方案。

因此：

```
accept_deadline
=
10月1日 08:50
```

而不能继续使用：

```
夏季08:00
```

## 跨日计算算法

伪流程：

```
remainingMinutes = 30
cursor = dispatchTime

while remainingMinutes > 0:

    查询 cursor 日期有效工作时间方案

    如果 cursor < 当日上班时间:
        cursor = 当日上班时间

    如果 cursor >= 当日下班时间:
        cursor = 下一天00:00
        continue

    availableMinutes =
        当日下班时间 - cursor

    如果 availableMinutes >= remainingMinutes:
        return cursor + remainingMinutes

    remainingMinutes -= availableMinutes

    cursor = 下一天00:00
```

下一天循环时：

```
重新查询该日期对应工作方案
```

即可自然支持：

```
夏季 → 冬季
冬季 → 夏季
```

切换。

## 没有工作时间方案

如果派单时找不到当前日期对应方案：

> 不能偷偷使用 `now + 30min`。

否则系统配置失效时会产生错误业务时间。

应该视为：

```
派单基础配置异常
```

派单失败原因：

```
NO_WORK_SCHEDULE
```

然后：

```
保持待派单
+
生成管理员异常待办
```

由管理员处理配置问题。

## 连续未来日期都没有工作方案

为了防止：

```
while
```

无限循环，必须设置搜索上限。

例如：

```
最多向后搜索 366 天
```

仍没有有效工作方案：

```
WORK_SCHEDULE_NOT_FOUND
```

直接失败。

## 休息日 / 法定节假日边界



## 维修人员请假与 accept_deadline 的关系

请假首先属于：

```
候选人过滤
```

即：

> 当前已经处于请假期的人员不会被派单。

对于：

```
现在正常
但30分钟有效响应期间即将进入请假
```

需求目前没有给出明确的逐分钟请假日历计算规则。

因此本阶段不要自行加入复杂逻辑。

阶段 5 先按：

```
派单时当前是否处于请假状态
```

过滤。

后续请假模块如果确认需要：

```
deadline 计算跨请假时间
```

再扩展 `RepairWorkTimeService`。

# 第九步：派单成功更新 repair_order

派单成功：

```
status = 待接单

current_assignee_id = selectedWorkerId

dispatch_time = now

accept_deadline = calculatedDeadline
```

当前 DDL 已有这些字段。

# 重新派单时需要清理旧接单字段

未来重新派单时，需要根据业务清理当前轮次数据。

至少：

```
accept_time = NULL
```

必要情况下：

```
expected_complete_time
complete_deadline
```

是否清理取决于具体转派业务。

阶段 5 的首次派单：

```
这些字段本来就是 NULL
```

暂不额外处理。

未来由：

```
sourceType
```

对应规则确定。

## 六十六、同一轮派单使用：

```
dispatchTime
```

一次取值。

不要分别：

```
LocalDateTime.now()
```

三次。

例如：

```
LocalDateTime dispatchTime = LocalDateTime.now();
```

同时用于：

```
repair_order.dispatch_time

accept_deadline 起算

repair_order_flow.operation_time
```

避免出现毫秒级不一致。

# 第十步：写 repair_order_flow

派单成功后必须记录：

```
repair_order_flow
```

表示：

> 本次负责人变化和派单来源。

# 六十八、首次自动派单 flow

例如：

```
orderId

fromStatus:
待派单

toStatus:
待接单

oldAssigneeId:
NULL

newAssigneeId:
selectedWorkerId

operationType:
AUTO_DISPATCH

sourceType:
INITIAL_REPORT

operationTime:
dispatchTime
```

# 六十九、重新派单 flow

以后：

```
oldAssigneeId:
workerA

newAssigneeId:
workerB

sourceType:
ACCEPT_TIMEOUT
/ LEAVE_REASSIGN
/ TRANSFER_REASSIGN
```

工单流转记录本来就承担“状态变化、原负责人、新负责人、原因和操作时间”的职责。

# 七十、派单成功事务

以下操作：

```
更新 repair_order
+
写 repair_order_flow
```

必须同事务。

即：

```
更新工单成功
flow失败
```

必须回滚。

不能出现：

```
工单已经派给张师傅
但历史记录看不出为什么
```

# 七十一、派单结果对象

建议：

```
DispatchResult
```

至少：

```
success

orderId

workerId

dispatchTime

acceptDeadline

failureReason
```

# 七十二、成功结果

例如：

```
success = true

workerId = 10001

acceptDeadline = 2026-09-24 08:20

failureReason = null
```

# 七十三、失败结果

例如：

```
success = false

workerId = null

failureReason = NO_CANDIDATE
```

# 七十四、派单失败处理

如果最终没有候选维修人员：

```
status = 待派单

current_assignee_id = NULL
```

并进入：

```
待人工派单
+
管理员提醒
```

需求明确规定：

> 自动派单匹配失败后进入待派单列表，通知管理员，由管理员人工派单；任何派单失败都不能导致工单在无人负责、无人知晓的情况下停留。

# 七十五、派单失败不是新的 status

不要增加：

```
8 = 派单失败
```

现有设计明确：

```
自动派单失败
超时
疑似重复
异常工单
```

都不是工单 status。

所以失败仍然：

```
status = 待派单
```

# 七十六、失败时 current_assignee_id

必须：

```
NULL
```

首次派单失败自然为空。

重新派单失败时也要结合后续来源业务明确是否解除旧负责人。

对于：

```
接单超时
请假生效
转派批准
```

旧负责人已经不应继续负责时：

```
current_assignee_id = NULL
```

并进入管理员处理范围。

# 七十七、派单失败原因

建议定义：

```
DispatchFailureReason
```

至少：

```
NO_SKILL_WORKER
    无技能匹配人员

NO_AREA_WORKER
    无区域匹配人员

NO_AVAILABLE_WORKER
    匹配人员全部停用 / 请假 / 被排除

NO_WORK_SCHEDULE
    无有效工作时间配置

ORDER_STATE_CHANGED
    工单并发状态已改变

SYSTEM_ERROR
    系统异常
```

这不是工单状态。

它主要用于：

```
管理员待办
异常提醒
日志
问题排查
```

# 七十八、管理员异常提醒

阶段 5 至少需要建立一个统一入口，例如：

```
AdminRepairAlertService
```

或者复用已有消息/待办机制。

调用：

```
createDispatchFailureAlert(
    orderId,
    failureReason
)
```

# 七十九、管理员提醒内容

至少：

```
工单号
故障类型
位置
派单失败时间
失败原因
```

userTip / 待办展示例如：

```
工单 RO202609230001 自动派单失败：
当前没有符合技能及负责区域条件的可用维修人员，
请人工派单。
```

# 八十、失败提醒必须防重复

例如：

```
定时任务
```

反复扫描到同一个待派单工单。

不能每10分钟给管理员制造一条相同提醒。

因此管理员待办/提醒应具备：

```
业务维度去重
```

例如：

```
orderId
+
reminderType
+
当前派单轮次
```

具体利用 `repair_reminder_record` 的去重能力可在提醒阶段进一步实现。

# 八十一、系统异常也不能静默失败

例如：

```
数据库查询异常
工作时间配置异常
区域配置异常
```

不能只是：

```
log.error(...)
```

然后让工单一直：

```
待派单
```

而管理员完全不知道。

需求的底线是：

> 所有无法自动处理的异常最终都必须进入管理员处理范围。

因此：

```
业务匹配失败
+
可识别配置失败
```

必须形成管理员待办。

真正数据库宕机这种事务整体不可用场景：

```
日志 + 监控
```

恢复后还需要由待派单扫描机制重新尝试。

# 八十二、候选查询实现方式

第一期建议：

> 分步骤批量查询，不做一条超级 SQL。

例如：

```
1. skillMapper
   查询技能匹配 workerIds

2. areaScopeMapper
   从 workerIds 中筛区域

3. workerMapper
   批量筛可用状态

4. leaveMapper
   批量筛当前请假

5. orderMapper
   GROUP BY 统计工作量

6. Java
   排序选择
```

# 八十三、为什么不建议一条超大 SQL

可以理论上写：

```
技能表
JOIN
区域表
JOIN
维修人员
LEFT JOIN
请假
LEFT JOIN
工单 COUNT
GROUP BY
ORDER BY
LIMIT 1
```

但会产生：

```
SQL复杂
区域范围 OR 条件复杂
请假判断复杂
重复行
GROUP BY复杂
测试困难
后续排除规则难维护
```

当前候选人数不会大到必须依赖超级 SQL。

分步骤批量查询：

```
更容易验证每一步为什么把某个维修人员排除
```

也更适合管理员后续排查派单问题。

# 八十四、但不能 N+1

分步骤不等于：

```
每一个维修人员查一次数据库
```

必须保持：

```
每一步批量查询
```

例如：

```
一次查所有技能候选

一次查所有区域匹配

一次查所有人员状态

一次查所有有效请假

一次 GROUP BY 工作量
```

总 SQL 数量稳定。

# 八十五、派单过程日志

建议至少输出 DEBUG / INFO 级别：

```
orderId
sourceType
技能候选数量
区域过滤后数量
状态过滤后数量
排除后数量
最终选中 workerId
工作量
acceptDeadline
```

不要记录：

```
密码
Token
隐私数据
```

# 八十六、为什么日志很重要

以后管理员问：

```
为什么这张工单没有派给张师傅？
```

至少可以快速判断：

```
技能不匹配
区域不匹配
正在请假
已经停用
本轮被排除
工作量更高
```

而不是只有：

```
派单失败
```

四个字。

# 八十七、索引与算法对应关系

阶段 5 验收必须逐项确认。

| 派单步骤   | 数据                     | 索引                        |
| ---------- | ------------------------ | --------------------------- |
| 技能匹配   | repair_worker_fault_type | `idx_fault_worker`          |
| 区域匹配   | repair_worker_area_scope | `idx_scope_location`        |
| 人员状态   | repair_worker            | `idx_worker_work_status`    |
| 工作量统计 | repair_order             | `idx_order_assignee_status` |
| 工作时间   | repair_work_schedule     | `idx_schedule_effective`    |

现有 DDL 本身就是按照这条自动派单执行链设计的。

# 八十八、核心 SQL 必须 EXPLAIN

至少对以下 SQL 做：

```
EXPLAIN
```

### 技能候选

```
fault_type_id
+
status
```

### 区域候选

```
campus
area
building
```

### 工作量统计

```
current_assignee_id
+
status
```

### 工作时间方案

```
status
+
start_date
+
end_date
```

不能只看到索引存在就认为一定生效。

# 八十九、阶段 5 与阶段 4 的连接

阶段 4：

```
学生报修
↓
创建 repair_order
↓
status = 待派单
↓
事务成功
```

然后调用：

```
dispatch(
    orderId,
    Collections.emptySet(),
    DispatchSourceType.INITIAL_REPORT
);
```

# 九十、首次派单成功

最终：

```
repair_order.status
=
待接单

current_assignee_id
=
selectedWorkerId

dispatch_time
=
当前派单时间

accept_deadline
=
30分钟有效工作时间后的实际截止点
```

# 九十一、首次派单失败

最终：

```
status = 待派单

current_assignee_id = NULL
```

然后：

```
管理员待办
```

学生报修本身：

> 仍然是创建成功。

不能因为：

```
当前没有维修人员
```

就把学生已经提交成功的报修事务回滚。

# 九十二、派单事务与报修事务分开

必须保持：

```
报修事务
↓
提交成功
↓
自动派单
```

而不是：

```
报修
+
整个派单算法
+
管理员提醒
```

塞进一个长事务。

这样即使：

```
自动派单失败
```

工单也仍然存在：

```
status = 待派单
```

管理员可以人工处理。

# 九十三、阶段 5 与未来接单超时的连接

未来定时任务查询：

```
status = 待接单
AND
accept_deadline <= now
```

现有索引：

```
idx_order_accept_timeout
```

就是为此设计。

超时后：

```
原 workerId
↓
加入 excludeWorkerIds
↓
dispatch(
    orderId,
    [oldWorkerId],
    ACCEPT_TIMEOUT
)
```

需求已经明确接单超时后重新执行自动派单。

# 九十四、阶段 5 与未来请假的连接

请假正式生效：

```
找到该维修人员未完成工单
↓
逐单执行
```

每一单：

```
dispatch(
    orderId,
    [leaveWorkerId],
    LEAVE_REASSIGN
)
```

需求明确请假导致的未完成工单需要逐单根据故障类型、位置、其他人员状态和工作量重新匹配。

# 九十五、阶段 5 与未来转派的连接

管理员审批：

```
同意转派
```

则：

```
dispatch(
    orderId,
    [oldWorkerId],
    TRANSFER_REASSIGN
)
```

成功：

```
新负责人
+
待接单
+
重新生成 accept_deadline
```

这与需求中的转派处理规则一致。

# 九十六、阶段 5 第一版不做的内容

暂不实现：

```
AI派单
维修人员评分
满意度权重
距离权重
维修效率权重
随机算法
轮询权重算法
复杂公平性模型
Redis全局派单锁
消息队列派单
分布式调度中心
```

第一版只实现：

```
技能
+
区域
+
可用状态
+
排除名单
+
当前工作量
```

足够满足当前业务需求。

# 九十七、Service 内部建议结构

保持简单：

```
RepairDispatchService
└─ RepairDispatchServiceImpl

RepairWorkTimeService
└─ RepairWorkTimeServiceImpl
```

Mapper 复用现有：

```
RepairOrderMapper

RepairWorkerMapper

RepairWorkerFaultTypeMapper

RepairWorkerAreaScopeMapper

RepairLeaveRequestMapper

RepairWorkScheduleMapper

RepairOrderFlowMapper
```

不要为了每一个算法步骤再建立一个 Service。

# 九十八、建议内部执行结构

`dispatch()` 大致：

```
loadOrder
↓
validateDispatchable
↓
findSkillCandidates
↓
filterAreaCandidates
↓
filterAvailableWorkers
↓
excludeWorkers
↓
loadWorkload
↓
selectWorker
↓
calculateAcceptDeadline
↓
updateOrderByCAS
↓
insertOrderFlow
↓
return success
```

其中任何候选阶段为空：

```
handleDispatchFailure
```

# 九十九、派单失败统一入口

建议：

```
handleDispatchFailure(
    order,
    sourceType,
    failureReason
)
```

统一处理：

```
工单保持 / 恢复待派单
清空负责人
必要流转记录
管理员待办
返回 DispatchResult.fail
```

不要每个 if：

```
技能没人
区域没人
请假过滤没人
```

各自写一套失败代码。

# 一百、工作时间计算验收

至少覆盖：

| 工作时间    | 派单时间 | 预期 deadline |
| ----------- | -------- | ------------- |
| 08:00-18:00 | 10:00    | 当日10:30     |
| 08:00-18:00 | 17:00    | 当日17:30     |
| 08:00-18:00 | 17:30    | 当日18:00     |
| 08:00-18:00 | 17:50    | 次日08:20     |
| 08:00-18:00 | 18:00    | 次日08:30     |
| 08:00-18:00 | 20:00    | 次日08:30     |
| 08:00-18:00 | 07:30    | 当日08:30     |

# 一百零一、跨夏冬作息验收

例如：

```
9月30日：
08:00-18:00

10月1日：
08:30-17:30
```

派单：

```
9月30日17:50
```

当天：

```
累计10分钟
```

剩余：

```
20分钟
```

10月1日：

```
08:30开始
```

结果：

```
accept_deadline
=
10月1日08:50
```

# 一百零二、派单算法验收：只有1名匹配人员

条件：

```
张师傅
技能匹配
区域匹配
正常工作
不请假
```

只有1人。

结果：

```
张师傅被选中

status = 待接单

current_assignee_id = 张师傅

accept_deadline 正确
```

# 一百零三、多个候选人

存在：

```
张师傅
李师傅
王师傅
```

全部满足：

```
技能
区域
状态
```

系统继续：

```
比较未完成工作量
```

# 一百零四、技能不匹配

张师傅：

```
负责该区域
工作量0
```

但：

```
没有该 fault_type 技能
```

结果：

```
不能进入候选池
```

工作量再低也不能派。

# 一百零五、区域不匹配

李师傅：

```
技能匹配
工作量0
```

但：

```
不负责当前校区 / 区域 / 楼栋
```

不能派。

# 一百零六、请假人员过滤

王师傅：

```
技能匹配
区域匹配
工作量0
```

但当前：

```
处于已批准请假有效区间
```

结果：

```
不能进入候选池
```

# 一百零七、停用人员过滤

维修人员：

```
技能匹配
区域匹配
```

但：

```
停用
```

结果：

```
不能派单
```

# 一百零八、工作量不同

候选：

```
A → 5
B → 2
C → 3
```

结果：

```
选择 B
```

# 一百零九、工作量相同

候选：

```
workerId=10 → 2单
workerId=20 → 2单
```

结果：

```
选择 workerId=10
```

保证结果稳定。

# 一百一十、无候选人

例如：

```
没有技能人员
```

或者：

```
有技能
但全都区域不匹配
```

或者：

```
技能区域都匹配
但全部停用 / 请假
```

结果必须：

```
status = 待派单

current_assignee_id = NULL

生成管理员待办
```

不能静默结束。

# 一百一十一、原维修人员排除

例如接单超时：

```
原负责人 = workerA
```

其他候选：

```
workerA → 0单
workerB → 5单
```

虽然：

```
workerA工作量更低
```

但：

```
workerA ∈ excludeWorkerIds
```

所以：

```
不能再次选择workerA
```

应选择：

```
workerB
```

# 一百一十二、全部候选都被排除

如果：

```
唯一技能 + 区域匹配人员
=
原维修人员
```

而本轮：

```
excludeWorkerIds = [原维修人员]
```

结果：

```
自动派单失败
↓
待人工派单
↓
通知管理员
```

不能为了派出去而重新选择被排除人员。

# 一百一十三、同一工单并发派单验收

线程 A：

```
dispatch(orderId)
```

线程 B 同时：

```
dispatch(orderId)
```

最终：

```
repair_order
只能存在一个 current_assignee_id
```

同时：

```
不能生成两条互相冲突的成功派单 flow
```

通过：

```
CAS 条件 UPDATE
+
事务
```

保证。

# 一百一十四、派单失败事务验收

模拟：

```
repair_order 更新成功
↓
flow INSERT 失败
```

要求：

```
repair_order 更新回滚
```

不能：

```
有负责人
但没有流转记录
```

# 一百一十五、工作时间配置异常验收

如果：

```
当前及未来无法找到有效工作时间方案
```

不能：

```
默认 now + 30min
```

必须：

```
派单失败
+
管理员异常提醒
```

# 一百一十六、EXPLAIN 验收

必须检查：

```
技能候选SQL
区域匹配SQL
工作量统计SQL
工作时间方案SQL
```

对应目标索引：

```
idx_fault_worker
idx_scope_location
idx_worker_work_status
idx_order_assignee_status
idx_schedule_effective
```

# 一百一十七、阶段 5 推荐开发顺序

```
5-01 DispatchSourceType

5-02 DispatchFailureReason

5-03 DispatchResult

5-04 RepairDispatchService 接口

5-05 工单派单状态校验

5-06 技能候选批量查询

5-07 区域候选批量过滤

5-08 维修人员状态过滤

5-09 当前请假过滤

5-10 excludeWorkerIds

5-11 未完成工单批量统计

5-12 最少工作量排序

5-13 相同工作量稳定排序

5-14 RepairWorkTimeService

5-15 当前工作时间计算

5-16 跨下班时间计算

5-17 上班前 / 下班后计算

5-18 跨工作方案日期计算

5-19 工作时间配置异常处理

5-20 repair_order CAS 派单更新

5-21 repair_order_flow

5-22 派单成功事务

5-23 派单失败统一处理

5-24 管理员异常提醒入口

5-25 阶段4报修创建接入自动派单

5-26 EXPLAIN

5-27 单元测试

5-28 并发测试

5-29 全链路验收
```

# 一百一十八、阶段 5 并行边界

可以拆成三个工作包。

## 工作包 A：候选人算法

```
技能
区域
人员状态
请假
排除名单
工作量
排序
```

## 工作包 B：工作时间

```
RepairWorkTimeService

夏季 / 冬季方案

上班前

下班后

跨下班

跨日期

跨方案
```

A 与 B 完全可以并行。

## 工作包 C：派单落库

依赖 A+B 的结果：

```
CAS UPDATE
repair_order_flow
派单失败
管理员提醒
```

最后：

```
A + B + C
↓
RepairDispatchService
```

整合。

# 一百一十九、阶段 5 估时

| 工作项                       | 估时           |
| ---------------------------- | -------------- |
| Dispatch DTO / Enum / Result | 0.25           |
| 技能匹配                     | 0.5            |
| 区域匹配                     | 0.5            |
| 状态 / 请假过滤              | 0.5            |
| excludeWorkerIds             | 0.25           |
| 工作量批量统计               | 0.5            |
| 排序选择                     | 0.25           |
| WorkTimeService              | 0.5            |
| 跨下班 / 跨日计算            | 0.75~1         |
| 跨夏冬方案                   | 0.25           |
| CAS 并发控制                 | 0.5            |
| repair_order_flow            | 0.25           |
| 派单失败兜底                 | 0.5            |
| 管理员异常提醒入口           | 0.5            |
| 阶段4接入                    | 0.25           |
| SQL EXPLAIN                  | 0.25           |
| 单元 / 并发 / 联调测试       | 1~1.5          |
| **总计**                     | **7~8.5 人日** |

如果工作时间方案已经非常稳定，并且管理员提醒基础设施已有：

```
约 6~7 人日
```

也可以完成。

# 一百二十、阶段 5 验收清单

## Service

- 只有一个 `RepairDispatchService`；
- 对外统一 `dispatch(orderId, excludeWorkerIds, sourceType)`；
- 新报修使用该 Service；
- 后续超时、请假、转派可以直接复用。

## 技能

- 只选择具备对应技能的人员；
- 停用技能关系无效；
- 无技能人员正确失败。

## 区域

- 整个校区匹配；
- 整个区域匹配；
- 指定楼栋匹配；
- 不负责当前位置的人被过滤。

## 人员状态

- 停用人员过滤；
- 当前请假人员过滤；
- 请假尚未开始人员仍可参与；
- excludeWorkerIds 生效。

## 工作量

- 一次 GROUP BY 批量统计；
- 未完成工单数量正确；
- 0工单候选正确处理；
- 最少者优先；
- 相同数量排序稳定。

## 工作时间

- 工作时间内派单；
- 上班前派单；
- 下班后派单；
- 下班前不足30分钟；
- 恰好剩30分钟；
- 跨日；
- 跨夏冬方案；
- 无工作时间方案正确进入异常处理；
- `accept_deadline` 创建后直接落库。

## 派单成功

- `status = 待接单`；
- `current_assignee_id` 正确；
- `dispatch_time` 正确；
- `accept_deadline` 正确；
- flow 正确；
- 派单来源正确。

## 派单失败

- `status = 待派单`；
- `current_assignee_id = NULL`；
- 失败原因明确；
- 管理员能够收到待办 / 异常提醒；
- 不会形成无人负责且无人知晓的工单。

## 并发

- 同一工单同时派单不会出现双负责人；
- CAS 更新有效；
- flow 不产生冲突记录；
- 不需要全局派单锁。

## SQL

- 技能查询命中目标索引；
- 区域查询命中目标索引；
- 工作量查询命中目标索引；
- 工作时间查询命中目标索引；
- 无候选人 N+1 查询。

# 一百二十一、Definition of Done

阶段 5 完成后，一张：

```
status = 待派单
```

的工单调用：

```
dispatch(orderId, excludeWorkerIds, sourceType)
```

系统能够完整完成：

```
故障技能匹配
↓
负责区域匹配
↓
停用 / 请假过滤
↓
本轮排除人员过滤
↓
统计候选人未完成工单
↓
选择工作量最少人员
↓
计算30分钟有效工作时间
↓
跨越非工作时段
↓
生成 accept_deadline
↓
CAS更新工单
↓
status = 待接单
↓
写工单流转
```

如果没有人可以处理：

```
自动派单失败
↓
status = 待派单
↓
current_assignee_id = NULL
↓
记录失败原因
↓
管理员待办 / 异常提醒
```

从而满足整个系统最重要的兜底原则：

> **任何自动派单或自动转派失败，都不能让工单在无人负责且无人知晓的情况下停留。**

完成阶段 5 后，下一阶段即可进入：

> **阶段 6：维修人员接单、接单提醒、接单超时与自动重新派单。**

届时接单超时不再重新开发匹配算法，只需要：

```
扫描 accept_deadline
↓
识别超时工单
↓
原维修人员加入 excludeWorkerIds
↓
再次调用 RepairDispatchService
```

即可。