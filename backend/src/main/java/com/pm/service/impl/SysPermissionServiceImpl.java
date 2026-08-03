package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.mapper.SysPermissionMapper;
import com.pm.model.entity.SysPermission;
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
