package com.pm.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * 阶段更新DTO
 */
@Data
public class StageUpdateDTO {
    private LocalDate planStart;
    private LocalDate planEnd;
    private LocalDate actualStart;
    private LocalDate actualEnd;
    private String status;
    private String remark;
}
