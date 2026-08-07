package com.pm.model.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    /** 数据权限: all=全部项目 custom=指定项目 */
    private String dataScope;
    /** 是否内置角色: 1是 0否 */
    private Integer isSystem;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
