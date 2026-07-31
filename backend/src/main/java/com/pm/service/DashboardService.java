package com.pm.service;

import com.pm.model.vo.DashboardVO;
import com.pm.model.vo.ProjectHealthVO;

import java.util.List;

public interface DashboardService {
    DashboardVO getSummary(Long userId, String role);
    ProjectHealthVO calculateHealth(Long projectId);
    List<ProjectHealthVO> calculateAllHealth(Long userId, String role);
}
