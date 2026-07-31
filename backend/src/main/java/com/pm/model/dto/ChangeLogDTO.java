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

    private String beforeValue;
    private String afterValue;
}
