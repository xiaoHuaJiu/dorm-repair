# PLAN-4：学生报修与 24h 疑似重复检测

# 一、阶段目标

阶段 4 是系统第一条完整业务链路。

本阶段完成：

```
学生填写报修信息
↓
参数校验
↓
校验位置是否合法
↓
校验故障类型
↓
24h 疑似重复查询
↓
存在重复时返回提示
↓
学生决定取消 / 仍然提交
↓
创建 repair_order
↓
记录 duplicate_flag
↓
写 repair_order_flow
↓
写 sys_operation_log
↓
进入自动派单
```

本阶段核心解决两个问题：

1. 学生如何安全、正确地创建一张维修工单；
2. 如何发现 24 小时内的疑似重复报修，同时不阻止学生继续提交。

# 二、阶段 4 范围

本阶段实现：

```
学生报修表单
位置校验
故障类型校验
重复报修检测
重复提示
学生确认继续提交
repair_order 创建
duplicate_flag 设置
repair_order_flow 初始记录
操作日志
提交事务
自动派单入口衔接
```

本阶段暂不实现完整：

```
自动派单算法
维修人员接单
转派
维修过程
维修材料
学生确认完成
评价
返工
超时扫描
```

其中：

> 自动派单在阶段 4 只完成“调用入口衔接”，具体派单算法进入下一阶段。

# 三、业务流程

完整流程：

```
学生提交报修信息
        ↓
校验登录身份
        ↓
参数校验
        ↓
校验位置
        ↓
校验故障类型
        ↓
查询24h疑似重复工单
        ↓
是否存在疑似重复？
     ┌───────────────┐
     │               │
    否               是
     │               ↓
     │         返回疑似工单信息
     │               ↓
     │         学生是否仍然提交？
     │          ┌────────┐
     │          │        │
     │         否        是
     │          │        │
     │       不创建      ↓
     │                 创建工单
     │                 duplicate_flag=1
     │
     ↓
创建工单
duplicate_flag=0
        ↓
写 repair_order_flow
        ↓
写 sys_operation_log
        ↓
事务提交
        ↓
进入自动派单
```

# 四、核心设计原则

阶段 4 必须遵守：

```
重复报修 = 风险提示 ≠ 禁止提交
```

即：

> 系统只负责告诉学生“可能已经有人报修过”，最终是否继续提交由学生决定。

不能因为检测到重复就直接：

```
HTTP 409
```

阻止创建。

# 五、为什么重复报修不能直接拦截

实际宿舍维修中：

```
同一个房间 + 同一个故障类型
```

不一定就是同一个故障。

例如：

```
第一次：
宿舍插座损坏

第二次：
同宿舍照明故障
```

如果故障类型都属于：

```
水电
```

系统无法百分百确认是否重复。

因此使用 **疑似重复** ，而不是 **重复禁止**。

# 六、接口设计建议

拆成：

```
重复检测接口
+
正式提交接口
```

# 七、接口 1：疑似重复检测

```
POST /student/repair-orders/check-duplicate
```

请求：

```
RepairOrderDuplicateCheckDTO
```

字段至少：

```
campusId
areaId
buildingId
roomId
faultTypeId
```

## 重复检测接口职责

流程：

```
参数校验
↓
校验当前位置组合是否合法
↓
校验故障类型是否存在且启用
↓
查询24h疑似重复
↓
返回结果
```

该接口：

```
不创建工单
不修改数据库业务状态
```

## 重复检测响应

无重复：

```
{
  "duplicate": false,
  "suspectedOrders": []
}
```

存在重复：

```
{
  "duplicate": true,
  "suspectedOrders": [
    {
      "orderId": 10001,
      "orderNo": "RO202609230001",
      "faultTypeName": "水电维修",
      "locationText":"具体故障说明",
      "reportTime": "2026-09-23T10:20:00",
      "status": 2,
      "statusName": "维修中"
    }
  ]
}
```

## 疑似工单返回字段

只返回学生需要判断的信息：

```
orderId
orderNo
faultTypeName
locationText
reportTime
status
statusName
```

不要返回：

```
维修人员内部信息
管理员备注
调度信息
其他学生隐私信息
```

如果疑似工单不是当前学生提交的，也尤其需要控制字段范围。

# 八、接口 2：正式提交报修

```
POST /student/repair-orders
```

请求：

```
CreateRepairOrderDTO
```

## CreateRepairOrderDTO

包含：

```
campusId
areaId
buildingId
roomId
location_detail

faultTypeId

problem_description

contactName
contactPhone

图片 / 附件信息

confirmDuplicate
```

其中：

```
confirmDuplicate
```

表示：

> 如果检测到疑似重复，学生是否确认仍然提交。

## confirmDuplicate 语义

```
false
→ 如果存在疑似重复，不创建，返回重复提示

true
→ 即使存在疑似重复，也允许创建
```

第一次提交前端：

```
confirmDuplicate = false
```

后端检测到重复后返回。

学生点击：

