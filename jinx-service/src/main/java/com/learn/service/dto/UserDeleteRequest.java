package com.learn.service.dto;

import lombok.Data;

/**
 * 删除用户请求对象
 */
@Data
public class UserDeleteRequest {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 第三方平台类型，如：dingtalk、feishu
     */
    private String thirdPartyType;

    /**
     * 第三方平台用户唯一标识
     */
    private String thirdPartyUserId;

    /**
     * 操作人id
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;
}
