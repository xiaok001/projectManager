package com.pm.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI风险建议VO(含项目信息)
 */
@Data
public class AiSuggestionVO {
    private Long id;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long stageId;
    private String sourceText;
    private String suggestedRiskDesc;
    private String status;
    private LocalDateTime createdAt;
}
