package com.pm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.ProjectStage;
import com.pm.model.dto.StageUpdateDTO;

import java.util.List;

public interface ProjectStageService extends IService<ProjectStage> {
    List<ProjectStage> listByProjectId(Long projectId);
    ProjectStage updateStage(Long id, StageUpdateDTO dto, Long operatorId);
    void initStagesForProject(Long projectId);
    void refreshDelayedStatus();
}
