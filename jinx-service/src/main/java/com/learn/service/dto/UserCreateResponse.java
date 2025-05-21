package com.learn.service.dto;

import lombok.Data;

/**
 * 创建用户响应对象
 */
@Data
public class UserCreateResponse {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 是否新创建的用户
     */
    private Boolean isNewUser;

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
