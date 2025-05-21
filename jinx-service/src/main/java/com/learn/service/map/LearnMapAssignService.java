package com.learn.service.map;

import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.dto.map.LearningMapAssignRequest;
import com.learn.dto.map.LearningMapAssignResponse;

/**
 * @author yujintao
 * @Description 定义学习的指派接口，例如培训的指派
 * @date 2025/4/21
 */
public interface  LearnMapAssignService {
    /**
     * 指派学习地图给用户
     * 1. 创建学习地图指派记录
     * 2. 为每个用户创建学习进度记录和任务记录
     * 3. 记录操作日志
     *
     * @param request 指派学习地图请求
     * @return 指派学习地图响应
     */
    AssignResponse assignLearningMap(AssignRequest request);
}
