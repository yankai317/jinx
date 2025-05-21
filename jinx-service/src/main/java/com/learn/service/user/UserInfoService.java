package com.learn.service.user;

import com.learn.dto.user.UserInfoResponse;

/**
 * 用户信息服务接口
 */
public interface UserInfoService {
    
    /**
     * 获取用户信息
     * 1. 查询用户基本信息
     * 2. 查询用户部门信息
     * 3. 查询用户角色信息
     * 4. 查询用户权限信息
     *
     * @param userId 用户ID
     * @return 用户信息响应
     */
    UserInfoResponse getUserInfo(Long userId);
}
