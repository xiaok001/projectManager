package com.pm.model.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.List;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String permName;
    private String permKey;
    private String type;  // menu/button
    private String path;
    private String icon;
    private Integer sortOrder;
    private Integer status;

    @TableField(exist = false)
    private List<SysPermission> children;

    @TableField(exist = false)
    private boolean checked;
}
