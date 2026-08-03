# 多项目管理系统 - 后端

> Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 + MySQL

## 快速开始

### 环境要求

| 环境 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |

```bash
mvn clean install              # 安装依赖
mvn spring-boot:run            # 开发运行 http://localhost:8080
mvn clean package -DskipTests  # 生产构建 → target/project-manager-1.0.0-SNAPSHOT.jar
java -jar app.jar --spring.config.location=classpath:/application.yml,./application.yml
```

## 配置说明

`src/main/resources/application.yml`，关键配置项：

### 数据库连接
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/project_manager?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

### 邮件发送（163 SMTP）
```yaml
spring:
  mail:
    host: smtp.163.com
    port: 465
    username: your-email@163.com
    password: your-smtp-auth-code   # 163邮箱SMTP授权码
    properties:
      mail.smtp.auth: true
      mail.smtp.ssl.enable: true
      mail.smtp.from: your-email@163.com
      mail.smtp.socketFactory.class: javax.net.ssl.SSLSocketFactory
      mail.smtp.socketFactory.port: 465
```

### AI 服务（可切换）
```yaml
ai:
  provider: ollama              # deepseek 或 ollama
  deepseek:
    api-key: your-api-key
    base-url: https://api.deepseek.com
    model: deepseek-chat
  ollama:
    base-url: http://192.168.4.161:11434
    model: qwen2.5:7b
```

### JWT
```yaml
jwt:
  secret: your-jwt-secret-key-change-it
  expiration: 86400000   # 24小时(毫秒)
```

### CORS
`SecurityConfig.java` 中配置允许的前端地址。

## 项目结构

```
src/main/java/com/pm/
├── ProjectManagerApplication.java          # 启动类(@EnableAsync @EnableScheduling)
├── config/
│   ├── SecurityConfig.java                 # Spring Security + CORS
│   ├── MybatisPlusConfig.java              # 分页插件 + 自动填充
│   ├── AppConfig.java                      # RestTemplate / ObjectMapper
│   └── OperationLogAspect.java             # AOP操作日志(POST/PUT/DELETE)
├── controller/                             # 13个REST控制器
│   ├── AuthController.java                 # 登录 + 忘记密码
│   ├── ProjectController.java              # 项目CRUD + 查询过滤
│   ├── ProjectStageController.java         # 阶段管理
│   ├── ProjectRiskController.java          # 风险管理
│   ├── ProjectTodoController.java          # 待办(全局+项目级)
│   ├── DashboardController.java            # 首页聚合 + 健康度
│   ├── AiSuggestionController.java         # AI建议 + 手动扫描
│   ├── ReportController.java               # AI周报
│   ├── UserController.java                 # 用户CRUD
│   ├── SysRoleController.java              # 角色 + 权限 + 数据权限
│   ├── SysPermissionController.java        # 权限树
│   ├── SystemConfigController.java         # 全局配置
│   ├── EmailDigestController.java          # 邮件摘要 + 测试发送
│   ├── ProjectChangeLogController.java     # 变更记录
│   └── OperationLogController.java         # 操作日志查询
├── service/
│   ├── ProjectService.java                 # 项目(含数据权限过滤+查询条件)
│   ├── ProjectStageService.java            # 阶段(延期判定+进度联动状态)
│   ├── ProjectRiskService.java             # 风险(编号生成+停滞判定)
│   ├── ProjectTodoService.java             # 待办(分页+逾期转风险)
│   ├── DashboardService.java               # Dashboard + 健康评分计算
│   ├── AiRiskSuggestionService.java        # AI建议 + 全量扫描
│   ├── ReportService.java                  # 周报数据聚合
│   ├── SysUserService.java                 # 用户(含忘记密码)
│   ├── SysRoleService.java                 # 角色 + 权限 + 数据权限
│   ├── SysPermissionService.java           # 权限树
│   ├── SystemConfigService.java            # 配置管理
│   ├── EmailDigestService.java             # 邮件摘要 + 定时发送
│   ├── ProjectChangeLogService.java        # 变更记录
│   ├── OperationLogService.java            # 操作日志
│   └── impl/
│       ├── AiProvider.java                 # AI抽象接口
│       ├── DeepSeekProvider.java           # DeepSeek实现
│       ├── OllamaProvider.java             # Ollama实现
│       └── *ServiceImpl.java               # 各业务实现
├── mapper/                                 # MyBatis-Plus Mapper (12个)
├── model/
│   ├── entity/                             # 数据库实体 (14个)
│   ├── dto/                                # 请求DTO (8个)
│   ├── vo/                                 # 响应VO (7个)
│   └── enums/                              # 枚举(ProjectLevel/Severity)
├── common/
│   ├── response/R.java                     # 统一响应 {code, message, data}
│   ├── exception/BusinessException.java    # 业务异常
│   ├── exception/GlobalExceptionHandler.java  # 全局异常(含字段名去前缀)
│   └── constants/Constants.java            # 常量(阶段名/角色/状态)
├── security/
│   ├── JwtUtil.java                        # JWT生成/解析/验证
│   └── JwtAuthenticationFilter.java        # JWT认证过滤器
└── schedule/
    └── ScheduledTasks.java                 # 4个定时任务
```

## 数据库表（12张 + 初始数据）

