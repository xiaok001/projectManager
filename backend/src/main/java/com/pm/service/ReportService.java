package com.pm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.model.entity.ReportWeeklyLog;
import com.pm.model.vo.WeeklyReportVO;

public interface ReportService {
    WeeklyReportVO generateWeeklyReport(Long projectId, Long userId, String role);
    Page<ReportWeeklyLog> getHistory(Integer pageNum, Integer pageSize, Long projectId);
}
