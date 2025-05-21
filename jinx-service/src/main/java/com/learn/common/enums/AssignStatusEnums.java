package com.learn.common.enums;

import lombok.Getter;

/**
 * 指派状态枚举
 */
@Getter
public enum AssignStatusEnums {
    /**
     * 通知状态：wait-待通知、process-通知中、success-已通知
     */
    WAIT("wait", "待通知"),
    PROCESS("process", "通知中"),
    SUCCESS("success", "已通知"),
    
    /**
     * 指派明细状态：待通知、通知成功、通知失败
     */
    WAIT_NOTIFY("0", "待通知"),
    NOTIFY_SUCCESS("1", "通知成功"),
    NOTIFY_FAILED("2", "通知失败");

    private String code;
    private String message;


    AssignStatusEnums(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
