package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.dto.UserDTO;
import com.pm.model.entity.SysUser;
import com.pm.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

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
