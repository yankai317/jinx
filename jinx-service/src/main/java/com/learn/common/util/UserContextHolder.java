package com.learn.common.util;

import com.learn.common.dto.UserTokenInfo;

/**
 * 用户上下文持有者
 * 使用ThreadLocal存储当前线程中的用户信息
 */
public class UserContextHolder {
    
    /**
     * 使用ThreadLocal存储用户信息
     */
    private static final ThreadLocal<UserTokenInfo> USER_CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置用户信息到当前线程
     *
     * @param userTokenInfo 用户令牌信息
     */
    public static void setUserInfo(UserTokenInfo userTokenInfo) {
        USER_CONTEXT.set(userTokenInfo);
    }
    
    /**
     * 获取当前线程中的用户信息
     *
     * @return 用户令牌信息
     */
    public static UserTokenInfo getUserInfo() {
        return USER_CONTEXT.get();
    }
    
    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未设置则返回null
     */
    public static Long getUserId() {
        UserTokenInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getUserId() : null;
    }
    
    /**
     * 获取当前企业ID
     *
     * @return 企业ID，如果未设置则返回null
     */
    public static String getCorpId() {
        UserTokenInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getCorpId() : null;
    }
    
    /**
     * 清除当前线程中的用户信息
     * 在请求完成后调用，防止内存泄漏
     */
    public static void clear() {
        USER_CONTEXT.remove();
    }
}
