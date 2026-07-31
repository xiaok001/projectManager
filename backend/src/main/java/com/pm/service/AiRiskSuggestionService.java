package com.pm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.AiRiskSuggestion;

import java.util.List;

public interface AiRiskSuggestionService extends IService<AiRiskSuggestion> {
    List<AiRiskSuggestion> listByStatus(String status, Long userId, String role);
    void accept(Long id, Long operatorId);
    void ignore(Long id);
    void analyzeRemark(Long projectId, Long stageId, String remark);
}
