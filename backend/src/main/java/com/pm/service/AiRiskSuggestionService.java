package com.pm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.AiRiskSuggestion;
import com.pm.model.vo.AiSuggestionVO;

import java.util.List;

public interface AiRiskSuggestionService extends IService<AiRiskSuggestion> {
    List<AiRiskSuggestion> listByStatus(String status, Long userId, String role);
    Page<AiSuggestionVO> pageList(Integer pageNum, Integer pageSize, String status,
                                   String projectCode, String projectName,
                                   String startDate, String endDate,
                                   Long userId, String role);
    void accept(Long id, Long operatorId);
    void ignore(Long id);
    void analyzeRemark(Long projectId, Long stageId, String remark);
    int scanAllProjectsForRisks();
}
