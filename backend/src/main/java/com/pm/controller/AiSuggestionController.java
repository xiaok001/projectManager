package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.entity.AiRiskSuggestion;
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
}
