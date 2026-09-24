# PLAN-1 验收记录

## 一、记录说明

- 对应需求：`doc/requirements/PLAN-1.md`
- 阶段名称：数据库与权限底座建设
- 记录性质：阶段完成后的回补验收
- 回补复验日期：2026-09-23
- 验收结论：通过（业务数据行级权限按阶段边界保留到对应业务阶段）

## 二、验收范围

本阶段验收以下内容：

1. `dorm_repair` 数据库的 17 张正式表及关键索引；
2. 正式表对应的 Entity、Mapper 和 Mapper XML；
3. 用户、维修人员和工单等已确定状态枚举；
4. JWT 登录认证、停用账号拦截和三角色识别；
5. 401、403 安全响应及后端角色授权；
6. 数据范围规则、身份字段来源和资源归属校验模式。

## 三、验收结果

### 3.1 数据库

| 验收项 | 结果 | 验收依据 |
|---|---|---|
| 正式表数量与设计一致 | 通过 | 数据库结构脚本复验为 17/17 |
| 字段、主键及类型与 DDL 一致 | 通过 | `SchemaSqlContractTest`、`EntitySchemaConsistencyTest` |
| 关键唯一索引和查询索引存在 | 通过 | 结构脚本复验为 27/27 |
| 字符集和存储引擎符合约定 | 通过 | 建表 DDL 与结构验证脚本 |
| 未建立不需要的外键 | 通过 | 外键数量为 0 |
| MySQL 重启后结构可持久化 | 通过 | 既有 MySQL 容器持久化重建验证记录；当前容器健康且结构复验通过 |

### 3.2 Entity、Mapper 与枚举

| 验收项 | 结果 | 验收依据 |
|---|---|---|
| 17 张正式表均有对应 Entity | 通过 | `EntitySchemaConsistencyTest` |
| Java 字段类型和数据库列一致 | 通过 | `EntitySchemaConsistencyTest` |
| 核心表均有 Mapper 与 XML | 通过 | `MapperXmlContractTest` |
| MyBatis 到真实 MySQL 链路可用 | 通过 | `MapperMySqlIntegrationTest` |
| 已确定状态使用统一枚举 | 通过 | `FoundationEnumsTest`，包含用户角色、维修人员状态、工单状态等基础枚举 |

### 3.3 登录认证与角色权限

| 验收项 | 结果 | 验收依据 |
|---|---|---|
| 管理员、维修人员、学生可登录 | 通过 | `AuthServiceTest`、`AuthControllerTest` 及本地开发账号链路 |
| 密码错误、不存在用户、停用账号不能登录 | 通过 | `AuthServiceTest` |
| JWT 可签发、解析并恢复登录上下文 | 通过 | `JwtTokenServiceTest`、`JwtAuthenticationFilterTest` |
| 未登录访问保护接口返回 401 | 通过 | `SecurityErrorHandlerTest`、接口集成测试 |
| 三角色能被后端正确识别 | 通过 | `RoleAuthorizationIntegrationTest` |
| 无权限访问返回 403 | 通过 | `RoleAuthorizationIntegrationTest` |
| 权限判断独立于前端页面 | 通过 | Spring Security 与方法级授权在后端执行 |

### 3.4 数据范围

| 验收项 | 结果 | 说明 |
|---|---|---|
| 管理员全量、维修人员本人/负责数据、学生本人数据的规则已定义 | 通过 | 规则记录于安全文档，`DataScopePatternTest` 固定实现模式 |
| 身份字段统一来自 `UserContext` | 通过 | 当前用户 ID、角色和操作人均由后端上下文提供 |
| 前端不能决定 `studentUid`、当前 `workerId`、`operatorId` | 通过 | 接口和服务层不接受客户端覆盖当前身份的设计 |
| 列表范围和单条资源归属校验模式已确定 | 通过 | 采用 Controller 角色权限、Service/Mapper 显式范围条件、资源归属检查三层模式 |
| 通用 SQL DataScope AOP | 不实现 | PLAN-1 明确不引入，避免不同业务归属字段被错误抽象 |
| 真实工单行级数据权限 | 后续实现 | PLAN-1 尚无工单业务接口，无法对不存在的业务查询做真实行级过滤；将在对应业务阶段随接口实现和测试 |

## 四、复验记录

### 4.1 数据库结构

```powershell
.\scripts\verify-database-schema.ps1
```

结果：正式表 17/17，关键索引 27/27，外键 0。

### 4.2 后端测试与打包

在加载根目录 `.env` 中的 MySQL、Redis、MinIO 和 JWT 本地配置后执行：

```powershell
cd backend
mvn "-Dmaven.repo.local=..\.m2\repository" test package
```

结果：60 个测试全部通过，Maven 打包成功。与本阶段直接相关的真实 MySQL 测试包括 `EntitySchemaConsistencyTest` 和 `MapperMySqlIntegrationTest`。

### 4.3 容器状态

```powershell
docker compose ps
```

结果：`dorm-repair-mysql`、`dorm-repair-redis`、`dorm-repair-minio`、`dorm-repair-nginx` 均运行，前三者为健康状态。

## 五、阶段边界与遗留项

- PLAN-1 不实现基础配置 CRUD 和正式工单业务。
- 已实现角色级接口权限和数据范围实施规范；真实行级数据过滤必须随具体业务查询实现，当前不能表述为“全部数据权限已完成”。
- 不使用通用 SQL DataScope AOP。
- 本地开发账号的凭据只存在于被忽略的 `.env`，未写入代码、测试或验收记录。

## 六、最终结论

数据库、Entity、Mapper、枚举、JWT 认证、三角色授权及数据范围实施模式均可供后续阶段直接使用。PLAN-1 在其明确范围内验收通过。
