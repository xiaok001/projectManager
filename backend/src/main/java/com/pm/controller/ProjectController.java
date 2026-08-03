package com.pm.controller;

import com.pm.common.exception.BusinessException;
import com.pm.common.response.R;
import com.pm.model.dto.ProjectDTO;
import com.pm.model.entity.Project;
import com.pm.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public R<Project> create(@Valid @RequestBody ProjectDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Project project = projectService.createProject(dto, userId);
        return R.ok(project);
    }

    @GetMapping
    public R<List<Project>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(projectService.listProjects(userId, role));
    }

    @GetMapping("/{id}")
    public R<Project> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(projectService.getProjectDetail(id, userId, role));
    }

    @PutMapping("/{id}")
    public R<Project> update(@PathVariable Long id, @Valid @RequestBody ProjectDTO dto,
                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(projectService.updateProject(id, dto, userId));
    }

    @PutMapping("/{id}/satisfaction")
    public R<Void> updateSatisfaction(@PathVariable Long id, @RequestBody Map<String, Integer> body,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        Integer score = body.get("score");
        if (score == null || score < 1 || score > 10) {
            throw new BusinessException("满意度分数必须在1-10之间");
        }
        projectService.updateSatisfaction(id, score, userId, role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        projectService.deleteProject(id, userId, role);
        return R.ok();
    }
}
