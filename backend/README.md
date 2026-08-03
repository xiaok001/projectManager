# 多项目管理系统 - 后端

> Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 + MySQL

## 快速开始

### 环境要求

| 环境 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |

### 安装依赖

```bash
mvn clean install
```

### 开发模式运行

```bash
mvn spring-boot:run
```

启动后监听 http://localhost:8080。

### 生产构建

```bash
mvn clean package -DskipTests
```

构建产物：`target/project-manager-1.0.0-SNAPSHOT.jar`

### 生产环境运行

```bash
java -jar target/project-manager-1.0.0-SNAPSHOT.jar
```

或指定外部配置文件：

```bash
java -jar app.jar --spring.config.location=classpath:/application.yml,./application.yml
```

## 配置说明

所有配置在 `src/main/resources/application.yml`，以下为关键配置项：

### 数据库连接

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/project_manager?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

### 邮件发送（163 邮箱 SMTP）

```yaml
spring:
  mail:
    host: smtp.163.com
    port: 465
    username: your-email@163.com
    password: your-smtp-auth-code   # 163邮箱SMTP授权码，非登录密码
    properties:
      mail.smtp.auth: true
      mail.smtp.ssl.enable: true
      mail.smtp.from: your-email@163.com
      mail.smtp.socketFactory.class: javax.net.ssl.SSLSocketFactory
      mail.smtp.socketFactory.port: 465
```

> 授权码获取：163邮箱网页版 → 设置 → POP3/SMTP/IMAP → 开启SMTP服务 → 生成授权码

### AI 服务（支持两种，可切换）

```yaml
ai:
  provider: ollama          # 切换: deepseek 或 ollama
  deepseek:
    api-key: your-api-key
    base-url: https://api.deepseek.com
    model: deepseek-chat
  ollama:
    base-url: http://192.168.4.161:11434
    model: qwen2.5:7b
```

- `ai.provider=deepseek`：调用 DeepSeek 云端 API（需 API Key）
- `ai.provider=ollama`：调用本地 Ollama 服务（无需 Key，需部署 Ollama）

### JWT 配置

```yaml
jwt:
  secret: your-jwt-secret-key-change-it   # 生产环境务必修改
  expiration: 86400000                     # Token有效期，默认24小时(毫秒)
```

### CORS 跨域配置

在 `SecurityConfig.java` 中配置允许的前端地址：

```java
configuration.setAllowedOrigins(List.of(
    "http://localhost:5173",           // 本地开发
    "http://192.168.4.161:8090"        // 生产环境
));
```

## 项目结构

```
backend/src/main/java/com/pm/
├── ProjectManagerApplication.java       # 启动类
├── config/
│   ├── SecurityConfig.java              # Spring Security + CORS 配置
│   ├── MybatisPlusConfig.java           # 分页插件 + 自动填充
│   ├── AppConfig.java                   # RestTemplate / ObjectMapper
│   └── OperationLogAspect.java          # AOP操作日志切面(记录增删改)
├── controller/                          # REST API 控制器
│   ├── AuthController.java              # 登录 / 用户列表
│   ├── ProjectController.java           # 项目CRUD
│   ├── ProjectStageController.java      # 阶段管理
│   ├── ProjectRiskController.java       # 风险管理
│   ├── DashboardController.java         # 首页聚合数据
│   ├── SystemConfigController.java      # 全局配置
│   ├── AiSuggestionController.java      # AI风险建议
│   ├── EmailDigestController.java       # 邮件摘要/测试发送
│   ├── ProjectChangeLogController.java  # 变更记录
│   ├── UserController.java              # 用户管理CRUD
│   └── OperationLogController.java      # 操作日志查询
├── service/
│   ├── ProjectService.java              # 项目业务接口
│   ├── ProjectStageService.java         # 阶段业务接口
│   ├── ProjectRiskService.java          # 风险业务接口
│   ├── DashboardService.java            # Dashboard聚合+健康评分计算
│   ├── SystemConfigService.java         # 配置管理
│   ├── AiRiskSuggestionService.java     # AI风险建议
│   ├── EmailDigestService.java          # 邮件摘要+定时发送
│   ├── ProjectChangeLogService.java     # 变更记录
│   ├── SysUserService.java              # 用户管理
│   ├── OperationLogService.java         # 操作日志
│   └── impl/                            # 实现类
│       ├── AiProvider.java              # AI服务抽象接口
│       ├── DeepSeekProvider.java        # DeepSeek实现
│       ├── OllamaProvider.java          # Ollama实现
│       └── ...ServiceImpl.java          # 各业务实现
├── mapper/                              # MyBatis-Plus Mapper接口
├── model/
│   ├── entity/                          # 数据库实体(9个)
│   ├── dto/                             # 请求DTO(7个)
│   ├── vo/                              # 响应VO(5个)
│   └── enums/                           # 枚举(ProjectLevel/Severity)
├── common/
│   ├── response/R.java                  # 统一响应 {code, message, data}
│   ├── exception/BusinessException.java # 业务异常
│   ├── exception/GlobalExceptionHandler.java  # 全局异常处理
│   └── constants/Constants.java         # 常量定义
├── security/
│   ├── JwtUtil.java                     # JWT工具类(生成/解析/验证)
│   └── JwtAuthenticationFilter.java     # JWT认证过滤器
└── schedule/
    └── ScheduledTasks.java              # 定时任务(阶段延期刷新/风险停滞刷新)
```

