package com.pm.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 邮件发送记录表实体
 */
@Data
@TableName("email_digest_log")
public class EmailDigestLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate sendDate;
    private String content;
    private String recipients;
    private String sendStatus;
    private String failReason;
    private LocalDateTime sentAt;
}
