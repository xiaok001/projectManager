package com.pm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.ProjectChangeLog;
import com.pm.model.dto.ChangeLogDTO;

import java.util.List;

public interface ProjectChangeLogService extends IService<ProjectChangeLog> {
    ProjectChangeLog createLog(Long projectId, ChangeLogDTO dto, Long operatorId);
    List<ProjectChangeLog> listByProjectId(Long projectId);
}
