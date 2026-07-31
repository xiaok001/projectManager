package com.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pm.model.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
