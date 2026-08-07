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
    /** 健康总分(1-100)，数据不足时为null */
    private Integer healthScore;
    /** 健康颜色: 绿/黄/红/灰(数据不足) */
    private String healthColor;
    /** 数据是否充足(false=显示"数据不足") */
    private Boolean dataSufficient;
    /** 时间维度得分 */
    private Integer timeScore;
    /** 风险维度得分 */
    private Integer riskScore;
    /** 交付维度得分 */
    private Integer deliveryScore;
    /** 客户满意度(1-10) */
    private Integer satisfactionScore;
    /** 高危风险数 */
    private Integer highRiskCount;
    /** 停滞风险数 */
    private Integer staleRiskCount;
    /** 评分明细 - 时间维度说明 */
    private String timeDetail;
    /** 评分明细 - 风险维度说明 */
    private String riskDetail;
    /** 评分明细 - 交付维度说明 */
    private String deliveryDetail;
}
