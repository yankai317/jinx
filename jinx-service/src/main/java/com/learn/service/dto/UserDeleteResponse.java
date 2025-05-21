package com.learn.service.dto;

import lombok.Data;

/**
 * 删除用户响应对象
 */
@Data
public class UserDeleteResponse {
    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
