# 多项目管理与风险提醒系统（PMM）

> 面向部门经理（统揽视角）和项目经理（数据录入方）的多项目协同治理平台

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0+ |
| 认证 | Spring Security + JWT + 动态权限加载 |
| 定时任务 | Spring DynamicScheduler（数据库驱动，热更新） |
| AI能力 | Ollama（本地）/ DeepSeek（云端，可切换，支持测试连接） |
| 邮件 | Spring Mail（163 SMTP） |
| 操作日志 | Spring AOP（仅记录增删改，GET不记录） |

## 快速开始

### 1. 数据库初始化

```bash
mysql -u root -p < sql/schema.sql
```

默认管理员账号: `admin` / `admin123`

### 2. 后端启动

```bash
cd backend && mvn spring-boot:run
```

### 3. 前端启动

```bash
cd frontend && npm install && npm run dev
```

### 4. 一键部署

```bash
./deploy.sh   # 选择：仅前端 / 仅后端 / 全部部署
```

## 数据库表（17张）

| 表名 | 说明 |
|---|---|
| sys_user | 用户表 |
| sys_role | 角色表（含 data_scope 数据权限、is_system 内置标识） |
| sys_permission | 权限表（菜单+按钮，树形） |
| sys_role_permission | 角色权限关联 |
| sys_role_project | 角色数据权限关联（项目级） |
| project | 项目主表（含 WBS 文档、业务状态） |
| project_stage | 项目阶段（含人天/成本/进度联动） |
| project_risk | 风险表（编号自动生成） |
| project_todo | 待办表（逾期自动转风险） |
| project_change_log | 项目变更记录（前后对比） |
| ai_risk_suggestion | AI 风险建议 |
| system_config | 全局配置 |
| email_digest_log | 邮件发送记录 |
| operation_log | 操作日志 |
| scheduled_task | 定时任务配置（6个任务） |
| scheduled_task_log | 定时任务执行记录 |
| report_weekly_log | 周报历史记录 |

## 核心功能

### 项目管理
- 项目编号唯一性校验，创建后自动初始化 7 个标准阶段
- **业务状态**：未启动 / 进行中 / 暂停 / 验收中 / 已关闭（手动设置）
- **健康状态**：红 / 黄 / 绿 / 灰（自动计算，独立于业务状态）
- WBS 在线文档链接 + 离线附件（拖拽上传/下载/覆盖替换）
- 项目列表支持名称/编号/等级/业务状态查询 + 分页
- 项目结束校验：关闭时检查非运维阶段是否完成

### 阶段管理
- 阶段名可编辑，支持标准阶段 + 自定义
- 预估/实际人天、预估/实际成本、完成进度（0-100%）
- 进度与状态联动：0%=未开始，1%-99%=进行中，100%=已完成
- 延期自动判定（动态调度器每小时刷新）

### 风险管理
- 风险编号自动生成（项目编号+日期+序号）
- 8 种类型枚举 + 支持自定义输入
- 停滞自动判定，支持手动覆盖

### 项目待办
- 待办编号自动生成，负责人支持下拉选择和手动输入
- 逾期待办自动创建风险记录
- 全局待办页面 + 项目详情待办 Tab

### AI 能力
- 隐性风险探测：阶段备注保存后异步分析
- 每晚 22:00 全量扫描 + 手动扫描（含原理说明弹窗）
- 周报 AI 叙述性总结（含生成规则 Tips）
- DeepSeek/Ollama 双实现，配置页支持测试连接

### 健康评分
- 时间 35% + 风险 40% + 交付 25% 自动计算
- 数据不足时显示"暂无可计算数据"（灰色），不默认给 100 分
- hover 显示评分明细（三维度得分 + 计算依据）
- 所有权重和阈值可在系统配置页面调整

### 系统管理（部门经理）
- **系统配置**：左右导航布局（基础/评分/AI），AI 配置动态字段 + 测试连接
- **用户管理**：CRUD + 个人资料修改 + 密码修改 + 邮箱手机号格式校验
- **角色管理**：页面/按钮权限 + 项目级数据权限，内置角色不可删除
- **操作日志**：AOP 自动记录增删改，按模块/类型过滤
- **定时任务**：Cron 可编辑（自由输入 + 常用示例）、启用/禁用、手动执行（二次确认弹窗）、执行记录

### 认证与安全
- JWT 无状态认证，动态权限加载（菜单根据权限树渲染）
- 忘记密码：验证账号 → 验证邮箱 → 发送新密码
- 403 自动跳登录页，404 自定义页面

