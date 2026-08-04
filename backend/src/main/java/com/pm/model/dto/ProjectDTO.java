package com.pm.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目创建/编辑DTO
 */
@Data
public class ProjectDTO {
    @NotBlank(message = "项目编号不能为空")
    private String projectCode;

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String type = "软件开发";

    @NotNull(message = "项目等级不能为空")
    private Integer level;

    private BigDecimal amount;

    @NotNull(message = "项目经理不能为空")
    private Long pmId;

    @NotNull(message = "立项日期不能为空")
    private LocalDate startDate;

    private LocalDate expectedEndDate;

    private String wbsOnlineUrl;

    private String status;
}
