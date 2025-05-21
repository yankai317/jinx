package com.learn.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    /**
     * 用户名/工号
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 第三方登录类型
     */
    private String thirdPartyType;
    
    /**
     * 第三方授权码
     */
    private String thirdPartyCode;
    
    /**
     * 企业ID，用于三方登录
     */
    private String corpId;
}
