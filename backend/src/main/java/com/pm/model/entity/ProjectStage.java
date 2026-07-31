package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目阶段表实体
 */
@Data
@TableName("project_stage")
public class ProjectStage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    /** 阶段名: 启动/调研/开发/测试验收/上线/试运行/运维 */
    private String stageName;
    /** 阶段排序 */
    private Integer sortOrder;
    private LocalDate planStart;
    private LocalDate planEnd;
    private LocalDate actualStart;
    private LocalDate actualEnd;
    /** 状态: 未开始/进行中/已完成/已延期 */
    private String status;
    /** 备注(AI风险探测数据源) */
    private String remark;
    /** 预估人天 */
    private BigDecimal planManDays;
    /** 实际人天 */
    private BigDecimal actualManDays;
    /** 预估成本(元) */
    private BigDecimal planCost;
    /** 实际成本(元) */
    private BigDecimal actualCost;
    /** 完成进度(0-100%) */
    private Integer progress;
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
