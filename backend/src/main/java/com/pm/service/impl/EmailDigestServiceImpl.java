package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.mapper.EmailDigestLogMapper;
import com.pm.model.entity.EmailDigestLog;
import com.pm.model.vo.DashboardVO;
import com.pm.service.DashboardService;
import com.pm.service.EmailDigestService;
import com.pm.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDigestServiceImpl extends ServiceImpl<EmailDigestLogMapper, EmailDigestLog>
        implements EmailDigestService {

    private final JavaMailSender mailSender;
    private final DashboardService dashboardService;
    private final SystemConfigService configService;
    private final AiProvider aiProvider;

    @Override
    @Scheduled(cron = "0 30 9 * * ?")
    public void sendDailyDigest() {
        log.info("开始发送每日邮件摘要...");

        String recipients = configService.getValue("digest_recipient_emails", "");
        if (recipients == null || recipients.trim().isEmpty()) {
            log.info("未配置收件人邮箱，跳过发送");
            return;
        }

        EmailDigestLog logEntry = new EmailDigestLog();
        logEntry.setSendDate(LocalDate.now());
        logEntry.setRecipients(recipients);

        try {
            // 获取Dashboard数据(部门经理视角，userId=null表示查看全部)
            DashboardVO dashboard = dashboardService.getSummary(null, "DEPT_MANAGER");

            // 构建结构化内容
            String structuredContent = buildStructuredContent(dashboard);

            // 尝试AI生成自然语言摘要
            String aiSummary = null;
            try {
                String prompt = "请根据以下项目管理数据，生成一段简洁的每日摘要，突出最需要关注的1-3件事：\n\n" + structuredContent;
                aiSummary = aiProvider.chat(prompt);
            } catch (Exception e) {
                log.warn("AI摘要生成失败，降级为结构化文本", e);
            }

            // 最终邮件内容
            String content = (aiSummary != null && !aiSummary.trim().isEmpty())
                    ? aiSummary + "\n\n---\n详细数据：\n" + structuredContent
                    : structuredContent;

            logEntry.setContent(content);

            // 发送邮件
            String[] emailList = recipients.split(",");
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(Arrays.stream(emailList).map(String::trim).toArray(String[]::new));
            message.setSubject("每日项目管理摘要 - " + LocalDate.now());
            message.setText(content);

            mailSender.send(message);

            logEntry.setSendStatus("成功");
            log.info("每日邮件摘要发送成功");

        } catch (Exception e) {
            log.error("每日邮件摘要发送失败", e);
            logEntry.setSendStatus("失败");
            logEntry.setFailReason(e.getMessage());
        }

        logEntry.setSentAt(LocalDateTime.now());
        save(logEntry);
    }

    private String buildStructuredContent(DashboardVO dashboard) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 每日项目管理摘要 ===\n\n");

        // 风险聚合
        if (dashboard.getRiskAggregation() != null && !dashboard.getRiskAggregation().isEmpty()) {
            sb.append("【待处理风险/问题】\n");
            dashboard.getRiskAggregation().forEach(risk -> {
                sb.append("- [").append(risk.getSeverity()).append("] ")
                  .append(risk.getProjectName()).append(": ")
                  .append(risk.getDescription());
                if (risk.getIsStale()) {
                    sb.append(" ⚠️停滞").append(risk.getStaleDays()).append("天");
                }
                sb.append("\n");
            });
            sb.append("\n");
        }

        // 未来节点
        if (dashboard.getFutureNodes() != null && !dashboard.getFutureNodes().isEmpty()) {
            sb.append("【未来关键节点】\n");
            dashboard.getFutureNodes().forEach(node -> {
                sb.append("- ").append(node.getProjectName())
                  .append(" - ").append(node.getStageName())
                  .append(" (").append(node.getPlanEnd()).append(")");
                if (node.getIsOverdue()) sb.append(" ⚠️已逾期");
                sb.append("\n");
            });
            sb.append("\n");
        }

        // 健康度
        if (dashboard.getProjectHealthList() != null && !dashboard.getProjectHealthList().isEmpty()) {
            sb.append("【项目健康度】\n");
            dashboard.getProjectHealthList().forEach(health -> {
                sb.append("- ").append(health.getProjectName())
                  .append(": ").append(health.getHealthScore()).append("分")
                  .append(" (").append(health.getHealthColor()).append(")")
                  .append(" 当前阶段: ").append(health.getCurrentStage() != null ? health.getCurrentStage() : "无")
                  .append("\n");
            });
        }

        return sb.toString();
    }
}
