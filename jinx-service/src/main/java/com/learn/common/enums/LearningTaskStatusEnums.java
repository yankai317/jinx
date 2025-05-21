package com.learn.common.enums;

import lombok.Getter;

/**
 * 学习任务类型枚举
 */
@Getter
public enum LearningTaskStatusEnums {
    UNSTARTED("not_started", "未开始"),
    LEARNING("learning", "学习中"),
    COMPLETED("completed", "已完成");
    private String code;
    private String message;

    LearningTaskStatusEnums(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
