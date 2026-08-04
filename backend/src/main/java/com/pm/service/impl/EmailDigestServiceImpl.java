package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.constants.Constants;
import com.pm.mapper.EmailDigestLogMapper;
import com.pm.model.entity.EmailDigestLog;
import com.pm.model.entity.Project;
import com.pm.model.entity.ProjectRisk;
import com.pm.model.entity.ProjectTodo;
import com.pm.model.vo.DashboardVO;
import com.pm.service.*;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDigestServiceImpl extends ServiceImpl<EmailDigestLogMapper, EmailDigestLog>
        implements EmailDigestService {

    private final JavaMailSender mailSender;
    private final DashboardService dashboardService;
    private final SystemConfigService configService;
    private final AiProvider aiProvider;
    private final ProjectService projectService;
    private final ProjectRiskService riskService;
    private final ProjectTodoService todoService;

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
            message.setFrom("多项目管理系统机器人 <nieyankang0205@163.com>");
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

    @Override
    public void sendDailyTodoAndRiskDigest() {
        log.info("开始发送项目待办与风险日报...");

        String recipients = configService.getValue("digest_recipient_emails", "");
        if (recipients == null || recipients.trim().isEmpty()) {
            log.info("未配置收件人邮箱，跳过发送");
            return;
        }

        EmailDigestLog logEntry = new EmailDigestLog();
        logEntry.setSendDate(LocalDate.now());
        logEntry.setRecipients(recipients);

        try {
            List<Project> projects = projectService.lambdaQuery()
                    .eq(Project::getStatus, "进行中")
                    .orderByAsc(Project::getProjectCode)
                    .list();

            StringBuilder content = new StringBuilder();
            content.append("=== 项目待办与风险日报 ===\n");
            content.append("日期：").append(LocalDate.now()).append("\n");
            content.append("在管项目：").append(projects.size()).append(" 个\n\n");

            for (Project project : projects) {
                content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                content.append("【").append(project.getProjectCode()).append("】").append(project.getName()).append("\n");
                content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

                // 待办事项
                List<ProjectTodo> todos = todoService.lambdaQuery()
                        .eq(ProjectTodo::getProjectId, project.getId())
                        .ne(ProjectTodo::getStatus, "已完成")
                        .ne(ProjectTodo::getStatus, "已取消")
                        .orderByAsc(ProjectTodo::getPriority)
                        .orderByAsc(ProjectTodo::getPlanEnd)
                        .list();

                content.append("📋 待办事项（").append(todos.size()).append("条）\n");
                if (todos.isEmpty()) {
                    content.append("  无待处理待办\n");
                } else {
                    for (ProjectTodo t : todos) {
                        String owner = t.getOwnerName() != null ? t.getOwnerName() : "未指定";
                        content.append("  • [").append(t.getPriority()).append("] ")
                               .append(t.getTitle())
                               .append(" | 负责人: ").append(owner)
                               .append(" | 状态: ").append(t.getStatus());
                        if (t.getPlanEnd() != null) {
                            content.append(" | 计划完成: ").append(t.getPlanEnd());
                        }
                        content.append("\n");
                    }
                }
                content.append("\n");

                // 风险列表
                List<ProjectRisk> risks = riskService.lambdaQuery()
                        .eq(ProjectRisk::getProjectId, project.getId())
                        .ne(ProjectRisk::getStatus, "已关闭")
                        .ne(ProjectRisk::getStatus, "已解决")
                        .orderByDesc(ProjectRisk::getSeverity)
                        .orderByDesc(ProjectRisk::getLastUpdatedAt)
                        .list();

                content.append("⚠️ 风险/问题（").append(risks.size()).append("条）\n");
                if (risks.isEmpty()) {
                    content.append("  无待处理风险\n");
                } else {
                    for (ProjectRisk r : risks) {
                        content.append("  • [").append(r.getSeverity()).append("][").append(r.getType()).append("] ")
                               .append(r.getDescription())
                               .append(" | 状态: ").append(r.getStatus());
                        if (Boolean.TRUE.equals(r.getIsStale())) {
                            content.append(" | ⚠️停滞");
                        }
                        content.append("\n");
                    }
                }
                content.append("\n");
            }

            // 尝试AI润色
            String finalContent;
            try {
                String aiPrompt = "请根据以下项目待办和风险日报数据，生成一段简洁的邮件开头摘要（2-3句话），突出今日最需要关注的项目和问题：\n\n" + content;
                String aiSummary = aiProvider.chat(aiPrompt);
                if (aiSummary != null && !aiSummary.trim().isEmpty()) {
                    finalContent = aiSummary.trim() + "\n\n---\n" + content;
                } else {
                    finalContent = content.toString();
                }
            } catch (Exception e) {
                log.warn("AI日报摘要生成失败，使用纯文本", e);
                finalContent = content.toString();
            }

            logEntry.setContent(finalContent);

            // 发送邮件
            String[] emailList = Arrays.stream(recipients.split(","))
                    .map(String::trim).toArray(String[]::new);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("多项目管理系统机器人 <nieyankang0205@163.com>");
            message.setTo(emailList);
            message.setSubject("项目待办与风险日报 - " + LocalDate.now());
            message.setText(finalContent);
            mailSender.send(message);

            logEntry.setSendStatus("成功");
            log.info("项目待办与风险日报发送成功");
        } catch (Exception e) {
            log.error("项目待办与风险日报发送失败", e);
            logEntry.setSendStatus("失败");
            logEntry.setFailReason(e.getMessage());
        }

        logEntry.setSentAt(LocalDateTime.now());
        save(logEntry);
    }
}
