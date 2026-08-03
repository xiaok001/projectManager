package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.entity.SysPermission;
import com.pm.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permService;

    @GetMapping("/tree")
    public R<List<SysPermission>> tree() {
        return R.ok(permService.getPermissionTree());
    }

    @GetMapping("/tree/{roleId}")
    public R<List<SysPermission>> treeWithChecked(@PathVariable Long roleId) {
        return R.ok(permService.getPermissionTreeWithChecked(roleId));
    }
}
