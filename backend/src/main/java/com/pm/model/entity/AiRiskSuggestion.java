package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI风险建议表实体
 */
@Data
@TableName("ai_risk_suggestion")
public class AiRiskSuggestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long stageId;
    /** 触发探测的原始备注文本 */
    private String sourceText;
    /** AI生成的建议风险描述 */
    private String suggestedRiskDesc;
    /** 状态: 待确认/已采纳/已忽略 */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
