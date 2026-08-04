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
import com.pm.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectStageService projectStageService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleService roleService;
    private final SysRoleProjectMapper roleProjectMapper;
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
        project.setExpectedEndDate(dto.getExpectedEndDate());
        project.setWbsOnlineUrl(dto.getWbsOnlineUrl());
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
        project.setExpectedEndDate(dto.getExpectedEndDate());
        if (dto.getWbsOnlineUrl() != null) project.setWbsOnlineUrl(dto.getWbsOnlineUrl());

        // 状态变更校验：如果要改为"已完成"，检查是否有未完成的阶段
        if (dto.getStatus() != null && "已完成".equals(dto.getStatus())
                && !"已完成".equals(project.getStatus())) {
            List<ProjectStage> stages = projectStageService.listByProjectId(id);
            boolean hasIncomplete = stages.stream()
                    .filter(s -> !"运维".equals(s.getStageName()))
                    .anyMatch(s -> !"已完成".equals(s.getStatus()));
            if (hasIncomplete) {
                throw new BusinessException("该项目存在未完成的阶段（运维阶段除外），请先完成所有阶段或在阶段备注中说明原因后再结束项目");
            }
        }
        if (dto.getStatus() != null) {
            project.setStatus(dto.getStatus());
        }

        updateById(project);

        log.info("Project updated: id={}, operatorId={}", id, operatorId);
        return project;
    }

    @Override
    public List<Project> listProjects(Long userId, String role) {
        return listProjects(userId, role, null, null, null);
    }

    @Override
    public List<Project> listProjects(Long userId, String role, String name, String projectCode, Integer level) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();

        // 数据权限过滤
        if (!Constants.ROLE_DEPT_MANAGER.equals(role)) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null && user.getRoleId() != null) {
                SysRole userRole = roleService.getById(user.getRoleId());
                if (userRole != null && "custom".equals(userRole.getDataScope())) {
                    List<Long> projectIds = roleService.getRoleProjectIds(user.getRoleId());
                    if (projectIds.isEmpty()) return new java.util.ArrayList<>();
                    wrapper.in(Project::getId, projectIds);
                } else {
                    wrapper.eq(Project::getPmId, userId);
                }
            } else {
                wrapper.eq(Project::getPmId, userId);
            }
        }

        // 查询条件
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Project::getName, name.trim());
        }
        if (projectCode != null && !projectCode.trim().isEmpty()) {
            wrapper.like(Project::getProjectCode, projectCode.trim());
        }
        if (level != null) {
            wrapper.eq(Project::getLevel, level);
        }

        wrapper.orderByDesc(Project::getCreatedAt);

        List<Project> projects = list(wrapper);
        fillPmNames(projects);
        fillOpsStartDates(projects);
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

    /**
     * 填充运维阶段的开始日期，作为项目结束日期展示
     */
    private void fillOpsStartDates(List<Project> projects) {
        if (projects == null || projects.isEmpty()) return;

        List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());

        // 批量查询所有项目的「运维」阶段
        List<ProjectStage> opsStages = projectStageMapper.selectList(
                new LambdaQueryWrapper<ProjectStage>()
                        .in(ProjectStage::getProjectId, projectIds)
                        .eq(ProjectStage::getStageName, "运维"));

        Map<Long, LocalDate> opsDateMap = opsStages.stream()
                .filter(s -> s.getActualStart() != null)
                .collect(Collectors.toMap(ProjectStage::getProjectId, ProjectStage::getActualStart, (a, b) -> a));

        for (Project project : projects) {
            project.setOpsStartDate(opsDateMap.get(project.getId()));
        }
    }
}
