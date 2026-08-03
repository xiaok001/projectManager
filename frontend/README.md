# 多项目管理系统 - 前端

> Vue 3 + TypeScript + Element Plus + Vite

## 快速开始

```bash
npm install          # 安装依赖
npm run dev          # 开发模式 http://localhost:5173
npm run build        # 生产构建 → dist/
npm run preview      # 预览构建结果
```

开发模式下 API 通过 Vite 代理转发到 `http://localhost:8080`。

## 技术栈

| 依赖 | 版本 | 说明 |
|---|---|---|
| Vue 3 | ^3.5 | 核心框架 |
| TypeScript | ~5.8 | 类型安全 |
| Vite | ^7.0 | 构建工具 |
| Element Plus | ^2.10 | UI 组件库（中文） |
| Vue Router 4 | ^4.5 | 路由管理 |
| Pinia | ^3.0 | 状态管理 |
| Axios | ^1.9 | HTTP 请求 |

## 项目结构

```
src/
├── main.ts                         # 入口（注册 Element Plus / Router / Pinia）
├── App.vue                         # 根组件
├── api/
│   └── index.ts                    # Axios 封装 + 全部业务接口
├── router/
│   └── index.ts                    # 路由配置 + 登录/角色守卫
├── store/
│   └── user.ts                     # 用户状态（token / 角色 / 登录登出）
├── layouts/
│   └── MainLayout.vue              # 主布局（侧边栏 / 顶栏 / 内容区）
├── views/
│   ├── auth/
│   │   ├── LoginView.vue           # 登录页（两栏布局）
│   │   └── ForgotPasswordView.vue  # 忘记密码（三步流程）
│   ├── dashboard/
│   │   └── DashboardView.vue       # 首页概览（风险聚合/未来节点/健康度）
│   ├── project/
│   │   ├── ProjectListView.vue     # 项目列表（搜索+分页+删除）
│   │   ├── ProjectFormView.vue     # 创建/编辑项目
│   │   └── ProjectDetailView.vue   # 项目详情（阶段/风险/待办/变更 四Tab）
│   ├── todo/
│   │   └── TodoListView.vue        # 全局项目待办（搜索+分页+CRUD）
│   ├── ai-suggestion/
│   │   └── AiSuggestionView.vue    # AI风险建议（分页+手动扫描）
│   ├── report/
│   │   └── ReportView.vue          # 周报（AI生成+统计+分区展示）
│   ├── config/
│   │   └── ConfigView.vue          # 系统配置（权重/阈值/邮件测试）
│   ├── user/
│   │   └── UserManagementView.vue  # 用户管理（CRUD+格式校验）
│   ├── role/
│   │   └── RoleManagementView.vue  # 角色管理（权限+数据权限分配）
│   ├── log/
│   │   └── OperationLogView.vue    # 操作日志（分页+模块过滤）
│   └── error/
│       └── NotFoundView.vue        # 404页面
├── components/                     # 公共组件（预留）
└── assets/
    └── styles/
        └── global.css              # 全局样式（tooltip溢出/健康度颜色/分页等）
```

## 核心模块

### API 层 (`api/index.ts`)

- 请求拦截器自动附加 JWT Token
- 响应拦截器统一错误处理（401/403 跳登录）
- 按业务模块导出：`authApi` / `projectApi` / `stageApi` / `riskApi` / `todoApi` / `dashboardApi` / `configApi` / `digestApi` / `changeLogApi` / `aiSuggestionApi` / `userApi` / `roleApi` / `permissionApi` / `operationLogApi` / `reportApi`

### 路由 (`router/index.ts`)

- 未登录自动跳 `/login`，已登录访问 `/login` 跳 `/dashboard`
- 未匹配路由统一跳 `/404`
- 支持 `?tab=risk` 参数定位项目详情的风险Tab
- `meta.roles` 控制页面级权限（部门经理专属页面）

### 用户状态 (`store/user.ts`)

- `useUserStore()`：token / 用户信息 / 角色判断
- `isDeptManager`：控制菜单和按钮权限

### 主布局 (`layouts/MainLayout.vue`)

- 侧边栏菜单，部门经理可见「系统管理」子菜单
- 顶栏显示用户姓名和角色标签
- 支持侧边栏折叠

### 页面风格

- 搜索栏与列表分隔为独立 `el-card`
- `page-header` 左标题右操作按钮布局
- 表格长文本 `show-overflow-tooltip`，全局 tooltip 400px 固定宽度+自动换行

## 环境配置

开发代理 (`vite.config.ts`)：
```ts
server: {
  proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
}
```

生产环境需 Nginx 反向代理 `/api/` 到后端服务。
