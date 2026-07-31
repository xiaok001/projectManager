package com.pm.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 阶段更新DTO
 */
@Data
public class StageUpdateDTO {
    private String stageName;
    private LocalDate planStart;
    private LocalDate planEnd;
    private LocalDate actualStart;
    private LocalDate actualEnd;
    private String status;
    private String remark;
    private BigDecimal planManDays;
    private BigDecimal actualManDays;
    private BigDecimal planCost;
    private BigDecimal actualCost;
    private Integer progress;
}
