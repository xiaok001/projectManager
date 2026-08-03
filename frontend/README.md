# 多项目管理系统 - 前端

> Vue 3 + TypeScript + Element Plus + Vite

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

启动后访问 http://localhost:5173，API 请求通过 Vite 代理转发到后端 http://localhost:8080。

### 生产构建

```bash
npm run build
```

构建产物输出到 `dist/` 目录，可直接部署到 Nginx 等静态服务器。

### 预览构建结果

```bash
npm run preview
```

## 技术栈

| 依赖 | 版本 | 说明 |
|---|---|---|
| Vue 3 | ^3.5 | 核心框架 |
| TypeScript | ~5.8 | 类型安全 |
| Vite | ^7.0 | 构建工具 |
| Element Plus | ^2.10 | UI 组件库 |
| Vue Router 4 | ^4.5 | 路由管理 |
| Pinia | ^3.0 | 状态管理 |
| Axios | ^1.9 | HTTP 请求 |
| ECharts | ^5.6 | 图表（预留） |

## 项目结构

```
frontend/src/
├── main.ts                    # 入口文件，注册全局插件(Element Plus/Router/Pinia)
├── App.vue                    # 根组件
├── api/
│   └── index.ts               # API接口封装(Axios拦截器/统一错误处理/所有业务接口)
├── router/
│   └── index.ts               # 路由配置(含登录守卫)
├── store/
│   └── user.ts                # 用户状态管理(Pinia，登录/登出/角色判断)
├── layouts/
│   └── MainLayout.vue         # 主布局(侧边栏菜单/顶栏面包屑/内容区)
├── views/
│   ├── auth/
│   │   └── LoginView.vue      # 登录页
│   ├── dashboard/
│   │   └── DashboardView.vue  # 首页概览(风险聚合/未来节点/健康度)
│   ├── project/
│   │   ├── ProjectListView.vue    # 项目列表
│   │   ├── ProjectFormView.vue    # 创建/编辑项目
│   │   └── ProjectDetailView.vue  # 项目详情(阶段管理/风险列表/变更记录)
│   ├── ai-suggestion/
│   │   └── AiSuggestionView.vue   # AI风险建议(采纳/忽略)
│   ├── config/
│   │   └── ConfigView.vue     # 系统配置(权重/阈值/邮件/AI/测试发送)
│   ├── user/
│   │   └── UserManagementView.vue # 用户管理(新增/编辑/禁用)
│   ├── log/
│   │   └── OperationLogView.vue   # 操作日志(分页/模块过滤)
│   └── report/
│       └── ReportView.vue     # 报告(项目健康度概览/按项目筛选)
├── components/                # 公共组件(预留)
└── assets/
    └── styles/
        └── global.css         # 全局样式
```

## 核心模块说明

### API 层 (`api/index.ts`)

- Axios 请求拦截器自动附加 JWT Token
- 响应拦截器统一处理错误（401 跳登录、403 提示无权限）
- 按业务模块导出：`authApi` / `projectApi` / `stageApi` / `riskApi` / `dashboardApi` / `configApi` / `digestApi` / `changeLogApi` / `aiSuggestionApi` / `userApi` / `operationLogApi`

### 路由 (`router/index.ts`)

- 路由守卫：未登录自动跳转 `/login`，已登录访问 `/login` 自动跳转 `/dashboard`
- 项目详情支持 `?tab=risk` 参数，从 Dashboard 点击风险可直接定位到风险 Tab

### 用户状态 (`store/user.ts`)

- `useUserStore()`：管理 token、用户信息、角色判断
- `isDeptManager`：判断是否部门经理，控制菜单和按钮权限

### 主布局 (`layouts/MainLayout.vue`)

- 侧边栏菜单，部门经理可见「系统管理」子菜单（系统配置/用户管理/操作日志）
- 顶栏显示当前用户姓名和角色标签
- 支持侧边栏折叠

## 环境变量

开发模式下 API 通过 Vite 代理转发，配置在 `vite.config.ts`：

```ts
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

生产环境需配置 Nginx 反向代理 `/api/` 到后端服务。
