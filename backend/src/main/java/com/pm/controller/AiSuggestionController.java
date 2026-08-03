package com.pm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.common.response.R;
import com.pm.model.entity.AiRiskSuggestion;
import com.pm.model.vo.AiSuggestionVO;
import com.pm.service.AiRiskSuggestionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-suggestions")
@RequiredArgsConstructor
public class AiSuggestionController {

    private final AiRiskSuggestionService suggestionService;

    @GetMapping
    public R<List<AiRiskSuggestion>> list(@RequestParam(required = false) String status,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(suggestionService.listByStatus(status, userId, role));
    }

    @GetMapping("/page")
    public R<Page<AiSuggestionVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(suggestionService.pageList(pageNum, pageSize, status, projectCode, projectName, startDate, endDate, userId, role));
    }

    @PostMapping("/{id}/accept")
    public R<Void> accept(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        suggestionService.accept(id, userId);
        return R.ok();
    }

    @PostMapping("/{id}/ignore")
    public R<Void> ignore(@PathVariable Long id) {
        suggestionService.ignore(id);
        return R.ok();
    }

    @PostMapping("/scan")
    public R<String> scanAllProjects() {
        int count = suggestionService.scanAllProjectsForRisks();
        return R.ok("扫描完成，发现 " + count + " 条新风险建议");
    }
}
