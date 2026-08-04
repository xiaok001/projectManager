package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目主表实体
 */
@Data
@TableName("project")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 项目编号(人工录入,唯一) */
    private String projectCode;
    private String name;
    /** 项目类型(默认软件开发) */
    private String type;
    /** 项目等级: 0-P0 1-P1 2-P2 */
    private Integer level;
    /** 项目金额 */
    private BigDecimal amount;
    /** 项目经理ID */
    private Long pmId;
    /** 立项日期 */
    private LocalDate startDate;
    /** 预期结束日期 */
    private LocalDate expectedEndDate;
    /** WBS在线文档链接 */
    private String wbsOnlineUrl;
    /** WBS离线附件路径 */
    private String wbsOfflineFile;
    /** 当前阶段 */
    private String currentStage;
    /** 项目状态: 进行中/已完成/已暂停 */
    private String status;
    /** 客户满意度(1-10) */
    private Integer satisfactionScore;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 非数据库字段: 项目经理姓名 */
    @TableField(exist = false)
    private String pmName;
    /** 非数据库字段: 运维阶段开始日期(作为项目结束日期展示) */
    @TableField(exist = false)
    private LocalDate opsStartDate;
}
