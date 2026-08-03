package com.pm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;  // 编辑时可不传

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String email;
    private String phone;

    private String role;  // DEPT_MANAGER / PM (兼容旧数据)

    /** 角色ID(关联sys_role) */
    private Long roleId;

    private Integer status = 1;
}