```
仍然提交
```

前端重新请求：

```
confirmDuplicate = true
```

## 正式提交接口仍然必须重新查重复

即使前端已经调用过：

```
/check-duplicate
```

正式创建时仍然必须：

> 再执行一次重复检测。

不能依赖第一次检测结果。

例如：

```
10:00:00
学生A检测
→ 无重复

10:00:03
学生B提交同一位置同一故障

10:00:05
学生A正式提交
```

如果正式提交不重新查：

> 学生A就永远不知道现在已经存在疑似重复工单。

因此：

```
/check-duplicate
```

只是用户体验预检查。

真正业务判断必须在：

```
POST /student/repair-orders
```

内再次执行。

## 正式提交完整流程

```
1. 获取当前学生
2. 参数校验
3. 校验位置
4. 校验故障类型
5. 查询24h疑似重复
6. 根据 confirmDuplicate 决定是否创建
7. 创建 repair_order
8. 写 repair_order_flow
9. 写 operation log
10. 提交事务
11. 触发自动派单
```

### 当前学生身份

学生身份必须来源：UserContext

例如：

```
Long studentUid = UserContext.getCurrentUserId();
```

禁止由前端指定：

```
CreateRepairOrderDTO.studentUid
```

### 角色限制

正式报修接口：**STUDENT** 才能访问。

### 参数校验

至少校验：

```
campusId 非空
areaId 非空
buildingId 非空
roomId 非空

faultTypeId 非空

faultDescription 非空
faultDescription 长度限制

contactPhone 格式

附件数量
附件类型
```

具体图片规则按照文件模块最终规范执行。

### 位置校验

不仅检查：

```
campusId 存在
areaId 存在
buildingId 存在
roomId 存在
```

还必须验证它们之间的层级关系。

即：

```
area 属于 campus
building 属于 area
room 属于 building
```

并且节点必须：

```
启用
```

### 位置校验流程

```
查询 campus
↓
不存在 / 停用 → 报错

查询 area
↓
不存在 / 停用 → 报错
↓
验证 area.parent = campus

查询 building
↓
不存在 / 停用 → 报错
↓
验证 building.parent = area

查询 room
↓
不存在 / 停用 → 报错
↓
验证 room.parent = building
```

如果最终采用统一位置树表，则按树结构验证。

#### 位置错误

例如：

```
AREA-B-xxxxx
位置不存在
HTTP 404
```

或：

```
AREA-A-xxxxx
位置层级关系不合法
HTTP 400
```

userTip：

```
报修位置已发生变化，请重新选择宿舍位置。
```

### 故障类型校验

必须校验：

```
fault_type 存在
+
status = 启用
```

不存在：

```
FAULT-B-xxxxx
HTTP 404
```

已停用：

```
FAULT-A-xxxxx
HTTP 409 / 400
```

userTip：

```
该故障类型已停用，请重新选择。
```

## 24h 疑似重复规则

重复条件必须全部满足：

```
同校区 AND 同区域 AND 同楼栋 AND 同房间 AND 同故障类型 AND 24小时内 AND 工单状态属于未关闭状态
```

即：

```
campus_id = 当前 campusId

area_id = 当前 areaId

building_id = 当前 buildingId

room_id = 当前 roomId

fault_type_id = 当前 faultTypeId

report_time >= 当前时间 - 24h

status IN 未关闭状态
```

### 未关闭状态定义

不要在 SQL 中直接散落：

```
status IN (0,1,2,3,4,5)
```

由`RepairOrderStatusEnum`统一定义。

提供：

```
RepairOrderStatusEnum.getOpenStatusCodes()
```

待派单、待接单、维修中、待确认、返工中、已中断均属于未关闭状态；6 已完成、7 已取消属于关闭状态。

### 重复检测 SQL

建议 Mapper：

```
selectSuspectedDuplicateOrders
```

逻辑：

```
WHERE campus_id = #{campusId}
  AND area_id = #{areaId}
  AND building_id = #{buildingId}
  AND room_id = #{roomId}
  AND fault_type_id = #{faultTypeId}
  AND report_time >= #{sinceTime}
  AND status IN (...)
```

默认：

```
ORDER BY report_time DESC
```

### 24小时的计算方式

后端计算：

```
LocalDateTime since = LocalDateTime.now().minusHours(24);
```

再传给 Mapper。

不要在 SQL 里到处写：

```
DATE_SUB(NOW(), INTERVAL 24 HOUR)
```

这样：测试更容易、逻辑更集中

**时间边界**：report_time >= now - 24h

> 恰好 24 小时边界算在检测范围内。

超过 24 小时：report_time < now - 24h

不算重复。

### 重复检测索引

当前重复查询条件非常稳定：

```
campus
area
building
room
fault_type
report_time
status
```

应使用数据库已经设计的：

```
idx_order_duplicate_check
```

阶段 4 开发完成后必须使用：

```
EXPLAIN
```

检查重复检测 SQL。

### 重复检测返回逻辑

- 不存在重复：

