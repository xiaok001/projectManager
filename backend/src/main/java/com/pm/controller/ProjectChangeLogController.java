package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.dto.ChangeLogDTO;
import com.pm.model.entity.ProjectChangeLog;
import com.pm.service.ProjectChangeLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/changes")
@RequiredArgsConstructor
public class ProjectChangeLogController {

    private final ProjectChangeLogService changeLogService;

    @PostMapping
    public R<ProjectChangeLog> create(@PathVariable Long projectId, @Valid @RequestBody ChangeLogDTO dto,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(changeLogService.createLog(projectId, dto, userId));
    }

    @GetMapping
    public R<List<ProjectChangeLog>> list(@PathVariable Long projectId) {
        return R.ok(changeLogService.listByProjectId(projectId));
    }
}
