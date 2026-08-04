package com.pm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.common.response.R;
import com.pm.model.entity.ScheduledTask;
import com.pm.model.entity.ScheduledTaskLog;
import com.pm.service.ScheduledTaskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scheduled-tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DEPT_MANAGER')")
public class ScheduledTaskController {

    private final ScheduledTaskService taskService;

    @GetMapping
    public R<List<ScheduledTask>> list() {
        return R.ok(taskService.listAll());
    }

    @PutMapping("/{id}/toggle")
    public R<Void> toggle(@PathVariable Long id) {
        taskService.toggleStatus(id);
        return R.ok();
    }

    @PostMapping("/{id}/run")
    public R<Void> run(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        taskService.runTask(id, userId);
        return R.ok();
    }

    @GetMapping("/logs")
    public R<Page<ScheduledTaskLog>> logs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long taskId) {
        return R.ok(taskService.getLogs(pageNum, pageSize, taskId));
    }
}
