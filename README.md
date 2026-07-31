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

## 快速开始

### 1. 数据库初始化

```bash
# 执行建表脚本
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
│   └── schema.sql                # 建表 + 初始数据
├── backend/                      # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/pm/
│       ├── config/               # 配置类 (Security, MyBatis-Plus, CORS)
│       ├── controller/           # REST API 控制器
│       ├── service/              # 业务接口
│       │   └── impl/             # 业务实现
│       ├── mapper/               # MyBatis-Plus Mapper接口
│       ├── model/
│       │   ├── entity/           # 数据库实体
│       │   ├── dto/              # 请求DTO
│       │   ├── vo/               # 响应VO
│       │   └── enums/            # 枚举
│       ├── common/               # 通用 (响应/异常/常量)
│       ├── security/             # JWT认证
│       └── schedule/             # 定时任务
├── frontend/                     # Vue3 前端
│   ├── src/
│   │   ├── api/                  # API调用封装
│   │   ├── router/               # 路由配置
│   │   ├── store/                # Pinia状态管理
│   │   ├── layouts/              # 布局组件
│   │   ├── views/                # 页面
│   │   │   ├── auth/             # 登录
│   │   │   ├── dashboard/        # 首页概览
│   │   │   ├── project/          # 项目管理 (列表/详情/表单)
│   │   │   ├── ai-suggestion/    # AI风险建议
│   │   │   ├── config/           # 系统配置
│   │   │   └── report/           # 报告
│   │   ├── components/           # 公共组件
│   │   └── assets/styles/        # 全局样式
│   └── vite.config.ts
└── CLAUDE.md                     # 项目说明
```

## API 接口

统一前缀: `/api/v1`，返回格式: `{code, message, data}`

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 认证 | POST | /auth/login | 登录 |
| 项目 | GET/POST | /projects | 列表/创建 |
| 项目 | GET/PUT | /projects/{id} | 详情/编辑 |
| 阶段 | GET | /projects/{id}/stages | 项目阶段列表 |
| 阶段 | PUT | /stages/{id} | 更新阶段 |
| 风险 | POST/GET | /projects/{id}/risks | 登记/列表 |
| 风险 | GET | /risks/aggregated | 跨项目风险聚合 |
| Dashboard | GET | /dashboard/summary | 首页聚合数据 |
| 配置 | GET/PUT | /config | 全局配置 |
| AI建议 | GET | /ai-suggestions | 建议列表 |
| 变更 | POST/GET | /projects/{id}/changes | 变更记录 |
| 邮件 | POST | /digest/send-now | 手动发送摘要 |

## 核心功能

1. **项目立项与管理** - 项目编号唯一性校验，创建后自动初始化7个标准阶段
2. **阶段延期自动判定** - 计划结束时间已过且未填写实际结束日期的阶段自动标记"已延期"
3. **风险停滞判定** - 超过配置天数未更新的风险自动标红，支持手动覆盖
4. **Dashboard** - 风险聚合区 / 未来关键节点 / 项目健康度总览
5. **健康评分** - 时间35% + 风险40% + 交付25% 自动计算，客户满意度独立展示
6. **每日邮件摘要** - 定时发送，含AI自然语言总结
7. **AI隐性风险探测** - 分析阶段备注文本，识别潜在风险并建议
8. **变更记录** - 项目重大变更登记，阶段更新时联动提醒
