package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report_weekly_log")
public class ReportWeeklyLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String projectName;
    private String period;
    private String reportContent;
    private String aiSummary;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String createdByName;
}
