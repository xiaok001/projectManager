package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作模块 */
    private String module;
    /** 操作类型 */
    private String operation;
    /** 操作描述 */
    private String description;
    /** 操作人ID */
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 请求方法 GET/POST/PUT/DELETE */
    private String requestMethod;
    /** 请求URL */
    private String requestUrl;
    /** 请求参数 */
    private String requestParams;
    /** 响应状态码 */
    private Integer responseCode;
    /** 操作IP */
    private String ip;
    /** 执行耗时(ms) */
    private Long executionTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
