package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.SysRoleMapper;
import com.pm.mapper.SysRolePermissionMapper;
import com.pm.mapper.SysRoleProjectMapper;
import com.pm.mapper.SysUserMapper;
import com.pm.model.entity.SysRole;
import com.pm.model.entity.SysRolePermission;
import com.pm.model.entity.SysRoleProject;
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
    private final SysRoleProjectMapper roleProjectMapper;
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
        if (Integer.valueOf(1).equals(role.getIsSystem())) {
            throw new BusinessException("系统内置角色不可删除");
        }
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

    @Override
    public List<Long> getRoleProjectIds(Long roleId) {
        return roleProjectMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleProject>()
                        .eq(SysRoleProject::getRoleId, roleId))
                .stream().map(SysRoleProject::getProjectId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignDataScope(Long roleId, String dataScope, List<Long> projectIds) {
        SysRole role = getById(roleId);
        if (role == null) throw new BusinessException("角色不存在");
        // 更新数据权限类型
        role.setDataScope(dataScope);
        updateById(role);
        // 先删除旧的项目关联
        roleProjectMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleProject>()
                        .eq(SysRoleProject::getRoleId, roleId));
        // 如果是自定义权限，插入指定项目
        if ("custom".equals(dataScope) && projectIds != null) {
            for (Long pid : projectIds) {
                SysRoleProject rp = new SysRoleProject();
                rp.setRoleId(roleId);
                rp.setProjectId(pid);
                roleProjectMapper.insert(rp);
            }
        }
    }
}
