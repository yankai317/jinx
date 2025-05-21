package com.learn.service.map;

import com.learn.dto.map.LearningMapCreateRequest;
import com.learn.dto.map.LearningMapCreateResponse;

/**
 * 学习地图创建服务接口
 */
public interface LearnMapCreateService {
    
    /**
     * 创建学习地图
     *
     * @param request 创建学习地图请求
     * @param userId 用户ID
     * @param userName 用户名称
     * @return 创建学习地图响应
     */
    LearningMapCreateResponse createLearningMap(LearningMapCreateRequest request, Long userId, String userName);
}
