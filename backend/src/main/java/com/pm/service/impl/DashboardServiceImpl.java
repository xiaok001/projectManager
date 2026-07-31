package com.pm.service.impl;

import com.pm.common.constants.Constants;
import com.pm.model.entity.Project;
import com.pm.model.entity.ProjectRisk;
import com.pm.model.entity.ProjectStage;
import com.pm.model.vo.*;
import com.pm.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ProjectService projectService;
    private final ProjectStageService stageService;
    private final ProjectRiskService riskService;
    private final SystemConfigService configService;

    @Override
    public DashboardVO getSummary(Long userId, String role) {
        DashboardVO dashboard = new DashboardVO();

        // 1. 风险/问题聚合区
        dashboard.setRiskAggregation(riskService.listAggregated(userId, role));

        // 2. 未来关键节点
        dashboard.setFutureNodes(getFutureNodes(userId, role));

        // 3. 项目健康度总览
        dashboard.setProjectHealthList(calculateAllHealth(userId, role));

        return dashboard;
    }

    @Override
    public ProjectHealthVO calculateHealth(Long projectId) {
        Project project = projectService.getById(projectId);
        if (project == null) return null;

        ProjectHealthVO vo = new ProjectHealthVO();
        vo.setProjectId(project.getId());
        vo.setProjectCode(project.getProjectCode());
        vo.setProjectName(project.getName());
        vo.setProjectLevel(project.getLevel());
        vo.setCurrentStage(project.getCurrentStage());
        vo.setProjectStatus(project.getStatus());
        vo.setSatisfactionScore(project.getSatisfactionScore());

        // 计算三个维度得分
        int timeScore = calculateTimeScore(projectId);
        int riskScore = calculateRiskScore(projectId);
        int deliveryScore = calculateDeliveryScore(projectId);

        vo.setTimeScore(timeScore);
        vo.setRiskScore(riskScore);
        vo.setDeliveryScore(deliveryScore);

        // 加权计算总分
        int timeWeight = configService.getIntValue("health_weight_time", 35);
        int riskWeight = configService.getIntValue("health_weight_risk", 40);
        int deliveryWeight = configService.getIntValue("health_weight_delivery", 25);

        int healthScore = (timeScore * timeWeight + riskScore * riskWeight + deliveryScore * deliveryWeight) / 100;
        vo.setHealthScore(healthScore);

        // 映射颜色
        int greenMin = configService.getIntValue("health_score_green_min", 80);
        int yellowMin = configService.getIntValue("health_score_yellow_min", 60);
        if (healthScore >= greenMin) {
            vo.setHealthColor("绿");
        } else if (healthScore >= yellowMin) {
            vo.setHealthColor("黄");
        } else {
            vo.setHealthColor("红");
        }

        // 风险统计
        List<ProjectRisk> activeRisks = riskService.lambdaQuery()
                .eq(ProjectRisk::getProjectId, projectId)
                .ne(ProjectRisk::getStatus, Constants.RISK_CLOSED)
                .ne(ProjectRisk::getStatus, Constants.RISK_RESOLVED)
                .list();
        vo.setHighRiskCount((int) activeRisks.stream().filter(r -> "高".equals(r.getSeverity())).count());
        vo.setStaleRiskCount((int) activeRisks.stream().filter(r -> Boolean.TRUE.equals(r.getIsStale())).count());

        return vo;
    }

    @Override
    public List<ProjectHealthVO> calculateAllHealth(Long userId, String role) {
        List<Project> projects = projectService.listProjects(userId, role);
        List<ProjectHealthVO> result = new ArrayList<>();
        for (Project project : projects) {
            ProjectHealthVO health = calculateHealth(project.getId());
            if (health != null) {
                result.add(health);
            }
        }
        return result;
    }

    /**
     * 时间维度得分
     */
    private int calculateTimeScore(Long projectId) {
        LocalDate today = LocalDate.now();
        int penaltyPerDay = configService.getIntValue("time_delay_penalty_per_day", 2);
        List<ProjectStage> stages = stageService.listByProjectId(projectId);

        int totalDelay = 0;
        for (ProjectStage stage : stages) {
            if (stage.getPlanEnd() == null || stage.getPlanEnd().isAfter(today)) continue;

            long delayDays = 0;
            if (stage.getActualEnd() != null) {
                // 已完成阶段: actual_end - plan_end
                delayDays = Math.max(0, ChronoUnit.DAYS.between(stage.getPlanEnd(), stage.getActualEnd()));
            } else {
                // 未完成阶段: today - plan_end
                delayDays = Math.max(0, ChronoUnit.DAYS.between(stage.getPlanEnd(), today));
            }
            totalDelay += delayDays;
        }

        return Math.max(0, 100 - totalDelay * penaltyPerDay);
    }

    /**
     * 风险维度得分
     */
    private int calculateRiskScore(Long projectId) {
        int penaltyHigh = configService.getIntValue("risk_penalty_high", 15);
        int penaltyMedium = configService.getIntValue("risk_penalty_medium", 8);
        int penaltyLow = configService.getIntValue("risk_penalty_low", 3);
        int penaltyStale = configService.getIntValue("risk_penalty_stale", 10);

        List<ProjectRisk> risks = riskService.lambdaQuery()
                .eq(ProjectRisk::getProjectId, projectId)
                .ne(ProjectRisk::getStatus, Constants.RISK_CLOSED)
                .ne(ProjectRisk::getStatus, Constants.RISK_RESOLVED)
                .list();

        int penalty = 0;
        int staleCount = 0;
        for (ProjectRisk risk : risks) {
            switch (risk.getSeverity()) {
                case "高": penalty += penaltyHigh; break;
                case "中": penalty += penaltyMedium; break;
                case "低": penalty += penaltyLow; break;
            }
            if (Boolean.TRUE.equals(risk.getIsStale())) staleCount++;
        }
        penalty += staleCount * penaltyStale;

        return Math.max(0, 100 - penalty);
    }

    /**
     * 交付维度得分
     */
    private int calculateDeliveryScore(Long projectId) {
        LocalDate today = LocalDate.now();
        List<ProjectStage> stages = stageService.listByProjectId(projectId);

        long totalDue = 0;
        long onTimeCompleted = 0;

        for (ProjectStage stage : stages) {
            if (stage.getPlanEnd() == null || stage.getPlanEnd().isAfter(today)) continue;
            totalDue++;

            if (stage.getActualEnd() != null
                    && !stage.getActualEnd().isAfter(stage.getPlanEnd())) {
                onTimeCompleted++;
            }
        }

        if (totalDue == 0) return 100;
        return (int) (onTimeCompleted * 100 / totalDue);
    }

    /**
     * 获取未来7-14天关键节点
     */
    private List<FutureNodeVO> getFutureNodes(Long userId, String role) {
        LocalDate today = LocalDate.now();
        LocalDate twoWeeksLater = today.plusDays(14);
        List<Project> projects = projectService.listProjects(userId, role);

        List<FutureNodeVO> nodes = new ArrayList<>();
        for (Project project : projects) {
            List<ProjectStage> stages = stageService.listByProjectId(project.getId());
            for (ProjectStage stage : stages) {
                boolean isOverdue = stage.getPlanEnd() != null
                        && stage.getPlanEnd().isBefore(today)
                        && !Constants.STAGE_COMPLETED.equals(stage.getStatus());

                boolean isFuture = stage.getPlanEnd() != null
                        && !stage.getPlanEnd().isBefore(today)
                        && stage.getPlanEnd().isBefore(twoWeeksLater);

                if (isOverdue || isFuture) {
                    FutureNodeVO node = new FutureNodeVO();
                    node.setStageId(stage.getId());
                    node.setProjectId(project.getId());
                    node.setProjectCode(project.getProjectCode());
                    node.setProjectName(project.getName());
                    node.setProjectLevel(project.getLevel());
                    node.setStageName(stage.getStageName());
                    node.setPlanEnd(stage.getPlanEnd());
                    node.setStageStatus(stage.getStatus());
                    node.setIsOverdue(isOverdue);
                    nodes.add(node);
                }
            }
        }

        // 排序: 已逾期置顶，然后按planEnd升序
        nodes.sort((a, b) -> {
            if (a.getIsOverdue() && !b.getIsOverdue()) return -1;
            if (!a.getIsOverdue() && b.getIsOverdue()) return 1;
            return a.getPlanEnd().compareTo(b.getPlanEnd());
        });

        return nodes;
    }
}
