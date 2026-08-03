package com.pm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

/**
 * 待办事项创建/编辑DTO
 */
@Data
public class TodoDTO {
    private Long projectId;
    private Long stageId;
    @NotBlank(message = "待办事项不能为空")
    private String title;
    private String source;
    private String priority = "中";
    private String urgency = "普通";
    private Long ownerId;
    private LocalDate planStart;
    private LocalDate planEnd;
    private LocalDate actualEnd;
    private String status = "待处理";
    private Integer progress = 0;
    private String blockIssue;
    private String riskDesc;
    private String outputDesc;
    private String remark;
}
