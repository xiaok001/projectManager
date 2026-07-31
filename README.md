# 多项目管理与风险提醒系统

> 面向部门经理（统揽视角）和项目经理（数据录入方）的多项目治理平台

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0+ |
| 认证 | Spring Security + JWT |
| 定时任务 | Spring Scheduler |
| AI能力 | DeepSeek API / Ollama (可切换) |
| 邮件 | Spring Mail (SMTP) |
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

# 修改数据库连接配置 (src/main/resources/application.yml)
# spring.datasource.url / username / password

mvn spring-boot:run
```

后端启动在 http://localhost:8080

### 3. 前端启动

```bash
cd frontend

npm install
npm run dev
```

前端启动在 http://localhost:5173

## 项目结构

```
projectManager/
├── sql/                          # 数据库脚本
│   └── schema.sql                # 9张表 + 初始数据
├── backend/                      # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/pm/
│       ├── config/               # 配置类 (Security, MyBatis-Plus, AOP日志, CORS)
│       ├── controller/           # REST API 控制器 (11个)
│       ├── service/              # 业务接口
│       │   └── impl/             # 业务实现 (含AI双实现)
│       ├── mapper/               # MyBatis-Plus Mapper接口
│       ├── model/
│       │   ├── entity/           # 数据库实体 (9个)
│       │   ├── dto/              # 请求DTO (7个)
│       │   ├── vo/               # 响应VO (5个)
│       │   └── enums/            # 枚举
│       ├── common/               # 通用 (响应/异常/常量)
│       ├── security/             # JWT认证
│       └── schedule/             # 定时任务
├── frontend/                     # Vue3 前端
│   └── src/
│       ├── api/                  # API调用封装
│       ├── router/               # 路由配置
│       ├── store/                # Pinia状态管理
│       ├── layouts/              # 布局组件
│       └── views/
│           ├── auth/             # 登录
│           ├── dashboard/        # 首页概览
│           ├── project/          # 项目管理 (列表/详情/表单)
│           ├── ai-suggestion/    # AI风险建议
│           ├── config/           # 系统配置
│           ├── report/           # 报告
│           ├── user/             # 用户管理
│           └── log/              # 操作日志
└── README.md
```

## 数据库表结构

| 表名 | 说明 |
|---|---|
| sys_user | 用户表 (自建账号体系) |
| project | 项目主表 |
| project_stage | 项目阶段表 (含人天/成本/进度) |
| project_risk | 风险/问题表 |
| system_config | 全局配置表 |
| ai_risk_suggestion | AI风险建议表 |
| email_digest_log | 邮件发送记录表 |
| project_change_log | 项目变更记录表 (含变更前后对比) |
| operation_log | 操作日志表 (AOP自动记录) |

## API 接口

统一前缀: `/api/v1`，返回格式: `{code, message, data}`

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 认证 | POST | /auth/login | 登录 |
| 用户 | GET/POST/PUT/DELETE | /users | 用户管理CRUD |
| 项目 | GET/POST | /projects | 列表/创建 |
| 项目 | GET/PUT | /projects/{id} | 详情/编辑 |
| 项目 | PUT | /projects/{id}/satisfaction | 更新客户满意度 |
| 阶段 | GET | /projects/{id}/stages | 项目阶段列表 |
| 阶段 | PUT | /stages/{id} | 更新阶段(含人天/成本/进度) |
| 风险 | POST/GET | /projects/{id}/risks | 登记/列表 |
| 风险 | GET | /risks/aggregated | 跨项目风险聚合 |
| 风险 | PUT | /risks/{id}/stale-override | 手动覆盖停滞状态 |
| Dashboard | GET | /dashboard/summary | 首页聚合数据 |
| Dashboard | GET | /dashboard/health | 项目健康度列表 |
| 配置 | GET/PUT | /config | 全局配置 |
| AI建议 | GET/POST | /ai-suggestions | 建议列表/采纳/忽略 |
| 变更 | POST/GET | /projects/{id}/changes | 变更记录(含前后对比) |
| 邮件 | POST/GET | /digest | 手动发送/发送记录 |
| 日志 | GET | /operation-logs | 操作日志查询(分页/模块过滤) |
| 报告 | GET | /reports/weekly,monthly | 周报/月报(规划中) |

## 核心功能

### 项目管理
1. **项目立项与管理** - 项目编号唯一性校验，创建后自动初始化7个标准阶段
2. **阶段管理** - 支持自定义阶段名，记录预估/实际人天、预估/实际成本、完成进度(0-100%)
3. **阶段延期自动判定** - 计划结束时间已过且未填写实际结束日期的阶段自动标记"已延期"

### 风险管理
4. **风险登记** - 风险编号自动生成（项目编号+日期+序号），类型下拉枚举（技术/进度/需求/资源/质量/外部/成本/管理风险）+ 支持自定义
5. **风险停滞判定** - 超过配置天数未更新的风险自动标红，支持手动覆盖

### Dashboard
6. **风险聚合区** - 跨项目风险汇总，可点击跳转项目详情处理，按等级→严重程度→停滞天数排序
7. **未来关键节点** - 汇总未来14天内的阶段节点 + 已逾期阶段（置顶），含数据来源提示
8. **项目健康度总览** - 健康评分（时间35%+风险40%+交付25%）红黄绿展示，可点击查看详情

### AI与自动化
9. **健康评分** - 三维自动计算，客户满意度独立展示（不参与加权）
10. **每日邮件摘要** - 定时发送，含AI自然语言总结
11. **AI隐性风险探测** - 分析阶段备注文本，识别潜在风险并建议

### 变更与审计
12. **变更记录** - 项目重大变更登记，支持变更前后对比展示（红色删除线→绿色高亮），阶段更新时联动提醒
13. **操作日志** - AOP自动记录所有增删改操作（GET查询不记录），支持按模块和类型过滤

### 系统管理（仅部门经理）
14. **系统配置** - 健康评分权重、停滞阈值、邮件时间、AI配置等全部可调
15. **用户管理** - 用户CRUD（新增/编辑/禁用），角色管理（部门经理/项目经理）
16. **操作日志** - 查看所有操作记录，支持按模块（项目/阶段/风险/系统管理/认证）和类型过滤

## 角色权限

| 角色 | 说明 |
|---|---|
| 部门经理 (DEPT_MANAGER) | 查看所有项目，管理全局配置、用户、操作日志 |
| 项目经理 (PM) | 只能操作自己负责的项目，数据自动过滤 |

## 分支说明

| 分支 | 用途 |
|---|---|
| main | 稳定版本 |
| dev | 开发分支 |
