package com.learn.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户登录响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * JWT令牌
     */
    private String token;
    
    /**
     * 用户信息
     */
    private UserInfo userInfo;
    
    /**
     * 权限列表
     */
    private List<String> permissions;
    
    /**
     * 用户基本信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户昵称
         */
        private String nickname;
        
        /**
         * 用户头像URL
         */
        private String avatar;
    }
}
