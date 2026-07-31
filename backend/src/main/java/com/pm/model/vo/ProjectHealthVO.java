package com.pm.model.vo;

import lombok.Data;

/**
 * 项目健康度VO
 */
@Data
public class ProjectHealthVO {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Integer projectLevel;
    private String currentStage;
    private String projectStatus;
    /** 健康总分(1-100) */
    private Integer healthScore;
    /** 健康颜色: 绿/黄/红 */
    private String healthColor;
    /** 时间维度得分 */
    private Integer timeScore;
    /** 风险维度得分 */
    private Integer riskScore;
    /** 交付维度得分 */
    private Integer deliveryScore;
    /** 客户满意度(1-10,独立展示) */
    private Integer satisfactionScore;
    /** 高危风险数 */
    private Integer highRiskCount;
    /** 停滞风险数 */
    private Integer staleRiskCount;
}
