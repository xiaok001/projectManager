# 多项目管理系统 - 前端

> Vue 3 + TypeScript + Element Plus + Vite

## 快速开始

```bash
npm install
npm run dev          # 开发模式 http://localhost:5173
npm run build        # 生产构建 → dist/
```

## 项目结构

```
src/
├── main.ts                         # 入口（Element Plus / Router / Pinia / 图标全局注册）
├── api/index.ts                    # Axios封装 + 16个业务模块接口
├── router/index.ts                 # 路由(权限守卫+动态权限加载+404兜底)
├── store/user.ts                   # 用户状态(token/权限/动态菜单/hasPermission)
├── utils/tooltip.ts                # Tooltip popper-class 配置
├── layouts/MainLayout.vue          # 主布局(动态菜单渲染+个人资料+修改密码)
└── views/
    ├── auth/                       # 登录 / 忘记密码(三步流程)
    ├── dashboard/                  # 首页概览(风险可点击/评分明细hover)
    ├── project/                    # 项目管理(列表/详情/WBS文档/待办Tab)
    ├── todo/                       # 全局项目待办
    ├── ai-suggestion/              # AI风险建议(手动扫描+规则说明)
    ├── report/                     # 周报(AI生成+历史记录)
    ├── schedule/                   # 定时任务(Cron可编辑+执行记录)
    ├── config/                     # 系统配置(左右导航+AI配置+测试连接)
    ├── user/                       # 用户管理
    ├── role/                       # 角色管理(权限+数据权限)
    ├── log/                        # 操作日志
    └── error/                      # 404页面
```

## 核心机制

### 权限系统
- 登录后调用 `GET /auth/permissions` 获取权限树存入 store
- 侧边栏根据 `menuPermissions` 动态渲染
- 路由守卫刷新时自动加载权限
- 页面按钮用 `userStore.hasPermission('key')` 控制

### 样式规范
- Tooltip: `:show-overflow-tooltip="{ popperClass: 'pm-tooltip' }"` (420px宽/280px高/内部滚动)
- 卡片: `shadow="never"` + 12px间距
- 搜索栏与列表: 独立 `el-card` 分隔
- 时间格式: `yyyy-MM-dd HH:mm:ss`
