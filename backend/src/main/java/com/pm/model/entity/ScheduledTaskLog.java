package com.pm.model.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("scheduled_task_log")
public class ScheduledTaskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String taskName;
    private String triggerType;
    private String status;
    private String resultMsg;
    private Long executionTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime executedAt;
}
