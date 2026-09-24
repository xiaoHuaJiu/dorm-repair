# PLAN-4 验收记录

## 一、实现范围

- 学生报修疑似重复预检查与正式创建接口。
- 四级位置和故障类型有效性校验。
- 24 小时未关闭工单疑似重复提示。
- `RedisConstant` 统一 Redis Key，Redis + MySQL 双层幂等。
- 工单、首条流转、操作日志和幂等成功记录的事务创建。
- 自动派单只保留提交后的空实现边界。
- 按用户要求不修改前端，只在联调文档约束 `bizNo` 和按钮防重复点击。

## 二、验收证据

| 验收项 | 结果 | 证据 |
|---|---|---|
| Redis Key 集中管理 | 通过 | `RedisConstant.repairOrderCreateKey`；生产代码仅常量类包含前缀字面量 |
| 公开 DTO 不接受 studentUid/duplicateFlag | 通过 | `Plan4ContractTest` |
| 开放状态统一为 0—5 | 通过 | `RepairOrderStatusEnum.getOpenStatusCodes()` |
| 24 小时边界由后端计算且使用 `>=` | 通过 | `RepairOrderDuplicateServiceTest`、Mapper SQL |
| 疑似重复未确认不创建、不占用 bizNo | 通过 | `Plan4MySqlRedisIntegrationTest` |
| 确认重复后 duplicate_flag=1 | 通过 | `Plan4MySqlRedisIntegrationTest` |
| 同一 bizNo 重试返回原结果 | 通过 | `Plan4MySqlRedisIntegrationTest` |
| 同一 bizNo 修改业务内容返回 409/17004 | 通过 | `Plan4MySqlRedisIntegrationTest` |
| 工单、首条 flow、操作日志、幂等 SUCCESS 同时存在 | 通过 | 真实 MySQL 集成断言 |
| 统一 Result、学生角色入口 | 通过 | MockMvc + JWT 集成测试 |

加载根目录 `.env` 后执行：

```powershell
cd backend
mvn "-Dmaven.repo.local=..\.m2\repository" clean test package
```

结果：78 个测试通过，失败 0、错误 0，Maven 打包成功。

执行 `scripts/verify-database-schema.ps1`：正式表 18/18、关键索引 28/28、外键 0。重复检测 SQL 的 `EXPLAIN` 实际选择 `idx_order_duplicate_check`，访问类型为 `range`，显示 `Using index condition`。Redis 返回 `PONG`；MySQL、Redis、MinIO 均健康，Nginx 正常运行。

## 三、阶段边界

- 未开发学生报修前端页面。
- 未实现图片上传接口，只接收已经上传完成的附件引用。
- 未实现自动派单算法、接单、维修过程写入及后续状态流转。
- 当前测试覆盖顺序重试和真实 Redis 门闩；更高强度并发压测留到部署前性能验收。
