package com.learn.service.dto;

import lombok.Data;

/**
 * 第三方用户信息传输对象
 * 用于从第三方平台获取的用户信息
 */
@Data
public class ThirdPartyUserInfo {
    
    /**
     * 第三方平台类型
     */
    private String thirdPartyType;
    
    /**
     * 第三方平台用户ID
     */
    private String thirdPartyUserId;
    
    /**
     * 第三方平台用户名
     */
    private String thirdPartyUsername;
    
    /**
     * 用户头像URL
     */
    private String avatar;
    
    /**
     * 用户邮箱
     */
    private String email;
    
    /**
     * 手机号码
     */
    private String mobile;
    
    /**
     * 原始数据JSON，保存第三方平台返回的完整数据
     */
    private String rawData;
} 