```
duplicate = false
```

正式提交直接继续创建

- 存在重复且：

```
confirmDuplicate = false
```

则不创建工单，返回：

```
duplicate = true
suspectedOrders = [...]
```

存在重复且：

```
confirmDuplicate = true
```

则允许创建，同时：

```
duplicate_flag = 1
```

### 重复提示不是异常

检测到疑似重复：

> 不是服务端异常。

因此不建议返回：HTTP 409

而是正常：HTTP 200

返回业务结果：

```
{
  "created": false,
  "duplicate": true,
  "suspectedOrders": [...]
}
```

因为系统并没有拒绝业务。

系统只是在等待：

> 学生下一步决定。

## 创建结果 VO

```
CreateRepairOrderResultVO
```

字段：

```
created

duplicate

orderId

orderNo

suspectedOrders
```

### 无重复创建结果

```
{
  "created": true,
  "duplicate": false,
  "orderId": 10010,
  "orderNo": "RO202609230010",
  "suspectedOrders": []
}
```

### 检测到重复但未确认

```
{
  "created": false,
  "duplicate": true,
  "orderId": null,
  "orderNo": null,
  "suspectedOrders": [
    {
      "orderId": 10001,
      "orderNo": "RO202609230001",
      "reportTime": "2026-09-23T10:00:00",
      "status": 2,
      "statusName": "维修中"
    }
  ]
}
```

### 学生坚持提交

```
{
  "created": true,
  "duplicate": true,
  "orderId": 10011,
  "orderNo": "RO202609230011",
  "suspectedOrders": [
    {
      "orderId": 10001,
      "orderNo": "RO202609230001"
    }
  ]
}
```

# 正式创建接口增加幂等控制

学生正式创建报修接口必须增加幂等控制，防止因为：

```
网络超时
客户端重试
浏览器重复发送
前端重复点击
网关重试
```

导致同一个报修请求被执行多次。

需要明确区分：

```
24h 疑似重复检测
→ 判断“这次报修是否和已有工单相似”

接口幂等
→ 判断“这是不是同一个创建请求被重复执行”
```

二者职责不同。

## 幂等目标

对于同一个学生、同一个创建请求：

```
第一次请求
→ 正常创建 repair_order

第二次重复请求
→ 不再次创建 repair_order

第三次重复请求
→ 仍然返回第一次创建结果
```

最终数据库中：

```
只能存在 1 张由该请求创建的工单
```

## 幂等键设计

正式创建接口请求中增加：

```
bizNo
```

例如：

```
{
  "bizNo": "8d4892f9-5f58-45a7-bd03-7cb8a670eb12",
  "campusId": 1,
  "areaId": 10,
  "buildingId": 100,
  "roomId": 1001,
  "faultTypeId": 3,
  "faultDescription": "宿舍灯不亮",
  "confirmDuplicate": false
}
```

`bizNo`：

> 由前端在用户进入本次报修提交动作时生成。

同一次业务提交即使发生网络重试：

```
bizNo 必须保持不变
```

新的报修：

```
生成新的 bizNo
```

**不能使用请求内容作为唯一幂等键**：

不要直接使用：

```
学生ID
+
房间
+
故障类型
+
描述
```

作为幂等键。

因为：

> 学生在不同时间完全可能合法提交内容完全相同的两个报修。

这会把：

```
合法的新业务
```

误判为：

```
重复请求
```

所以幂等必须基于：

```
显式 bizNo
```

而不是业务字段拼接唯一。

## 幂等存储方式

继续沿用：

```
Redis + MySQL 幂等记录
```

双层方案。

Redis 用于：

```
快速拦截短时间重复请求
```

MySQL 用于：

```
最终幂等兜底
```

### Redis 幂等控制

Key：

```
idempotent:repair-order:create:{studentUid}:{bizNo}
```

例如：

```
idempotent:repair-order:create:10001:8d4892f9...
```

使用：

```
SET key value NX EX
```

建议 TTL：

```
10 分钟
```

用于覆盖：

```
重复点击
网络重试
短时间重复请求
```

#### Redis Value 建议

处理过程中可以保存：

```
PROCESSING
```

成功后可以更新：

```
SUCCESS:{orderId}
```

例如：

```
SUCCESS:10086
```

这样重复请求可以快速识别：

```
该请求已经成功执行
```

**不要把Redis 作为唯一幂等保证**

如果出现：

```
请求处理超过 TTL
Redis Key 被清理
Redis 服务异常
应用重启
```

单靠 Redis 可能再次执行创建。

因此数据库还需要：

```
幂等记录表
```

作为最终兜底。

### 数据库幂等记录

新建统一幂等表。

建议字段：

```
biz_no
biz_type
user_id
request_hash
status
result_snapshot
expire_time
create_time
update_time
```

其中：

```
biz_type = REPAIR_ORDER_CREATE
```

唯一约束建议：

```
UNIQUE(biz_type, user_id, biz_no)
```

#### 要保存 request_hash

