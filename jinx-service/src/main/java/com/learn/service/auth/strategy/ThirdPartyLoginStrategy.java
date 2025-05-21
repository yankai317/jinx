package com.learn.service.auth.strategy;

import com.learn.service.dto.ThirdPartyUserInfo;

/**
 * 第三方登录策略接口
 * 使用策略模式支持不同平台的免登流程
 */
public interface ThirdPartyLoginStrategy {
    
    /**
     * 获取策略支持的第三方平台类型
     * @return 平台类型标识
     */
    String getThirdPartyType();
    
    /**
     * 通过第三方授权码/Token获取用户信息
     *
     * @param code   授权码或Token
     * @param corpId
     * @return 第三方用户信息
     */
    ThirdPartyUserInfo getUserInfoByCode(String code, String corpId);
} 
