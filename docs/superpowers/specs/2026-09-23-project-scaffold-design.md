# 宿舍报修系统可启动技术骨架设计

## 1. 目标

在现有文档与静态原型基础上，为宿舍报修系统建立可独立启动的前后端工程及本地基础设施环境。本阶段只建设技术骨架，不实现工单、派单、请假、权限等业务逻辑，也不完整还原 `prototype/` 中的页面。

完成后应具备以下能力：

- 前端开发服务可在本机 `8810` 端口启动并显示空白项目首页。
- 后端服务可在本机 `8811` 端口启动并提供无需登录的健康检查接口。
- MySQL、Redis、MinIO、Nginx 可由 Docker Compose 独立启动。
- MySQL、Redis、MinIO 数据持久化到项目目录下的宿主机目录。
- Nginx 可转发前端请求及 `/api/` 后端请求。
- 本地密码和连接信息通过未纳入版本控制的环境变量文件提供。

## 2. 范围

### 2.1 本阶段包含

- 创建 `frontend/` Vue 3 + Vite + TypeScript 工程。
- 接入 Vant、Element Plus、Pinia、Vue Router、Axios。
- 创建空白首页、基础路由、状态管理和请求客户端骨架。
- 创建 `backend/` Java 17 + Spring Boot 3 + Maven 工程。
- 接入 Spring MVC、Spring Security、JWT、MyBatis、Hibernate Validator、Spring Scheduling。
- 接入 MySQL、Redis、MinIO 的基础配置。
- 创建无需登录的健康检查接口。
- 创建 MySQL 8、Redis、MinIO、Nginx 的 Docker Compose 编排。
- 建立本地环境变量文件、配置示例和忽略规则。
- 验证 MySQL 数据在容器删除并重建后仍然存在。

### 2.2 本阶段不包含

- 不实现报修、工单、派单、转派、请假、评价、返工等业务功能。
- 不创建未经确认的业务接口、表结构或权限规则。
- 不完整迁移或还原 `prototype/` 页面。
- 不实现 SSE 或其他实时消息能力。
- 不修改、启动、停止或删除其他项目的容器、网络和数据卷。
- 不执行外部推送或部署。

## 3. 项目结构

```text
dorm-repair-system/
├─ frontend/
├─ backend/
├─ nginx/
│  └─ nginx.conf
├─ docker-compose.yml
├─ .env
├─ .env.example
├─ .gitignore
└─ .docker-data/
   ├─ mysql/
   ├─ redis/
   └─ minio/
```

`.env` 与 `.docker-data/` 只用于本机开发并加入 `.gitignore`。`.env.example` 可保留变量名和安全占位值，但不得包含真实密码或令牌。

## 4. 前端设计

前端采用 Vue 3、Vite 和 TypeScript，开发端口固定为 `8810`。

基础能力包括：

- Vue Router：提供空白首页路由，并为后续学生端、维修人员端和管理端页面预留扩展位置。
- Pinia：创建应用级状态管理入口，不提前定义业务状态。
- Axios：创建统一请求客户端，基础地址读取 Vite 环境变量。
- Vant：作为后续学生端和维修人员端组件库完成全局接入。
- Element Plus：作为后续管理端组件库完成全局接入。

空白首页只用于确认应用已启动，不复制具体原型页面，不展示虚构业务数据。

## 5. 后端设计

后端采用 Java 17、Spring Boot 3 和 Maven，服务端口固定为 `8811`。

基础能力包括：

- Spring MVC：提供 HTTP 接口。
- Spring Security：建立安全过滤链，当前仅放行健康检查接口。
- JWT：引入令牌处理依赖并预留配置，不签发业务令牌。
- MyBatis：接入数据访问框架，不创建业务映射器和业务表。
- Hibernate Validator：提供参数校验能力。
- Spring Scheduling：启用定时任务基础能力，不创建业务定时任务。
- MySQL：作为关系型数据库连接目标。
- Redis：作为缓存和并发控制的后续依赖。
- MinIO：通过配置属性和客户端 Bean 接入对象存储。

