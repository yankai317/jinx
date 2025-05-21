package com.learn.common.enums;

import lombok.Getter;

/**
 * 范围模块功能类型
 */
@Getter
public enum RangeModelTypeEnums {
    /**
     * 可见范围、任务指派、协同使用
     */
    VISIBILITY("visibility", "可见范围"),
    COLLABORATORS("collaborators", "协同使用"),
    EDITORS("editors", "协同编辑"),
    ASSIGN("assign", "指派");

    private String code;
    private String message;

    RangeModelTypeEnums(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