| 表名 | 说明 |
|---|---|
| sys_user | 用户表（BCrypt密码） |
| sys_role | 角色表（含data_scope数据权限） |
| sys_permission | 权限表（菜单+按钮，树形结构） |
| sys_role_permission | 角色权限关联 |
| sys_role_project | 角色数据权限关联（项目级） |
| project | 项目主表（含expected_end_date） |
| project_stage | 阶段表（含人天/成本/进度） |
| project_risk | 风险表（编号自动生成） |
| project_todo | 待办表（编号自动生成，逾期自动转风险） |
| project_change_log | 变更记录（含change_field对比字段） |
| ai_risk_suggestion | AI风险建议 |
| system_config | 全局配置（18项） |
| email_digest_log | 邮件发送记录 |
| operation_log | 操作日志 |

建表脚本：`sql/schema.sql`

## API 接口

统一前缀：`/api/v1`，返回：`{code, message, data}`

认证：`Authorization: Bearer <token>`（登录/忘记密码接口除外）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /auth/login | 登录 |
| POST | /auth/verify-username | 验证账号 |
| POST | /auth/reset-password | 重置密码(发邮件) |
| GET/POST/PUT/DELETE | /users | 用户管理 |
| GET/POST/PUT/DELETE | /roles | 角色管理 |
| GET | /roles/{id}/permissions | 获取角色权限 |
| PUT | /roles/{id}/permissions | 分配权限 |
| GET | /roles/{id}/data-scope | 获取数据权限 |
| PUT | /roles/{id}/data-scope | 分配数据权限 |
| GET | /permissions/tree | 权限树 |
| GET/POST | /projects | 项目列表(含name/projectCode/level查询) |
| GET/PUT/DELETE | /projects/{id} | 项目详情/编辑/删除 |
| PUT | /projects/{id}/satisfaction | 更新满意度 |
| GET | /projects/{id}/stages | 阶段列表 |
| PUT | /stages/{id} | 更新阶段(进度联动状态) |
| POST/GET | /projects/{id}/risks | 风险登记/列表 |
| GET | /risks/aggregated | 跨项目风险聚合 |
| PUT | /risks/{id}/stale-override | 手动覆盖停滞 |
| GET/POST | /projects/{id}/todos | 项目待办列表/创建 |
| GET | /todos/page | 全局待办分页(含项目/状态/优先级/关键词) |
| POST | /todos | 全局创建待办 |
| PUT/DELETE | /todos/{id} | 编辑/删除待办 |
| GET | /ai-suggestions/page | AI建议分页(含项目/状态/时间范围) |
| GET | /ai-suggestions | AI建议列表 |
| POST | /ai-suggestions/{id}/accept | 采纳建议 |
| POST | /ai-suggestions/{id}/ignore | 忽略建议 |
| POST | /ai-suggestions/scan | 手动AI扫描 |
| GET | /dashboard/summary | Dashboard聚合 |
| GET | /dashboard/health | 项目健康度 |
| GET/PUT | /config | 全局配置 |
| POST | /digest/send-now | 手动发送摘要 |
| POST | /digest/test | 测试邮件发送 |
| GET | /digest/logs | 邮件发送记录 |
| POST/GET | /projects/{id}/changes | 变更记录 |
| GET | /reports/weekly | AI周报(支持projectId参数) |
| GET | /operation-logs | 操作日志分页 |

## 核心业务逻辑

### 健康评分
```
总分 = 时间得分 × 35% + 风险得分 × 40% + 交付得分 × 25%
```
- 时间：100 - 延期天数 × 2（每天扣2分）
- 风险：100 - 高×15 - 中×8 - 低×3 - 停滞×10
- 交付：按时完成阶段数 ÷ 应完成阶段数 × 100
- 颜色：≥80 绿 / 60-79 黄 / <60 红

### 风险编号
`{项目编号}-{日期}-{序号}`，如 `PRJ-A01-20260803-02`

### 待办编号
`{项目编号}-TD-{日期}-{序号}`，如 `PRJ-A01-TD-20260803-01`

### 进度与状态联动
| 进度 | 状态 | 额外动作 |
|---|---|---|
| 0% | 未开始 | — |
| 1%-99% | 进行中 | 自动填写实际开始日期 |
| 100% | 已完成 | 自动填写实际结束日期 |

### 项目结束校验
- 编辑项目状态改为「已完成」时，检查非运维阶段是否全部完成
- 未完成时后端拒绝保存，前端显示未完成阶段列表+二次确认

### AI风险探测
1. 阶段备注保存 → 异步调用AI分析 → 写入建议表（待确认）
2. 全量扫描：遍历所有进行中项目阶段备注，按`(stageId+remark哈希)`去重
3. 每晚22:00自动执行，前端可手动触发

### 周报生成
- 聚合本周完成阶段/新增关闭风险/待办统计
- 排除运维阶段和已结束项目
- AI生成3-5句叙述性总结

### 操作日志
- AOP切面拦截所有Controller方法，仅记录POST/PUT/DELETE
- 按URL路径自动识别操作模块
- 支持按模块和操作类型过滤

### 数据权限
- 角色可配置「全部项目」或「指定项目」
- 项目查询时自动按角色数据权限过滤
