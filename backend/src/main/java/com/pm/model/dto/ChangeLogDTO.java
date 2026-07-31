package com.pm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 变更记录DTO
 */
@Data
public class ChangeLogDTO {
    @NotBlank(message = "变更类型不能为空")
    private String changeType;

    @NotBlank(message = "变更描述不能为空")
    private String changeDesc;

    /** 变更字段名(如: 风险等级、负责人、处理措施等) */
    private String changeField;

    private String beforeValue;
    private String afterValue;
}
