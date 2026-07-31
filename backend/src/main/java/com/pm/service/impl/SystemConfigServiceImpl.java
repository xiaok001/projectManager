package com.pm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.SystemConfigMapper;
import com.pm.model.entity.SystemConfig;
import com.pm.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig>
        implements SystemConfigService {

    @Override
    public List<SystemConfig> listAll() {
        return list();
    }

    @Override
    public Map<String, String> getConfigMap() {
        return list().stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue));
    }

    @Override
    @Transactional
    public void updateConfigs(List<Map<String, String>> configs) {
        for (Map<String, String> config : configs) {
            String key = config.get("configKey");
            String value = config.get("configValue");
            if (key == null || value == null) continue;

            SystemConfig existing = lambdaQuery()
                    .eq(SystemConfig::getConfigKey, key)
                    .one();
            if (existing != null) {
                existing.setConfigValue(value);
                updateById(existing);
            } else {
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(key);
                newConfig.setConfigValue(value);
                save(newConfig);
            }
        }

        // 校验健康评分权重之和为100%
        validateHealthWeights();
    }

    @Override
    public String getValue(String key) {
        return getValue(key, null);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        SystemConfig config = lambdaQuery()
                .eq(SystemConfig::getConfigKey, key)
                .one();
        return config != null ? config.getConfigValue() : defaultValue;
    }

    @Override
    public int getIntValue(String key, int defaultValue) {
        String value = getValue(key);
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void validateHealthWeights() {
        int timeWeight = getIntValue("health_weight_time", 35);
        int riskWeight = getIntValue("health_weight_risk", 40);
        int deliveryWeight = getIntValue("health_weight_delivery", 25);

        if (timeWeight + riskWeight + deliveryWeight != 100) {
            throw new BusinessException("健康评分三项权重之和必须为100%，当前: 时间"
                    + timeWeight + "% + 风险" + riskWeight + "% + 交付" + deliveryWeight + "%");
        }
    }
}
