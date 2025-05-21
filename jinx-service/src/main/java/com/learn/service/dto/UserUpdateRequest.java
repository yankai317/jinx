package com.learn.service.dto;

import com.learn.service.dto.base.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户更新请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserUpdateRequest extends BaseRequest {
    /**
     * 用户ID
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
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;
}
