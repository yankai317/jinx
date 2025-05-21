package com.learn.service.auth;

import com.learn.dto.auth.LoginRequest;
import com.learn.dto.auth.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     * 1. 验证用户凭证
     * 2. 生成JWT令牌
     * 3. 记录用户登录时间
     * 4. 返回用户基本信息和权限信息
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 第三方登录
     * 1. 使用第三方登录策略获取第三方用户信息
     * 2. 关联或创建本地用户
     * 3. 生成JWT令牌
     * 4. 记录用户登录时间
     * 5. 返回用户基本信息和权限信息
     *
     * @param thirdPartyType 第三方平台类型
     * @param thirdPartyCode 第三方授权码
     * @param corpId 企业ID
     * @return 登录响应
     */
    LoginResponse thirdPartyLogin(String thirdPartyType, String thirdPartyCode, String corpId);
}
