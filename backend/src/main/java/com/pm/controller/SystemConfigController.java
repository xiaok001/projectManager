package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.entity.SystemConfig;
import com.pm.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService configService;

    @GetMapping
    public R<List<SystemConfig>> list() {
        return R.ok(configService.listAll());
    }

    @PutMapping
    @PreAuthorize("hasRole('DEPT_MANAGER')")
    public R<Void> update(@RequestBody List<Map<String, String>> configs) {
        configService.updateConfigs(configs);
        return R.ok();
    }
}
