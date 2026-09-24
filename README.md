# 宿舍报修系统

当前仓库已建立前后端分离技术骨架，并完成用户注册、基础配置、三角色工单查询以及学生报修创建后端能力。前端已完成登录注册、管理员基础配置四个页面、F6 学生端全部页面（首页、提交报修、我的工单、详情、确认评价）、F7 维修人员端全部页面（工作台、我的工单、详情、接单、过程记录、材料登记、中断与恢复、完成维修、请假、消息）和 F8 管理员端全部页面（工作台、全部工单、工单详情、异常工单、转派审批、待人工派单、请假审批）。请假、消息、转派与审批类页面因后端接口缺失暂以空态提示呈现，详见《前端页面接口依赖矩阵》第 8 节阻塞清单。

## 环境要求

- Node.js 18 或更高版本。
- npm 9 或更高版本。
- Java 17 或更高版本。
- Maven 3.9 或更高版本。
- Docker Desktop，使用 Linux 容器引擎。

## 本地配置

根目录 `.env` 保存本机开发凭据，已加入 `.gitignore`，不得提交或复制到公开位置。需要重新准备配置时，以 `.env.example` 为变量清单，为每个密码生成独立随机值。

前端公开配置示例位于 `frontend/.env.example`。前端默认使用 `/api`，由 Vite 或 Nginx 转发至后端。

## 启动基础设施

在项目根目录执行：

```powershell
docker compose up -d
docker compose ps
```

本项目创建并管理以下容器：

- `dorm-repair-mysql`
- `dorm-repair-redis`
- `dorm-repair-minio`
- `dorm-repair-nginx`

不要操作名称不以 `dorm-repair-` 开头的其他项目容器。

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

访问地址：<http://localhost:8810/>

## 启动后端

根目录脚本会读取 `.env` 并仅向当前后端进程注入连接参数，不会回显密码：

```powershell
.\scripts\start-backend.ps1
```

健康检查地址：<http://localhost:8811/api/health>

未定义或需要认证的接口默认返回 `401`。

## Nginx 统一入口

- 前端入口：<http://localhost:8080/>
- 后端健康检查：<http://localhost:8080/api/health>

Nginx 将 `/api/` 请求转发到本机后端 `8811`，其余请求转发到本机前端 `8810`。因此使用 Nginx 前，应先启动前端和后端开发服务。

## 本地服务连接地址

| 服务 | 地址 | 凭据来源 |
|---|---|---|
| MySQL | `localhost:3307` | 根目录 `.env` 中的 `MYSQL_USER`、`MYSQL_PASSWORD` |
| Redis | `localhost:6379` | 根目录 `.env` 中的 `REDIS_PASSWORD` |
| MinIO API | <http://localhost:9000> | 根目录 `.env` 中的 `MINIO_ROOT_USER`、`MINIO_ROOT_PASSWORD` |
| MinIO 控制台 | <http://localhost:9001> | 与 MinIO API 相同 |

MySQL 使用 `3307` 是因为本机已有独立 MySQL 进程占用 `3306`；容器内部仍使用标准端口 `3306`。

## 停止与重新启动

仅暂停容器并保留容器实例：

```powershell
docker compose stop
```

重新启动已暂停的容器：

```powershell
docker compose start
```

删除本项目容器和网络，但保留宿主机绑定目录中的数据：

```powershell
docker compose down
```

重新创建容器：

```powershell
docker compose up -d
```

日常操作不要使用 `docker compose down -v`，也不要删除 `.docker-data/`。删除 `.docker-data/` 会永久删除本项目的本地数据库、Redis 和 MinIO 数据。

## 数据实际位置

数据直接保存在当前项目目录：

- MySQL：`D:\work\dorm-repair-system\.docker-data\mysql\`
- Redis：`D:\work\dorm-repair-system\.docker-data\redis\`
- MinIO：`D:\work\dorm-repair-system\.docker-data\minio\`

容器删除或通过 `docker compose down` 重建后，这些目录不会被自动删除。

## 验证命令

阶段 0 已建立统一响应、HTTP 状态与异常处理、参数校验、PageHelper 分页、登录用户上下文和前端 Axios 公共请求层。接口约定见 `doc/api/公共接口响应规范.md`。

前端：

```powershell
cd frontend
npm run test -- --run
npm run build
```

后端：

```powershell
cd backend
mvn "-Dmaven.repo.local=..\.m2\repository" test
mvn "-Dmaven.repo.local=..\.m2\repository" package
```

MySQL 容器重建持久化验证：

```powershell
.\scripts\verify-mysql-persistence.ps1
```

该脚本只删除并重建本项目的 `dorm-repair-mysql` 容器，不删除宿主机数据目录。

## 当前实现范围

阶段 1 建立了 17 张核心业务表，阶段 4 增加统一接口幂等记录表，阶段 5 增加节假日配置表和自动派单失败告警表，阶段 7 增加工单业务告警表，当前共 21 张正式表。现有后端包含 JWT 登录、三角色授权、基础配置、工单查询详情、学生报修、异步自动派单、维修人员接单与维修写入、学生确认完成、评价和返工能力。

当前已实现三角色工单分页查询、真实行级数据范围、四级位置树、工单详情聚合、24 小时疑似重复检查、Redis + MySQL 双层幂等报修、自动派单、接单、过程与材料、提交结果、学生确认、一次性评价及原工单返工。
前端三角色页面已按契约接入上述查询与写入接口：维修人员端已接入接单、过程记录、材料登记、中断、恢复、提交结果六个主流程接口；学生端取消、确认、评价、返工与图片上传，维修端转派、请假、消息，管理端统计、转派审批、人工派单、请假审批因后端接口缺失暂以禁用加提示或空态呈现。仍未实现请假、转派、评价写入、返工写入、提醒和异常工单中心。

数据库结构可使用以下命令幂等应用和验证：

```powershell
.\scripts\apply-database-schema.ps1
.\scripts\verify-database-schema.ps1
```

本地开发账号由 `.env` 中的 `DEV_*` 变量提供，后端以 `DEV_SEED_ENABLED=true` 启动时使用 BCrypt 幂等初始化；账号密码不会写入代码或文档。
