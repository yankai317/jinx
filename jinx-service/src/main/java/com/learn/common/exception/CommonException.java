package com.learn.common.exception;

import lombok.Data;

/**
 * 全局通用异常
 */
@Data
public class CommonException extends RuntimeException {

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 构造函数
     * @param message 错误信息
     */
    public CommonException(String message) {
        super(message);
    }

    /**
     * 构造函数
     * @param message 错误信息
     * @param cause 原始异常
     */
    public CommonException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public CommonException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     * @param errorCode 错误码
     * @param message 错误信息
     * @param cause 原始异常
     */
    public CommonException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
}
