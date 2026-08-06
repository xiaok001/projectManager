package com.pm.schedule;

import com.pm.model.entity.ScheduledTask;
import com.pm.service.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicScheduler {

    private final TaskScheduler taskScheduler;
    private final ProjectStageService stageService;
    private final ProjectRiskService riskService;
    private final ProjectTodoService todoService;
    private final AiRiskSuggestionService suggestionService;
    private final ScheduledTaskService taskService;
    private final EmailDigestService emailDigestService;

    /** 存储每个任务的 ScheduledFuture，用于取消/重新调度 */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 启动时从数据库加载所有任务并调度
        refreshAll();
        log.info("动态调度器初始化完成，已加载 {} 个任务", scheduledTasks.size());
    }

    /**
     * 刷新所有任务调度（数据库变更后调用）
     */
    public void refreshAll() {
        // 取消所有现有任务
        scheduledTasks.values().forEach(f -> f.cancel(false));
        scheduledTasks.clear();

        // 从数据库加载并调度
        java.util.List<ScheduledTask> tasks = taskService.listAll();
        for (ScheduledTask task : tasks) {
            scheduleTask(task);
        }
    }

    /**
     * 刷新单个任务（Cron更新或状态切换后调用）
     */
    public void refreshTask(Long taskId) {
        ScheduledTask task = taskService.getById(taskId);
        if (task == null) return;

        // 取消旧的
        String key = task.getTaskKey();
        ScheduledFuture<?> existing = scheduledTasks.remove(key);
        if (existing != null) existing.cancel(false);

        // 重新调度
        scheduleTask(task);
    }

    private void scheduleTask(ScheduledTask task) {
        if (task.getStatus() != null && task.getStatus() != 1) {
            log.info("定时任务已禁用，跳过调度: {}", task.getTaskName());
            return;
        }

        String cronExpr = task.getCronExpr();
        if (cronExpr == null || cronExpr.isBlank()) {
            log.warn("定时任务Cron表达式为空，跳过: {}", task.getTaskName());
            return;
        }

        try {
            Runnable runnable = getTaskRunnable(task.getTaskKey(), task.getTaskName());
            if (runnable == null) {
                log.warn("未知任务标识: {}", task.getTaskKey());
                return;
            }

            ScheduledFuture<?> future = taskScheduler.schedule(runnable, new CronTrigger(cronExpr));
            scheduledTasks.put(task.getTaskKey(), future);
            log.info("定时任务已调度: {} [{}]", task.getTaskName(), cronExpr);
        } catch (Exception e) {
            log.error("定时任务调度失败: {} [{}]", task.getTaskName(), cronExpr, e);
        }
    }

    private Runnable getTaskRunnable(String taskKey, String taskName) {
        return () -> {
            Long taskId = null;
            java.util.List<ScheduledTask> tasks = taskService.lambdaQuery()
                    .eq(ScheduledTask::getTaskKey, taskKey).list();
            if (!tasks.isEmpty()) {
                if (tasks.get(0).getStatus() != 1) {
                    log.info("定时任务已禁用，跳过: {}", taskName);
                    return;
                }
                taskId = tasks.get(0).getId();
            }

            long start = System.currentTimeMillis();
            String resultMsg;
            String status;

            try {
                resultMsg = executeTask(taskKey);
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
        };
    }

    private String executeTask(String taskKey) {
        switch (taskKey) {
            case "refreshDelayedStages":
                stageService.refreshDelayedStatus();
                return "阶段延期刷新完成";
            case "refreshStaleRisks":
                riskService.refreshStaleStatus();
                return "风险停滞刷新完成";
            case "refreshOverdueTodos":
                todoService.refreshOverdueTodos();
                return "待办逾期刷新完成";
            case "nightlyAiRiskScan":
                int count = suggestionService.scanAllProjectsForRisks();
                return "AI风险扫描完成，发现 " + count + " 条新建议";
            case "dailyTodoAndRiskDigest":
                emailDigestService.sendDailyTodoAndRiskDigest();
                return "项目待办与风险日报发送完成";
            case "dailyDigest":
                emailDigestService.sendDailyDigest();
                return "每日项目管理摘要发送完成";
            default:
                throw new RuntimeException("未知任务: " + taskKey);
        }
    }
}
