package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.dto.StageUpdateDTO;
import com.pm.model.entity.ProjectStage;
import com.pm.service.ProjectStageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectStageController {

    private final ProjectStageService stageService;

    @GetMapping("/projects/{projectId}/stages")
    public R<List<ProjectStage>> listByProject(@PathVariable Long projectId) {
        return R.ok(stageService.listByProjectId(projectId));
    }

    @PutMapping("/stages/{id}")
    public R<ProjectStage> update(@PathVariable Long id, @Valid @RequestBody StageUpdateDTO dto,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ProjectStage stage = stageService.updateStage(id, dto, userId);
        return R.ok(stage);
    }
}
