package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.entity.SysRole;
import com.pm.service.SysRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DEPT_MANAGER')")
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping
    public R<List<SysRole>> list() {
        return R.ok(roleService.listRoles());
    }

    @PostMapping
    public R<SysRole> create(@RequestBody SysRole role) {
        return R.ok(roleService.createRole(role));
    }

    @PutMapping("/{id}")
    public R<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        return R.ok(roleService.updateRole(id, role));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    @GetMapping("/{id}/permissions")
    public R<List<Long>> getPermissions(@PathVariable Long id) {
        return R.ok(roleService.getRolePermissionIds(id));
    }

    @PutMapping("/{id}/permissions")
    public R<Void> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignPermissions(id, body.get("permissionIds"));
        return R.ok();
    }
}
