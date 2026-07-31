package com.pm.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 严重程度枚举
 */
@Getter
@AllArgsConstructor
public enum Severity {
    HIGH("高", "高"),
    MEDIUM("中", "中"),
    LOW("低", "低");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    public static Severity fromCode(String code) {
        for (Severity s : values()) {
            if (s.code.equals(code)) return s;
        }
        return MEDIUM;
    }
}
