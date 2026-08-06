# 多项目管理与风险提醒系统（PMM）

> 面向部门经理（统揽视角）和项目经理（数据录入方）的多项目协同治理平台

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0+ |
| 认证 | Spring Security + JWT |
| 定时任务 | Spring DynamicScheduler（数据库驱动，支持热更新） |
| AI能力 | Ollama（本地）/ DeepSeek（云端，可切换，支持测试连接） |
| 邮件 | Spring Mail（163 SMTP，发件人：多项目管理系统机器人） |
| 操作日志 | Spring AOP 自动记录（仅记录增删改，GET不记录） |
| 文件存储 | 本地磁盘（WBS附件，UUID文件名避免中文问题） |

## 快速开始

### 1. 数据库初始化

```bash
mysql -u root -p < sql/schema.sql
```

默认管理员账号: `admin` / `admin123`

### 2. 后端启动

```bash
cd backend
mvn spring-boot:run
```

后端 http://localhost:8080

### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端 http://localhost:5173

### 4. 一键部署

```bash
./deploy.sh   # 选择：仅前端 / 仅后端 / 全部
```

## 项目结构

```
projectManager/
├── deploy.sh                         # 一键部署脚本
├── sql/schema.sql                    # 16张表 + 初始数据
├── backend/                          # Spring Boot 后端
│   └── src/main/java/com/pm/
│       ├── config/                   # Security, MyBatis-Plus, AOP操作日志, CORS
│       ├── controller/               # 14个REST控制器
│       ├── service/                  # 业务接口 + 实现
│       ├── mapper/                   # MyBatis-Plus Mapper (14个)
│       ├── model/                    # entity/dto/vo/enums
│       ├── common/                   # 响应/异常/常量
│       ├── security/                 # JWT认证
│       └── schedule/                 # DynamicScheduler动态调度器
├── frontend/                         # Vue3 前端
│   └── src/
│       ├── api/                      # Axios封装 + 16个业务模块接口
│       ├── router/                   # 路由(含权限守卫+动态权限加载)
│       ├── store/                    # Pinia(用户/权限/动态菜单)
│       ├── utils/                    # Tooltip配置
│       └── views/
│           ├── auth/                 # 登录 / 忘记密码
│           ├── dashboard/            # 首页概览(风险可点击/评分明细)
│           ├── project/              # 项目管理(列表/详情/WBS文档)
│           ├── todo/                 # 项目待办(全局+项目级)
│           ├── ai-suggestion/        # AI风险建议(手动扫描+规则说明)
│           ├── report/               # 周报(AI生成+历史记录)
│           ├── schedule/             # 定时任务(Cron可编辑+执行记录)
│           ├── config/               # 系统配置(左右导航+AI测试连接)
│           ├── user/                 # 用户管理
│           ├── role/                 # 角色管理(权限+数据权限)
│           ├── log/                  # 操作日志
│           └── error/                # 404页面
└── README.md
```

## 数据库表结构（16张表）

| 表名 | 说明 |
|---|---|
| sys_user | 用户表（BCrypt密码 + role_id） |
| sys_role | 角色表（含data_scope数据权限） |
| sys_permission | 权限表（菜单+按钮，树形） |
| sys_role_permission | 角色权限关联 |
| sys_role_project | 角色数据权限关联（项目级） |
| project | 项目主表（含WBS文档/预期结束日期） |
| project_stage | 项目阶段（含人天/成本/进度联动状态） |
| project_risk | 风险表（编号自动生成） |
| project_todo | 待办表（逾期自动转风险） |
| project_change_log | 项目变更记录（含前后对比） |
| ai_risk_suggestion | AI风险建议 |
| system_config | 全局配置（可配参数） |
| email_digest_log | 邮件发送记录 |
| operation_log | 操作日志 |
| scheduled_task | 定时任务配置（5个任务，Cron可编辑） |
| scheduled_task_log | 定时任务执行记录 |
| report_weekly_log | 周报历史记录 |

## API 接口

统一前缀: `/api/v1`，返回格式: `{code, message, data}`

