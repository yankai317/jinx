package com.learn.service.user.strategy;

import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.infrastructure.repository.entity.UserLearningTask;

/**
 * 学习任务策略接口
 * 定义不同类型学习任务的处理策略
 *
 * @author yujintao
 * @date 2023/5/10
 */
public interface LearningTaskStrategy {
    
    /**
     * 处理学习任务记录
     *
     * @param userId 用户ID
     * @param request 学习进度请求
     * @return 用户学习任务记录
     */
    UserLearningTask processLearningTask(Long userId, RecordLearningProgressRequest request);
    
    /**
     * 更新学习进度
     * 
     * @param learningTask 学习任务
     * @param request 学习进度请求
     */
    void updateLearningProgress(UserLearningTask learningTask, RecordLearningProgressRequest request);
}
