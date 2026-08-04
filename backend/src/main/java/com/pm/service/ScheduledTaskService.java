package com.pm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.ScheduledTask;
import com.pm.model.entity.ScheduledTaskLog;

import java.util.List;

public interface ScheduledTaskService extends IService<ScheduledTask> {
    List<ScheduledTask> listAll();
    void toggleStatus(Long id);
    void updateCron(Long id, String cronExpr);
    void runTask(Long id, Long operatorId);
    Page<ScheduledTaskLog> getLogs(Integer pageNum, Integer pageSize, Long taskId);
    void recordLog(Long taskId, String taskName, String triggerType, String status, String resultMsg, Long executionTime);
}
