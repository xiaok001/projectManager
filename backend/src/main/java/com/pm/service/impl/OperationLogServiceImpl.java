package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.mapper.OperationLogMapper;
import com.pm.model.entity.OperationLog;
import com.pm.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    @Override
    public Page<OperationLog> pageList(Integer pageNum, Integer pageSize, String module, String operation) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.eq(OperationLog::getOperation, operation);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        return page(page, wrapper);
    }
}
