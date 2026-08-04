package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.ScheduledTaskLogMapper;
import com.pm.mapper.ScheduledTaskMapper;
import com.pm.model.entity.ScheduledTask;
import com.pm.model.entity.ScheduledTaskLog;
import com.pm.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskServiceImpl extends ServiceImpl<ScheduledTaskMapper, ScheduledTask>
        implements ScheduledTaskService {

    private final ScheduledTaskLogMapper taskLogMapper;
    private final ProjectStageService stageService;
    private final ProjectRiskService riskService;
    private final ProjectTodoService todoService;
    private final AiRiskSuggestionService suggestionService;

    @Override
    public List<ScheduledTask> listAll() {
        return lambdaQuery().orderByAsc(ScheduledTask::getId).list();
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        ScheduledTask task = getById(id);
        if (task == null) throw new BusinessException("任务不存在");
        task.setStatus(task.getStatus() == 1 ? 0 : 1);
        updateById(task);
        log.info("定时任务状态切换: task={}, status={}", task.getTaskName(), task.getStatus());
    }

    @Override
    public void runTask(Long id, Long operatorId) {
        ScheduledTask task = getById(id);
        if (task == null) throw new BusinessException("任务不存在");

        long start = System.currentTimeMillis();
        String resultMsg;
        String status;

        try {
            switch (task.getTaskKey()) {
                case "refreshDelayedStages":
                    stageService.refreshDelayedStatus();
                    resultMsg = "阶段延期刷新完成";
                    break;
                case "refreshStaleRisks":
                    riskService.refreshStaleStatus();
                    resultMsg = "风险停滞刷新完成";
                    break;
                case "refreshOverdueTodos":
                    todoService.refreshOverdueTodos();
                    resultMsg = "待办逾期刷新完成";
                    break;
                case "nightlyAiRiskScan":
                    int count = suggestionService.scanAllProjectsForRisks();
                    resultMsg = "AI风险扫描完成，发现 " + count + " 条新建议";
                    break;
                default:
                    throw new BusinessException("未知任务: " + task.getTaskKey());
            }
            status = "成功";
        } catch (Exception e) {
            resultMsg = "执行失败: " + e.getMessage();
            status = "失败";
            log.error("手动执行任务失败: {}", task.getTaskName(), e);
        }

        long executionTime = System.currentTimeMillis() - start;
        recordLog(id, task.getTaskName(), "手动", status, resultMsg, executionTime);
    }

    @Override
    public Page<ScheduledTaskLog> getLogs(Integer pageNum, Integer pageSize, Long taskId) {
        Page<ScheduledTaskLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ScheduledTaskLog> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(ScheduledTaskLog::getTaskId, taskId);
        }
        wrapper.orderByDesc(ScheduledTaskLog::getExecutedAt);
        return taskLogMapper.selectPage(page, wrapper);
    }

    @Override
    public void recordLog(Long taskId, String taskName, String triggerType, String status, String resultMsg, Long executionTime) {
        ScheduledTaskLog logEntry = new ScheduledTaskLog();
        logEntry.setTaskId(taskId);
        logEntry.setTaskName(taskName);
        logEntry.setTriggerType(triggerType);
        logEntry.setStatus(status);
        logEntry.setResultMsg(resultMsg);
        logEntry.setExecutionTime(executionTime);
        logEntry.setExecutedAt(LocalDateTime.now());
        taskLogMapper.insert(logEntry);
    }
}
