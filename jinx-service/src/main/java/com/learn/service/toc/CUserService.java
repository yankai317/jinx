package com.learn.service.toc;

import com.learn.dto.toc.user.UserInfoResponse;

/**
 * C端用户服务接口
 */
public interface CUserService {
    /**
     * 获取当前登录用户信息
     *1. 查询用户基本信息
     * 2. 查询用户部门信息
     * 3. 查询用户学习统计数据
     *
     * @param userId 用户ID
     * @return 用户信息响应
     */
    UserInfoResponse getUserInfo(Long userId);
}
