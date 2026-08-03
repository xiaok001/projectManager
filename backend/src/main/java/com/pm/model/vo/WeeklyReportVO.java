package com.pm.model.vo;

import lombok.Data;
import java.util.List;

/**
 * 周报VO
 */
@Data
public class WeeklyReportVO {
    /** 报告周期 */
    private String period;
    /** 项目名称(单项目时) */
    private String projectName;
    /** 本周完成的阶段 */
    private List<StageSummary> completedStages;
    /** 本周新增风险 */
    private List<RiskSummary> newRisks;
    /** 本周关闭的风险 */
    private List<RiskSummary> closedRisks;
    /** 当前未关闭风险统计 */
    private RiskStats riskStats;
    /** 下周计划节点 */
    private List<StageSummary> upcomingStages;
    /** 待办统计 */
    private TodoStats todoStats;
    /** AI生成的叙述性总结 */
    private String aiSummary;

    @Data
    public static class StageSummary {
        private String projectCode;
        private String projectName;
        private String stageName;
        private String planEnd;
        private String actualEnd;
        private String status;
    }

    @Data
    public static class RiskSummary {
        private String riskCode;
        private String projectCode;
        private String projectName;
        private String description;
        private String severity;
        private String status;
    }

    @Data
    public static class RiskStats {
        private int total;
        private int high;
        private int medium;
        private int low;
        private int stale;
    }

    @Data
    public static class TodoStats {
        private int total;
        private int completed;
        private int inProgress;
        private int overdue;
    }
}