同一个bizNo只能代表同一份请求。

例如第一次：

```
房间 = 502
故障 = 水电
```

第二次请求却携带同一个 bizNo：

```
房间 = 503
故障 = 空调
```

这不能直接返回第一次结果。

因此需要：request_hash 来校验请求内容是否一致。

#### request_hash 计算范围

建议参与 Hash 的字段：

```
campusId
areaId
buildingId
roomId
faultTypeId
faultDescription
contactPhone
附件引用
confirmDuplicate
studentUid
```

其中必须使用：

```
服务端 UserContext 获取的 studentUid
```

而不是前端值。

#### Hash 算法

使用：SHA-256

对标准化后的请求内容计算。

关键点不是加密，而是：

> 稳定识别同一个 bizNo 是否被复用于不同请求内容。

### 幂等状态

```
0 = PROCESSING
1 = SUCCESS
2 = FAILED
```

项目统一枚举，建立：

```
IdempotentStatusEnum
```

## 首次请求流程

正式创建时：

```
获取当前 studentUid
↓
校验 bizNo
↓
计算 request_hash
↓
Redis SET NX
↓
成功获得处理权
↓
查询 MySQL 幂等记录
↓
不存在
↓
事务内插入 PROCESSING 幂等记录
↓
执行报修创建业务
↓
创建 repair_order
↓
写 repair_order_flow
↓
写 operation log
↓
更新幂等记录 SUCCESS
↓
保存 result_snapshot
↓
事务提交
↓
Redis 更新 SUCCESS
```

## 重复请求：Redis 已成功

如果：

```
Redis Key 已存在
```

不能简单统一返回：

```
请勿重复提交
```

需要区分当前状态。

如果：

```
SUCCESS:{orderId}
```

则：

> 返回第一次成功结果。

例如：

```
{
  "created": true,
  "orderId": 10086,
  "orderNo": "RO202609230086"
}
```

这样客户端即使：

```
第一次服务端已经成功
但响应在网络中丢失
```

第二次重试仍然能拿到真实成功结果。

## 重复请求：正在处理

如果 Redis：

```
PROCESSING
```

说明同一个请求当前仍在执行。

可以返回：

```
HTTP 409
```

例如错误码：

```
ORDER-A-xxxxx
```

userTip：

```
报修正在提交中，请勿重复操作。
```

也可以根据前端设计返回业务处理中状态。

第一期建议：

```
HTTP 409
```

更简单。

## MySQL 幂等兜底

即使 Redis Key 不存在，也必须检查：

```
biz_type
+
user_id
+
biz_no
```

对应记录。

### 已 SUCCESS

如果：

```
request_hash 相同
```

直接返回：

```
result_snapshot
```

不执行任何创建 SQL。

### 已 SUCCESS 但 Hash 不同

说明客户端错误复用了：

```
bizNo
```

返回：

```
HTTP 409
```

userTip：

```
当前提交标识已被使用，请刷新页面后重新提交。
```

### PROCESSING

说明：

```
同一请求仍在执行
```

返回处理中提示。

## result_snapshot

成功后建议保存最小结果：

```
{
  "orderId": 10086,
  "orderNo": "RO202609230086",
  "duplicate": false
}
```

不需要保存整张工单详情。

目的只是：

> 重复请求时可以返回与第一次一致的业务结果。

## 幂等事务边界

以下操作必须在同一个数据库事务：

```
插入幂等 PROCESSING 记录
↓
创建 repair_order
↓
写 repair_order_flow
↓
写 operation log
↓
更新幂等 SUCCESS
```

如果中间任何一步失败：

```
数据库事务整体回滚
```

这样不会出现：

```
工单已经创建
但幂等记录仍不存在
```

导致重试再次创建。

## 异常后的 Redis 处理

如果事务失败：

```
删除 Redis 幂等 Key
```

允许客户端：

```
使用同一 bizNo
```

重新提交。

因为数据库事务已经回滚，没有产生真实业务结果。

### FAILED 状态的处理

如果使用事务整体回滚，那么：

```
PROCESSING 记录
```

也会回滚。

第一版其实可以不必强行保存：

```
FAILED
```

记录。

即：

```
成功
→ 保存 SUCCESS

失败
→ 数据库回滚 + 删除 Redis Key
```

更简单。

如果以后需要：

```
失败请求审计
```

再保留 FAILED。

## 重复检测与幂等的执行顺序

正式创建接口推荐：

```
1. 获取当前学生
2. 校验 bizNo
3. 幂等前置判断
4. 参数校验
5. 校验位置
6. 校验故障类型
7. 重新执行24h重复检测
8. 判断 confirmDuplicate
9. 创建 repair_order
10. 写 flow
11. 写 operation log
12. 更新幂等成功结果
13. 事务提交
14. 进入自动派单
```

但需要注意：

> “检测到疑似重复且学生尚未确认”并没有真正创建业务数据。

因此这种返回不能把当前 bizNo 永久标记为创建成功。

