package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.ProjectChangeLogMapper;
import com.pm.model.dto.ChangeLogDTO;
import com.pm.model.entity.ProjectChangeLog;
import com.pm.service.ProjectChangeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectChangeLogServiceImpl extends ServiceImpl<ProjectChangeLogMapper, ProjectChangeLog>
        implements ProjectChangeLogService {

    @Override
    public ProjectChangeLog createLog(Long projectId, ChangeLogDTO dto, Long operatorId) {
        ProjectChangeLog changeLog = new ProjectChangeLog();
        changeLog.setProjectId(projectId);
        changeLog.setChangeType(dto.getChangeType());
        changeLog.setChangeDesc(dto.getChangeDesc());
        changeLog.setBeforeValue(dto.getBeforeValue());
        changeLog.setAfterValue(dto.getAfterValue());
        changeLog.setChangedBy(operatorId);
        changeLog.setChangedAt(LocalDateTime.now());

        save(changeLog);
        return changeLog;
    }

    @Override
    public List<ProjectChangeLog> listByProjectId(Long projectId) {
        return lambdaQuery()
                .eq(ProjectChangeLog::getProjectId, projectId)
                .orderByDesc(ProjectChangeLog::getChangedAt)
                .list();
    }
}
