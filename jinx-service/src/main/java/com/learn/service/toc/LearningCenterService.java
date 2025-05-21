package com.learn.service.toc;

import com.learn.dto.toc.learning.LearningCenterRequest;
import com.learn.dto.toc.learning.LearningCenterResponse;

/**
 * 学习中心服务接口
 */
public interface LearningCenterService {
    
    /**
     * 获取学习中心数据
     *
     * @param userId 用户ID
     * @param request 请求参数
     * @return 学习中心数据
     */
    LearningCenterResponse getLearningCenter(Long userId, LearningCenterRequest request);
}
