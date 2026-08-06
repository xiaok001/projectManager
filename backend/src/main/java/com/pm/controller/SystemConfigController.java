package com.pm.controller;

import com.pm.common.exception.BusinessException;
import com.pm.common.response.R;
import com.pm.model.entity.SystemConfig;
import com.pm.service.SystemConfigService;
import com.pm.service.impl.AiProvider;
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
    private final AiProvider aiProvider;

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

    @PostMapping("/test-ai")
    @PreAuthorize("hasRole('DEPT_MANAGER')")
    public R<String> testAiConnection() {
        try {
            String result = aiProvider.chat("请回复OK两个字母，不要输出任何其他内容。");
            if (result != null && !result.trim().isEmpty()) {
                return R.ok("AI连接成功，响应: " + result.trim().substring(0, Math.min(result.trim().length(), 50)));
            }
            throw new BusinessException("AI返回内容为空");
        } catch (Exception e) {
            throw new BusinessException("AI连接失败: " + e.getMessage());
        }
    }
}
