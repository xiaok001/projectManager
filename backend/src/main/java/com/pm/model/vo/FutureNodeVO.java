package com.pm.model.vo;

import lombok.Data;
import java.time.LocalDate;

/**
 * 未来关键节点VO
 */
@Data
public class FutureNodeVO {
    private Long stageId;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Integer projectLevel;
    private String stageName;
    private LocalDate planEnd;
    private String stageStatus;
    /** 是否已逾期 */
    private Boolean isOverdue;
}
