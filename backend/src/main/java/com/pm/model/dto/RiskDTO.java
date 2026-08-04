package com.pm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 风险创建/编辑DTO
 */
@Data
public class RiskDTO {
    private Long id;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotBlank(message = "类型不能为空")
    private String type;

    private String severity = "中";
    private String status;
    private Long ownerId;
    private String owner;
    private String actionPlan;
}
