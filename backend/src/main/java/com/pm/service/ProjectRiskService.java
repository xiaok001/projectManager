package com.pm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.ProjectRisk;
import com.pm.model.dto.RiskDTO;
import com.pm.model.vo.RiskAggregationVO;

import java.util.List;

public interface ProjectRiskService extends IService<ProjectRisk> {
    ProjectRisk createRisk(Long projectId, RiskDTO dto, Long operatorId);
    ProjectRisk updateRisk(Long id, RiskDTO dto, Long operatorId);
    List<ProjectRisk> listByProjectId(Long projectId);
    List<RiskAggregationVO> listAggregated(Long userId, String role);
    void updateStaleStatus(Long id, Boolean staleOverride, Long operatorId);
    void refreshStaleStatus();
    String generateRiskCode(Long projectId);
}
