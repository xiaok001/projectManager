package com.pm.controller;

import com.pm.common.exception.BusinessException;
import com.pm.common.response.R;
import com.pm.model.dto.UserDTO;
import com.pm.model.entity.SysUser;
import com.pm.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    // ========== 个人资料 ==========

    @GetMapping("/me")
    public R<SysUser> getMe(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userService.getById(userId);
        user.setPassword(null); // 不返回密码
        return R.ok(user);
    }

    @PutMapping("/me")
    public R<Void> updateMe(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateMyProfile(userId, body.get("realName"), body.get("email"), body.get("phone"));
        return R.ok();
    }

    @PutMapping("/me/password")
    public R<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || oldPassword.isBlank()) throw new BusinessException("请输入原密码");
        if (newPassword == null || newPassword.isBlank()) throw new BusinessException("请输入新密码");
        if (newPassword.length() < 6) throw new BusinessException("新密码长度不能少于6位");
        userService.changePassword(userId, oldPassword, newPassword);
        return R.ok();
    }

    // ========== 管理员操作 ==========

    @GetMapping
    public R<List<SysUser>> list() {
        return R.ok(userService.listAllUsers());
    }

    @GetMapping("/{id}")
    public R<SysUser> detail(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('DEPT_MANAGER')")
    public R<SysUser> create(@Valid @RequestBody UserDTO dto) {
        return R.ok(userService.createUser(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEPT_MANAGER')")
    public R<SysUser> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        return R.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DEPT_MANAGER')")
    public R<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }
}
