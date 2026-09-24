# 宿舍报修系统可启动技术骨架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建可启动的 Vue 3 前端、Spring Boot 3 后端以及 MySQL、Redis、MinIO、Nginx 本地 Docker Compose 环境，并验证 MySQL 容器重建后数据仍然存在。

**Architecture:** 前端和后端分别位于 `frontend/` 与 `backend/`，通过 HTTP API 解耦。根目录 Compose 文件只编排本项目的基础设施容器，数据通过宿主机绑定目录持久化；Nginx 将普通请求代理到前端 `8810`，将 `/api/` 请求代理到后端 `8811`。

**Tech Stack:** Vue 3、Vite、TypeScript、Vant、Element Plus、Pinia、Vue Router、Axios；Java 17、Spring Boot 3、Maven、Spring MVC、Spring Security、JWT、MyBatis、Hibernate Validator、Spring Scheduling；MySQL 8、Redis、MinIO、Nginx、Docker Compose。

**Spec:** `docs/superpowers/specs/2026-09-23-project-scaffold-design.md`

## Global Constraints

- 前端开发端口固定为 `8810`，后端服务端口固定为 `8811`。
- 本阶段只实现可启动技术骨架，不实现业务逻辑，不完整还原 `prototype/` 页面。
- 后端使用 Java 17 和 Maven；前端使用 TypeScript。
- 不创建未经确认的业务接口、表结构、权限规则或定时任务。
- 本地密码、令牌和连接配置只能存放在被忽略的环境变量文件中。
- `.docker-data/` 使用宿主机绑定目录，确保容器删除重建后数据不丢失。
- 不修改、启动、停止或删除其他项目的容器、网络、数据卷和文件。
- 当前目录不是 Git 仓库；不得擅自初始化 Git，也不执行提交或推送。

## Review Focus

- 本机默认端口被其他程序占用时，启动应在修改任何容器前失败并明确指出冲突端口。
- `.env`、前后端本地环境文件及 `.docker-data/` 必须确实被忽略，不能只写在说明里。
- 后端健康检查必须无需认证，但其他未定义路径不能被默认放行。
- MySQL 容器删除重建验证只能删除本项目容器，不能删除持久化目录或其他项目资源。
- Nginx 应同时正确代理前端根路径和后端 `/api/health`，避免路径重写导致 404。

---

## 文件结构与职责

### 根目录与基础设施

- Create: `.gitignore` — 忽略本地环境变量、构建产物、日志和持久化数据。
- Create: `.env` — 本机 Compose 凭据，仅保存在本机。
- Create: `.env.example` — 不含真实凭据的变量清单。
- Create: `docker-compose.yml` — 编排 MySQL、Redis、MinIO、Nginx。
- Create: `nginx/nginx.conf` — 前端与后端反向代理规则。
- Create: `scripts/verify-mysql-persistence.ps1` — 只针对本项目 MySQL 服务执行写入、删容器、重建和查询验证。

### 前端

- Create: `frontend/package.json` — 前端依赖与启动、构建、测试脚本。
- Create: `frontend/package-lock.json` — 锁定 npm 依赖树。
- Create: `frontend/index.html` — Vite HTML 入口。
- Create: `frontend/tsconfig.json` — TypeScript 项目配置。
- Create: `frontend/tsconfig.app.json` — 浏览器应用 TypeScript 配置。
- Create: `frontend/tsconfig.node.json` — Vite 配置 TypeScript 设置。
- Create: `frontend/vite.config.ts` — 固定端口、代理及 Vitest 配置。
- Create: `frontend/.env.example` — 前端公开环境变量模板。
- Create: `frontend/src/env.d.ts` — Vite 环境变量类型。
- Create: `frontend/src/main.ts` — 注册 Vue、Pinia、Router、Vant、Element Plus。
- Create: `frontend/src/App.vue` — 根路由出口。
- Create: `frontend/src/router/index.ts` — 空白首页路由。
- Create: `frontend/src/stores/app.ts` — 最小应用状态仓库。
- Create: `frontend/src/api/http.ts` — Axios 客户端。
- Create: `frontend/src/views/HomeView.vue` — 可启动状态页。
- Create: `frontend/src/styles/main.css` — 最小全局样式。
- Create: `frontend/src/views/HomeView.spec.ts` — 验证空白首页与技术骨架信息。

