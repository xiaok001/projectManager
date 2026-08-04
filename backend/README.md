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

### 文件上传
```yaml
file:
  upload-root: ./project-files    # WBS附件存储根目录
server:
  servlet:
    multipart:
      max-file-size: 50MB
```

### JWT
```yaml
jwt:
  secret: your-jwt-secret-key-change-it
  expiration: 86400000   # 24小时(毫秒)
```

## 项目结构

```
src/main/java/com/pm/
├── ProjectManagerApplication.java          # 启动类(@EnableAsync @EnableScheduling)
├── config/
│   ├── SecurityConfig.java                 # Spring Security + CORS
│   ├── MybatisPlusConfig.java              # 分页插件 + 自动填充
│   ├── AppConfig.java                      # RestTemplate + ObjectMapper(LocalDateTime格式)
│   └── OperationLogAspect.java             # AOP操作日志(POST/PUT/DELETE)
├── controller/                             # 14个REST控制器
│   ├── AuthController.java                 # 登录 + 忘记密码
│   ├── ProjectController.java              # 项目CRUD + WBS文件上传下载 + WBS链接更新
│   ├── ProjectStageController.java         # 阶段管理
│   ├── ProjectRiskController.java          # 风险管理
│   ├── ProjectTodoController.java          # 待办(全局分页+项目级)
│   ├── DashboardController.java            # 首页聚合 + 健康度
│   ├── AiSuggestionController.java         # AI建议 + 手动扫描
│   ├── ReportController.java               # AI周报
│   ├── UserController.java                 # 用户CRUD
│   ├── SysRoleController.java              # 角色 + 权限 + 数据权限
│   ├── SysPermissionController.java        # 权限树
│   ├── SystemConfigController.java         # 全局配置
│   ├── EmailDigestController.java          # 邮件摘要 + 测试发送
│   ├── ProjectChangeLogController.java     # 变更记录
│   ├── OperationLogController.java         # 操作日志查询
│   └── ScheduledTaskController.java        # 定时任务管理 + 执行记录
├── service/                                # 15个业务接口 + 实现
│   ├── impl/
│   │   ├── AiProvider.java                 # AI抽象接口
│   │   ├── DeepSeekProvider.java           # DeepSeek实现
│   │   ├── OllamaProvider.java             # Ollama实现
│   │   └── *ServiceImpl.java               # 各业务实现
├── mapper/                                 # MyBatis-Plus Mapper (13个)
├── model/
│   ├── entity/                             # 数据库实体 (16个)
│   ├── dto/                                # 请求DTO (8个)
│   ├── vo/                                 # 响应VO (7个)
│   └── enums/                              # 枚举(ProjectLevel/Severity)
├── common/
│   ├── response/R.java                     # 统一响应 {code, message, data}
│   ├── exception/                          # 业务异常 + 全局异常处理
│   └── constants/Constants.java            # 常量
├── security/
│   ├── JwtUtil.java                        # JWT工具
│   └── JwtAuthenticationFilter.java        # JWT过滤器
└── schedule/
    └── ScheduledTasks.java                 # 4个定时任务(含日志记录+启用检查)
```

## 数据库表（15张 + 初始数据）

| 表名 | 说明 |
|---|---|
| sys_user | 用户表（BCrypt密码 + role_id） |
| sys_role | 角色表（含data_scope数据权限） |
| sys_permission | 权限表（菜单+按钮，树形） |
| sys_role_permission | 角色权限关联 |
| sys_role_project | 角色数据权限关联（项目级） |
| project | 项目主表（含expected_end_date/wbs_online_url/wbs_offline_file） |
| project_stage | 阶段表（含人天/成本/进度） |
| project_risk | 风险表（编号自动生成） |
| project_todo | 待办表（含owner_name手动输入） |
| project_change_log | 变更记录（含change_field） |
| ai_risk_suggestion | AI风险建议 |
| system_config | 全局配置（18项） |
| email_digest_log | 邮件发送记录 |
| operation_log | 操作日志 |
| scheduled_task | 定时任务配置（4个任务） |
| scheduled_task_log | 定时任务执行记录 |

建表脚本：`sql/schema.sql`

## 核心业务逻辑

### 健康评分
```
总分 = 时间得分 × 35% + 风险得分 × 40% + 交付得分 × 25%
```

### 风险编号 / 待办编号
- 风险：`{项目编号}-{日期}-{序号}`，如 `PRJ-A01-20260803-02`
- 待办：`{项目编号}-TD-{日期}-{序号}`，如 `PRJ-A01-TD-20260803-01`

### 进度与状态联动
| 进度 | 状态 | 额外动作 |
|---|---|---|
| 0% | 未开始 | — |
| 1%-99% | 进行中 | 自动填写实际开始日期 |
| 100% | 已完成 | 自动填写实际结束日期 |

### 定时任务
- 每小时：阶段延期刷新 + 风险停滞刷新
- 每小时:15：待办逾期刷新（自动创建风险）
- 每晚22:00：AI全量扫描
- 所有任务支持启用/禁用、手动执行、执行日志记录

### 文件上传
- WBS附件存储路径：`{upload-root}/{项目编号}/{日期}/{原始文件名}`
- 覆盖式替换，数据库只保留最新路径
- 最大 50MB

### 操作日志
- AOP切面拦截 POST/PUT/DELETE，GET 不记录
- 按URL路径自动识别操作模块

### 数据权限
- 角色可配置「全部项目」或「指定项目」
- 项目查询自动按角色数据权限过滤
