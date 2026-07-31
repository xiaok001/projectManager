package com.pm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {
    Page<OperationLog> pageList(Integer pageNum, Integer pageSize, String module, String operation);
}
