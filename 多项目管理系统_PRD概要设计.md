# 多项目管理与风险提醒系统 —— PRD / 概要设计文档

版本：v3.0（已实现）
日期：2026-08-07
面向角色：部门经理（统揽视角），项目经理（数据录入方）

---

## 一、背景与目标

### 1.1 背景
公司现有项目管理系统面向项目经理的执行层需求（详细WBS、任务分解），无法满足部门经理"跨项目治理"的视角需求：每天需要快速掌握多个项目的风险、待处理问题和近期计划，作为介入沟通的依据。

### 1.2 目标
- **标准化**：统一立项、阶段、风险、待办记录的字段与流程，使不同项目、不同项目经理填报的数据可横向比较。
- **自动化提醒**：系统自动判断阶段延期、风险停滞、待办逾期，无需人工巡检即可发现问题。
- **AI赋能**：通过AI自动识别隐性风险、生成周报摘要、辅助决策。
- **权限精细化**：支持页面权限、按钮权限、数据权限（项目级）三级权限控制。

### 1.3 非目标（本期不做）
- 不做详细任务/工时管理（仍由项目经理在原有WBS工具中管理）。
- 不做资源排期、人力分配。
- 不做客户满意度的自动采集（本期人工填写）。

---

## 二、用户角色与权限体系

### 2.1 角色体系

| 角色 | 标识 | 数据权限 | 说明 |
|---|---|---|---|
| 超级管理员 | admin | 全部项目 | 拥有所有权限，内置角色不可删除 |
| 项目经理 | pm | 指定项目 | 操作自己负责的项目 |
| 查看者 | viewer | 全部项目 | 只读权限 |

- 角色支持「全部项目」或「指定项目」两种数据权限
- 角色可配置页面权限（菜单）+ 按钮权限（操作）
- 内置角色（admin/pm/viewer）不可删除

### 2.2 核心场景
1. 部门经理打开系统首页，5分钟内了解所有在管项目的健康状况。
2. 部门经理每天收到邮件摘要和项目待办与风险日报。
3. 项目经理每周更新阶段进度、风险状态和待办事项。
4. 系统每周自动生成AI叙述性周报。
5. 系统每晚自动扫描项目阶段备注，AI识别潜在风险。

---

## 三、功能范围

| 序号 | 功能 | 说明 |
|---|---|---|
| 1 | 项目立项与管理 | 项目编号唯一性校验，自动初始化7个标准阶段，预期结束日期，WBS在线/离线文档 |
| 2 | 阶段管理 | 预估/实际人天、成本、完成进度(0-100%)，进度与状态联动，延期自动判定 |
| 3 | 风险/问题管理 | 风险编号自动生成，8种类型枚举+自定义，停滞判定+手动覆盖 |
| 4 | 项目待办 | 待办编号自动生成，阶段/来源/优先级/紧急程度/负责人/进度/阻塞问题/风险说明，逾期自动转风险 |
| 5 | Dashboard | 风险聚合区(可点击跳转) + 未来关键节点(含数据来源提示) + 项目健康度总览(评分明细hover) |
| 6 | 健康评分 | 时间35%+风险40%+交付25%，数据不足显示灰色，评分明细弹窗 |
| 7 | AI风险探测 | 阶段备注异步分析 + 每晚全量扫描 + 手动扫描(含原理说明) |
| 8 | AI周报 | 聚合数据生成叙述性总结，排除运维阶段和已结束项目，历史记录 |
| 9 | 邮件摘要 | 定时发送+手动发送+测试发送，支持项目待办与风险日报 |
| 10 | 用户管理 | CRUD + 邮箱/手机号校验 + 个人资料修改 + 密码修改 |
| 11 | 角色管理 | CRUD + 页面/按钮权限分配 + 数据权限(项目级) + 内置角色保护 |
| 12 | 系统配置 | 健康权重/停滞阈值/邮件/AI(动态字段+测试连接)，左右导航布局 |
| 13 | 操作日志 | AOP自动记录增删改，按模块/类型过滤 |
| 14 | 定时任务 | 6个任务/启用禁用/修改Cron/手动执行(二次确认)/执行记录，动态调度 |
| 15 | 变更记录 | 人员/内容/范围/风险变更登记，前后对比展示 |
| 16 | 认证与安全 | JWT，动态权限加载，403跳登录，忘记密码，404页面 |

---

## 四、数据模型设计

### 4.1 用户与权限（5张表）

**sys_user** - 用户表
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| username | varchar | 用户名，唯一 |
| password | varchar | BCrypt加密 |
| real_name | varchar | 真实姓名 |
| email / phone | varchar | 邮箱/手机号 |
| role | varchar | 角色标识（向后兼容） |
| role_id | bigint | 关联sys_role |
| status | tinyint | 1启用/0禁用 |

**sys_role** - 角色表
| 字段 | 类型 | 说明 |
|---|---|---|
| role_name | varchar | 角色名称 |
| role_key | varchar | 角色标识，唯一 |
| data_scope | varchar | all=全部/custom=指定项目 |
| is_system | tinyint | 1=内置角色不可删除 |

