# PLAN-0 验收记录

## 一、记录说明

- 对应需求：`doc/requirements/PLAN-0.md`
- 阶段名称：基础工程规范化与公共能力建设
- 记录性质：阶段完成后的回补验收
- 回补复验日期：2026-09-23
- 验收结论：通过

本记录依据当前代码、自动化测试、构建结果和运行环境复验形成。回补验收只确认 PLAN-0 的公共能力，不把后续 PLAN-1、PLAN-2 已实现的业务能力倒算为本阶段交付。

## 二、验收范围

本阶段验收以下内容：

1. 后端基础包结构和公共类职责；
2. 统一 `Result<T>` 与 `PageResult<T>` 响应；
3. 数字错误码、HTTP 状态码和全局异常处理；
4. Jakarta Validation 参数校验；
5. PageHelper 分页约定和分页上限；
6. `LoginUser`、`UserContext` 和操作人获取入口；
7. 前端 Axios、登录失效和分页公共处理；
8. 前后端构建及基础设施运行状态。

## 三、验收结果

| 验收项 | 结果 | 验收依据 |
|---|---|---|
| 后端包结构统一，公共类职责明确 | 通过 | `common`、`config`、`security`、各业务功能包已按职责划分；未发现重复的当前用户工具入口 |
| Controller 响应统一为 `Result<T>` | 通过 | `ResultTest`、`GlobalExceptionHandlerTest`、后续接口集成测试 |
| 成功与失败 JSON 结构统一 | 通过 | 固定使用 `code`、`message`、`data`；分页数据置于 `data` |
| HTTP 状态码符合确认后的约定 | 通过 | 成功 200；参数错误 400；未登录 401；无权限 403；资源不存在 404；冲突 409；系统异常 500 |
| `BusinessException` 可统一转换 | 通过 | `BusinessExceptionTest`、`GlobalExceptionHandlerTest` |
| 参数校验及参数类型错误可统一转换 | 通过 | `GlobalExceptionHandlerTest` 覆盖 Bean Validation、绑定和类型错误场景 |
| 未知异常不向前端暴露内部细节 | 通过 | 测试以包含内部连接信息的异常验证客户端仅收到通用系统错误，服务端保留 Throwable 日志 |
| PageHelper 和统一分页结果可用 | 通过 | `PageQueryTest`、`PageResultIntegrationTest`；请求使用 `pageNum`、`pageSize`，响应使用 `total`、`pageNum`、`pageSize`、`records` |
| `pageSize` 有最大限制 | 通过 | 分页测试覆盖最大 100 条限制和非法参数归一化 |
| 可统一获取当前用户、用户 ID 和角色 | 通过 | `UserContextTest` 覆盖登录用户、角色判断和未登录异常 |
| 操作人身份不由前端决定 | 通过 | 公共约定及后续 Service 使用 `UserContext.getCurrentUserId()` |
| Axios 统一解析业务响应 | 通过 | `frontend/src/api/http.spec.ts` |
| 登录失效具有统一处理入口 | 通过 | `frontend/src/api/http.spec.ts`、`tokenStorage.spec.ts` |
| 前端分页字段统一 | 通过 | `frontend/src/api/pagination.spec.ts` |

## 四、复验记录

### 4.1 后端测试与打包

在加载根目录 `.env` 中的本地开发环境变量后执行：

```powershell
cd backend
mvn "-Dmaven.repo.local=..\.m2\repository" test package
```

结果：

- 测试共 60 个，失败 0，错误 0，跳过 0；
- Maven 输出 `BUILD SUCCESS`；
- 成功生成可执行 JAR。

说明：全量 60 个测试包含后续阶段测试；其中与 PLAN-0 直接相关的测试包括 `ResultTest`、`BusinessExceptionTest`、`GlobalExceptionHandlerTest`、`PageQueryTest`、`PageResultIntegrationTest`、`UserContextTest` 及前端公共请求测试。

### 4.2 前端测试与构建

```powershell
cd frontend
npm run test -- --run
npm run build
```

结果：

- 4 个测试文件通过；
- 9 个测试通过；
- TypeScript 检查及 Vite 生产构建成功。

### 4.3 基础设施状态

```powershell
docker compose ps
```

结果：MySQL、Redis、MinIO、Nginx 均在运行；MySQL、Redis、MinIO 健康检查为 `healthy`。

## 五、阶段边界与遗留项

- PLAN-0 不包含具体基础配置 CRUD、报修、派单和工单业务。
- JWT 登录、角色授权和数据库底座属于 PLAN-1。
- 前端目前仍是技术骨架，完整业务页面不属于 PLAN-0。
- Vite 构建存在单个产物超过 500 kB 的警告，不影响本阶段构建通过；待业务页面开发时通过按需导入和路由拆包处理。

## 六、最终结论

PLAN-0 的公共响应、异常、分页、登录用户上下文和前端请求规范均具备可复用实现，后续模块可直接沿用统一调用链。阶段验收通过。
