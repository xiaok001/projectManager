package com.pm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.SystemConfig;

import java.util.List;
import java.util.Map;

public interface SystemConfigService extends IService<SystemConfig> {
    List<SystemConfig> listAll();
    Map<String, String> getConfigMap();
    void updateConfigs(List<Map<String, String>> configs);
    String getValue(String key);
    String getValue(String key, String defaultValue);
    int getIntValue(String key, int defaultValue);
}
