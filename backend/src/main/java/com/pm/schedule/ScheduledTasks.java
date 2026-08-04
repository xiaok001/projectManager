package com.pm.schedule;

import com.pm.model.entity.ScheduledTask;
import com.pm.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final ProjectStageService stageService;
    private final ProjectRiskService riskService;
    private final ProjectTodoService todoService;
    private final AiRiskSuggestionService suggestionService;
    private final ScheduledTaskService taskService;
    private final EmailDigestService emailDigestService;

    @Scheduled(cron = "0 0 * * * ?")
    public void refreshDelayedStages() {
        executeAndLog("refreshDelayedStages", "阶段延期刷新", () -> {
            stageService.refreshDelayedStatus();
            return "阶段延期刷新完成";
        });
    }

    @Scheduled(cron = "0 30 * * * ?")
    public void refreshStaleRisks() {
        executeAndLog("refreshStaleRisks", "风险停滞刷新", () -> {
            riskService.refreshStaleStatus();
            return "风险停滞刷新完成";
        });
    }

    @Scheduled(cron = "0 15 * * * ?")
    public void refreshOverdueTodos() {
        executeAndLog("refreshOverdueTodos", "待办逾期刷新", () -> {
            todoService.refreshOverdueTodos();
            return "待办逾期刷新完成";
        });
    }

    @Scheduled(cron = "0 0 22 * * ?")
    public void nightlyAiRiskScan() {
        executeAndLog("nightlyAiRiskScan", "AI风险扫描", () -> {
            int count = suggestionService.scanAllProjectsForRisks();
            return "AI风险扫描完成，发现 " + count + " 条新建议";
        });
    }

    /**
     * 每天9:20 项目待办与风险日报
     */
    @Scheduled(cron = "0 20 9 * * ?")
    public void dailyTodoAndRiskDigest() {
        executeAndLog("dailyTodoAndRiskDigest", "项目待办与风险日报", () -> {
            emailDigestService.sendDailyTodoAndRiskDigest();
            return "项目待办与风险日报发送完成";
        });
    }

    /**
     * 统一执行+记录日志（先检查任务是否启用）
     */
    private void executeAndLog(String taskKey, String taskName, TaskAction action) {
        // 检查任务是否启用
        List<ScheduledTask> tasks = taskService.lambdaQuery()
                .eq(ScheduledTask::getTaskKey, taskKey)
                .list();
        if (!tasks.isEmpty() && tasks.get(0).getStatus() != 1) {
            log.info("定时任务已禁用，跳过: {}", taskName);
            return;
        }

        Long taskId = tasks.isEmpty() ? null : tasks.get(0).getId();
        long start = System.currentTimeMillis();
        String resultMsg;
        String status;

        try {
            resultMsg = action.execute();
            status = "成功";
            log.info("定时任务执行成功: {}", taskName);
        } catch (Exception e) {
            resultMsg = "执行失败: " + e.getMessage();
            status = "失败";
            log.error("定时任务执行失败: {}", taskName, e);
        }

        long executionTime = System.currentTimeMillis() - start;
        if (taskId != null) {
            taskService.recordLog(taskId, taskName, "自动", status, resultMsg, executionTime);
        }
    }

    @FunctionalInterface
    interface TaskAction {
        String execute() throws Exception;
    }
}