## 疑似重复提示时的 bizNo 处理

场景：

```
第一次正式提交
bizNo = X
confirmDuplicate = false
↓
发现疑似重复
↓
返回 created=false
```

此时学生点击：

```
仍然提交
```

可以继续使用：

```
同一个 bizNo = X
```

并设置：

```
confirmDuplicate = true
```

因此：

> 疑似重复提示阶段不能把 bizNo 锁死为 SUCCESS。

### request_hash 与 confirmDuplicate 的特殊处理

因为：

```
confirmDuplicate=false
```

变成：

```
confirmDuplicate=true
```

是正常的两阶段提交过程。

因此建议：

> `request_hash` 不包含 `confirmDuplicate`。

Hash 只包含真正的报修业务内容：

```
位置
故障类型
故障描述
联系方式
附件
```

这样：

```
同一个 bizNo
+
同一份报修内容
+
confirmDuplicate 从 false → true
```

仍然被视为：

```
同一个业务请求
```

这是本项目比较关键的一个细节。

## 疑似重复返回后的 Redis Key

如果第一次：

```
confirmDuplicate=false
```

发现疑似重复且没有创建工单：

```
删除当前 Redis PROCESSING Key
```

让学生第二次：

```
confirmDuplicate=true
```

能够重新获取执行权。

MySQL 也不写：

```
SUCCESS 幂等记录
```

因为业务还没真正完成。

## 幂等流程与重复检测最终关系

完整流程变为：

```
客户端生成 bizNo
        ↓
正式提交
        ↓
幂等校验
        ↓
参数 / 位置 / 故障校验
        ↓
24h重复检测
        ↓
存在重复？
     ┌──────────────┐
     │              │
    否              是
     │              ↓
     │      confirmDuplicate？
     │         │          │
     │        否          是
     │         │          │
     │      返回提示       │
     │      不占用bizNo    │
     │                    │
     └──────────┬─────────┘
                ↓
         创建 repair_order
                ↓
         duplicate_flag
                ↓
         flow + operation log
                ↓
         幂等 SUCCESS
                ↓
         返回创建结果
```

## 幂等控制不能依赖 duplicate_flag

再次强调：

```
duplicate_flag
```

只能表示：

> 该工单创建时发现了业务上相似的工单。

不能表示：

```
这是同一个 HTTP 请求重复执行
```

# 前端配合

（这部分写到接口说明中，做为接口契约。）

进入报修页面或开始填写报修表单时生成：

```
bizNo
```

例如使用：

```
UUID
```

第一次提交：

```
bizNo = X
confirmDuplicate = false
```

存在疑似重复后：

学生点击仍然提交：

```
bizNo = X
confirmDuplicate = true
```

如果发生：

```
网络超时
```

重试：

```
bizNo 仍然 = X
```

只有：

```
本次报修已经明确完成
或
用户放弃后重新发起全新的报修
```

才生成新的 bizNo。

# 幂等验收场景

## Case 1：正常请求

```
bizNo = A
```

第一次：

```
创建工单 10001
```

数据库：

```
只有1条 repair_order
```

## Case 2：成功后网络响应丢失

第一次：

```
bizNo=A
↓
工单创建成功
↓
客户端没收到响应
```

客户端重试：

```
bizNo=A
```

结果：

```
返回原 orderId=10001
```

数据库：

```
仍然只有1条工单
```

## Case 3：连续点击两次

并发：

```
请求1 bizNo=A
请求2 bizNo=A
```

结果：

```
只有一个请求获得执行权
```

最终：

```
repair_order 只有1条
```

## Case 4：相同业务内容但不同 bizNo

```
请求1 bizNo=A
请求2 bizNo=B
```

如果符合 24h 重复规则：

```
走疑似重复提示
```

而不是幂等拦截。

这验证：

> 幂等和业务重复检测没有混淆。

## Case 5：同一 bizNo 不同业务内容

第一次：

```
bizNo=A
房间=502
```

第二次：

```
bizNo=A
房间=503
```

结果：

```
HTTP 409
```

不得：

```
创建第二张工单
```

## Case 6：疑似重复后确认

第一次：

```
bizNo=A
confirmDuplicate=false
```

检测到疑似重复：

```
created=false
```

第二次：

```
bizNo=A
confirmDuplicate=true
```

结果：

```
创建成功
duplicate_flag=1
```

不能被幂等机制误拦截。

## Case 7：创建过程异常

```
bizNo=A
```

执行到：

```
flow INSERT
```

失败。

要求：

```
repair_order 回滚
幂等数据库记录回滚
Redis Key 删除
```

随后客户端使用：

```
bizNo=A
```

重新提交：

```
允许重新执行
```

# 阶段 4 创建链路最终版

阶段 4 正式创建链路调整为：

