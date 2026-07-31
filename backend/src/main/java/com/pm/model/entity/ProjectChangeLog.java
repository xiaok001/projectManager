package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目变更记录表实体
 */
@Data
@TableName("project_change_log")
public class ProjectChangeLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    /** 变更类型: 人员变更/内容变更/范围变更/风险变更/其他 */
    private String changeType;
    /** 变更字段名 */
    private String changeField;
    /** 变更内容描述 */
    private String changeDesc;
    /** 变更前 */
    private String beforeValue;
    /** 变更后 */
    private String afterValue;
    private Long changedBy;
    private LocalDateTime changedAt;

    /** 非数据库字段: 记录人姓名 */
    @TableField(exist = false)
    private String changedByName;
}
