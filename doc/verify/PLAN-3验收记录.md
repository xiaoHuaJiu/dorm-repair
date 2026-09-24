# PLAN-3 验收记录

## 一、记录说明

- 对应需求：`doc/requirements/PLAN-3.md`
- 阶段名称：工单通用查询骨架与详情聚合
- 验收日期：2026-09-23
- 验收结论：通过

## 二、实现范围

- 区域树升级为单位/校区、区域、楼栋、房间四级，房间只能挂到楼栋。
- 管理员、维修人员、学生三个工单分页入口。
- 管理员组合筛选、稳定排序、PageHelper 分页和后端超时计算。
- 学生按 `student_uid`、维修人员按 `current_assignee_id` 的真实 SQL 数据范围。
- 三角色详情入口、统一资源访问校验及过程/材料/返工/评价/流转聚合。
- 数字工单错误码 17001、17002 和统一 HTTP 404/403。

## 三、验收结果

| 验收项 | 结果 | 证据 |
|---|---|---|
| 四级区域父子关系 | 通过 | `FoundationConfigurationServiceTest.roomCanOnlyBeCreatedBelowBuilding` |
| 学生只能查询本人列表 | 通过 | `RepairOrderScopeAccessTest`、`Plan3AuthorizationIntegrationTest` |
| 维修人员 userId 正确转换 workerId | 通过 | `RepairOrderScopeAccessTest` |
| 维修人员只能查询当前指派工单 | 通过 | `Plan3AuthorizationIntegrationTest` |
| 管理员可查询全部工单 | 通过 | `Plan3AuthorizationIntegrationTest` |
| 客户端不能提交 studentUid/currentAssigneeId | 通过 | `RepairOrderContractTest` |
| 房间、异常、重复、超时组合筛选 | 通过 | `RepairOrderMapperMySqlTest`（真实 MySQL） |
| 超时标签由后端计算 | 通过 | `RepairOrderQueryServiceTest` |
| 详情先鉴权再查询子表 | 通过 | `RepairOrderDetailServiceTest` |
| 详情空集合和空评价 | 通过 | `RepairOrderDetailServiceTest`、`Plan3AuthorizationIntegrationTest` |
| 详情按角色裁剪管理员字段 | 通过 | 学生响应不含 `duplicateOrderId`，管理员响应保留该字段；`Plan3AuthorizationIntegrationTest` |
| 不存在 404、越权 403 | 通过 | 数字错误码契约及真实接口集成测试 |
| 列表无一对多 JOIN | 通过 | `RepairOrderMapper.xml` 仅关联用户、故障类型、四级位置和当前维修人员 |

## 四、SQL 与索引验证

对学生范围、维修人员范围、接单超时和处理超时查询执行了 MySQL `EXPLAIN`。学生查询实际使用 `idx_order_student`；维修人员和超时查询的 `possible_keys` 包含既有 `idx_order_assignee_status`、`idx_order_accept_timeout`、`idx_order_complete_timeout`。当前数据库数据量很小，优化器部分选择状态索引，本阶段不据此新增重复索引。

## 五、自动化验证

加载根目录 `.env` 后执行：

```powershell
cd backend
mvn "-Dmaven.repo.local=..\.m2\repository" test package
```

结果：72 个测试通过，失败 0，错误 0；Maven 打包成功。

前端执行 `npm test -- --run` 和 `npm run build`：9 个测试通过，生产构建成功；仅有既存的大包体积提示。数据库结构脚本验证正式表 17/17、关键索引 27/27、外键 0。Docker 中 MySQL、Redis、MinIO 均为健康状态，Nginx 正常运行；后端 `/api/health` 返回 HTTP 200，未登录访问工单接口返回 HTTP 401 / 10002。

## 六、阶段边界与遗留项

- 本阶段只实现查询和详情骨架，不创建报修工单，不执行自动派单或状态流转写入。
- 详情主信息已通过专用响应 VO 裁剪内部维护字段，管理员专用的重复工单关联 ID 不向学生和维修人员开放；后续新增管理员内部备注或调度失败详情时仍须显式裁剪。
- 当前数据量不足以评价大规模查询性能，索引结论需在接近真实数据规模后重新执行 `EXPLAIN ANALYZE`。
- 前端业务页面仍未实现，本阶段提供接口契约供后续联调。

## 七、最终结论

三角色已能按真实身份范围分页查询工单，并在资源归属校验后读取统一聚合详情。后续报修、派单和维修业务写入相应表后，可直接复用本阶段查询体系。
