package com.learn.service.dto;

import lombok.Data;

/**
 * 创建用户请求对象
 */
@Data
public class UserCreateRequest {
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
     * 创建人id
     */
    private Long creatorId;

    /**
     * 创建人名称
     */
    private String creatorName;

    /**
     * 扩展属性
     */
    private String attributes;

}
