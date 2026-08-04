package com.pm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.common.exception.BusinessException;
import com.pm.common.response.R;
import com.pm.model.entity.EmailDigestLog;
import com.pm.service.EmailDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/digest")
@RequiredArgsConstructor
public class EmailDigestController {

    private final EmailDigestService emailDigestService;
    private final JavaMailSender mailSender;

    @PostMapping("/send-now")
    public R<Void> sendNow() {
        emailDigestService.sendDailyDigest();
        return R.ok();
    }

    @PostMapping("/test")
    public R<Void> testEmail(@RequestParam String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("多项目管理系统机器人 <nieyankang0205@163.com>");
            message.setTo(email.trim());
            message.setSubject("项目管理系统 - 邮件配置测试");
            message.setText("这是一封测试邮件，说明您的邮件配置正确。\n\n发送时间：" + java.time.LocalDateTime.now());
            mailSender.send(message);
            return R.ok();
        } catch (Exception e) {
            throw new BusinessException("邮件发送失败: " + e.getMessage());
        }
    }

    @GetMapping("/logs")
    public R<Page<EmailDigestLog>> logs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<EmailDigestLog> page = new Page<>(pageNum, pageSize);
        return R.ok(emailDigestService.page(page));
    }
}