健康检查使用固定的非业务响应，作用是确认服务进程和 Nginx 代理链路可用。敏感配置全部从环境变量读取，不写入 Java 源码或可提交配置。

## 6. Docker Compose 设计

本项目使用独立 Compose 项目，不复用其他项目资源。服务和端口如下：

| 服务 | 本机端口 | 容器端口 | 持久化位置 |
|---|---:|---:|---|
| MySQL 8 | 3307 | 3306 | `.docker-data/mysql/` |
| Redis | 6379 | 6379 | `.docker-data/redis/` |
| MinIO API | 9000 | 9000 | `.docker-data/minio/` |
| MinIO 控制台 | 9001 | 9001 | `.docker-data/minio/` |
| Nginx | 8080 | 80 | 不保存业务数据 |

MySQL 使用宿主机端口 `3307`，因为实施前检查发现本机已有独立 `mysqld` 进程占用 `3306`。该调整避免停止或修改现有服务，容器内部仍使用标准端口 `3306`。

所有服务加入本项目独立网络。容器名称使用项目专属前缀，避免与现有 Docker 资源混淆。MySQL、Redis 和 MinIO 配置健康检查；Nginx 在依赖服务满足启动条件后提供入口。

Nginx 路由规则：

- `/api/` 转发至宿主机 `8811` 后端服务。
- 其他路径转发至宿主机 `8810` 前端开发服务。

在 Docker Desktop 环境中，容器通过 `host.docker.internal` 访问宿主机上的前后端开发服务。

## 7. 配置与安全

- 根目录 `.env` 保存 Docker Compose 所需的本地开发账号、密码和数据库名。
- 后端连接配置从操作系统环境变量读取。
- 前端仅保存可公开的 API 基础路径，不保存服务端密码或密钥。
- 本地开发密码使用随机值生成，不在交付消息中完整回显。
- `.env`、前后端本地环境文件、构建产物、日志和 `.docker-data/` 必须加入 `.gitignore`。
- `.env.example` 仅提供变量说明和占位符。

## 8. 验证方案

### 8.1 静态检查

- 校验前后端目录和关键配置文件存在。
- 校验 Docker Compose 配置可解析。
- 校验环境变量文件已被忽略。

### 8.2 基础设施验证

1. 启动本项目的 MySQL、Redis、MinIO、Nginx 容器。
2. 检查各容器健康状态和端口映射。
3. 在 MySQL 开发数据库中创建独立验证表并写入一条测试记录。
4. 删除本项目的 MySQL 容器，不删除宿主机持久化目录。
5. 使用相同 Compose 配置重建 MySQL 容器。
6. 查询验证表并确认测试记录仍存在。
7. 不操作其他项目的容器、网络或数据卷。

验证表只用于持久化验收，不作为正式业务表。验证结果确认后保留该测试数据，便于用户复核；未经明确允许不执行清理。

### 8.3 应用验证

- 安装前端依赖并执行构建检查。
- 启动前端开发服务，访问 `http://localhost:8810/`。
- 执行后端测试和 Maven 构建。
- 启动后端服务，访问 `http://localhost:8811/api/health`。
- 通过 Nginx 访问 `http://localhost:8080/` 和 `http://localhost:8080/api/health`。

## 9. 完成标准

- 前端与后端均能独立启动。
- 空白前端页面、后端健康检查和 Nginx 代理链路均可访问。
- Docker Compose 中四个基础设施服务能够启动。
- MySQL 容器删除并重建后，验证数据仍存在。
- 所有本地敏感配置和持久化数据均已被版本控制忽略。
- 提供启动、停止、访问地址、连接信息来源及宿主机数据位置说明。
- 未实现或未验证的内容在交付说明中明确列出。
