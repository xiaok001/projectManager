package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.mapper.ProjectChangeLogMapper;
import com.pm.mapper.SysUserMapper;
import com.pm.model.dto.ChangeLogDTO;
import com.pm.model.entity.ProjectChangeLog;
import com.pm.model.entity.SysUser;
import com.pm.service.ProjectChangeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectChangeLogServiceImpl extends ServiceImpl<ProjectChangeLogMapper, ProjectChangeLog>
        implements ProjectChangeLogService {

    private final SysUserMapper sysUserMapper;

    @Override
    public ProjectChangeLog createLog(Long projectId, ChangeLogDTO dto, Long operatorId) {
        ProjectChangeLog changeLog = new ProjectChangeLog();
        changeLog.setProjectId(projectId);
        changeLog.setChangeType(dto.getChangeType());
        changeLog.setChangeField(dto.getChangeField());
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
        List<ProjectChangeLog> list = lambdaQuery()
                .eq(ProjectChangeLog::getProjectId, projectId)
                .orderByDesc(ProjectChangeLog::getChangedAt)
                .list();

        // 填充操作人姓名
        List<Long> userIds = list.stream()
                .map(ProjectChangeLog::getChangedBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (!userIds.isEmpty()) {
            Map<Long, String> nameMap = sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            list.forEach(log -> {
                if (log.getChangedBy() != null) {
                    log.setChangedByName(nameMap.get(log.getChangedBy()));
                }
            });
        }

        return list;
    }
}