### 后端

- Create: `backend/pom.xml` — Maven 依赖、Java 17 和构建插件。
- Create: `backend/src/main/java/com/dormrepair/DormRepairApplication.java` — Spring Boot 启动类并启用定时任务。
- Create: `backend/src/main/java/com/dormrepair/config/SecurityConfig.java` — 只放行健康检查。
- Create: `backend/src/main/java/com/dormrepair/config/MinioConfig.java` — 从配置创建 MinIO 客户端。
- Create: `backend/src/main/java/com/dormrepair/config/MinioProperties.java` — MinIO 配置属性。
- Create: `backend/src/main/java/com/dormrepair/health/HealthController.java` — 固定健康检查响应。
- Create: `backend/src/main/resources/application.yml` — 端口及外部依赖环境变量映射。
- Create: `backend/src/test/resources/application-test.yml` — 使用 H2 和本地占位连接的测试配置。
- Create: `backend/src/test/java/com/dormrepair/health/HealthControllerTest.java` — 健康检查和安全边界测试。

### 文档

- Create: `README.md` — 本地启动、停止、访问地址、连接方式和数据位置。

---

### Task 1: 本地配置与 Docker Compose 基础设施

**Files:**
- Create: `.gitignore`
- Create: `.env`
- Create: `.env.example`
- Create: `docker-compose.yml`
- Create: `nginx/nginx.conf`
- Create: `scripts/verify-mysql-persistence.ps1`
- Test: Docker Compose 配置解析、忽略规则、端口检查、MySQL 持久化脚本

**Interfaces:**
- Consumes: Docker Desktop Linux Engine；宿主机端口 `3307`、`6379`、`9000`、`9001`、`8080`。实施前检查确认 `3306` 已由本机独立 MySQL 占用。
- Produces: Compose 服务名 `mysql`、`redis`、`minio`、`nginx`；网络 `dorm-repair-network`；后端使用的环境变量名称。

- [ ] **Step 1: 在任何容器变更前检查 Docker 与端口**

Run:

```powershell
docker version
docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}'
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
  Where-Object { $_.LocalPort -in @(3306,3307,6379,9000,9001,8080,8810,8811) } |
  Sort-Object LocalPort
```

Expected: Docker Client 与 Server 均可访问；目标端口没有监听。若发现端口冲突，停止本任务并向用户报告占用端口，不修改现有容器。

- [ ] **Step 2: 生成本机随机开发凭据**

使用 PowerShell 的加密随机数生成器分别生成 MySQL root 密码、项目数据库密码、Redis 密码、MinIO 用户名和 MinIO 密码。不得在对话、测试日志或 `.env.example` 中回显完整值。

```powershell
function New-LocalSecret([int]$Length = 32) {
  $bytes = [byte[]]::new($Length)
  [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
  [Convert]::ToHexString($bytes).ToLowerInvariant()
}

$mysqlRootPassword = New-LocalSecret 24
$mysqlAppPassword = New-LocalSecret 24
$redisPassword = New-LocalSecret 24
$minioRootUser = 'dormrepairadmin'
$minioRootPassword = New-LocalSecret 24
```

Expected: 五个变量均为非空值，密码长度不少于 32 个字符。

- [ ] **Step 3: 创建忽略规则和环境变量文件**

`.gitignore` 必须包含：

```gitignore
.env
.env.*
!.env.example
.docker-data/
frontend/node_modules/
frontend/dist/
frontend/.env.local
frontend/.env.*.local
backend/target/
*.log
.idea/
.vscode/
```

`.env.example` 必须包含以下变量名并使用说明性示例值：

