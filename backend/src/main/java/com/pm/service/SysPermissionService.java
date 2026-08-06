package com.pm.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.SysPermission;
import java.util.List;

public interface SysPermissionService extends IService<SysPermission> {
    List<SysPermission> getPermissionTree();
    List<SysPermission> getPermissionTreeWithChecked(Long roleId);
    List<SysPermission> getUserPermissionTree(Long userId, String role);
}
