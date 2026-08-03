package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.constants.Constants;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.*;
import com.pm.model.dto.ProjectDTO;
import com.pm.model.entity.*;
import com.pm.service.ProjectService;
import com.pm.service.ProjectStageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectStageService projectStageService;
    private final SysUserMapper sysUserMapper;
    private final ProjectStageMapper projectStageMapper;
    private final ProjectRiskMapper projectRiskMapper;
    private final ProjectChangeLogMapper changeLogMapper;
    private final AiRiskSuggestionMapper aiSuggestionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Project createProject(ProjectDTO dto, Long operatorId) {
        // Validate project_code uniqueness
        long count = lambdaQuery()
                .eq(Project::getProjectCode, dto.getProjectCode())
                .count();
        if (count > 0) {
            throw new BusinessException("项目编号已存在: " + dto.getProjectCode());
        }

        // Build and save project
        Project project = new Project();
        project.setProjectCode(dto.getProjectCode());
        project.setName(dto.getName());
        project.setType(dto.getType());
        project.setLevel(dto.getLevel());
        project.setAmount(dto.getAmount());
        project.setPmId(dto.getPmId());
        project.setStartDate(dto.getStartDate());
        project.setStatus(Constants.STATUS_ACTIVE);
        save(project);

        // Initialize default stages for this project
        projectStageService.initStagesForProject(project.getId());

        log.info("Project created: id={}, code={}, operatorId={}", project.getId(), project.getProjectCode(), operatorId);
        return project;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Project updateProject(Long id, ProjectDTO dto, Long operatorId) {
        Project project = getById(id);
        if (project == null) {
            throw new BusinessException("项目不存在: " + id);
        }

        // Check permission: only the PM of this project or a DEPT_MANAGER can update
        checkUpdatePermission(project, operatorId);

        // Update fields from DTO
        project.setProjectCode(dto.getProjectCode());
        project.setName(dto.getName());
        project.setType(dto.getType());
        project.setLevel(dto.getLevel());
        project.setAmount(dto.getAmount());
        project.setPmId(dto.getPmId());
        project.setStartDate(dto.getStartDate());
        updateById(project);

        log.info("Project updated: id={}, operatorId={}", id, operatorId);
        return project;
    }

    @Override
    public List<Project> listProjects(Long userId, String role) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();

        // DEPT_MANAGER sees all projects; PM sees only their own
        if (!Constants.ROLE_DEPT_MANAGER.equals(role)) {
            wrapper.eq(Project::getPmId, userId);
        }
        wrapper.orderByDesc(Project::getCreatedAt);

        List<Project> projects = list(wrapper);

        // Populate pmName by joining with sys_user
        fillPmNames(projects);
        return projects;
    }

    @Override
    public Project getProjectDetail(Long id, Long userId, String role) {
        Project project = getById(id);
        if (project == null) {
            throw new BusinessException("项目不存在: " + id);
        }

        // Check permission: DEPT_MANAGER can view all; PM can only view own projects
        checkViewPermission(project, userId, role);

        // Populate pmName
        fillPmName(project);
        return project;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSatisfaction(Long id, Integer score, Long userId, String role) {
        Project project = getById(id);
        if (project == null) {
            throw new BusinessException("项目不存在: " + id);
        }

        // Check permission
        checkViewPermission(project, userId, role);

        // Update only satisfaction_score
        lambdaUpdate()
                .eq(Project::getId, id)
                .set(Project::getSatisfactionScore, score)
                .update();

        log.info("Satisfaction updated: projectId={}, score={}, operatorId={}", id, score, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id, Long userId, String role) {
        Project project = getById(id);
        if (project == null) {
            throw new BusinessException("项目不存在: " + id);
        }
        checkViewPermission(project, userId, role);

        // 级联删除：阶段、风险、变更记录、AI建议
        projectStageMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectStage>()
                .eq(ProjectStage::getProjectId, id));
        projectRiskMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectRisk>()
                .eq(ProjectRisk::getProjectId, id));
        changeLogMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectChangeLog>()
                .eq(ProjectChangeLog::getProjectId, id));
        aiSuggestionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiRiskSuggestion>()
                .eq(AiRiskSuggestion::getProjectId, id));

        removeById(id);
        log.info("Project deleted: id={}, operatorId={}", id, userId);
    }

    // ======================== Permission Helpers ========================

    /**
     * Check if the operator has update permission on the project.
     * Only the PM of the project or a DEPT_MANAGER is allowed.
     */
    private void checkUpdatePermission(Project project, Long operatorId) {
        SysUser operator = sysUserMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException("操作用户不存在: " + operatorId);
        }

        if (Constants.ROLE_DEPT_MANAGER.equals(operator.getRole())) {
            return; // DEPT_MANAGER can update any project
        }

        if (project.getPmId() != null && project.getPmId().equals(operatorId)) {
            return; // PM can update their own project
        }

        throw new BusinessException(403, "无权限操作此项目");
    }

    /**
     * Check if the operator has view permission on the project.
     * DEPT_MANAGER can view all; PM can only view their own.
     */
    private void checkViewPermission(Project project, Long userId, String role) {
        if (Constants.ROLE_DEPT_MANAGER.equals(role)) {
            return;
        }

        if (Constants.ROLE_PM.equals(role) && project.getPmId() != null && project.getPmId().equals(userId)) {
            return;
        }

        throw new BusinessException(403, "无权限查看此项目");
    }

    // ======================== PM Name Helpers ========================

    /**
     * Fill pmName for a list of projects by batch-loading the related SysUser records.
     */
    private void fillPmNames(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }

        // Collect distinct pmIds
        List<Long> pmIds = projects.stream()
                .map(Project::getPmId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (pmIds.isEmpty()) {
            return;
        }

        // Batch query PM users
        List<SysUser> pmUsers = sysUserMapper.selectBatchIds(pmIds);
        java.util.Map<Long, String> pmNameMap = new java.util.HashMap<>();
        for (SysUser user : pmUsers) {
            pmNameMap.put(user.getId(), user.getRealName());
        }

        // Set pmName on each project
        for (Project project : projects) {
            if (project.getPmId() != null) {
                project.setPmName(pmNameMap.get(project.getPmId()));
            }
        }
    }

    /**
     * Fill pmName for a single project.
     */
    private void fillPmName(Project project) {
        if (project.getPmId() != null) {
            SysUser pm = sysUserMapper.selectById(project.getPmId());
            if (pm != null) {
                project.setPmName(pm.getRealName());
            }
        }
    }
}
