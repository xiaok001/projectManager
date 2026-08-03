package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.SysRoleMapper;
import com.pm.mapper.SysRolePermissionMapper;
import com.pm.mapper.SysUserMapper;
import com.pm.model.entity.SysRole;
import com.pm.model.entity.SysRolePermission;
import com.pm.model.entity.SysUser;
import com.pm.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRolePermissionMapper rolePermMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<SysRole> listRoles() {
        return lambdaQuery().orderByAsc(SysRole::getSortOrder).list();
    }

    @Override
    @Transactional
    public SysRole createRole(SysRole role) {
        long count = lambdaQuery().eq(SysRole::getRoleKey, role.getRoleKey()).count();
        if (count > 0) throw new BusinessException("角色标识已存在: " + role.getRoleKey());
        save(role);
        return role;
    }

    @Override
    @Transactional
    public SysRole updateRole(Long id, SysRole role) {
        SysRole existing = getById(id);
        if (existing == null) throw new BusinessException("角色不存在");
        role.setId(id);
        updateById(role);
        return role;
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        SysRole role = getById(id);
        if (role == null) throw new BusinessException("角色不存在");
        // 检查是否有用户使用此角色
        long userCount = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, id));
        if (userCount > 0) throw new BusinessException("该角色下还有" + userCount + "个用户，无法删除");
        // 删除角色权限关联
        rolePermMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
        removeById(id);
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 先删除旧的
        rolePermMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        // 再插入新的
        for (Long permId : permissionIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermMapper.insert(rp);
        }
    }
}
