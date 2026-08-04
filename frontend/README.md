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
├── main.ts                         # 入口（Element Plus / Router / Pinia / 图标全局注册）
├── App.vue                         # 根组件
├── api/
│   └── index.ts                    # Axios 封装 + 全部业务接口（15个模块）
├── router/
│   └── index.ts                    # 路由配置 + 登录/角色守卫 + 404兜底
├── store/
│   └── user.ts                     # 用户状态（token / 角色 / 登录登出）
├── layouts/
│   └── MainLayout.vue              # 主布局（侧边栏分组菜单 / 顶栏 / 内容区）
├── utils/
│   └── tooltip.ts                  # Tooltip popper-class 公共配置
├── views/
│   ├── auth/
│   │   ├── LoginView.vue           # 登录页（两栏布局 / 品牌区 + 表单区）
│   │   └── ForgotPasswordView.vue  # 忘记密码（三步流程：验证账号→验证邮箱→发送新密码）
│   ├── dashboard/
│   │   └── DashboardView.vue       # 首页概览（风险聚合可点击 / 未来节点 / 健康度卡片）
│   ├── project/
│   │   ├── ProjectListView.vue     # 项目列表（搜索+分页+编辑+删除）
│   │   ├── ProjectFormView.vue     # 创建/编辑项目（含WBS字段/状态管理/阶段校验）
│   │   └── ProjectDetailView.vue   # 项目详情（阶段/风险/待办/变更/WBS文档）
│   ├── todo/
│   │   └── TodoListView.vue        # 全局项目待办（搜索+分页+CRUD）
│   ├── ai-suggestion/
│   │   └── AiSuggestionView.vue    # AI风险建议（分页+手动扫描+生成规则说明）
│   ├── report/
│   │   └── ReportView.vue          # 周报（AI生成+统计卡片+规则Tips）
│   ├── schedule/
│   │   └── ScheduledTaskView.vue   # 定时任务（任务列表+手动执行+执行记录）
│   ├── config/
│   │   └── ConfigView.vue          # 系统配置（权重/阈值/邮件测试/AI配置）
│   ├── user/
│   │   └── UserManagementView.vue  # 用户管理（CRUD+格式校验）
│   ├── role/
│   │   └── RoleManagementView.vue  # 角色管理（权限树+数据权限分配）
│   ├── log/
│   │   └── OperationLogView.vue    # 操作日志（分页+模块过滤）
│   └── error/
│       └── NotFoundView.vue        # 404页面（渐变背景+动画图标）
├── components/                     # 公共组件（预留）
└── assets/
    └── styles/
        └── global.css              # 全局样式（Tooltip pm-tooltip / 健康度颜色 / 分页）
```

## 核心模块

### API 层 (`api/index.ts`)

- 请求拦截器自动附加 JWT Token
- 响应拦截器统一错误处理（401/403 跳登录）
- 按业务模块导出 15 个 API 对象

### 路由 (`router/index.ts`)

- 未登录自动跳 `/login`，已登录访问 `/login` 跳 `/dashboard`
- 未匹配路由统一跳 `/404`
- 支持 `?tab=risk` 参数定位项目详情的风险Tab
- `meta.roles` 控制页面级权限（部门经理专属页面）

### 侧边栏导航

```
首页概览 / 项目管理 / AI风险建议 / 项目待办
运营中心 → 项目报告 / 定时任务
系统管理 → 系统配置 / 用户管理 / 角色管理 / 操作日志
```

### 页面风格规范

- 搜索栏与列表分隔为独立 `el-card shadow="never"`
- `page-header` 左标题右操作按钮
- 表格 `border` + `stripe`，长文本用 `:show-overflow-tooltip="{ popperClass: 'pm-tooltip' }"`
- Tooltip 全局 420px 宽 / 280px 最高 / 内部滚动 / 自动换行
- 编辑模式用 `v-if/v-else` 展示态/编辑态切换
- 拖拽上传区 28px 内边距 + 8px 圆角 + 文件类型提示

## 环境配置

开发代理 (`vite.config.ts`)：
```ts
server: {
  proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
}
```

生产环境需 Nginx 反向代理 `/api/` 到后端服务。
