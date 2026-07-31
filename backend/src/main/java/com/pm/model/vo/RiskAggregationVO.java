package com.pm.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 风险聚合VO(Dashboard用)
 */
@Data
public class RiskAggregationVO {
    private Long riskId;
    private String riskCode;
    private String description;
    private String riskType;
    private String severity;
    private String status;
    private Boolean isStale;
    private Integer staleDays;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Integer projectLevel;
    private String ownerName;
    private LocalDateTime lastUpdatedAt;
}
