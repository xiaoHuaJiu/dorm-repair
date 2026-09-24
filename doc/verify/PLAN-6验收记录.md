# PLAN-6 验收记录

## 验收范围

- 当前负责人接单及完成时限计算；
- 普通维修过程和材料使用记录；
- 中断、恢复、提交维修结果；
- 状态条件更新、事务、工单流转和操作日志；
- 维修人员数据权限与学生材料明细隐藏；
- 接口联调说明同步。

## 自动化验证

| 验收项 | 命令或方式 | 预期/结果 | 截图 |
|---|---|---|---|
| 维修主流程专项测试 | `mvn -Dtest=WorkerRepairOrderCommandServiceTest,RepairOrderDetailServiceTest,Plan6MySqlIntegrationTest test` | 8 个测试通过，0 失败、0 错误 | `images/PLAN-6-维修主流程专项测试.png` |
| MySQL 全链路集成测试 | `mvn -Dtest=Plan6MySqlIntegrationTest test` | 待接单到待确认全链路、4 条过程、1 条材料、4 条流转、6 条日志通过 | `images/PLAN-6-MySQL全链路集成测试.png` |
| 并发状态测试 | `mvn -Dtest=Plan6ConcurrencyMySqlTest test` | 1 个真实 MySQL 并发测试通过，两个接单更新仅一个成功 | `images/PLAN-6-并发状态测试.png` |
| Maven 全量回归 | 加载本地测试环境变量并关闭开发种子初始化后执行 `mvn test` | 92 个测试通过，0 失败、0 错误 | `images/PLAN-6-Maven全量测试.png` |
| Maven 打包 | `mvn -DskipTests package` | BUILD SUCCESS | `images/PLAN-6-Maven打包.png` |
| 接口冒烟 | 最新后端登录、角色保护及维修接口路由检查 | 维修账号登录成功；不存在工单 404；学生越权 403 | `images/PLAN-6-接口冒烟测试.png` |
| 服务启动与健康检查 | 重启后访问 `/api/health` | HTTP 200、`code=0`、`status=UP`，监听 PID 28904 | `images/PLAN-6-后端启动与健康检查.png` |

## 数据与接口结论

- 未新增业务表，复用 `repair_order`、`repair_process_record`、`repair_material_usage`、`repair_order_flow`、`sys_operation_log`；
- 状态变化均使用负责人和原状态条件更新；普通过程和材料在数据库行锁内再次校验；
- 接单、中断、恢复、提交结果与对应过程、流转、日志处于同一事务；
- 学生详情不查询也不返回材料明细；
- 接口统一返回 `Result`，成功为 `code=0`。

## 尚未纳入本阶段

- 文件上传接口；
- 接单超时自动转派、完成超时扫描；
- 主动转派、请假转派和管理员人工干预；
- 学生确认、返工申请和维修评价；
- 前端页面实现。
