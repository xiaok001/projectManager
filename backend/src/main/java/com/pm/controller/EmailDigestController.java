package com.pm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.common.response.R;
import com.pm.model.entity.EmailDigestLog;
import com.pm.service.EmailDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/digest")
@RequiredArgsConstructor
public class EmailDigestController {

    private final EmailDigestService emailDigestService;

    @PostMapping("/send-now")
    public R<Void> sendNow() {
        emailDigestService.sendDailyDigest();
        return R.ok();
    }

    @GetMapping("/logs")
    public R<Page<EmailDigestLog>> logs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<EmailDigestLog> page = new Page<>(pageNum, pageSize);
        return R.ok(emailDigestService.page(page));
    }
}
