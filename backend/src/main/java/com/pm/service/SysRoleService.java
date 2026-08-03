package com.pm.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.SysRole;
import java.util.List;

public interface SysRoleService extends IService<SysRole> {
    List<SysRole> listRoles();
    SysRole createRole(SysRole role);
    SysRole updateRole(Long id, SysRole role);
    void deleteRole(Long id);
    List<Long> getRolePermissionIds(Long roleId);
    void assignPermissions(Long roleId, List<Long> permissionIds);
}