**sys_permission** - 权限表（树形）
| 字段 | 类型 | 说明 |
|---|---|---|
| parent_id | bigint | 父权限ID |
| perm_key | varchar | 权限标识 |
| type | varchar | menu/button |
| path | varchar | 页面路由 |

**sys_role_permission** / **sys_role_project** - 关联表

### 4.2 项目主表 Project
| 字段 | 类型 | 说明 |
|---|---|---|
| project_code | varchar | 项目编号，唯一 |
| name / type / level | | 基础信息 |
| amount | decimal | 项目金额 |
| pm_id | bigint | 项目经理 |
| start_date / expected_end_date | date | 立项日期/预期结束日期 |
| wbs_online_url | varchar | WBS在线文档链接 |
| wbs_offline_file / wbs_offline_name | varchar | WBS附件路径/原始文件名 |
| status | varchar | 业务状态：未启动/进行中/暂停/验收中/已关闭 |
| satisfaction_score | tinyint | 客户满意度(1-10) |

**业务状态 vs 健康状态**：
- 业务状态（status）：手动设置，反映项目整体生命周期
- 健康状态（healthColor）：自动计算，红/黄/绿/灰

### 4.3 阶段表 ProjectStage
| 字段 | 类型 | 说明 |
|---|---|---|
| stage_name | varchar | 阶段名（可自定义） |
| plan_start/plan_end, actual_start/actual_end | date | 计划/实际起止 |
| status | varchar | 未开始/进行中/已完成/已延期 |
| plan_man_days / actual_man_days | decimal | 预估/实际人天 |
| plan_cost / actual_cost | decimal | 预估/实际成本 |
| progress | int | 完成进度(0-100)，联动状态 |

### 4.4 风险表 ProjectRisk
| 字段 | 类型 | 说明 |
|---|---|---|
| risk_code | varchar | 风险编号(项目编号-日期-序号) |
| type | varchar | 风险/问题 |
| severity | varchar | 高/中/低 |
| is_stale / stale_override | boolean | 停滞判定+手动覆盖 |

### 4.5 待办表 ProjectTodo
| 字段 | 类型 | 说明 |
|---|---|---|
| todo_code | varchar | 待办编号(项目编号-TD-日期-序号) |
| source | varchar | 来源 |
| priority / urgency | varchar | 优先级/紧急程度 |
| owner_id / owner_name | | 负责人（支持手动输入） |
| progress | int | 完成进度，联动状态 |

### 4.6 其他表
| 表名 | 说明 |
|---|---|
| system_config | 全局配置 |
| ai_risk_suggestion | AI风险建议 |
| email_digest_log | 邮件发送记录 |
| project_change_log | 变更记录(含change_field) |
| operation_log | 操作日志 |
| scheduled_task | 定时任务配置 |
| scheduled_task_log | 定时任务执行记录 |
| report_weekly_log | 周报历史记录 |

---

## 五、核心功能设计

### 5.1 Dashboard
- 风险聚合区：可点击跳转项目详情风险Tab，「去处理」按钮
- 未来关键节点：含数据来源提示
- 项目健康度：hover显示评分明细（三维度+计算依据）

### 5.2 健康评分
```
总分 = 时间 × 35% + 风险 × 40% + 交付 × 25%
```
- **数据不足判定**：所有阶段plan_end均未到期且无待处理风险 → 显示"暂无可计算数据"（灰色）
- **评分明细**：每项维度返回得分+人类可读的计算依据

### 5.3 阶段管理
- 进度联动状态：0%=未开始，1%-99%=进行中，100%=已完成
- 进度<100时清除actual_end（允许回退进度）

### 5.4 定时任务（动态调度）
- DynamicScheduler：启动时从数据库读Cron，修改后立即生效
- 6个任务：阶段延期/风险停滞/待办逾期/AI扫描/待办日报/管理摘要
- 手动执行二次确认弹窗（显示任务名称+影响范围）

### 5.5 AI服务
- AiProvider接口：DeepSeek/Ollama双实现
- 配置页动态字段+测试连接
- 降级策略：AI失败时退化为纯结构化文本

---

## 六、技术选型

| 层 | 选型 |
|---|---|
| 前端 | Vue3 + TypeScript + Element Plus + Vite |
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0+ |
| 认证 | Spring Security + JWT + 动态权限 |
| 定时任务 | Spring DynamicScheduler |
| AI | Ollama / DeepSeek（可切换） |
| 邮件 | Spring Mail（163 SMTP） |
| 操作日志 | Spring AOP |

---

## 七、部署信息

| 项目 | 信息 |
|---|---|
| 服务器 | 192.168.4.161 |
| 前端 | http://192.168.4.161:8090 |
| 后端 | http://192.168.4.161:8080 |
| Ollama | http://192.168.4.161:11434 |
| 数据库 | 192.168.4.195:3306/project_manager |
| JDK | 17 (/opt/jdk17) |
| Nginx | 端口8090 (/etc/nginx/conf.d/pm.conf) |
| 一键部署 | `./deploy.sh` |
