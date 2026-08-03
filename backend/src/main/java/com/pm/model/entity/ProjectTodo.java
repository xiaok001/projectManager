package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目待办事项表实体
 */
@Data
@TableName("project_todo")
public class ProjectTodo {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 待办编号(系统自动生成) */
    private String todoCode;
    private Long projectId;
    private Long stageId;
    /** 待办事项标题 */
    private String title;
    /** 来源 */
    private String source;
    /** 优先级: 高/中/低 */
    private String priority;
    /** 紧急程度: 紧急/普通 */
    private String urgency;
    /** 责任人ID */
    private Long ownerId;
    private LocalDate planStart;
    private LocalDate planEnd;
    private LocalDate actualEnd;
    /** 状态: 待处理/进行中/已完成/已取消/已逾期 */
    private String status;
    /** 进度(0-100) */
    private Integer progress;
    /** 阻塞问题 */
    private String blockIssue;
    /** 风险描述 */
    private String riskDesc;
    /** 产出描述 */
    private String outputDesc;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // non-db fields
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private String stageName;
    @TableField(exist = false)
    private String projectCode;
}