| 模块 | 接口 | 说明 |
|---|---|---|
| 认证 | POST /auth/login | 登录 |
| 认证 | POST /auth/verify-username | 验证账号 |
| 认证 | POST /auth/reset-password | 重置密码（发邮件） |
| 认证 | GET /auth/permissions | 当前用户权限树 |
| 用户 | GET/POST/PUT/DELETE /users | 用户CRUD |
| 用户 | GET/PUT /users/me | 个人资料 |
| 用户 | PUT /users/me/password | 修改密码 |
| 角色 | GET/POST/PUT/DELETE /roles | 角色CRUD |
| 角色 | GET/PUT /roles/{id}/permissions | 权限分配 |
| 角色 | GET/PUT /roles/{id}/data-scope | 数据权限 |
| 权限 | GET /permissions/tree | 权限树 |
| 项目 | GET/POST/PUT/DELETE /projects | 项目CRUD（含查询过滤） |
| 项目 | PUT /projects/{id}/wbs-url | WBS在线链接 |
| 项目 | POST/GET /projects/{id}/wbs-file | WBS附件上传/下载 |
| 阶段 | GET /projects/{id}/stages | 阶段列表 |
| 阶段 | PUT /stages/{id} | 更新阶段（含人天/成本/进度） |
| 风险 | GET/POST/PUT /projects/{id}/risks | 风险CRUD |
| 风险 | GET /risks/aggregated | 跨项目风险聚合 |
| 待办 | GET/POST/PUT/DELETE /todos | 待办CRUD（全局+项目级） |
| 待办 | GET /todos/page | 全局待办分页 |
| AI建议 | GET /ai-suggestions/page | AI建议分页 |
| AI建议 | POST /ai-suggestions/scan | 手动AI扫描 |
| AI建议 | POST /ai-suggestions/{id}/accept | 采纳 |
| AI建议 | POST /ai-suggestions/{id}/ignore | 忽略 |
| Dashboard | GET /dashboard/summary | 首页聚合 |
| Dashboard | GET /dashboard/health | 项目健康度 |
| 配置 | GET/PUT /config | 全局配置 |
| 配置 | POST /config/test-ai | 测试AI连接 |
| 邮件 | POST /digest/send-now | 手动发送摘要 |
| 邮件 | POST /digest/test | 测试邮件发送 |
| 邮件 | GET /digest/logs | 邮件发送记录 |
| 变更 | GET/POST /projects/{id}/changes | 变更记录 |
| 报告 | GET /reports/weekly | AI周报 |
| 报告 | GET /reports/weekly/history | 周报历史记录 |
| 日志 | GET /operation-logs | 操作日志 |
| 定时任务 | GET /scheduled-tasks | 任务列表 |
| 定时任务 | PUT /scheduled-tasks/{id}/toggle | 启用/禁用 |
| 定时任务 | PUT /scheduled-tasks/{id}/cron | 修改执行频率 |
| 定时任务 | POST /scheduled-tasks/{id}/run | 手动执行 |
| 定时任务 | GET /scheduled-tasks/logs | 执行记录 |

## 核心功能

### 项目管理
- 项目编号唯一性校验，创建后自动初始化7个标准阶段
- WBS在线文档链接（可点击跳转）+ 离线附件（拖拽上传/下载/覆盖替换）
- 编辑时支持状态变更，选择「已完成」校验非运维阶段是否全部完成
- 项目列表支持名称/编号/等级查询 + 分页

### 阶段管理
- 阶段名可编辑，支持标准阶段+自定义
- 预估/实际人天、预估/实际成本、完成进度（0-100%）
- 进度与状态联动：0%=未开始，1%-99%=进行中，100%=已完成
- 延期自动判定（动态调度器每小时刷新）

### 风险管理
- 风险编号自动生成（项目编号+日期+序号）
- 8种类型枚举 + 支持自定义输入
- 停滞自动判定，支持手动覆盖
- Dashboard 风险聚合区可点击跳转处理

### 项目待办
- 待办编号自动生成，负责人支持下拉选择和手动输入
- 逾期待办自动创建风险记录
- 全局待办页面 + 项目详情待办Tab

### AI能力
- 隐性风险探测：阶段备注保存后异步分析
- 每晚22:00全量扫描 + 手动扫描（含原理说明弹窗）
- 周报AI叙述性总结（含生成规则Tips）
- DeepSeek/Ollama 双实现，配置页支持测试连接

### 健康评分
- 时间35% + 风险40% + 交付25% 自动计算
- Dashboard卡片 hover 显示评分明细（三维度得分+加权公式）
- 所有权重和阈值可在系统配置页面调整

### 系统管理（部门经理）
- 系统配置：左右导航布局（基础/评分/AI），AI配置动态字段+测试连接
- 用户管理：CRUD + 个人资料修改 + 密码修改
- 角色管理：页面/按钮权限 + 项目级数据权限
- 操作日志：AOP自动记录增删改
- 定时任务：Cron可编辑（自由输入+常用示例提示）、启用/禁用、手动执行、执行记录

### 认证与安全
- JWT无状态认证，动态权限加载（菜单根据权限树渲染）
- 忘记密码：验证账号→验证邮箱→发送新密码
- 403自动跳登录，404自定义页面

## 侧边栏导航（动态权限渲染）

```
首页概览
项目管理
AI风险建议
项目待办
运营中心
  ├─ 项目报告（AI周报 + 历史记录）
  └─ 定时任务
系统管理（仅部门经理）
  ├─ 系统配置
  ├─ 用户管理
  ├─ 角色管理
  └─ 操作日志
```

## 定时任务（5个，动态调度）

| 任务 | 默认Cron | 说明 |
|---|---|---|
| 阶段延期刷新 | 每小时 | 自动标记已延期阶段 |
| 风险停滞刷新 | 每小时:30 | 自动标记停滞风险 |
| 待办逾期刷新 | 每小时:15 | 逾期待办自动创建风险 |
| AI风险扫描 | 每晚22:00 | 全量扫描阶段备注 |
| 项目待办与风险日报 | 每天9:20 | 按项目汇总待办+风险，邮件发送 |
| 每日项目管理摘要 | 每天9:30 | Dashboard数据AI总结，邮件发送 |

所有任务支持启用/禁用、修改执行频率、手动执行、执行日志记录，修改后立即生效无需重启。

## 部署

| 项目 | 信息 |
|---|---|
| 服务器 | 192.168.4.161 |
| 前端地址 | http://192.168.4.161:8090 |
| 后端地址 | http://192.168.4.161:8080 |
| Ollama | http://192.168.4.161:11434 (qwen2.5:7b) |
| 一键部署 | `./deploy.sh`（选择仅前端/仅后端/全部） |
