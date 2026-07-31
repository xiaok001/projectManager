package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.dto.RiskDTO;
import com.pm.model.entity.ProjectRisk;
import com.pm.model.vo.RiskAggregationVO;
import com.pm.service.ProjectRiskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectRiskController {

    private final ProjectRiskService riskService;

    @PostMapping("/projects/{projectId}/risks")
    public R<ProjectRisk> create(@PathVariable Long projectId, @Valid @RequestBody RiskDTO dto,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(riskService.createRisk(projectId, dto, userId));
    }

    @GetMapping("/projects/{projectId}/risks")
    public R<List<ProjectRisk>> listByProject(@PathVariable Long projectId) {
        return R.ok(riskService.listByProjectId(projectId));
    }

    @GetMapping("/risks/aggregated")
    public R<List<RiskAggregationVO>> aggregated(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(riskService.listAggregated(userId, role));
    }

    @PutMapping("/risks/{id}")
    public R<ProjectRisk> update(@PathVariable Long id, @Valid @RequestBody RiskDTO dto,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(riskService.updateRisk(id, dto, userId));
    }

    @PutMapping("/risks/{id}/stale-override")
    public R<Void> staleOverride(@PathVariable Long id, @RequestBody Map<String, Boolean> body,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        riskService.updateStaleStatus(id, body.get("staleOverride"), userId);
        return R.ok();
    }
}
