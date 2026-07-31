package com.pm.common.constants;

/**
 * Application-wide constants for the Multi-Project Management System.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // ======================== Project Stages ========================

    /** Default project lifecycle stages. */
    public static final String[] DEFAULT_STAGES = {
            "启动", "调研", "开发", "测试验收", "上线", "试运行", "运维"
    };

    // ======================== Roles ========================

    public static final String ROLE_DEPT_MANAGER = "DEPT_MANAGER";
    public static final String ROLE_PM = "PM";

    // ======================== Project Status ========================

    public static final String STATUS_ACTIVE = "进行中";
    public static final String STATUS_COMPLETED = "已完成";
    public static final String STATUS_PAUSED = "已暂停";

    // ======================== Stage Status ========================

    public static final String STAGE_NOT_STARTED = "未开始";
    public static final String STAGE_IN_PROGRESS = "进行中";
    public static final String STAGE_COMPLETED = "已完成";
    public static final String STAGE_DELAYED = "已延期";

    // ======================== Risk Status ========================

    public static final String RISK_PENDING = "待处理";
    public static final String RISK_IN_PROGRESS = "处理中";
    public static final String RISK_RESOLVED = "已解决";
    public static final String RISK_CLOSED = "已关闭";
}
