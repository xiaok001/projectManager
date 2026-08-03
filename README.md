# 多项目管理与风险提醒系统（PMM）

> 面向部门经理（统揽视角）和项目经理（数据录入方）的多项目协同治理平台

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0+ |
| 认证 | Spring Security + JWT |
| 定时任务 | Spring Scheduler |
| AI能力 | Ollama（本地）/ DeepSeek（云端，可切换） |
| 邮件 | Spring Mail（163 SMTP） |
| 操作日志 | Spring AOP 自动记录（仅记录增删改） |

## 快速开始

### 1. 数据库初始化

```bash
mysql -u root -p < sql/schema.sql
```

默认管理员账号: `admin` / `admin123`

### 2. 后端启动

```bash
cd backend
# 修改 application.yml 中的数据库连接、邮箱、AI配置
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

## 项目结构

```
projectManager/
├── sql/schema.sql                    # 12张表 + 初始数据
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/pm/
│       ├── config/                   # Security, MyBatis-Plus, AOP操作日志, CORS
│       ├── controller/               # 13个REST控制器
│       ├── service/                  # 业务接口 + 实现
│       ├── mapper/                   # MyBatis-Plus Mapper
│       ├── model/                    # entity/dto/vo/enums
│       ├── common/                   # 响应/异常/常量
│       ├── security/                 # JWT认证
│       └── schedule/                 # 定时任务(延期/停滞/逾期/AI扫描)
├── frontend/                         # Vue3 前端
│   └── src/
│       ├── api/                      # API封装(Axios拦截器)
│       ├── router/                   # 路由(含权限守卫)
│       ├── store/                    # Pinia状态管理
│       ├── layouts/                  # 主布局(侧边栏/顶栏)
│       └── views/
│           ├── auth/                 # 登录 / 忘记密码
│           ├── dashboard/            # 首页概览
│           ├── project/              # 项目管理(列表/详情/表单)
│           ├── todo/                 # 项目待办
│           ├── ai-suggestion/        # AI风险建议
│           ├── report/               # 周报(AI生成)
│           ├── config/               # 系统配置
│           ├── user/                 # 用户管理
│           ├── role/                 # 角色管理(权限+数据权限)
│           ├── log/                  # 操作日志
│           └── error/                # 404页面
└── README.md
```

## 数据库表结构（12张表）

| 表名 | 说明 |
|---|---|
| sys_user | 用户表 |
| sys_role | 角色表 |
| sys_permission | 权限表（菜单+按钮） |
| sys_role_permission | 角色权限关联 |
| sys_role_project | 角色数据权限关联（项目级） |
| project | 项目主表 |
| project_stage | 项目阶段（含人天/成本/进度） |
| project_risk | 风险/问题表 |
| project_todo | 待办事项表 |
| project_change_log | 项目变更记录 |
| ai_risk_suggestion | AI风险建议 |
| system_config | 全局配置 |
| email_digest_log | 邮件发送记录 |
| operation_log | 操作日志 |

## API 接口

统一前缀: `/api/v1`，返回格式: `{code, message, data}`

| 模块 | 接口 | 说明 |
|---|---|---|
| 认证 | POST /auth/login, /auth/verify-username, /auth/reset-password | 登录/忘记密码 |
| 用户 | GET/POST/PUT/DELETE /users | 用户CRUD |
| 角色 | GET/POST/PUT/DELETE /roles | 角色CRUD |
| 权限 | GET /permissions/tree | 权限树 |
| 项目 | GET/POST/PUT/DELETE /projects | 项目CRUD（含查询过滤） |
| 阶段 | GET /projects/{id}/stages, PUT /stages/{id} | 阶段管理 |
| 风险 | GET/POST/PUT /projects/{id}/risks, GET /risks/aggregated | 风险管理 |
| 待办 | GET/POST/PUT/DELETE /todos | 待办CRUD（全局+项目级） |
| AI建议 | GET /ai-suggestions/page, POST /ai-suggestions/scan | AI建议+手动扫描 |
| Dashboard | GET /dashboard/summary, /dashboard/health | 首页聚合 |
| 配置 | GET/PUT /config | 全局配置 |
| 邮件 | POST /digest/send-now, /digest/test | 邮件发送/测试 |
| 变更 | GET/POST /projects/{id}/changes | 变更记录 |
| 报告 | GET /reports/weekly | AI周报 |
| 日志 | GET /operation-logs | 操作日志 |

## 核心功能

### 项目管理
- 项目编号唯一性校验，创建后自动初始化7个标准阶段
- 预期结束日期、启动日期、结束日期（运维阶段开始日期）
- 编辑时支持状态变更，选择「已完成」自动校验未完成阶段（排除运维）
- 项目列表支持项目名称/编号/等级查询 + 分页

### 阶段管理
- 阶段名可编辑，支持标准阶段+自定义
- 预估/实际人天、预估/实际成本、完成进度（0-100%）
- 进度与状态联动：0%=未开始，1%-99%=进行中，100%=已完成
- 延期自动判定（定时任务每小时刷新）

### 风险管理
- 风险编号自动生成（项目编号+日期+序号）
- 类型下拉枚举（8种）+ 支持自定义
- 停滞自动判定，支持手动覆盖
- Dashboard 风险聚合区可点击跳转处理

### 项目待办
- 待办编号自动生成（项目编号-TD-日期-序号）
- 所属阶段、来源、优先级、紧急程度、负责人、进度、阻塞问题、风险说明、输出物
- 逾期待办自动创建风险记录（定时任务每小时检查）
- 全局待办页面 + 项目详情待办Tab

### AI能力
- 隐性风险探测：阶段备注保存后异步调用AI分析
- 每晚22:00自动全量扫描，手动扫描按钮（含原理说明弹窗）
- 周报AI叙述性总结
- 支持 Ollama（本地）/ DeepSeek（云端）切换

### 健康评分
- 时间35% + 风险40% + 交付25% 自动计算
- 客户满意度独立展示（不参与加权）
- 所有权重和阈值可配置

### 系统管理（部门经理）
- 系统配置：健康权重/停滞阈值/邮件/AI
- 用户管理：CRUD + 邮箱手机号格式校验
- 角色管理：CRUD + 页面/按钮权限分配 + 数据权限（项目级）
- 操作日志：AOP自动记录增删改，按模块/类型过滤

### 认证与权限
- JWT无状态认证
- 角色权限：页面权限 + 按钮权限 + 数据权限（项目级）
- 路由守卫 + 接口鉴权 + 403自动跳登录
- 忘记密码：验证账号→验证邮箱→发送新密码

## 角色权限

| 角色 | 说明 |
|---|---|
| 超级管理员 | 全部权限 |
| 项目经理 | 操作自己负责的项目 |
| 查看者 | 只读权限 |

## 定时任务

| 任务 | 频率 | 说明 |
|---|---|---|
| 阶段延期刷新 | 每小时 | 自动标记已延期阶段 |
| 风险停滞刷新 | 每小时 | 自动标记停滞风险 |
| 待办逾期刷新 | 每小时:15 | 逾期自动创建风险 |
| AI风险扫描 | 每晚22:00 | 全量扫描阶段备注 |

## 部署

服务器部署详见各子目录 README。
