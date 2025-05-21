package com.learn.common.enums;

import lombok.Getter;

/**
 * 范围目标类型
 */
@Getter
public enum RangeTargetTypeEnums {
    /**
     * 部门、角色、用户
     */
    DEPARTMENT("department", "部门"),
    ROLE("role", "角色"),
    USER("user", "用户");
    private String code;
    private String message;

    RangeTargetTypeEnums(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