## 侧边栏导航（动态权限）

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

## 定时任务（6 个，动态调度）

| 任务 | 默认Cron | 说明 |
|---|---|---|
| 阶段延期刷新 | 每小时 | 自动标记已延期阶段 |
| 风险停滞刷新 | 每小时:30 | 自动标记停滞风险 |
| 待办逾期刷新 | 每小时:15 | 逾期待办自动创建风险 |
| AI风险扫描 | 每天 22:00 | 全量扫描阶段备注 |
| 项目待办与风险日报 | 每天 9:20 | 按项目汇总待办+风险，邮件发送 |
| 每日项目管理摘要 | 每天 9:30 | Dashboard AI 总结，邮件发送 |

所有任务支持启用/禁用、修改 Cron 频率、手动执行（二次确认）、执行日志。修改后立即生效，无需重启。

## API 接口

统一前缀: `/api/v1`，返回: `{code, message, data}`

<details>
<summary>点击展开完整接口列表（50+）</summary>

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 认证 | POST | /auth/login | 登录 |
| 认证 | POST | /auth/verify-username | 验证账号 |
| 认证 | POST | /auth/reset-password | 重置密码 |
| 认证 | GET | /auth/permissions | 当前用户权限树 |
| 用户 | GET/POST/PUT/DELETE | /users | 用户 CRUD |
| 用户 | GET/PUT | /users/me | 个人资料 |
| 用户 | PUT | /users/me/password | 修改密码 |
| 角色 | GET/POST/PUT/DELETE | /roles | 角色 CRUD |
| 角色 | GET/PUT | /roles/{id}/permissions | 权限分配 |
| 角色 | GET/PUT | /roles/{id}/data-scope | 数据权限 |
| 权限 | GET | /permissions/tree | 权限树 |
| 项目 | GET/POST/PUT/DELETE | /projects | 项目 CRUD（支持 name/projectCode/level/status 查询） |
| 项目 | PUT | /projects/{id}/wbs-url | WBS 在线链接 |
| 项目 | POST/GET | /projects/{id}/wbs-file | WBS 附件上传/下载 |
| 阶段 | GET | /projects/{id}/stages | 阶段列表 |
| 阶段 | PUT | /stages/{id} | 更新阶段 |
| 风险 | GET/POST/PUT | /projects/{id}/risks | 风险 CRUD |
| 风险 | GET | /risks/aggregated | 跨项目风险聚合 |
| 待办 | GET/POST/PUT/DELETE | /todos | 待办 CRUD |
| 待办 | GET | /todos/page | 全局待办分页 |
| AI建议 | GET | /ai-suggestions/page | AI 建议分页 |
| AI建议 | POST | /ai-suggestions/scan | 手动 AI 扫描 |
| AI建议 | POST | /ai-suggestions/{id}/accept | 采纳 |
| AI建议 | POST | /ai-suggestions/{id}/ignore | 忽略 |
| Dashboard | GET | /dashboard/summary | 首页聚合 |
| Dashboard | GET | /dashboard/health | 项目健康度 |
| 配置 | GET/PUT | /config | 全局配置 |
| 配置 | POST | /config/test-ai | 测试 AI 连接 |
| 邮件 | POST | /digest/send-now | 手动发送摘要 |
| 邮件 | POST | /digest/test | 测试邮件发送 |
| 邮件 | GET | /digest/logs | 邮件发送记录 |
| 变更 | GET/POST | /projects/{id}/changes | 变更记录 |
| 报告 | GET | /reports/weekly | AI 周报 |
| 报告 | GET | /reports/weekly/history | 周报历史记录 |
| 日志 | GET | /operation-logs | 操作日志 |
| 定时任务 | GET | /scheduled-tasks | 任务列表 |
| 定时任务 | PUT | /scheduled-tasks/{id}/toggle | 启用/禁用 |
| 定时任务 | PUT | /scheduled-tasks/{id}/cron | 修改执行频率 |
| 定时任务 | POST | /scheduled-tasks/{id}/run | 手动执行 |
| 定时任务 | GET | /scheduled-tasks/logs | 执行记录 |

</details>

## 部署

| 项目 | 信息 |
|---|---|
| 服务器 | 192.168.4.161 |
| 前端 | http://192.168.4.161:8090 |
| 后端 | http://192.168.4.161:8080 |
| Ollama | http://192.168.4.161:11434 (qwen2.5:7b) |
| 一键部署 | `./deploy.sh` |
