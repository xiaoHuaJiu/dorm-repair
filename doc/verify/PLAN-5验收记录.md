# PLAN-5 验收记录

## 验收范围

- 统一自动派单入口与来源状态校验；
- 技能、区域、人员状态、请假、排除人员和工作量排序；
- 30 分钟有效工作时间与 2026 年节假日/调休配置；
- 工单 CAS 更新和流转记录事务；
- 事务提交后异步派单、有界线程池、失败告警与定时补偿；
- 数据库、接口联调说明和架构文档同步。

## 自动化验证

| 验收项 | 命令或方式 | 结果 | 截图 |
|---|---|---|---|
| 自动派单专项单元测试 | `mvn -Dtest=WorkingTimeCalculatorTest,RepairDispatchServiceTest,RepairDispatchRecoveryJobTest test` | 6 个测试通过 | `images/PLAN-5-自动派单专项测试.png` |
| MySQL 自动派单集成测试 | `mvn -Dtest=Plan5MySqlIntegrationTest test` | 已通过 | `images/PLAN-5-MySQL自动派单集成测试.png` |
| 后端全量回归 | 加载本地测试环境变量后执行 `mvn test` | 85 个测试通过，0 失败、0 错误 | `images/PLAN-5-Maven全量测试.png` |
| Maven 打包 | `mvn -DskipTests package` | BUILD SUCCESS | `images/PLAN-5-Maven打包.png` |
| 数据库结构 | `scripts/verify-database-schema.ps1` | 20/20 张正式表、32/32 个关键索引、0 外键 | `images/PLAN-5-数据库结构与节假日数据.png` |
| 自动派单候选查询执行计划 | MySQL `EXPLAIN` | 技能查询使用 `idx_fault_worker`，关联查询使用主键/范围索引 | `images/PLAN-5-候选查询EXPLAIN.png` |
| 服务启动与健康检查 | 重启后访问 `/api/health` | HTTP 200，`code=0`，`status=UP`，监听进程 PID 22288 | `images/PLAN-5-后端启动与健康检查.png` |

## 数据库结果

- 新增 `repair_holiday_calendar` 和 `repair_dispatch_alert`，正式表从 18 张增加到 20 张；
- `repair_holiday_calendar` 已导入 2026 年 39 条日期配置；
- 主结构 SQL 不包含 `DROP TABLE`、`DROP DATABASE` 或外键；
- 既有 `scaffold_persistence_check` 是早期 Docker 持久化验收辅助表，不属于 20 张正式业务表。

## 接口影响

PLAN-5 不新增 HTTP 接口。`POST /api/student/repair-orders` 的成功响应只确认工单持久化；派单在事务提交后异步执行，前端必须通过工单查询获得最新状态和负责人。

## 尚未纳入本阶段

- 管理员派单告警列表、处理和人工派单接口；
- 接单超时、请假和转派业务对统一派单服务的正式调用；
- SSE 实时通知。
