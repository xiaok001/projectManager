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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SysRoleService roleService;
    private final JavaMailSender mailSender;

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

    @Override
    public void verifyUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("请输入账号");
        }
        SysUser user = getByUsername(username.trim());
        if (user == null) {
            throw new BusinessException("账号不存在，请检查后重新输入");
        }
    }

    @Override
    public void resetPassword(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("请输入账号");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("请输入邮箱地址");
        }
        SysUser user = getByUsername(username.trim());
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email.trim())) {
            throw new BusinessException("邮箱与账号不匹配，请检查后重新输入");
        }

        // 生成随机密码
        String newPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 更新密码
        lambdaUpdate()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, encodedPassword)
                .update();

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("多项目管理系统机器人 <nieyankang0205@163.com>");
            message.setTo(email.trim());
            message.setSubject("多项目管理系统 - 密码重置通知");
            message.setText("尊敬的 " + user.getRealName() + "：\n\n"
                    + "您的账号 " + username + " 密码已被重置。\n\n"
                    + "新密码为：" + newPassword + "\n\n"
                    + "请使用此密码登录后，及时修改为您的个人密码。\n\n"
                    + "如非本人操作，请联系系统管理员。");
            mailSender.send(message);
            log.info("密码重置邮件已发送: username={}, email={}", username, email);
        } catch (Exception e) {
            log.error("密码重置邮件发送失败: username={}", username, e);
            throw new BusinessException("邮件发送失败，请稍后重试或联系管理员");
        }
    }

    @Override
    public void updateMyProfile(Long userId, String realName, String email, String phone) {
        SysUser user = getById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        if (realName != null && !realName.isBlank()) user.setRealName(realName.trim());
        user.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        user.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        updateById(user);
        log.info("用户资料更新: userId={}", userId);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        log.info("密码修改成功: userId={}", userId);
    }
}
