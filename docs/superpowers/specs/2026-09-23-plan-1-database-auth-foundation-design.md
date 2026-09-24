# PLAN-1 数据库与权限底座设计规格

## 1. 目标与边界

本阶段在阶段 0 公共能力之上完成数据库、基础数据访问、JWT 登录认证、三角色授权和数据范围规范。完成后，后续业务模块能够直接使用稳定的表结构、Entity、Mapper、枚举、`LoginUser`、`UserContext` 和权限边界。

本阶段不实现基础配置 CRUD、工单业务 CRUD、自动派单、接单、维修过程、转派、请假、返工、评价、提醒任务或对应前端页面。

## 2. 数据库现状与执行原则

沿用 Docker MySQL 中已经存在的数据库 `dorm_repair`，不创建 `dormrepair`，不删除或迁移旧库。现有 `scaffold_persistence_check` 是此前持久化验证表，本阶段不删除；17 张正式表建立后数据库共有 18 张物理表，验收时明确区分“17 张正式表”和“1 张既有验证表”。

建表使用 MySQL 8、InnoDB、`utf8mb4`，不建立外键。DDL 以 `doc/database/数据库DDL及索引设计.md` 为字段、约束和索引的执行依据；`宿舍报修与维修工单系统核心数据表设计 V1.md` 用于解释业务职责和关系；出现冲突时，以用户最新要求、PLAN-1 和最终 DDL 的顺序裁决并记录。

执行前将可重复执行的正式 SQL 保存到项目数据库文档目录。DDL 应使用明确的库名和 `CREATE TABLE IF NOT EXISTS` 或等价的安全策略；不得通过删除现有表来获得干净状态。若同名表已存在但结构不一致，停止对应表操作并报告差异，不自动覆盖。

## 3. 正式表范围

正式表共 17 张：

1. `sys_user`
2. `repair_area`
3. `repair_worker`
4. `repair_fault_type`
5. `repair_worker_fault_type`
6. `repair_worker_area_scope`
7. `repair_work_schedule`
8. `repair_order`
9. `repair_process_record`
10. `repair_material_usage`
11. `repair_transfer_request`
12. `repair_leave_request`
13. `repair_rework_record`
14. `repair_evaluation`
15. `repair_order_flow`
16. `repair_reminder_record`
17. `sys_operation_log`

必须核对主键、NULL 约束、默认值、唯一索引、普通索引、引擎和字符集。逻辑删除不参与配置关系的复合唯一键；配置停用通过状态字段表达。历史业务数据不在正常业务流程中物理删除。

## 4. 数据库验证

执行 DDL 后从 `information_schema` 和 `SHOW CREATE TABLE` 验证 17 张正式表。核心索引必须覆盖 PLAN-1 列出的重复报修、负责人状态、超时扫描、请假任务、转派审批、维修过程、材料、返工、流转、评价和提醒去重场景。

容器重启验证只使用 `docker compose restart mysql`，重启后再次核对正式表与一条专用测试数据。验证数据必须位于测试用途明确的记录中，并在清理前取得授权；若不清理，则使用可识别且不参与业务的测试账号或测试记录。

## 5. Entity 与 Mapper

每张正式表建立一一对应的 Entity。Entity 仅映射数据库字段，不包含展示字段、请求字段或业务计算字段。Java 类型固定为：`BIGINT → Long`、`TINYINT/INT → Integer`、`VARCHAR/TEXT → String`、`DATE → LocalDate`、`TIME → LocalTime`、`DATETIME → LocalDateTime`、`DECIMAL → BigDecimal`。

每张正式表建立 `XxxMapper.java` 与 `XxxMapper.xml`。XML 必须包含正确的 `namespace`、完整 `resultMap`、基础列片段以及必要的 `selectById`。本阶段不添加通用动态 SQL 框架，也不提前实现复杂业务 SQL。

Mapper 通过 MySQL 集成测试验证，不用 H2 模拟 MySQL 特性。测试读取本机忽略提交的环境配置，不在源码、测试和日志中写入数据库密码。

## 6. 基础枚举

所有确定枚举至少提供 `code` 和中文 `description`。本阶段包括：

- `UserRoleEnum`：学生 1、维修人员 2、管理员 3；补齐阶段 0 中缺少的描述。
- `WorkerWorkStatusEnum`：正常 0、请假中 1、停用 2。
- `RepairOrderStatusEnum`：待派单 0、待接单 1、维修中 2、待确认 3、返工中 4、已中断 5、已完成 6、已取消 7。
- DDL 已明确且不会歧义的请假审批、转派审批/执行、维修过程、返工、提醒类型和提醒级别枚举。

