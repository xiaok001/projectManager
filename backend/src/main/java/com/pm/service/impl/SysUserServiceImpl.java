package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.SysUserMapper;
import com.pm.model.dto.LoginDTO;
import com.pm.model.dto.UserDTO;
import com.pm.model.entity.SysRole;
import com.pm.model.entity.SysUser;
import com.pm.model.vo.LoginVO;
import com.pm.security.JwtUtil;
import com.pm.service.SysRoleService;
import com.pm.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SysRoleService roleService;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername())
                .one();

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setToken(token);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return vo;
    }

    @Override
    public List<SysUser> listAllUsers() {
        List<SysUser> users = lambdaQuery().list();
        // 自动补全 roleId：旧数据可能没有 roleId，通过 role 字段匹配
        for (SysUser user : users) {
            if (user.getRoleId() == null && user.getRole() != null) {
                SysRole matchedRole = roleService.lambdaQuery()
                        .eq(SysRole::getRoleKey, user.getRole().toLowerCase())
                        .or()
                        .eq(SysRole::getRoleKey, user.getRole())
                        .one();
                if (matchedRole != null) {
                    user.setRoleId(matchedRole.getId());
                    // 回写数据库
                    lambdaUpdate().eq(SysUser::getId, user.getId())
                            .set(SysUser::getRoleId, matchedRole.getId())
                            .update();
                }
            }
        }
        return users;
    }

    @Override
    public SysUser getByUsername(String username) {
        return lambdaQuery()
                .eq(SysUser::getUsername, username)
                .one();
    }

    @Override
    public SysUser createUser(UserDTO dto) {
        // Check username uniqueness
        SysUser existing = getByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus());

        save(user);
        log.info("创建用户成功: userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Override
    public SysUser updateUser(Long id, UserDTO dto) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // Check username uniqueness if changed
        if (!user.getUsername().equals(dto.getUsername())) {
            SysUser existing = getByUsername(dto.getUsername());
            if (existing != null) {
                throw new BusinessException("用户名已存在");
            }
        }

        user.setUsername(dto.getUsername());
        // Only encode password if provided
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus());

        updateById(user);
        log.info("更新用户成功: userId={}", user.getId());
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 不能删除自己
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("不能删除超级管理员账号");
        }
        removeById(id);
        log.info("删除用户成功: userId={}, username={}", id, user.getUsername());
    }
}