```dotenv
COMPOSE_PROJECT_NAME=dorm-repair-system
MYSQL_PORT=3307
MYSQL_DATABASE=dorm_repair
MYSQL_USER=dorm_repair
MYSQL_PASSWORD=replace-with-local-password
MYSQL_ROOT_PASSWORD=replace-with-local-root-password
REDIS_PASSWORD=replace-with-local-password
MINIO_ROOT_USER=dormrepairadmin
MINIO_ROOT_PASSWORD=replace-with-local-password
```

`.env` 使用 Step 2 生成的值，变量名与 `.env.example` 完全一致。使用 `apply_patch` 创建文件，不使用命令行重定向写入。

- [ ] **Step 4: 创建 Nginx 反向代理配置**

`nginx/nginx.conf`：

```nginx
events {}

http {
    server {
        listen 80;
        server_name _;

        location /api/ {
            proxy_pass http://host.docker.internal:8811;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location / {
            proxy_pass http://host.docker.internal:8810;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }
    }
}
```

- [ ] **Step 5: 创建 Compose 文件**

`docker-compose.yml` 使用项目专属服务名和容器名：

```yaml
name: dorm-repair-system

services:
  mysql:
    image: mysql:8.4
    container_name: dorm-repair-mysql
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      TZ: Asia/Shanghai
    ports:
      - "${MYSQL_PORT:-3307}:3306"
    volumes:
      - ./.docker-data/mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD --silent"]
      interval: 5s
      timeout: 5s
      retries: 30
      start_period: 20s
    networks: [dorm-repair-network]

  redis:
    image: redis:7.4-alpine
    container_name: dorm-repair-redis
    restart: unless-stopped
    command: ["redis-server", "--appendonly", "yes", "--requirepass", "${REDIS_PASSWORD}"]
    ports:
      - "6379:6379"
    volumes:
      - ./.docker-data/redis:/data
    healthcheck:
      test: ["CMD-SHELL", "redis-cli -a $$REDIS_PASSWORD ping | grep PONG"]
      interval: 5s
      timeout: 5s
      retries: 20
    networks: [dorm-repair-network]

  minio:
    image: minio/minio@sha256:14cea493d9a34af32f524e538b8346cf79f3321eff8e708c1e2960462bd8936e
    container_name: dorm-repair-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - ./.docker-data/minio:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 5s
      timeout: 5s
      retries: 20
    networks: [dorm-repair-network]

  nginx:
    image: nginx:1.27-alpine
    container_name: dorm-repair-nginx
    restart: unless-stopped
    ports:
      - "8080:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks: [dorm-repair-network]

networks:
  dorm-repair-network:
    name: dorm-repair-network
```

- [ ] **Step 6: 创建安全限定的 MySQL 持久化验证脚本**

`scripts/verify-mysql-persistence.ps1` 从 Compose 服务环境读取数据库名、用户和密码，执行以下固定流程：

1. 用 `docker compose ps --status running mysql` 确认服务名为 `mysql`。
2. 在 `scaffold_persistence_check` 表不存在时创建表。
3. 以固定键 `mysql-container-recreate` 插入或更新一行。
4. 运行 `docker compose rm --stop --force mysql`，只删除 Compose 中的 `mysql` 容器。
5. 运行 `docker compose up -d mysql` 并等待健康。
6. 查询固定键并要求返回值为 `persisted`，否则以非零状态退出。

脚本不得运行 `docker compose down -v`、`docker volume rm`，不得删除 `.docker-data/mysql/`。

- [ ] **Step 7: 验证静态配置**

Run:

```powershell
docker compose config --quiet
git check-ignore .env .docker-data/mysql/probe 2>$null
```

Expected: Compose 配置退出码为 `0`。若当前仍不是 Git 仓库，改用读取 `.gitignore` 的自动化断言确认 `.env` 和 `.docker-data/` 规则存在，不初始化 Git。

---

### Task 2: Vue 3 + Vite + TypeScript 前端骨架

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/package-lock.json`
- Create: `frontend/index.html`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.app.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/.env.example`
- Create: `frontend/src/env.d.ts`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/stores/app.ts`
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/views/HomeView.spec.ts`
- Create: `frontend/src/styles/main.css`

