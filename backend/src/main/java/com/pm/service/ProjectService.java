package com.pm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.Project;
import com.pm.model.dto.ProjectDTO;

import java.util.List;

public interface ProjectService extends IService<Project> {
    Project createProject(ProjectDTO dto, Long operatorId);
    Project updateProject(Long id, ProjectDTO dto, Long operatorId);
    List<Project> listProjects(Long userId, String role);
    Project getProjectDetail(Long id, Long userId, String role);
    void updateSatisfaction(Long id, Integer score, Long userId, String role);
}
