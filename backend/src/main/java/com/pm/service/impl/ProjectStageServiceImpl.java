package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.constants.Constants;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.ProjectMapper;
import com.pm.mapper.ProjectStageMapper;
import com.pm.model.dto.StageUpdateDTO;
import com.pm.model.entity.Project;
import com.pm.model.entity.ProjectStage;
import com.pm.service.AiRiskSuggestionService;
import com.pm.service.ProjectStageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ProjectStageServiceImpl extends ServiceImpl<ProjectStageMapper, ProjectStage>
        implements ProjectStageService {

    private final ProjectMapper projectMapper;
    private final AiRiskSuggestionService aiRiskSuggestionService;

    public ProjectStageServiceImpl(ProjectMapper projectMapper,
                                   @Lazy AiRiskSuggestionService aiRiskSuggestionService) {
        this.projectMapper = projectMapper;
        this.aiRiskSuggestionService = aiRiskSuggestionService;
    }

    @Override
    public List<ProjectStage> listByProjectId(Long projectId) {
        return lambdaQuery()
                .eq(ProjectStage::getProjectId, projectId)
                .orderByAsc(ProjectStage::getSortOrder)
                .list();
    }

    @Override
    public ProjectStage updateStage(Long id, StageUpdateDTO dto, Long operatorId) {
        ProjectStage stage = getById(id);
        if (stage == null) {
            throw new BusinessException("阶段不存在");
        }

        if (dto.getStageName() != null) stage.setStageName(dto.getStageName());
        if (dto.getPlanStart() != null) stage.setPlanStart(dto.getPlanStart());
        if (dto.getPlanEnd() != null) stage.setPlanEnd(dto.getPlanEnd());
        if (dto.getActualStart() != null) stage.setActualStart(dto.getActualStart());
        if (dto.getActualEnd() != null) stage.setActualEnd(dto.getActualEnd());
        if (dto.getStatus() != null) stage.setStatus(dto.getStatus());
        if (dto.getRemark() != null) stage.setRemark(dto.getRemark());
        if (dto.getPlanManDays() != null) stage.setPlanManDays(dto.getPlanManDays());
        if (dto.getActualManDays() != null) stage.setActualManDays(dto.getActualManDays());
        if (dto.getPlanCost() != null) stage.setPlanCost(dto.getPlanCost());
        if (dto.getActualCost() != null) stage.setActualCost(dto.getActualCost());
        if (dto.getProgress() != null) stage.setProgress(dto.getProgress());
        stage.setUpdatedBy(operatorId);

        // 根据进度自动联动状态
        if (dto.getProgress() != null) {
            int progress = dto.getProgress();
            if (progress >= 100) {
                stage.setProgress(100);
                stage.setStatus(Constants.STAGE_COMPLETED);
                // 自动填写实际结束日期
                if (stage.getActualEnd() == null) {
                    stage.setActualEnd(LocalDate.now());
                }
            } else if (progress > 0) {
                stage.setStatus(Constants.STAGE_IN_PROGRESS);
                // 自动填写实际开始日期
                if (stage.getActualStart() == null) {
                    stage.setActualStart(LocalDate.now());
                }
            } else if (progress == 0) {
                if (stage.getActualStart() == null) {
                    stage.setStatus(Constants.STAGE_NOT_STARTED);
                }
            }
        }

        // 进度 < 100 时清除实际结束日期（允许回退进度）
        if (stage.getProgress() != null && stage.getProgress() < 100) {
            stage.setActualEnd(null);
        }

        updateById(stage);

        // 更新项目的当前阶段
        updateProjectCurrentStage(stage.getProjectId());

        // 异步触发AI风险探测
        if (dto.getRemark() != null && !dto.getRemark().trim().isEmpty()) {
            triggerAiRiskDetection(stage.getProjectId(), id, dto.getRemark());
        }

        return stage;
    }

    @Override
    public void initStagesForProject(Long projectId) {
        String[] stageNames = Constants.DEFAULT_STAGES;
        for (int i = 0; i < stageNames.length; i++) {
            ProjectStage stage = new ProjectStage();
            stage.setProjectId(projectId);
            stage.setStageName(stageNames[i]);
            stage.setSortOrder(i);
            stage.setStatus(Constants.STAGE_NOT_STARTED);
            save(stage);
        }
    }

    @Override
    public void refreshDelayedStatus() {
        LocalDate today = LocalDate.now();
        List<ProjectStage> stages = lambdaQuery()
                .ne(ProjectStage::getStatus, Constants.STAGE_COMPLETED)
                .isNotNull(ProjectStage::getPlanEnd)
                .le(ProjectStage::getPlanEnd, today)
                .list();

        for (ProjectStage stage : stages) {
            // 已填写实际结束日期的不再判定延期
            if (stage.getActualEnd() != null) continue;

            String newStatus = stage.getActualStart() != null ? Constants.STAGE_DELAYED : Constants.STAGE_DELAYED;
            if (!newStatus.equals(stage.getStatus())) {
                stage.setStatus(Constants.STAGE_DELAYED);
                updateById(stage);
            }
        }
        log.info("刷新延期状态完成，更新{}条记录", stages.size());
    }

    private void updateProjectCurrentStage(Long projectId) {
        List<ProjectStage> stages = listByProjectId(projectId);
        String currentStage = stages.stream()
                .filter(s -> Constants.STAGE_IN_PROGRESS.equals(s.getStatus()) || Constants.STAGE_DELAYED.equals(s.getStatus()))
                .map(ProjectStage::getStageName)
                .findFirst()
                .orElse(null);

        // 直接用Mapper更新，避免依赖ProjectService导致循环依赖
        Project project = projectMapper.selectById(projectId);
        if (project != null) {
            project.setCurrentStage(currentStage);
            projectMapper.updateById(project);
        }
    }

    @Async
    protected void triggerAiRiskDetection(Long projectId, Long stageId, String remark) {
        try {
            aiRiskSuggestionService.analyzeRemark(projectId, stageId, remark);
        } catch (Exception e) {
            log.error("AI风险探测失败, projectId={}, stageId={}", projectId, stageId, e);
        }
    }
}
