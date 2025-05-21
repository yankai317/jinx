package com.learn.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API通用响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;
    /**
     * 总数
     */
    private Long total;

    /**
     * 成功响应
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 成功响应
     *
     * @param message 消息
     * @param data    数据
     * @param <T>     数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     *
     * @param message
     * @param data
     * @param total
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> success(String message, T data, Long total) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .total(total)
                .build();
    }

    /**
     * 失败响应
     *
     * @param code    状态码
     * @param message 消息
     * @param <T>     数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 失败响应
     *
     * @param message 消息
     * @param <T>     数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .code(500)
                .message(message)
                .build();
    }
}
