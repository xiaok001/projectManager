package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_role_project")
public class SysRoleProject {
    private Long roleId;
    private Long projectId;
}
