package com.learn.service.toc;

import com.learn.dto.toc.personal.*;

/**
 * 个人中心服务接口
 */
public interface PersonalCenterService {
    
    /**
     * 获取用户学习记录
     *
     * @param userId 用户ID
     * @param request 获取用户学习记录请求
     * @return 用户学习记录响应
     */
    UserLearningRecordsResponse getUserLearningRecords(Long userId, UserLearningRecordsRequest request);

    
    /**
     * 获取用户个人中心信息
     *
     * @param userId 用户ID
     * @return 用户个人中心信息响应
     */
    UserProfileResponse getUserProfile(Long userId);


    /**
     * 获取用户学习总数
     * @param userId
     * @return
     */
    UserLearningTotalResponse getUserLearningTotal(Long userId, boolean countSelective);
}
