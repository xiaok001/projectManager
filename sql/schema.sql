-- ============================================================
-- 多项目管理与风险提醒系统 - 数据库初始化脚本
-- 版本: v2.0 (完整版)
-- 数据库: MySQL 8.0+
-- 说明: 包含全部14张表 + 初始数据
-- ============================================================

CREATE DATABASE IF NOT EXISTS project_manager
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE project_manager;

-- -----------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50)     NOT NULL COMMENT '用户名',
    password        VARCHAR(100)    NOT NULL COMMENT '密码(BCrypt加密)',
    real_name       VARCHAR(50)     NOT NULL COMMENT '真实姓名',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    role            VARCHAR(20)     NOT NULL COMMENT '角色: DEPT_MANAGER / PM',
    role_id         BIGINT          DEFAULT NULL COMMENT '角色ID(关联sys_role)',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------
-- 2. 项目主表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS project (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_code        VARCHAR(50)     NOT NULL COMMENT '项目编号(人工录入,唯一)',
    name                VARCHAR(200)    NOT NULL COMMENT '项目名称',
    type                VARCHAR(50)     NOT NULL DEFAULT '软件开发' COMMENT '项目类型',
    level               TINYINT         NOT NULL DEFAULT 2 COMMENT '项目等级: 0-P0 1-P1 2-P2',
    amount              DECIMAL(15,2)   DEFAULT NULL COMMENT '项目金额',
    pm_id               BIGINT          NOT NULL COMMENT '项目经理(关联sys_user.id)',
    start_date          DATE            NOT NULL COMMENT '立项日期',
    expected_end_date   DATE            DEFAULT NULL COMMENT '预期结束日期',
    wbs_online_url      VARCHAR(500)    DEFAULT NULL COMMENT 'WBS在线文档链接',
    wbs_offline_file    VARCHAR(500)    DEFAULT NULL COMMENT 'WBS离线附件路径',
    wbs_offline_name    VARCHAR(200)    DEFAULT NULL COMMENT 'WBS附件原始文件名',
    current_stage       VARCHAR(50)     DEFAULT NULL COMMENT '当前阶段',
    status              VARCHAR(20)     NOT NULL DEFAULT '进行中' COMMENT '项目状态: 进行中/已完成/已暂停',
    satisfaction_score  TINYINT         DEFAULT NULL COMMENT '客户满意度(1-10)',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_code (project_code),
    KEY idx_pm_id (pm_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目主表';

-- -----------------------------------------------------------
-- 3. 项目阶段表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_stage (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id      BIGINT          NOT NULL COMMENT '关联项目',
    stage_name      VARCHAR(50)     NOT NULL COMMENT '阶段名',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '阶段排序',
    plan_start      DATE            DEFAULT NULL COMMENT '计划开始日期',
    plan_end        DATE            DEFAULT NULL COMMENT '计划结束日期',
    actual_start    DATE            DEFAULT NULL COMMENT '实际开始日期',
    actual_end      DATE            DEFAULT NULL COMMENT '实际结束日期',
    status          VARCHAR(20)     NOT NULL DEFAULT '未开始' COMMENT '状态: 未开始/进行中/已完成/已延期',
    remark          TEXT            DEFAULT NULL COMMENT '备注(AI风险探测数据源)',
    plan_man_days   DECIMAL(10,2)   DEFAULT NULL COMMENT '预估人天',
    actual_man_days DECIMAL(10,2)   DEFAULT NULL COMMENT '实际人天',
    plan_cost       DECIMAL(15,2)   DEFAULT NULL COMMENT '预估成本(元)',
    actual_cost     DECIMAL(15,2)   DEFAULT NULL COMMENT '实际成本(元)',
    progress        INT             DEFAULT 0 COMMENT '完成进度(0-100)',
    updated_by      BIGINT          DEFAULT NULL COMMENT '最近操作人',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_project_id (project_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目阶段表';

-- -----------------------------------------------------------
-- 4. 风险/问题表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_risk (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    risk_code           VARCHAR(100)    NOT NULL COMMENT '风险编号(系统自动生成)',
    project_id          BIGINT          NOT NULL COMMENT '关联项目',
    description         TEXT            NOT NULL COMMENT '风险/问题描述',
    type                VARCHAR(20)     NOT NULL COMMENT '类型: 风险(未发生)/问题(已发生)',
    severity            VARCHAR(10)     NOT NULL DEFAULT '中' COMMENT '严重程度: 高/中/低',
    owner_id            BIGINT          DEFAULT NULL COMMENT '责任人',
    status              VARCHAR(20)     NOT NULL DEFAULT '待处理' COMMENT '状态: 待处理/处理中/已解决/已关闭',
    action_plan         TEXT            DEFAULT NULL COMMENT '处理措施',
    is_stale            TINYINT         NOT NULL DEFAULT 0 COMMENT '是否停滞',
    stale_override      TINYINT         DEFAULT NULL COMMENT '手动覆盖停滞: 0非停滞 1停滞 NULL自动',
    last_updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '最近操作人',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_code (risk_code),
    KEY idx_project_id (project_id),
    KEY idx_status (status),
    KEY idx_severity (severity),
    KEY idx_stale (is_stale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险/问题表';

-- -----------------------------------------------------------
-- 5. 待办事项表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_todo (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    todo_code       VARCHAR(100)    NOT NULL COMMENT '待办编号(项目编号-TD-日期-序号)',
    project_id      BIGINT          NOT NULL COMMENT '所属项目',
    stage_id        BIGINT          DEFAULT NULL COMMENT '所属阶段',
    title           VARCHAR(500)    NOT NULL COMMENT '待办事项',
    source          VARCHAR(50)     DEFAULT NULL COMMENT '来源: 会议纪要/客户需求/内部评估/风险跟踪/其他',
    priority        VARCHAR(10)     DEFAULT '中' COMMENT '优先级: 高/中/低',
    urgency         VARCHAR(10)     DEFAULT '普通' COMMENT '紧急程度: 紧急/特急/普通',
    owner_id        BIGINT          DEFAULT NULL COMMENT '负责人ID',
    owner_name      VARCHAR(50)     DEFAULT NULL COMMENT '负责人姓名(手动输入时使用)',
    plan_start      DATE            DEFAULT NULL COMMENT '计划开始时间',
    plan_end        DATE            DEFAULT NULL COMMENT '计划完成时间',
    actual_end      DATE            DEFAULT NULL COMMENT '实际完成时间',
    status          VARCHAR(20)     DEFAULT '待处理' COMMENT '状态: 待处理/进行中/已完成/已取消/已逾期',
    progress        INT             DEFAULT 0 COMMENT '完成百分比(0-100)',
    block_issue     TEXT            DEFAULT NULL COMMENT '阻塞问题',
    risk_desc       TEXT            DEFAULT NULL COMMENT '风险说明',
    output_desc     VARCHAR(500)    DEFAULT NULL COMMENT '输出物',
    remark          TEXT            DEFAULT NULL COMMENT '备注',
    created_by      BIGINT          DEFAULT NULL COMMENT '创建人',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_todo_code (todo_code),
    KEY idx_project_id (project_id),
    KEY idx_status (status),
    KEY idx_plan_end (plan_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办事项表';

-- -----------------------------------------------------------
-- 6. 全局配置表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key      VARCHAR(100)    NOT NULL COMMENT '配置键',
    config_value    VARCHAR(500)    NOT NULL COMMENT '配置值',
    description     VARCHAR(200)    DEFAULT NULL COMMENT '配置说明',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全局配置表';

-- -----------------------------------------------------------
-- 7. AI风险建议表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_risk_suggestion (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id          BIGINT          NOT NULL COMMENT '关联项目',
    stage_id            BIGINT          DEFAULT NULL COMMENT '关联阶段',
    source_text         TEXT            NOT NULL COMMENT '触发探测的原始备注文本',
    suggested_risk_desc TEXT            NOT NULL COMMENT 'AI生成的建议风险描述',
    status              VARCHAR(20)     NOT NULL DEFAULT '待确认' COMMENT '状态: 待确认/已采纳/已忽略',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_project_id (project_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI风险建议表';

-- -----------------------------------------------------------
-- 8. 邮件发送记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_digest_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    send_date       DATE            NOT NULL COMMENT '发送日期',
    content         TEXT            NOT NULL COMMENT '邮件正文内容',
    recipients      VARCHAR(500)    NOT NULL COMMENT '接收人邮箱列表',
    send_status     VARCHAR(10)     NOT NULL COMMENT '发送状态: 成功/失败',
    fail_reason     VARCHAR(500)    DEFAULT NULL COMMENT '失败原因',
    sent_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_send_date (send_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件发送记录表';

-- -----------------------------------------------------------
-- 9. 项目变更记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_change_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id      BIGINT          NOT NULL COMMENT '关联项目',
    change_type     VARCHAR(20)     NOT NULL COMMENT '变更类型: 人员变更/内容变更/范围变更/风险变更/其他',
    change_field    VARCHAR(50)     DEFAULT NULL COMMENT '变更字段名',
    change_desc     TEXT            NOT NULL COMMENT '变更内容描述',
    before_value    TEXT            DEFAULT NULL COMMENT '变更前',
    after_value     TEXT            DEFAULT NULL COMMENT '变更后',
    changed_by      BIGINT          DEFAULT NULL COMMENT '记录人',
    changed_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    PRIMARY KEY (id),
    KEY idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目变更记录表';

-- -----------------------------------------------------------
-- 10. 操作日志表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    module          VARCHAR(50)     NOT NULL COMMENT '操作模块',
    operation       VARCHAR(20)     NOT NULL COMMENT '操作类型: 新增/修改/删除/登录',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '操作描述',
    operator_id     BIGINT          DEFAULT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(50)     DEFAULT NULL COMMENT '操作人姓名',
    request_method  VARCHAR(10)     DEFAULT NULL COMMENT '请求方法',
    request_url     VARCHAR(300)    DEFAULT NULL COMMENT '请求URL',
    request_params  TEXT            DEFAULT NULL COMMENT '请求参数',
    response_code   INT             DEFAULT NULL COMMENT '响应状态码',
    ip              VARCHAR(50)     DEFAULT NULL COMMENT '操作IP',
    execution_time  BIGINT          DEFAULT NULL COMMENT '执行耗时(ms)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_operator (operator_id),
    KEY idx_module (module),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- -----------------------------------------------------------
-- 11. 角色表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    role_name       VARCHAR(50)     NOT NULL COMMENT '角色名称',
    role_key        VARCHAR(50)     NOT NULL COMMENT '角色标识',
    sort_order      INT             DEFAULT 0,
    status          TINYINT         DEFAULT 1,
    remark          VARCHAR(200)    DEFAULT NULL,
    data_scope      VARCHAR(20)     DEFAULT 'all' COMMENT '数据权限: all=全部项目 custom=指定项目',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- -----------------------------------------------------------
-- 12. 权限表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    parent_id       BIGINT          DEFAULT 0 COMMENT '父权限ID(0=顶级)',
    perm_name       VARCHAR(50)     NOT NULL COMMENT '权限名称',
    perm_key        VARCHAR(100)    NOT NULL COMMENT '权限标识',
    type            VARCHAR(10)     NOT NULL COMMENT 'menu/button',
    path            VARCHAR(200)    DEFAULT NULL COMMENT '页面路由',
    icon            VARCHAR(50)     DEFAULT NULL COMMENT '图标',
    sort_order      INT             DEFAULT 0,
    status          TINYINT         DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_key (perm_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- -----------------------------------------------------------
-- 13. 角色-权限关联表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id         BIGINT          NOT NULL,
    permission_id   BIGINT          NOT NULL,
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- -----------------------------------------------------------
-- 14. 角色-项目数据权限关联表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_project (
    role_id         BIGINT          NOT NULL,
    project_id      BIGINT          NOT NULL,
    PRIMARY KEY (role_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色数据权限关联表';

-- ============================================================
-- 初始数据
-- ============================================================

-- -----------------------------------------------------------
-- 系统配置
-- -----------------------------------------------------------
INSERT INTO system_config (config_key, config_value, description) VALUES
    ('stale_threshold_days',       '7',        '风险停滞判定天数'),
    ('daily_digest_send_time',     '09:30',    '每日摘要发送时间'),
    ('digest_recipient_emails',    '',         '收件人邮箱列表(逗号分隔)'),
    ('health_weight_time',         '35',       '健康评分-时间维度权重(%)'),
    ('health_weight_risk',         '40',       '健康评分-风险维度权重(%)'),
    ('health_weight_delivery',     '25',       '健康评分-交付维度权重(%)'),
    ('health_score_green_min',     '80',       '健康度绿色最低分'),
    ('health_score_yellow_min',    '60',       '健康度黄色最低分'),
    ('time_delay_penalty_per_day', '2',        '时间维度每天延期扣分'),
    ('risk_penalty_high',          '15',       '高危风险扣分'),
    ('risk_penalty_medium',        '8',        '中危风险扣分'),
    ('risk_penalty_low',           '3',        '低危风险扣分'),
    ('risk_penalty_stale',         '10',       '停滞风险扣分'),
    ('ai_provider',                'ollama',   'AI服务提供者: deepseek/ollama'),
    ('ai_deepseek_api_key',        '',         'DeepSeek API Key'),
    ('ai_deepseek_base_url',       'https://api.deepseek.com', 'DeepSeek API地址'),
    ('ai_ollama_base_url',         'http://localhost:11434', 'Ollama API地址'),
    ('ai_ollama_model',            'qwen2.5:7b', 'Ollama模型名称');

-- -----------------------------------------------------------
-- 默认角色
-- -----------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_key, sort_order, remark, data_scope) VALUES
    (1, '超级管理员', 'admin',   1, '拥有所有权限',          'all'),
    (2, '项目经理',   'pm',      2, '管理自己负责的项目',    'custom'),
    (3, '查看者',     'viewer',  3, '只读权限',              'all');

-- -----------------------------------------------------------
-- 权限数据
-- -----------------------------------------------------------
-- 顶级菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, type, path, icon, sort_order) VALUES
    (1,  0, '首页概览',  'dashboard',     'menu', '/dashboard',       'Odometer',  1),
    (2,  0, '项目管理',  'project',       'menu', '/projects',        'Folder',    2),
    (3,  0, 'AI建议',   'ai-suggestion', 'menu', '/ai-suggestions',  'Cpu',       3),
    (4,  0, '项目待办',  'todo',          'menu', '/todos',           'Finished',  4),
    (5,  0, '报告',     'report',        'menu', '/reports',         'Document',  5),
    (6,  0, '系统管理',  'system',        'menu', NULL,               'Setting',   6);

-- 系统管理子菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, type, path, icon, sort_order) VALUES
    (7,  6, '系统配置',  'system:config', 'menu', '/config',  'Setting',   1),
    (8,  6, '用户管理',  'system:user',   'menu', '/users',   'UserFilled', 2),
    (9,  6, '角色管理',  'system:role',   'menu', '/roles',   'User',      3),
    (10, 6, '操作日志',  'system:log',    'menu', '/logs',    'Notebook',  4);

-- 按钮权限
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, type, sort_order) VALUES
    (11, 2, '查看项目', 'project:view',   'button', 1),
    (12, 2, '新增项目', 'project:create', 'button', 2),
    (13, 2, '编辑项目', 'project:edit',   'button', 3),
    (14, 2, '删除项目', 'project:delete', 'button', 4);

-- -----------------------------------------------------------
-- 管理员角色分配全部权限
-- -----------------------------------------------------------
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- -----------------------------------------------------------
-- 默认管理员账号 (密码: admin123)
-- -----------------------------------------------------------
INSERT INTO sys_user (username, password, real_name, email, role, role_id, status) VALUES
    ('admin', '$2b$10$eKmIwvnso7zSG3uCuUPirOmltxW4P.Co8w1bOd4vyeHmljeYYdTli', '系统管理员', 'admin@company.com', 'DEPT_MANAGER', 1, 1);
