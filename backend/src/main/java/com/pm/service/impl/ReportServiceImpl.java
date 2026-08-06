package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.common.constants.Constants;
import com.pm.mapper.ReportWeeklyLogMapper;
import com.pm.mapper.SysUserMapper;
import com.pm.model.entity.*;
import com.pm.model.vo.WeeklyReportVO;
import com.pm.model.vo.WeeklyReportVO.*;
import com.pm.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ProjectService projectService;
    private final ProjectStageService stageService;
    private final ProjectRiskService riskService;
    private final ProjectTodoService todoService;
    private final AiProvider aiProvider;
    private final ReportWeeklyLogMapper reportLogMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    @Override
    public WeeklyReportVO generateWeeklyReport(Long projectId, Long userId, String role) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        LocalDate nextWeekEnd = weekEnd.plusWeeks(1);
        String period = weekStart.format(DateTimeFormatter.ofPattern("MM.dd")) + " - "
                + weekEnd.format(DateTimeFormatter.ofPattern("MM.dd"));

        WeeklyReportVO report = new WeeklyReportVO();
        report.setPeriod(period);

        // 获取项目列表
        List<Project> projects;
        if (projectId != null) {
            Project p = projectService.getById(projectId);
            projects = p != null ? List.of(p) : List.of();
            if (p != null) report.setProjectName(p.getName());
        } else {
            // 全部项目模式：自动排除已结束的项目
            projects = projectService.listProjects(userId, role).stream()
                    .filter(p -> !"已完成".equals(p.getStatus()))
                    .collect(Collectors.toList());
        }

        List<StageSummary> completedStages = new ArrayList<>();
        List<RiskSummary> newRisks = new ArrayList<>();
        List<RiskSummary> closedRisks = new ArrayList<>();
        List<StageSummary> upcomingStages = new ArrayList<>();
        int totalRisks = 0, highRisks = 0, mediumRisks = 0, lowRisks = 0, staleRisks = 0;
        int todoTotal = 0, todoCompleted = 0, todoInProgress = 0, todoOverdue = 0;

        for (Project project : projects) {
            String pc = project.getProjectCode();
            String pn = project.getName();

            // 本周完成的阶段（排除运维阶段）
            List<ProjectStage> stages = stageService.listByProjectId(project.getId());
            for (ProjectStage s : stages) {
                if ("运维".equals(s.getStageName())) continue;

                if (s.getActualEnd() != null && !s.getActualEnd().isBefore(weekStart) && !s.getActualEnd().isAfter(weekEnd)) {
                    StageSummary ss = new StageSummary();
                    ss.setProjectCode(pc); ss.setProjectName(pn);
                    ss.setStageName(s.getStageName());
                    ss.setActualEnd(s.getActualEnd().toString());
                    ss.setStatus("已完成");
                    completedStages.add(ss);
                }
                // 下周计划节点（排除运维阶段）
                if (s.getPlanEnd() != null && !s.getPlanEnd().isBefore(today) && s.getPlanEnd().isBefore(nextWeekEnd)
                        && !Constants.STAGE_COMPLETED.equals(s.getStatus())
                        && !"运维".equals(s.getStageName())) {
                    StageSummary us = new StageSummary();
                    us.setProjectCode(pc); us.setProjectName(pn);
                    us.setStageName(s.getStageName());
                    us.setPlanEnd(s.getPlanEnd().toString());
                    us.setStatus(s.getStatus());
                    upcomingStages.add(us);
                }
            }

            // 本周新增/关闭的风险
            List<ProjectRisk> risks = riskService.lambdaQuery().eq(ProjectRisk::getProjectId, project.getId()).list();
            for (ProjectRisk r : risks) {
                if (r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isBefore(weekStart)
                        && !r.getCreatedAt().toLocalDate().isAfter(weekEnd)) {
                    newRisks.add(toRiskSummary(r, pc, pn));
                }
                // 本周关闭的（简化：通过updated_at判断）
                if ((Constants.RISK_CLOSED.equals(r.getStatus()) || Constants.RISK_RESOLVED.equals(r.getStatus()))
                        && r.getLastUpdatedAt() != null && !r.getLastUpdatedAt().toLocalDate().isBefore(weekStart)) {
                    closedRisks.add(toRiskSummary(r, pc, pn));
                }
            }

            // 未关闭风险统计
            List<ProjectRisk> activeRisks = risks.stream()
                    .filter(r -> !Constants.RISK_CLOSED.equals(r.getStatus()) && !Constants.RISK_RESOLVED.equals(r.getStatus()))
                    .collect(Collectors.toList());
            totalRisks += activeRisks.size();
            highRisks += activeRisks.stream().filter(r -> "高".equals(r.getSeverity())).count();
            mediumRisks += activeRisks.stream().filter(r -> "中".equals(r.getSeverity())).count();
            lowRisks += activeRisks.stream().filter(r -> "低".equals(r.getSeverity())).count();
            staleRisks += activeRisks.stream().filter(r -> Boolean.TRUE.equals(r.getIsStale())).count();

            // 待办统计
            List<ProjectTodo> todos = todoService.lambdaQuery().eq(ProjectTodo::getProjectId, project.getId()).list();
            todoTotal += todos.size();
            todoCompleted += todos.stream().filter(t -> "已完成".equals(t.getStatus())).count();
            todoInProgress += todos.stream().filter(t -> "进行中".equals(t.getStatus())).count();
            todoOverdue += todos.stream().filter(t -> "已逾期".equals(t.getStatus())).count();
        }

        // 组装风险统计
        RiskStats rs = new RiskStats();
        rs.setTotal(totalRisks); rs.setHigh(highRisks); rs.setMedium(mediumRisks);
        rs.setLow(lowRisks); rs.setStale(staleRisks);
        report.setRiskStats(rs);

        // 组装待办统计
        TodoStats ts = new TodoStats();
        ts.setTotal(todoTotal); ts.setCompleted(todoCompleted);
        ts.setInProgress(todoInProgress); ts.setOverdue(todoOverdue);
        report.setTodoStats(ts);

        report.setCompletedStages(completedStages);
        report.setNewRisks(newRisks);
        report.setClosedRisks(closedRisks);
        report.setUpcomingStages(upcomingStages);

        // AI生成叙述性总结
        report.setAiSummary(generateAiSummary(report));

        // 保存历史记录
        try {
            ReportWeeklyLog logEntry = new ReportWeeklyLog();
            logEntry.setProjectId(projectId);
            logEntry.setProjectName(report.getProjectName());
            logEntry.setPeriod(report.getPeriod());
            logEntry.setAiSummary(report.getAiSummary());
            logEntry.setReportContent(objectMapper.writeValueAsString(report));
            logEntry.setCreatedBy(userId);
            reportLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("周报历史保存失败: {}", e.getMessage());
        }

        return report;
    }

    private RiskSummary toRiskSummary(ProjectRisk r, String pc, String pn) {
        RiskSummary rs = new RiskSummary();
        rs.setRiskCode(r.getRiskCode());
        rs.setProjectCode(pc);
        rs.setProjectName(pn);
        rs.setDescription(r.getDescription());
        rs.setSeverity(r.getSeverity());
        rs.setStatus(r.getStatus());
        return rs;
    }

    private String generateAiSummary(WeeklyReportVO report) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下项目周报数据，生成一段简洁的周报总结（3-5句话），突出本周重点成果、主要风险和下周关注点：\n\n");

        sb.append("报告周期: ").append(report.getPeriod()).append("\n");
        if (report.getProjectName() != null) {
            sb.append("项目: ").append(report.getProjectName()).append("\n");
        }

        sb.append("\n【本周完成阶段】");
        if (report.getCompletedStages().isEmpty()) {
            sb.append("无");
        } else {
            for (StageSummary s : report.getCompletedStages()) {
                sb.append("\n- ").append(s.getProjectName()).append(" - ").append(s.getStageName());
            }
        }

        sb.append("\n\n【本周新增风险】").append(report.getNewRisks().size()).append("条");
        for (RiskSummary r : report.getNewRisks()) {
            sb.append("\n- [").append(r.getSeverity()).append("] ").append(r.getProjectName()).append(": ").append(r.getDescription());
        }

        sb.append("\n\n【本周关闭风险】").append(report.getClosedRisks().size()).append("条");

        RiskStats rs = report.getRiskStats();
        sb.append("\n\n【当前未关闭风险】共").append(rs.getTotal()).append("条")
          .append(" (高").append(rs.getHigh()).append(" 中").append(rs.getMedium())
          .append(" 低").append(rs.getLow()).append(" 停滞").append(rs.getStale()).append(")");

        sb.append("\n\n【下周计划节点】");
        if (report.getUpcomingStages().isEmpty()) {
            sb.append("无");
        } else {
            for (StageSummary s : report.getUpcomingStages()) {
                sb.append("\n- ").append(s.getProjectName()).append(" - ").append(s.getStageName()).append(" (").append(s.getPlanEnd()).append(")");
            }
        }

        TodoStats ts = report.getTodoStats();
        sb.append("\n\n【待办事项】共").append(ts.getTotal()).append("条")
          .append(" (已完成").append(ts.getCompleted()).append(" 进行中").append(ts.getInProgress())
          .append(" 逾期").append(ts.getOverdue()).append(")");

        try {
            String result = aiProvider.chat(sb.toString());
            if (result != null && !result.trim().isEmpty()) {
                return result.trim();
            }
        } catch (Exception e) {
            log.warn("AI周报总结生成失败: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Page<ReportWeeklyLog> getHistory(Integer pageNum, Integer pageSize, Long projectId) {
        Page<ReportWeeklyLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportWeeklyLog> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(ReportWeeklyLog::getProjectId, projectId);
        }
        wrapper.orderByDesc(ReportWeeklyLog::getCreatedAt);
        Page<ReportWeeklyLog> result = reportLogMapper.selectPage(page, wrapper);
        // 填充创建人姓名
        List<Long> userIds = result.getRecords().stream()
                .map(ReportWeeklyLog::getCreatedBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            Map<Long, String> nameMap = sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            result.getRecords().forEach(r -> {
                if (r.getCreatedBy() != null) r.setCreatedByName(nameMap.get(r.getCreatedBy()));
            });
        }
        return result;
    }
}
