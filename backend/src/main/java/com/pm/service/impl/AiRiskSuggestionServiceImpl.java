package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.constants.Constants;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.AiRiskSuggestionMapper;
import com.pm.model.entity.AiRiskSuggestion;
import com.pm.model.entity.Project;
import com.pm.model.entity.ProjectRisk;
import com.pm.service.AiRiskSuggestionService;
import com.pm.service.ProjectRiskService;
import com.pm.service.ProjectService;
import com.pm.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRiskSuggestionServiceImpl extends ServiceImpl<AiRiskSuggestionMapper, AiRiskSuggestion>
        implements AiRiskSuggestionService {

    private final ProjectService projectService;
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
}
