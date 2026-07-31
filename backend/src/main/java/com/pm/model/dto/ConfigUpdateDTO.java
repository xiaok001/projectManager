package com.pm.model.dto;

import lombok.Data;

/**
 * 配置更新DTO (Map形式批量更新)
 */
@Data
public class ConfigUpdateDTO {
    private String configKey;
    private String configValue;
}
