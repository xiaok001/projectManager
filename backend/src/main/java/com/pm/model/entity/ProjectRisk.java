package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 风险/问题表实体
 */
@Data
@TableName("project_risk")
public class ProjectRisk {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 风险编号(系统自动生成) */
    private String riskCode;
    private Long projectId;
    /** 风险/问题描述 */
    private String description;
    /** 类型: 风险/问题 */
    private String type;
    /** 严重程度: 高/中/低 */
    private String severity;
    /** 责任人ID */
    private Long ownerId;
    /** 状态: 待处理/处理中/已解决/已关闭 */
    private String status;
    /** 处理措施 */
    private String actionPlan;
    /** 是否停滞(系统自动判断) */
    private Boolean isStale;
    /** 手动覆盖停滞状态 */
    private Boolean staleOverride;
    /** 最近更新时间(停滞判定依据) */
    private LocalDateTime lastUpdatedAt;
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 非数据库字段: 责任人姓名 */
    @TableField(exist = false)
    private String ownerName;
    /** 非数据库字段: 项目编号 */
    @TableField(exist = false)
    private String projectCode;
    /** 非数据库字段: 项目名称 */
    @TableField(exist = false)
    private String projectName;
    /** 非数据库字段: 项目等级 */
    @TableField(exist = false)
    private Integer projectLevel;
    /** 非数据库字段: 停滞天数 */
    @TableField(exist = false)
    private Integer staleDays;
}
