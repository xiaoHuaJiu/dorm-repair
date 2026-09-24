# PLAN-10 验收记录

## 验收范围

- 接单剩余 10 分钟、5 分钟提醒。
- 接单到期后的二次状态校验、原负责人排除和统一自动改派。
- 维修完成剩余 10 分钟、5 分钟与到期提醒。
- 维修完成超时不修改工单状态。
- 提醒唯一键防重、截止时间变化后的新一轮提醒能力。
- 单张工单处理失败不终止同批次其他工单。
- Cron、任务开关和批次大小配置化。

## 验收结果

- `RepairReminderServiceTest`：专项单元测试覆盖提醒窗口、接收人、完成超时状态边界、接单超时再校验、原负责人排除和批次异常隔离。
- `Plan10MySqlIntegrationTest`：真实 MySQL 验证重复扫描只生成一条提醒、维修超时不改变状态、接单超时成功改派并生成新截止时间。
- 定向执行 `mvn '-Dtest=RepairReminderServiceTest,Plan10MySqlIntegrationTest,MapperXmlContractTest,MapperMySqlIntegrationTest,EntitySchemaConsistencyTest' test`，共执行 12 项测试，失败 0、错误 0、跳过 0。
- 接单扫描 SQL 的 `EXPLAIN key=idx_order_accept_timeout`。
- 完成扫描 SQL 的 `EXPLAIN key=idx_order_complete_timeout`。
- 按用户最新要求不生成测试截图，不执行无关全量测试。

## 配置

- `REPAIR_TASK_ENABLED`：统一任务开关，默认启用。
- `REMINDER_SCAN_CRON`：10 分钟和 5 分钟提醒扫描，默认每分钟。
- `TIMEOUT_SCAN_CRON`：到期扫描，默认每分钟。
- `TASK_BATCH_SIZE`：单次扫描上限，默认 100。

## 未完成项

- PLAN-9 使用独立的请假生效、结束任务配置；PLAN-10 的提醒与超时任务不改变其调度方式。
- 当前没有独立站内信表或消息中心；`repair_reminder_record` 作为第一期站内提醒事实记录，`send_status=1` 表示记录已生成。
