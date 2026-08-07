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

        // 获取阶段和风险数据
        List<ProjectStage> stages = stageService.listByProjectId(projectId);
        List<ProjectRisk> risks = riskService.lambdaQuery()
                .eq(ProjectRisk::getProjectId, projectId)
                .ne(ProjectRisk::getStatus, Constants.RISK_CLOSED)
                .ne(ProjectRisk::getStatus, Constants.RISK_RESOLVED)
                .list();

        // 判断数据是否充足
        LocalDate today = LocalDate.now();
        boolean hasStages = !stages.isEmpty();
        boolean hasDueStages = stages.stream().anyMatch(s -> s.getPlanEnd() != null && !s.getPlanEnd().isAfter(today));
        boolean hasRisks = !risks.isEmpty();
        boolean dataSufficient = hasDueStages || hasRisks;

        vo.setDataSufficient(dataSufficient);

        if (!dataSufficient) {
            // 数据不足：不显示分数和颜色
            vo.setHealthScore(null);
            vo.setHealthColor("灰");
            vo.setTimeScore(null);
            vo.setRiskScore(null);
            vo.setDeliveryScore(null);
            vo.setTimeDetail("尚无阶段到期，无法计算");
            vo.setRiskDetail("暂无待处理风险");
            vo.setDeliveryDetail("尚无阶段到期，无法计算");
            vo.setHighRiskCount(0);
            vo.setStaleRiskCount(0);
            return vo;
        }

        // 计算三个维度
        DimensionResult timeResult = calcTimeScore(stages, today);
        DimensionResult riskResult = calcRiskScore(risks);
        DimensionResult deliveryResult = calcDeliveryScore(stages, today);

        vo.setTimeScore(timeResult.score);
        vo.setRiskScore(riskResult.score);
        vo.setDeliveryScore(deliveryResult.score);
        vo.setTimeDetail(timeResult.detail);
        vo.setRiskDetail(riskResult.detail);
        vo.setDeliveryDetail(deliveryResult.detail);

        // 加权计算总分
        int timeWeight = configService.getIntValue("health_weight_time", 35);
        int riskWeight = configService.getIntValue("health_weight_risk", 40);
        int deliveryWeight = configService.getIntValue("health_weight_delivery", 25);
        int healthScore = (timeResult.score * timeWeight + riskResult.score * riskWeight + deliveryResult.score * deliveryWeight) / 100;
        vo.setHealthScore(healthScore);

        // 映射颜色
        int greenMin = configService.getIntValue("health_score_green_min", 80);
        int yellowMin = configService.getIntValue("health_score_yellow_min", 60);
        vo.setHealthColor(healthScore >= greenMin ? "绿" : healthScore >= yellowMin ? "黄" : "红");

        // 风险统计
        vo.setHighRiskCount((int) risks.stream().filter(r -> "高".equals(r.getSeverity())).count());
        vo.setStaleRiskCount((int) risks.stream().filter(r -> Boolean.TRUE.equals(r.getIsStale())).count());

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

    // ======================== 维度计算（带明细） ========================

    private static class DimensionResult {
        int score;
        String detail;
        DimensionResult(int score, String detail) { this.score = score; this.detail = detail; }
    }

    private DimensionResult calcTimeScore(List<ProjectStage> stages, LocalDate today) {
        int penaltyPerDay = configService.getIntValue("time_delay_penalty_per_day", 2);
        int totalDelay = 0;
        int dueCount = 0;
        int delayedCount = 0;

        for (ProjectStage stage : stages) {
            if (stage.getPlanEnd() == null || stage.getPlanEnd().isAfter(today)) continue;
            dueCount++;
            long delayDays = 0;
            if (stage.getActualEnd() != null) {
                delayDays = Math.max(0, ChronoUnit.DAYS.between(stage.getPlanEnd(), stage.getActualEnd()));
            } else {
                delayDays = Math.max(0, ChronoUnit.DAYS.between(stage.getPlanEnd(), today));
            }
            if (delayDays > 0) delayedCount++;
            totalDelay += delayDays;
        }

        int score = Math.max(0, 100 - totalDelay * penaltyPerDay);
        String detail = String.format("共%d个阶段已到期，%d个延期共%d天（每天扣%d分）", dueCount, delayedCount, totalDelay, penaltyPerDay);
        return new DimensionResult(score, detail);
    }

    private DimensionResult calcRiskScore(List<ProjectRisk> risks) {
        int penaltyHigh = configService.getIntValue("risk_penalty_high", 15);
        int penaltyMedium = configService.getIntValue("risk_penalty_medium", 8);
        int penaltyLow = configService.getIntValue("risk_penalty_low", 3);
        int penaltyStale = configService.getIntValue("risk_penalty_stale", 10);

        int high = 0, medium = 0, low = 0, stale = 0;
        for (ProjectRisk r : risks) {
            switch (r.getSeverity()) {
                case "高": high++; break;
                case "中": medium++; break;
                case "低": low++; break;
            }
            if (Boolean.TRUE.equals(r.getIsStale())) stale++;
        }

        int penalty = high * penaltyHigh + medium * penaltyMedium + low * penaltyLow + stale * penaltyStale;
        int score = Math.max(0, 100 - penalty);
        String detail = String.format("高危%d(-%d) 中危%d(-%d) 低危%d(-%d) 停滞%d(-%d)",
                high, high * penaltyHigh, medium, medium * penaltyMedium, low, low * penaltyLow, stale, stale * penaltyStale);
        return new DimensionResult(score, detail);
    }

    private DimensionResult calcDeliveryScore(List<ProjectStage> stages, LocalDate today) {
        long totalDue = 0;
        long onTime = 0;

        for (ProjectStage stage : stages) {
            if (stage.getPlanEnd() == null || stage.getPlanEnd().isAfter(today)) continue;
            totalDue++;
            if (stage.getActualEnd() != null && !stage.getActualEnd().isAfter(stage.getPlanEnd())) {
                onTime++;
            }
        }

        int score = totalDue == 0 ? 100 : (int) (onTime * 100 / totalDue);
        String detail = totalDue == 0 ? "尚无阶段到期" : String.format("%d个阶段已到期，%d个按时完成", totalDue, onTime);
        return new DimensionResult(score, detail);
    }

    /**
     * 时间维度得分(兼容旧调用)
     */
    private int calculateTimeScore(Long projectId) {
        return calcTimeScore(stageService.listByProjectId(projectId), LocalDate.now()).score;
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
