package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.constants.Constants;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.AiRiskSuggestionMapper;
import com.pm.model.entity.AiRiskSuggestion;
import com.pm.model.entity.Project;
import com.pm.model.entity.ProjectRisk;
import com.pm.model.entity.ProjectStage;
import com.pm.model.vo.AiSuggestionVO;
import com.pm.service.AiRiskSuggestionService;
import com.pm.service.ProjectRiskService;
import com.pm.service.ProjectService;
import com.pm.service.ProjectStageService;
import com.pm.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRiskSuggestionServiceImpl extends ServiceImpl<AiRiskSuggestionMapper, AiRiskSuggestion>
        implements AiRiskSuggestionService {

    private final ProjectService projectService;
    private final ProjectStageService stageService;
    private final ProjectRiskService riskService;
    private final AiProvider aiProvider;

    @Override
    public List<AiRiskSuggestion> listByStatus(String status, Long userId, String role) {
        LambdaQueryWrapper<AiRiskSuggestion> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AiRiskSuggestion::getStatus, status);
        }

        // PM只能看自己项目的建议
        if (Constants.ROLE_PM.equals(role)) {
            List<Project> projects = projectService.listProjects(userId, role);
            List<Long> projectIds = new ArrayList<>();
            for (Project p : projects) {
                projectIds.add(p.getId());
            }
            if (projectIds.isEmpty()) return new ArrayList<>();
            wrapper.in(AiRiskSuggestion::getProjectId, projectIds);
        }

        return list(wrapper.orderByDesc(AiRiskSuggestion::getCreatedAt));
    }

    @Override
    public Page<AiSuggestionVO> pageList(Integer pageNum, Integer pageSize, String status,
                                          String projectCode, String projectName,
                                          String startDate, String endDate,
                                          Long userId, String role) {
        Page<AiRiskSuggestion> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiRiskSuggestion> wrapper = new LambdaQueryWrapper<>();

        // 状态过滤
        if (StringUtils.hasText(status)) {
            wrapper.eq(AiRiskSuggestion::getStatus, status);
        }

        // 时间范围过滤
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(AiRiskSuggestion::getCreatedAt, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(AiRiskSuggestion::getCreatedAt, endDate + " 23:59:59");
        }

        // PM只能看自己项目的建议
        List<Long> allowedProjectIds = null;
        if (Constants.ROLE_PM.equals(role)) {
            List<Project> projects = projectService.listProjects(userId, role);
            allowedProjectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
            if (allowedProjectIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
            wrapper.in(AiRiskSuggestion::getProjectId, allowedProjectIds);
        }

        wrapper.orderByDesc(AiRiskSuggestion::getCreatedAt);
        Page<AiRiskSuggestion> resultPage = page(page, wrapper);

        // 获取关联的项目信息
        List<Long> projectIds = resultPage.getRecords().stream()
                .map(AiRiskSuggestion::getProjectId).distinct().collect(Collectors.toList());

        Map<Long, Project> projectMap = new java.util.HashMap<>();
        if (!projectIds.isEmpty()) {
            List<Project> projects = projectService.listByIds(projectIds);
            projectMap = projects.stream().collect(Collectors.toMap(Project::getId, p -> p));
        }

        // 组装VO，按项目编号/名称过滤
        Map<Long, Project> finalProjectMap = projectMap;
        List<AiSuggestionVO> voList = new ArrayList<>();
        for (AiRiskSuggestion s : resultPage.getRecords()) {
            Project p = finalProjectMap.get(s.getProjectId());
            if (p == null) continue;
            // 项目编号/名称过滤（后端过滤）
            if (StringUtils.hasText(projectCode) && !p.getProjectCode().contains(projectCode)) continue;
            if (StringUtils.hasText(projectName) && !p.getName().contains(projectName)) continue;

            AiSuggestionVO vo = new AiSuggestionVO();
            vo.setId(s.getId());
            vo.setProjectId(s.getProjectId());
            vo.setProjectCode(p.getProjectCode());
            vo.setProjectName(p.getName());
            vo.setStageId(s.getStageId());
            vo.setSourceText(s.getSourceText());
            vo.setSuggestedRiskDesc(s.getSuggestedRiskDesc());
            vo.setStatus(s.getStatus());
            vo.setCreatedAt(s.getCreatedAt());
            voList.add(vo);
        }

        // 构建返回的分页对象
        Page<AiSuggestionVO> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public void accept(Long id, Long operatorId) {
        AiRiskSuggestion suggestion = getById(id);
        if (suggestion == null) {
            throw new BusinessException("建议不存在");
        }
        if (!"待确认".equals(suggestion.getStatus())) {
            throw new BusinessException("该建议已被处理");
        }

        // 创建正式风险记录
        ProjectRisk risk = new ProjectRisk();
        risk.setRiskCode(riskService.generateRiskCode(suggestion.getProjectId()));
        risk.setProjectId(suggestion.getProjectId());
        risk.setDescription(suggestion.getSuggestedRiskDesc());
        risk.setType("风险");
        risk.setSeverity("中");
        risk.setStatus(Constants.RISK_PENDING);
        risk.setIsStale(false);
        risk.setUpdatedBy(operatorId);
        riskService.save(risk);

        // 更新建议状态
        suggestion.setStatus("已采纳");
        updateById(suggestion);
    }

    @Override
    public void ignore(Long id) {
        AiRiskSuggestion suggestion = getById(id);
        if (suggestion == null) {
            throw new BusinessException("建议不存在");
        }
        suggestion.setStatus("已忽略");
        updateById(suggestion);
    }

    @Override
    @Async
    public void analyzeRemark(Long projectId, Long stageId, String remark) {
        try {
            String prompt = "请分析以下项目阶段备注文本，判断是否存在潜在的项目风险。"
                    + "如果发现风险，返回风险描述；如果没有风险，返回'无风险'。\n\n"
                    + "备注内容: " + remark;

            String result = aiProvider.chat(prompt);

            if (result != null && !result.contains("无风险") && !result.trim().isEmpty()) {
                AiRiskSuggestion suggestion = new AiRiskSuggestion();
                suggestion.setProjectId(projectId);
                suggestion.setStageId(stageId);
                suggestion.setSourceText(remark);
                suggestion.setSuggestedRiskDesc(result.trim());
                suggestion.setStatus("待确认");
                save(suggestion);
                log.info("AI探测到隐性风险, projectId={}, stageId={}", projectId, stageId);
            }
        } catch (Exception e) {
            log.error("AI风险探测调用失败, projectId={}, stageId={}", projectId, stageId, e);
        }
    }

    @Override
    public int scanAllProjectsForRisks() {
        log.info("开始全量扫描项目风险...");
        int newCount = 0;

        // 获取所有进行中的项目
        List<Project> projects = projectService.lambdaQuery()
                .eq(Project::getStatus, "进行中")
                .list();

        // 获取已有的AI建议的(stageId+sourceText)用于去重
        List<AiRiskSuggestion> existing = lambdaQuery()
                .ne(AiRiskSuggestion::getStatus, "已忽略")
                .list();
        java.util.Set<String> existingKeys = new java.util.HashSet<>();
        for (AiRiskSuggestion s : existing) {
            existingKeys.add(s.getStageId() + ":" + s.getSourceText().hashCode());
        }

        for (Project project : projects) {
            List<ProjectStage> stages = stageService.lambdaQuery()
                    .eq(ProjectStage::getProjectId, project.getId())
                    .isNotNull(ProjectStage::getRemark)
                    .ne(ProjectStage::getRemark, "")
                    .list();

            for (ProjectStage stage : stages) {
                String remark = stage.getRemark();
                if (remark == null || remark.trim().isEmpty()) continue;

                String key = stage.getId() + ":" + remark.hashCode();
                if (existingKeys.contains(key)) continue;

                try {
                    String prompt = "请分析以下项目阶段备注文本，判断是否存在潜在的项目风险。"
                            + "如果发现风险，返回风险描述；如果没有风险，返回'无风险'。\n\n"
                            + "项目: " + project.getName() + "\n阶段: " + stage.getStageName()
                            + "\n备注: " + remark;

                    String result = aiProvider.chat(prompt);
                    if (result != null && !result.contains("无风险") && !result.trim().isEmpty()) {
                        AiRiskSuggestion suggestion = new AiRiskSuggestion();
                        suggestion.setProjectId(project.getId());
                        suggestion.setStageId(stage.getId());
                        suggestion.setSourceText(remark);
                        suggestion.setSuggestedRiskDesc(result.trim());
                        suggestion.setStatus("待确认");
                        save(suggestion);
                        existingKeys.add(key);
                        newCount++;
                        log.info("扫描发现风险: project={}, stage={}", project.getName(), stage.getStageName());
                    }
                } catch (Exception e) {
                    log.error("扫描阶段风险失败: projectId={}, stageId={}", project.getId(), stage.getId(), e);
                }
            }
        }
        log.info("全量扫描完成，共发现 {} 条新风险建议", newCount);
        return newCount;
    }
}
