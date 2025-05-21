package com.learn.common.enums;

import lombok.Getter;

/**
 * 指派类型枚举
 */
@Getter
public enum AssignTypeEnums {
    /**
     * 单次通知、自动通知
     */
    ONCE("once", "单次通知"),
    AUTO("auto", "自动通知");

    private String code;
    private String message;

    AssignTypeEnums(String code, String message) {
        this.code = code;
        this.message = message;
    }
} 
