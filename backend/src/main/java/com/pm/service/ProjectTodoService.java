package com.pm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.dto.TodoDTO;
import com.pm.model.entity.ProjectTodo;

import java.util.List;

public interface ProjectTodoService extends IService<ProjectTodo> {
    List<ProjectTodo> listByProjectId(Long projectId);
    Page<ProjectTodo> pageList(Integer pageNum, Integer pageSize, Long projectId,
                                String status, String priority, String keyword, Long userId, String role);
    ProjectTodo createTodo(Long projectId, TodoDTO dto, Long operatorId);
    ProjectTodo updateTodo(Long id, TodoDTO dto, Long operatorId);
    void deleteTodo(Long id);
    void refreshOverdueTodos();
    String generateTodoCode(Long projectId);
}