**Interfaces:**
- Consumes: 环境变量 `VITE_API_BASE_URL`，默认 `/api`。
- Produces: `http` Axios 实例、`useAppStore()`、根路由 `/`、开发服务 `http://localhost:8810/`。

- [ ] **Step 1: 使用 Vite 官方模板生成 TypeScript 项目**

Run:

```powershell
npm create vite@5.4.14 frontend -- --template vue-ts
Set-Location frontend
npm install
npm install vue-router@4 pinia axios vant element-plus
npm install --save-dev vitest @vue/test-utils jsdom
```

Expected: `frontend/package-lock.json` 生成，npm 安装退出码为 `0`。若网络受限，申请网络授权后重试相同命令。

- [ ] **Step 2: 先创建失败的首页测试**

`frontend/src/views/HomeView.spec.ts`：

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import HomeView from './HomeView.vue'

describe('HomeView', () => {
  it('显示技术骨架已启动且不展示虚构业务数据', () => {
    const wrapper = mount(HomeView)
    expect(wrapper.get('h1').text()).toBe('宿舍报修系统')
    expect(wrapper.text()).toContain('前端技术骨架已启动')
    expect(wrapper.text()).not.toContain('模拟工单')
  })
})
```

- [ ] **Step 3: 运行测试确认失败**

Run: `npm run test -- --run`

Expected: FAIL，因为 `HomeView.vue` 尚未提供约定内容或测试脚本尚未配置。

- [ ] **Step 4: 配置 Vite、Vitest 和 npm 脚本**

`vite.config.ts` 必须包含：

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 8810,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8811',
    },
  },
  test: {
    environment: 'jsdom',
  },
})
```

在 `package.json` 中保留 `dev`、`build`、`preview`，新增 `test: "vitest"`。

- [ ] **Step 5: 创建最小应用骨架**

`src/stores/app.ts`：

```ts
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({ ready: true }),
})
```

`src/api/http.ts`：

```ts
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
})
```

`src/router/index.ts` 创建 `/` 路由并指向 `HomeView.vue`。`App.vue` 只包含 `<RouterView />`。`main.ts` 注册 Pinia、Router、Vant 与 Element Plus，并导入两个组件库样式和 `styles/main.css`。

`HomeView.vue` 显示标题“宿舍报修系统”和状态“前端技术骨架已启动”，不展示业务卡片或模拟数据。

- [ ] **Step 6: 运行前端单元测试和构建**

Run:

```powershell
npm run test -- --run
npm run build
```

Expected: 首页测试通过，TypeScript 检查和 Vite 构建退出码均为 `0`。

- [ ] **Step 7: 启动前端并验证端口**

Run: `npm run dev -- --host 0.0.0.0`

Expected: `http://localhost:8810/` 返回 `200`，页面包含“前端技术骨架已启动”。保持进程供集成验证使用。

---

### Task 3: Spring Boot 3 + Maven 后端骨架

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/dormrepair/DormRepairApplication.java`
- Create: `backend/src/main/java/com/dormrepair/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/dormrepair/config/MinioConfig.java`
- Create: `backend/src/main/java/com/dormrepair/config/MinioProperties.java`
- Create: `backend/src/main/java/com/dormrepair/health/HealthController.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/resources/application-test.yml`
- Create: `backend/src/test/java/com/dormrepair/health/HealthControllerTest.java`

**Interfaces:**
- Consumes: `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`。
- Produces: `GET /api/health`，响应 JSON `{"status":"UP","service":"dorm-repair-backend"}`；服务端口 `8811`。

- [ ] **Step 1: 创建 Maven 项目定义**

`pom.xml` 使用 Spring Boot parent `3.3.13`、Java `17`，依赖如下：

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-data-redis`
- `mybatis-spring-boot-starter:3.0.4`
- `mysql-connector-j`（runtime）
- `minio:8.5.17`
- `jjwt-api:0.12.6`
- `jjwt-impl:0.12.6`（runtime）
- `jjwt-jackson:0.12.6`（runtime）
- `spring-boot-starter-test`（test）
- `spring-security-test`（test）
- `h2`（test）

