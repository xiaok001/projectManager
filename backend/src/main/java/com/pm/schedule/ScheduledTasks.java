package com.pm.schedule;

import com.pm.service.AiRiskSuggestionService;
import com.pm.service.ProjectRiskService;
import com.pm.service.ProjectStageService;
import com.pm.service.ProjectTodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final ProjectStageService stageService;
    private final ProjectRiskService riskService;
    private final AiRiskSuggestionService suggestionService;
    private final ProjectTodoService todoService;

    @Scheduled(cron = "0 0 * * * ?")
    public void refreshDelayedStages() {
        log.info("定时任务: 刷新阶段延期状态");
        stageService.refreshDelayedStatus();
    }

    @Scheduled(cron = "0 30 * * * ?")
    public void refreshStaleRisks() {
        log.info("定时任务: 刷新风险停滞状态");
        riskService.refreshStaleStatus();
    }

    /**
     * 每小时刷新待办逾期状态，逾期自动创建风险
     */
    @Scheduled(cron = "0 15 * * * ?")
    public void refreshOverdueTodos() {
        log.info("定时任务: 刷新待办逾期状态");
        todoService.refreshOverdueTodos();
    }

    /**
     * 每晚22:00自动扫描所有项目，AI识别潜在风险
     */
    @Scheduled(cron = "0 0 22 * * ?")
    public void nightlyAiRiskScan() {
        log.info("定时任务: 每晚AI风险扫描开始");
        try {
            int count = suggestionService.scanAllProjectsForRisks();
            log.info("定时任务: AI风险扫描完成，发现 {} 条新建议", count);
        } catch (Exception e) {
            log.error("定时任务: AI风险扫描失败", e);
        }
    }
}
