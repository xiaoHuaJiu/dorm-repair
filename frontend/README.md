# 宿舍报修系统前端

Vue 3 + Vite + TypeScript 前端工程。学生端与维修人员端使用 Vant，管理员端使用 Element Plus；所有业务数据必须来自正式 API，不允许在页面或 API 模块中内置演示数据。

## 本地命令

```bash
npm install
npm run dev
npm run typecheck
npm run test:run
npm run build
npm run check
```

开发服务器默认使用 `8810` 端口，并将 `/api` 代理到 `http://localhost:8811`。

## 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_API_BASE_URL` | `/api` | 后端 API 基础路径 |
| `VITE_REQUEST_TIMEOUT_MS` | `10000` | 请求超时时间，单位毫秒且必须为正整数 |

仓库只维护不含秘密的环境基线。个人覆盖配置使用 `.env.local` 或 `.env.*.local`，这些文件不会纳入版本控制。

## 目录约定

- `api/`：接口调用、统一响应、错误和请求控制。
- `components/`：跨页面复用组件。
- `config/`：运行环境配置。
- `constants/`：状态与固定映射。
- `layouts/`：三端页面框架。
- `router/`：路由和权限守卫。
- `stores/`：Pinia 状态。
- `styles/`：设计令牌和全局样式。
- `types/`：跨业务域公共类型。
- `utils/`：无业务状态的通用函数。
- `views/`：按 `auth`、`student`、`worker`、`admin`、`system` 划分页面。

页面和路由基线见 [`doc/architecture/前端页面与路由映射.md`](../doc/architecture/前端页面与路由映射.md)，接口状态见 [`doc/api/前端页面接口依赖矩阵.md`](../doc/api/前端页面接口依赖矩阵.md)。
