package com.pm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.common.response.R;
import com.pm.model.dto.TodoDTO;
import com.pm.model.entity.ProjectTodo;
import com.pm.service.ProjectTodoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectTodoController {

    private final ProjectTodoService todoService;

    @GetMapping("/projects/{projectId}/todos")
    public R<List<ProjectTodo>> list(@PathVariable Long projectId) {
        return R.ok(todoService.listByProjectId(projectId));
    }

    @GetMapping("/todos/page")
    public R<Page<ProjectTodo>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(todoService.pageList(pageNum, pageSize, projectId, status, priority, keyword, userId, role));
    }

    @PostMapping("/todos")
    public R<ProjectTodo> createGlobal(@Valid @RequestBody TodoDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (dto.getProjectId() == null) {
            throw new com.pm.common.exception.BusinessException("请选择项目");
        }
        return R.ok(todoService.createTodo(dto.getProjectId(), dto, userId));
    }

    @PostMapping("/projects/{projectId}/todos")
    public R<ProjectTodo> create(@PathVariable Long projectId, @Valid @RequestBody TodoDTO dto,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(todoService.createTodo(projectId, dto, userId));
    }

    @PutMapping("/todos/{id}")
    public R<ProjectTodo> update(@PathVariable Long id, @Valid @RequestBody TodoDTO dto,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(todoService.updateTodo(id, dto, userId));
    }

    @DeleteMapping("/todos/{id}")
    public R<Void> delete(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return R.ok();
    }
}