```
学生提交
↓
UserContext 获取学生
↓
bizNo 幂等检查
↓
参数校验
↓
位置校验
↓
故障类型校验
↓
24h 疑似重复检测
↓
重复但未确认
→ 返回提示
→ 不创建
→ 释放幂等占用
↓
无重复 / 已确认重复
↓
事务开始
↓
写幂等 PROCESSING
↓
创建 repair_order
↓
设置 duplicate_flag
↓
写 repair_order_flow
↓
写 operation log
↓
幂等记录 SUCCESS + result_snapshot
↓
事务提交
↓
Redis 标记 SUCCESS
↓
进入自动派单
```

# 阶段 4 新增验收项

在原有重复报修验收基础上增加：

- 创建接口必须携带 `bizNo`；
- 同一 `bizNo` 重复请求只创建一张工单；
- 成功后重复请求返回原创建结果；
- 并发重复请求不会创建两张工单；
- 同一 bizNo 不同请求内容被拦截；
- 不同 bizNo 的相同报修内容不会被幂等机制拦截；
- 不同 bizNo 仍按 24h 疑似重复规则判断；
- 疑似重复未确认时不永久占用 bizNo；
- 同一 bizNo 可用于 `confirmDuplicate=false → true`；
- `request_hash` 不因 confirmDuplicate 改变；
- 创建事务失败后可以使用原 bizNo 重试；
- Redis 异常时 MySQL 唯一约束仍可兜底；
- 数据库最终不会因网络重试产生重复工单。



# repair_order 创建

通过校验后创建：

```
repair_order
```

至少写入：

```
order_no

student_uid

campus_id
area_id
building_id
room_id
location_detail

fault_type_id
problem_description

contact_name
contact_phone

image_urls（可选）

status

duplicate_flag

report_time

create_time
update_time
```

具体字段以最终 DDL 为准。

**初始工单状态**：创建成功后status = 0-待派单

然后进入自动派单，如果下一阶段自动派单同步成功：

```
0-待派单
→ 1-待接单
```

如果派单失败：

```
仍然待派单 / 异常标记
```

按后续自动派单设计处理。

### 工单编号生成

需要生成order_no

要求：唯一、可读、不可依赖前端

例如：

```
RO + 日期 + 序列
```

具体格式可按项目最终规则。

数据库必须：

```
UNIQUE(order_no)
```

兜底。

### duplicate_flag

不存在重复：

```
duplicate_flag = 0
```

存在重复并坚持提交：

```
duplicate_flag = 1
```

### 是否记录关联的疑似工单

如果当前 `repair_order` 已有：

```
duplicate_source_order_id
```

则可以记录：

```
最近一张 或 主要关联工单
```

### 附件处理

如果报修支持图片：

```
前端先上传 MinIO
↓
获得 objectId / objectName
↓
创建工单时提交附件引用
```

不要：

```
创建工单事务
+
直接上传大文件
```

放在一个数据库事务内。

# 九、任务 4.1：工单流转记录

工单创建后必须写：

```
repair_order_flow
```

第一条流转：

```
学生提交报修
```

## 首条 flow 内容

建议记录：

```
order_id
from_status = null
to_status = 1-待派单

operator_type = CREATE
operator_id = currentUserId
operator_role = 1学生

remark = 学生提交报修

flow_time
```

具体字段按照最终 DDL。

**flow 的意义：**是工单生命周期业务时间线。

后续详情已经建立：

```
repair_order_flow
```

查询能力。

因此工单一创建：

> 详情时间线立即出现“学生提交报修”。

# 十、任务 4.2：操作日志

同时记录：

```
sys_operation_log
```

表示：

> 某个用户执行了创建报修操作。

## 操作日志内容

至少：

```
operator_id
operator_role
operation_type
business_type
business_id
request_uri
operation_result
operation_time
```

具体以最终表结构为准。

## Flow 与 OperationLog 区别

必须继续保持：

```
repair_order_flow
→ 工单业务生命周期

sys_operation_log
→ 系统操作审计
```

不能互相替代。

## 事务设计

以下必须在一个事务中：

```
INSERT repair_order
↓
INSERT repair_order_flow
↓
INSERT operation log
```

如果任意一步失败：

```
全部回滚
```

不能出现：

```
有工单
但没有初始 flow
```

# 自动派单调用边界

```
创建工单事务成功
↓
再进入自动派单
```

不要把**复杂派单算法**直接塞进创建报修事务里。

否则：

```
派单慢
↓
学生提交接口事务长
↓
数据库锁持有时间增长
```

# 阶段 4 与自动派单的衔接

当前阶段可以定义：

```
RepairDispatchService.dispatch(orderId)
```

但具体实现进入阶段 5。

阶段 4 暂时：

```
创建成功
↓
调用 dispatch 接口占位
```

> 工单创建事务和派单事务逻辑上分开。

即：

```
创建失败
→ 不派单

创建成功
→ 才派单
```

## 并发重复检测问题

重复检测只提示，不要求强一致去重

例如：

```
学生A
学生B
同时提交同位置同故障
```

两个人都可能在重复检测时：

```
查不到对方
```

然后都创建。

这是允许的,因为需求本身就是：

```
提示重复
而不是禁止重复
```