使用 `spring-boot-maven-plugin` 完成打包。

- [ ] **Step 2: 先创建失败的健康检查与安全测试**

`HealthControllerTest.java` 使用 `@SpringBootTest`、`@AutoConfigureMockMvc` 和 `@ActiveProfiles("test")`，包含两个测试：

```java
@Test
void healthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.service").value("dorm-repair-backend"));
}

@Test
void undefinedEndpointsAreNotPublic() throws Exception {
    mockMvc.perform(get("/api/private-probe"))
        .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `mvn test`

Expected: FAIL，因为启动类、控制器和安全配置尚未创建。

- [ ] **Step 4: 创建启动类与健康检查**

`DormRepairApplication.java` 使用 `@SpringBootApplication`、`@EnableScheduling` 和标准 `main` 方法。

`HealthController.java`：

```java
@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "dorm-repair-backend");
    }
}
```

- [ ] **Step 5: 创建最小安全边界**

`SecurityConfig.java` 提供 `SecurityFilterChain`：关闭仅用于当前无状态 API 骨架的 CSRF；关闭表单登录和 HTTP Basic；将未认证请求返回 `401`；仅允许 `GET /api/health`，其余请求要求认证。当前不创建用户、密码或 JWT 签发逻辑。

- [ ] **Step 6: 创建 MinIO 类型化配置**

`MinioProperties.java` 使用 `@ConfigurationProperties(prefix = "app.minio")`，字段为 `endpoint`、`accessKey`、`secretKey`。

`MinioConfig.java` 使用 `@EnableConfigurationProperties(MinioProperties.class)` 并创建：

```java
@Bean
MinioClient minioClient(MinioProperties properties) {
    return MinioClient.builder()
        .endpoint(properties.getEndpoint())
        .credentials(properties.getAccessKey(), properties.getSecretKey())
        .build();
}
```

创建客户端时不发起网络请求。

- [ ] **Step 7: 创建运行与测试配置**

`application.yml` 固定 `server.port: 8811`，并将数据源、Redis、MinIO 参数映射到 Interfaces 中列出的环境变量。数据源 JDBC URL 使用：

```yaml
jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3307}/${DB_NAME:dorm_repair}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
```

`application-test.yml` 使用内存 H2 数据库、空 Redis 仓库扫描、MinIO 测试地址和非敏感测试凭据，确保单元测试不依赖 Docker 服务。

- [ ] **Step 8: 运行后端测试和打包**

Run:

```powershell
mvn test
mvn package
```

Expected: 两个安全测试通过，Maven 构建成功并生成可执行 jar。

- [ ] **Step 9: 使用本机环境变量启动后端**

从根目录 `.env` 读取对应值并仅注入当前后端进程，不把值写入命令历史或可提交文件。启动命令：`mvn spring-boot:run`。

Expected: `http://localhost:8811/api/health` 返回 `200` 和固定 JSON；`http://localhost:8811/api/private-probe` 返回 `401`。保持进程供集成验证使用。

---

### Task 4: 集成启动、持久化验收与使用文档

**Files:**
- Modify: `README.md`
- Test: 前端、后端、Nginx、MySQL、Redis、MinIO 综合验证

**Interfaces:**
- Consumes: Task 1 的 Compose 服务、Task 2 的 `8810` 前端、Task 3 的 `8811` 后端。
- Produces: 可复核的启动与停止命令、连接地址、数据路径和验证结果。

- [ ] **Step 1: 启动本项目基础设施**

Run:

```powershell
docker compose up -d mysql redis minio nginx
docker compose ps
```

Expected: 只出现本项目四个服务；MySQL、Redis、MinIO 最终为 healthy，Nginx 为 running。

- [ ] **Step 2: 验证服务端口和认证**

Run:

```powershell
Test-NetConnection localhost -Port 3307
Test-NetConnection localhost -Port 6379
Test-NetConnection localhost -Port 9000
Test-NetConnection localhost -Port 9001
Test-NetConnection localhost -Port 8080
```

Expected: 五个端口的 `TcpTestSucceeded` 均为 `True`。使用 `.env` 中密码执行 `redis-cli ping` 和 MySQL `SELECT 1`，两者成功且日志不打印完整密码。

- [ ] **Step 3: 执行 MySQL 容器重建持久化验证**

Run: `powershell -ExecutionPolicy Bypass -File scripts/verify-mysql-persistence.ps1`，凭据通过进程环境变量读取。

Expected: 脚本确认固定键 `mysql-container-recreate` 的值为 `persisted`；`docker compose ps mysql` 显示重建后的 MySQL 为 healthy；`.docker-data/mysql/` 仍存在且包含数据文件。

- [ ] **Step 4: 验证应用直连地址**

在 Task 2 与 Task 3 的开发进程运行时执行：

```powershell
$frontend = Invoke-WebRequest http://localhost:8810/ -UseBasicParsing
$backend = Invoke-RestMethod http://localhost:8811/api/health
if ($frontend.StatusCode -ne 200) { throw '前端未返回 200' }
if ($backend.status -ne 'UP') { throw '后端健康检查失败' }
```

Expected: 前端返回 `200`，后端返回 `status=UP`。

- [ ] **Step 5: 验证 Nginx 两条代理链路**

Run:

```powershell
$proxyFrontend = Invoke-WebRequest http://localhost:8080/ -UseBasicParsing
$proxyBackend = Invoke-RestMethod http://localhost:8080/api/health
if ($proxyFrontend.StatusCode -ne 200) { throw 'Nginx 前端代理失败' }
if ($proxyBackend.status -ne 'UP') { throw 'Nginx 后端代理失败' }
```

Expected: 两条代理链路均成功，`/api/health` 未被错误重写。

- [ ] **Step 6: 创建根目录使用文档**

`README.md` 必须说明：

- 前置版本：Node.js、npm、Java 17、Maven、Docker Desktop。
- 如何从 `.env.example` 准备本机 `.env`，且不得提交 `.env`。
- `docker compose up -d`、`docker compose stop`、`docker compose start`、`docker compose down` 的区别。
- 不使用 `docker compose down -v`，因为该命令可能删除卷；本项目实际数据使用绑定目录，但仍禁止把它作为日常停止命令。
- 前端启动：`cd frontend` 后执行 `npm install`、`npm run dev`。
- 后端启动：注入本机环境变量后，在 `backend` 执行 `mvn spring-boot:run`。
- 访问地址：前端 `8810`、后端 `8811/api/health`、Nginx `8080`、MinIO 控制台 `9001`。
- MySQL 连接地址 `localhost:3307`、数据库名和用户名从 `.env` 读取；容器内部端口为 `3306`。
- Redis 连接地址 `localhost:6379`，密码从 `.env` 读取。
- MinIO API 地址 `http://localhost:9000`，账号从 `.env` 读取。
- 数据实际位置：项目根目录 `.docker-data/mysql/`、`.docker-data/redis/`、`.docker-data/minio/`。
- 当前只完成技术骨架，业务逻辑和完整原型页面尚未实现。

- [ ] **Step 7: 运行最终全量验证**

Run:

```powershell
Set-Location frontend
npm run test -- --run
npm run build
Set-Location ../backend
mvn test
mvn package
Set-Location ..
docker compose config --quiet
docker compose ps
```

Expected: 前端测试与构建、后端测试与打包、Compose 配置均以退出码 `0` 完成；四个容器状态符合预期。

- [ ] **Step 8: 核对未触碰其他项目资源**

对比实施前记录的 `docker ps -a`、网络和卷列表，只允许新增 `dorm-repair-*` 资源。不得以名称相似为由清理任何既有资源。

Expected: 其他项目容器、网络和卷的名称与状态未被本任务修改。
