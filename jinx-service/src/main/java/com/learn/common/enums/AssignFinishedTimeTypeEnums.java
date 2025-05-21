package com.learn.common.enums;

import lombok.Getter;

/**
 * 指派任务的完成时间类型枚举
 */
@Getter
public enum AssignFinishedTimeTypeEnums {
    /**
     * 不设置
     * 1-1周
     * 2-2周
     * 3-4周
     * 4-自定义
     */
    NONE,
    ONE_WEEK,
    TWO_WEEK,
    FOUR_WEEK,
    CUSTOM;

}