所以：

> 不需要为疑似重复检测加 Redis 锁、数据库锁或唯一索引。

这是阶段 4 一个重要边界。

## 不要把重复检测做成幂等控制

必须区分：重复报修检测 和 同一次请求重复提交

前者是：

> 业务相似性提示。

后者才是：

> 接口幂等问题。

### 防止按钮连点

正式创建接口仍需要基础防重复提交能力。

例如：

```
前端提交按钮 loading
```

作为第一层。

正式创建接口必须使用本方案前文确定的 `bizNo + Redis + MySQL` 双层幂等机制；前端按钮 loading 只是交互层补充，不能替代服务端幂等。

不要拿24h 疑似重复检测来代替接口幂等。

## 学生取消行为

存在疑似重复后：

```
学生点击取消
```

实际服务端行为是：

```
不再调用正式创建
```

或者第一次正式提交已经返回：

```
created = false
```

此时：

```
repair_order
repair_order_flow
operation log
```

均不能产生创建记录。

## 管理员后续如何看到 duplicate_flag

阶段 3 管理员查询已经支持：

```
疑似重复
```

筛选。

阶段 4 创建：

```
duplicate_flag = 1
```

之后：

> 管理员列表无需新增查询结构即可直接筛出这些工单。

## 阶段 4 服务划分建议

不要拆太多。

建议：

```
StudentRepairOrderService
```

负责：

```
checkDuplicate()

createRepairOrder()
```

同时复用：

```
RepairOrderMapper
RepairOrderFlowMapper
OperationLogService
```

## 重复检测逻辑抽取

建议独立：

```
RepairOrderDuplicateService
```

# 建议接口清单

两个接口：

```
POST /student/repair-orders/check-duplicate

POST /student/repair-orders
```

如果图片上传已有公共接口：

```
POST /files/upload
```

直接复用，不在本阶段重复建设。

# check-duplicate 接口验收

## Case 1：不存在重复

输入：

```
东校区
A区
1号楼
502
水电
```

过去 24h 无符合工单。

返回：

```
duplicate = false
suspectedOrders = []
```

## Case 2：存在重复

过去 24h 已有：

```
同校区
同区域
同楼栋
同房间
同故障类型
维修中
```

返回：

```
duplicate = true
```

并返回疑似工单。

# 正式创建验收：不存在重复

条件：

```
不存在疑似重复
```

学生提交：

```
confirmDuplicate = false
```

结果：

```
创建成功
duplicate_flag = 0
```

同时：

```
repair_order
repair_order_flow
operation log
```

均存在。

# 正式创建验收：存在重复但学生取消

存在疑似重复。

第一次提交：

```
confirmDuplicate = false
```

结果：

```
created = false
duplicate = true
```

数据库：

```
repair_order 不新增

repair_order_flow 不新增

创建类 operation log 不新增
```

# 正式创建验收：学生坚持提交

存在疑似重复。

提交：

```
confirmDuplicate = true
```

结果：

```
创建成功
duplicate_flag = 1
```

同时：

```
repair_order_flow
operation log
```

正常写入。

## 验收：已完成工单

过去 24h 有完全相同工单：

```
status = 已完成
```

结果：

```
不算重复
```

创建：

```
duplicate_flag = 0
```

## 验收：已取消工单

如果状态：

```
已取消
```

同样属于关闭状态。

结果：

```
不算重复
```

## 验收：超过24小时

存在完全相同工单：

```
report_time < now - 24h
```

结果：

```
不算重复
```

## 验收：不同故障类型

位置完全相同：

```
校区相同
区域相同
楼栋相同
房间相同
```

但是：

```
fault_type_id 不同
```

结果：

```
不算重复
```

## 验收：位置不同

以下任意不同：

```
校区
区域
楼栋
房间
```

都：

```
不算重复
```

## 验收：24h 边界

创建一张工单：

```
正好 now - 24h
```

如果规则使用：

```
>=
```

则：

```
算疑似重复
```

需要测试。

## 验收：非法位置

例如：

```
campus = 东校区
area = 西校区A区
```

即使 ID 都存在：

```
仍然必须提交失败
```

返回：

```
HTTP 400
AREA-A-xxxxx
```

## 验收：停用故障类型

学生使用：

```
status = 0
```

故障类型提交。

结果：

```
不允许创建
```

## 验收：身份伪造

学生A请求中手动加入：

```
studentUid = 学生B
```

即使 DTO 出现未知字段：

> 最终创建的 `student_uid` 必须仍然是学生A。

## 验收：事务回滚

模拟：

```
repair_order INSERT 成功
↓
repair_order_flow INSERT 失败
```

最终：

```
repair_order 不得残留
```

同理：

```
operation log 写失败
```

按照最终审计事务策略决定是否回滚。

如果已经确定操作日志必须与业务强一致：

```
必须整体回滚
```

## 重复检测 EXPLAIN

必须针对：

```
同位置
同故障类型
24h
未关闭
```

SQL 执行：

