package com.pm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.EmailDigestLog;

public interface EmailDigestService extends IService<EmailDigestLog> {
    void sendDailyDigest();
}