## 数据库表

| 表名 | 说明 |
|---|---|
| `sys_user` | 用户表 (自建账号体系，BCrypt密码加密) |
| `project` | 项目主表 |
| `project_stage` | 项目阶段表 (含人天/成本/进度字段) |
| `project_risk` | 风险/问题表 (风险编号自动生成) |
| `system_config` | 全局配置表 (18项可配参数) |
| `ai_risk_suggestion` | AI风险建议表 |
| `email_digest_log` | 邮件发送记录表 |
| `project_change_log` | 项目变更记录表 (含变更前后对比) |
| `operation_log` | 操作日志表 (AOP自动记录) |

建表脚本：`sql/schema.sql`，默认管理员：`admin` / `admin123`

## API 接口

统一前缀：`/api/v1`，返回格式：`{"code": 200, "message": "success", "data": ...}`

认证方式：请求头 `Authorization: Bearer <token>`（登录接口除外）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | /auth/login | 登录 | 公开 |
| GET | /auth/users | 用户列表 | 已认证 |
| GET/POST | /projects | 项目列表/创建 | 已认证 |
| GET/PUT | /projects/{id} | 项目详情/编辑 | 部门经理/本人PM |
| PUT | /projects/{id}/satisfaction | 更新满意度 | 部门经理/本人PM |
| GET | /projects/{id}/stages | 阶段列表 | 已认证 |
| PUT | /stages/{id} | 更新阶段 | 已认证 |
| POST/GET | /projects/{id}/risks | 风险登记/列表 | 已认证 |
| GET | /risks/aggregated | 跨项目风险聚合 | 已认证 |
| PUT | /risks/{id}/stale-override | 手动覆盖停滞 | 已认证 |
| GET | /dashboard/summary | Dashboard聚合 | 已认证 |
| GET | /dashboard/health | 健康度列表 | 已认证 |
| GET/PUT | /config | 全局配置 | 部门经理 |
| GET | /ai-suggestions | AI建议列表 | 已认证 |
| POST | /ai-suggestions/{id}/accept | 采纳建议 | 已认证 |
| POST | /ai-suggestions/{id}/ignore | 忽略建议 | 已认证 |
| POST/GET | /projects/{id}/changes | 变更记录 | 已认证 |
| POST | /digest/send-now | 手动发送摘要 | 部门经理 |
| POST | /digest/test?email=xxx | 测试邮件发送 | 部门经理 |
| GET | /digest/logs | 邮件发送记录 | 部门经理 |
| GET/POST/PUT/DELETE | /users | 用户管理 | 部门经理 |
| GET | /operation-logs | 操作日志 | 部门经理 |

## 核心业务逻辑

### 项目健康评分

```
总分 = 时间得分 × 35% + 风险得分 × 40% + 交付得分 × 25%
```

- 时间得分：100 - 延期天数 × 2（每天扣2分）
- 风险得分：100 - 高危×15 - 中危×8 - 低危×3 - 停滞×10
- 交付得分：按时完成阶段数 ÷ 应完成阶段数 × 100
- 颜色映射：≥80 绿 / 60-79 黄 / <60 红
- 所有权重和阈值均可通过系统配置页面调整

### 风险编号生成规则

格式：`{项目编号}-{日期}-{序号}`，如 `PRJ-A01-20260731-02`

### AI 风险探测

1. 项目经理更新阶段备注 → 异步调用 AI 分析文本
2. AI 识别潜在风险 → 写入 `ai_risk_suggestion` 表（状态：待确认）
3. 用户可选择「采纳」（自动创建正式风险）或「忽略」

### 操作日志

AOP 切面自动拦截所有 Controller 方法，仅记录 POST/PUT/DELETE（GET 查询不记录），按 URL 路径自动识别操作模块。
