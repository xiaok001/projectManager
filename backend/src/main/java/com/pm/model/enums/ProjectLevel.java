package com.pm.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目等级枚举
 */
@Getter
@AllArgsConstructor
public enum ProjectLevel {
    P0(0, "P0-紧急"),
    P1(1, "P1-重要"),
    P2(2, "P2-一般");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    public static ProjectLevel fromCode(int code) {
        for (ProjectLevel level : values()) {
            if (level.code == code) return level;
        }
        return P2;
    }
}
