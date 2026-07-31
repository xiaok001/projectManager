package com.pm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pm.common.response.R;
import com.pm.model.entity.OperationLog;
import com.pm.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService logService;

    @GetMapping
    public R<Page<OperationLog>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation) {
        return R.ok(logService.pageList(pageNum, pageSize, module, operation));
    }
}