```
EXPLAIN
```

确认优先使用：

```
idx_order_duplicate_check
```

避免全表扫描。

# 阶段 4 前端交互

学生填写：

```
位置
故障类型
故障描述
联系方式
图片
```

点击：

```
提交报修
```

## 无重复前端流程

```
点击提交
↓
正式创建
↓
created=true
↓
提示：
报修提交成功
↓
进入工单详情
```

## 存在重复前端流程

```
点击提交
↓
后端检测到疑似重复
↓
created=false
duplicate=true
↓
弹窗
```

例如：

```
该宿舍24小时内已有相同故障类型的未完成报修。

工单号：ROxxxx
当前状态：维修中
报修时间：xxxx

是否仍要继续提交？
```

按钮：

```
取消

仍然提交
```

## 学生取消

点击：

```
取消
```

关闭弹窗。

不发送第二次创建请求。

## 学生仍然提交

第二次请求：

```
confirmDuplicate = true
```

后端：

```
再次执行重复检测
↓
创建工单
↓
duplicate_flag = 1
```

## 前端禁止自行决定 duplicate_flag

前端不能提交：

```
duplicateFlag = 1
```

数据库：

```
duplicate_flag
```

必须由后端根据重复检测结果设置。

# 错误码建议

本阶段增加：

```
ORDER
```

相关错误码。

例如：

```
ORDER-A-00001
报修信息不完整
HTTP 400
```

如果参数统一使用：

```
COMMON-A
```

也可以不单独增加。

位置：

```
AREA-A-xxxxx
位置层级关系不合法
HTTP 400
AREA-B-xxxxx
位置不存在
HTTP 404
```

故障：

```
FAULT-B-xxxxx
故障类型不存在
HTTP 404
FAULT-A-xxxxx
故障类型已停用
HTTP 409
```

疑似重复：

> 不使用错误码。

因为它不是错误。

# 并行边界

可以拆：

## 工作包 A：重复检测

```
DuplicateCheckDTO
重复查询 SQL
疑似工单 VO
索引验证
```

## 工作包 B：创建工单

```
CreateRepairOrderDTO
位置校验
故障类型校验
order_no
repair_order INSERT
```

## 工作包 C：审计

```
repair_order_flow
operation log
事务
```

## 工作包 D：前端

```
报修表单
重复提示弹窗
确认再次提交
提交结果
```

A/B/C 与前端可以并行。

# 推荐开发顺序

```
4-01 CreateRepairOrderDTO

4-02 DuplicateCheckDTO

4-03 SuspectedOrderVO

4-04 位置合法性校验

4-05 故障类型校验

4-06 重复检测 Mapper SQL

4-07 重复检测 Service

4-08 check-duplicate 接口

4-09 order_no 生成

4-10 repair_order 创建

4-11 duplicate_flag 设置

4-12 repair_order_flow 首条记录

4-13 operation log

4-14 创建事务

4-15 正式创建接口

4-16 confirmDuplicate 流程

4-17 前端报修表单

4-18 疑似重复弹窗

4-19 学生取消

4-20 学生坚持提交

4-21 阶段3列表联调

4-22 阶段3详情联调

4-23 EXPLAIN 重复查询

4-24 边界测试

4-25 阶段验收
```



# 阶段 4 核心验收矩阵

| 场景                   | 是否提示重复 | 是否创建 | duplicate_flag |
| ---------------------- | ------------ | -------- | -------------- |
| 不存在重复             | 否           | 是       | 0              |
| 存在重复，学生未确认   | 是           | 否       | -              |
| 存在重复，学生坚持提交 | 是           | 是       | 1              |
| 相同条件但已完成       | 否           | 是       | 0              |
| 相同条件但已取消       | 否           | 是       | 0              |
| 相同条件但超过24h      | 否           | 是       | 0              |
| 同位置不同故障类型     | 否           | 是       | 0              |
| 不同房间相同故障       | 否           | 是       | 0              |

# Definition of Done

阶段 4 完成后，学生应该可以完成第一条正式业务链路：

```
登录
↓
选择报修位置
↓
选择故障类型
↓
填写故障描述
↓
提交报修
↓
系统检查24h疑似重复
↓
无重复
→ 创建

有重复
→ 提示已有工单
→ 学生取消
   → 不创建

→ 学生仍然提交
   → duplicate_flag=1
   → 创建
↓
repair_order
↓
repair_order_flow
↓
operation log
↓
阶段3工单列表立即可见
↓
阶段3工单详情立即可见
↓
等待进入自动派单
```

本阶段最重要的业务约束：

> **疑似重复检测是提醒机制，不是去重约束。**

因此：

```
不使用唯一索引阻止
不使用数据库锁阻止
不使用 Redis 锁阻止
不返回冲突错误阻止
```

只负责：

```
检测
↓
提示
↓
学生决定
↓
记录 duplicate_flag
```

阶段 4 完成后进入：

> **阶段 5：自动派单、派单失败处理与待接单状态建立。**