未在 DDL 中稳定定义的值不提前创建枚举，业务代码不得散落已确定状态的魔法数字。

## 7. 登录与 JWT

登录统一使用 `sys_user`，接口采用 `POST /api/auth/login`，请求字段为 `username`、`password`。密码只保存 BCrypt 哈希。登录至少校验用户存在、密码正确、账号启用、未逻辑删除。

成功后返回统一 `Result`，`data` 包含 JWT 和前端识别当前用户所需的最小信息；JWT 通过 `Authorization: Bearer <token>` 携带。JWT 密钥、有效期和开发账号初始密码通过本机环境变量注入，不硬编码、不提交 Git、不出现在日志中。

JWT 过滤器验证签名和有效期后重新查询或校验用户必要状态，构造包含 `userId`、`username`、`realName`、`roleType` 的 `LoginUser` 并写入 `SecurityContext`。无效或过期令牌按未认证处理，不向客户端泄露解析异常。

开发验收准备管理员、维修人员、学生和停用账号。初始化应可重复执行，不覆盖已存在的同名用户；凭据只记录在本机忽略文件中。

## 8. 统一响应与错误码

继续沿用阶段 0 的数字 `Result.code`，不引入 PLAN-1 示例中的字母错误码体系。HTTP 状态保持：登录成功 200、请求参数错误 400、未认证 401、无权限 403、资源不存在 404、状态冲突 409、系统异常 500。

新增认证和用户错误码仍使用数字分段，并登记到 API 文档。用户名不存在和密码错误对外使用相同的登录失败提示，避免账号枚举；停用账号返回安全且明确的“账号不可用”提示。Spring Security 过滤器错误继续使用统一 JSON `Result`。

## 9. 角色权限

开启方法级安全，正式接口通过 `@PreAuthorize` 声明角色。角色专属接口严格隔离：管理员不会因为角色等级自动获得维修人员或学生专属接口权限；需要多角色访问时必须显式声明多个角色。

阶段验收使用测试源码中的测试控制器验证 ADMIN、WORKER、STUDENT 权限矩阵。生产源码不保留 `/api/test/**` 验收接口。未登录访问返回 401，已登录但角色不匹配返回 403。

## 10. 数据范围

本阶段不引入 SQL DataScope AOP。边界固定为：

- Controller：通过角色权限控制能否调用接口。
- Service/Mapper：列表查询显式加入身份范围条件。
- Service：详情、修改和操作接口显式校验资源归属及状态。

管理员默认访问全部业务数据；维修人员访问本人数据和当前由本人负责的数据；学生访问本人提交的数据。`studentUid`、`workerId`、`operatorId`、`reviewAdminId`、`applicantUid` 等身份字段必须从 `UserContext` 或后端映射关系获得，不信任前端输入。

由于本阶段没有正式业务 CRUD，使用测试源码中的最小资源归属服务验证学生 A 不能访问学生 B 资源、维修人员不能访问其他维修人员资源、管理员可访问全部资源。测试辅助代码不进入生产包。

## 11. 前端范围

前端只补齐与认证底座直接相关的内容：登录请求类型、JWT 的本地保存与请求头注入、401 清理登录状态，以及最小登录可用链路。不得在本阶段扩展管理页面、配置 CRUD 或业务页面。令牌存储方案应集中封装，业务组件不得直接操作存储键。

## 12. 测试与验收

按测试驱动方式覆盖：

- 17 张正式表及关键索引、字符集、引擎、无外键。
- 17 个 Entity 与表字段、类型的一致性。
- 每个 Mapper 的 MySQL `selectById` 基础链路。
- 三种有效角色、停用账号、错误密码、不存在账号。
- JWT 签发、有效认证、失效/篡改令牌和账号状态变化。
- `UserContext` 的用户及角色信息。
- 三角色接口权限矩阵、401 与 403 统一响应。
- 列表范围和单条资源归属模式的越权测试。
- 后端完整测试和 Maven 打包。
- MySQL 重启后的结构与验证数据持久性。

测试通过只证明覆盖场景成立；最终还需逐项对照 PLAN-1 验收清单，并明确哪些业务能力因阶段边界尚未实现。

## 13. 安全与变更约束

不删除已有数据库、表或数据，不覆盖同名表，不输出凭据，不把真实生产数据写入测试。任何必须删除或重建对象的结构冲突都停止执行并由用户决定。代码只保留在本机，不初始化 Git、不推送外部平台。
