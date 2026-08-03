package com.pm.service;

import com.pm.model.vo.WeeklyReportVO;

public interface ReportService {
    WeeklyReportVO generateWeeklyReport(Long projectId, Long userId, String role);
}
