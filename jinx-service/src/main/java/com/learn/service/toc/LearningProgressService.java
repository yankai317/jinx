package com.learn.service.toc;

import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.toc.learning.RecordLearningProgressResponse;

/**
 * 学习进度服务接口
 */
public interface LearningProgressService {
    
    /**
     * 记录学习进度
     *
     * @param userId 用户ID
     * @param request 记录学习进度请求
     * @return 记录学习进度响应
     */
    RecordLearningProgressResponse recordProgress(Long userId, RecordLearningProgressRequest request);

    /**
     * 验证学习内容是否有效
     * request
     * @return 是否有效
     */
    boolean validateContent(RecordLearningProgressRequest request);
}
