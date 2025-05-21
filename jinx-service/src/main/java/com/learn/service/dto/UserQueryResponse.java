package com.learn.service.dto;

import lombok.Data;

/**
 * 查询用户响应对象
 */
@Data
public class UserQueryResponse {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 第三方平台类型，如：dingtalk、feishu
     */
    private String thirdPartyType;

    /**
     * 第三方平台用户唯一标识
     */
    private String thirdPartyUserId;

    /**
     * 第三方平台用户名
     */
    private String thirdPartyUsername;
}
