package com.pm.schedule;

import com.pm.service.ProjectRiskService;
import com.pm.service.ProjectStageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务调度器
 * - 刷新阶段延期状态(每小时)
 * - 刷新风险停滞状态(每小时)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final ProjectStageService stageService;
    private final ProjectRiskService riskService;

    /**
     * 每小时刷新阶段延期状态
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void refreshDelayedStages() {
        log.info("定时任务: 刷新阶段延期状态");
        stageService.refreshDelayedStatus();
    }

    /**
     * 每小时刷新风险停滞状态
     */
    @Scheduled(cron = "0 30 * * * ?")
    public void refreshStaleRisks() {
        log.info("定时任务: 刷新风险停滞状态");
        riskService.refreshStaleStatus();
    }
}
