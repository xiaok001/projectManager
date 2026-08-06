package com.pm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.common.response.R;
import com.pm.model.entity.ReportWeeklyLog;
import com.pm.model.vo.WeeklyReportVO;
import com.pm.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/weekly")
    public R<WeeklyReportVO> weekly(
            @RequestParam(required = false) Long projectId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(reportService.generateWeeklyReport(projectId, userId, role));
    }

    @GetMapping("/weekly/history")
    public R<Page<ReportWeeklyLog>> weeklyHistory(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long projectId) {
        return R.ok(reportService.getHistory(pageNum, pageSize, projectId));
    }
}
