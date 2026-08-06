# 多项目管理系统 - 后端

> Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 + MySQL

## 快速开始

```bash
mvn clean install              # 安装依赖
mvn spring-boot:run            # 开发运行 http://localhost:8080
mvn clean package -DskipTests  # 生产构建 → target/project-manager-1.0.0-SNAPSHOT.jar
```

## 核心配置 (`application.yml`)

| 配置项 | 说明 |
|---|---|
| spring.datasource | MySQL 连接 |
| spring.mail | 163 SMTP 邮件 |
| ai.provider | `deepseek` 或 `ollama` 切换 |
| ai.ollama.base-url | Ollama 服务地址 |
| jwt.secret | JWT 签名密钥 |
| file.upload-root | WBS附件存储根目录 |

## 项目结构

```
src/main/java/com/pm/
├── config/
│   ├── SecurityConfig.java          # Spring Security + CORS
│   ├── MybatisPlusConfig.java       # 分页 + 自动填充
│   ├── AppConfig.java               # RestTemplate + LocalDateTime格式
│   └── OperationLogAspect.java      # AOP操作日志(POST/PUT/DELETE)
├── controller/                      # 14个REST控制器
├── service/                         # 15个接口 + 实现
│   └── impl/
│       ├── AiProvider.java          # AI抽象接口
│       ├── DeepSeekProvider.java    # DeepSeek实现
│       ├── OllamaProvider.java      # Ollama实现
│       └── *ServiceImpl.java        # 各业务实现
├── model/
│   ├── entity/                      # 17个实体
│   ├── dto/                         # 8个DTO
│   └── vo/                          # 7个VO
├── mapper/                          # 14个Mapper
├── common/                          # 响应/异常/常量
├── security/                        # JWT工具+过滤器
└── schedule/
    └── DynamicScheduler.java        # 动态调度器(数据库驱动Cron)
```

## 核心机制

### 动态定时任务
- `DynamicScheduler` 启动时从 `scheduled_task` 表读取 Cron 表达式
- 修改 Cron 或切换启用/禁用后立即重新调度，无需重启
- 每次执行自动记录日志到 `scheduled_task_log`

### AI 服务
- `AiProvider` 接口统一抽象，`DeepSeekProvider` / `OllamaProvider` 各自实现
- 配置 `ai.provider` 切换，`POST /config/test-ai` 测试连接
- 调用失败降级为纯结构化文本，不影响主流程

### 操作日志
- AOP 切面拦截 POST/PUT/DELETE，GET 不记录
- 按 URL 路径自动识别操作模块（项目/阶段/风险/用户/配置等）

### 数据权限
- 角色可配置「全部项目」或「指定项目」
- 项目查询自动按角色数据权限过滤
