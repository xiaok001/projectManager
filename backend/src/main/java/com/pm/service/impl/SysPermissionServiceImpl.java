package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.constants.Constants;
import com.pm.mapper.SysPermissionMapper;
import com.pm.mapper.SysUserMapper;
import com.pm.model.entity.SysPermission;
import com.pm.model.entity.SysRole;
import com.pm.model.entity.SysUser;
import com.pm.service.SysPermissionService;
import com.pm.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    private final SysRoleService roleService;
    private final SysUserMapper userMapper;

    @Override
    public List<SysPermission> getPermissionTree() {
        List<SysPermission> all = lambdaQuery().orderByAsc(SysPermission::getSortOrder).list();
        return buildTree(all, 0L);
    }

    @Override
    public List<SysPermission> getPermissionTreeWithChecked(Long roleId) {
        List<SysPermission> all = lambdaQuery().orderByAsc(SysPermission::getSortOrder).list();
        List<Long> checkedIds = roleService.getRolePermissionIds(roleId);
        all.forEach(p -> p.setChecked(checkedIds.contains(p.getId())));
        return buildTree(all, 0L);
    }

    @Override
    public List<SysPermission> getUserPermissionTree(Long userId, String role) {
        // DEPT_MANAGER 获取全部权限
        if (Constants.ROLE_DEPT_MANAGER.equals(role)) {
            return getPermissionTree();
        }

        // 其他角色按关联的权限返回
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return new ArrayList<>();
        }

        SysRole userRole = roleService.getById(user.getRoleId());
        if (userRole == null) {
            return new ArrayList<>();
        }

        // admin 角色标识也返回全部
        if ("admin".equals(userRole.getRoleKey())) {
            return getPermissionTree();
        }

        List<Long> permIds = roleService.getRolePermissionIds(user.getRoleId());
        if (permIds.isEmpty()) return new ArrayList<>();

        List<SysPermission> all = lambdaQuery()
                .in(SysPermission::getId, permIds)
                .orderByAsc(SysPermission::getSortOrder)
                .list();
        return buildTree(all, 0L);
    }

    private List<SysPermission> buildTree(List<SysPermission> all, Long parentId) {
        List<SysPermission> tree = new ArrayList<>();
        for (SysPermission p : all) {
            if (parentId.equals(p.getParentId())) {
                p.setChildren(buildTree(all, p.getId()));
                tree.add(p);
            }
        }
        return tree;
    }
}
