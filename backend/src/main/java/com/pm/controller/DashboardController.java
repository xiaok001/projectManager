package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.vo.DashboardVO;
import com.pm.model.vo.ProjectHealthVO;
import com.pm.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public R<DashboardVO> summary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(dashboardService.getSummary(userId, role));
    }

    @GetMapping("/health")
    public R<List<ProjectHealthVO>> healthList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(dashboardService.calculateAllHealth(userId, role));
    }

    @GetMapping("/health/{projectId}")
    public R<ProjectHealthVO> healthDetail(@PathVariable Long projectId) {
        return R.ok(dashboardService.calculateHealth(projectId));
    }
}
