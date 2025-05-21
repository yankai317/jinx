package com.learn.service.toc;

import com.learn.dto.toc.learning.LearningTasksResponse;

/**
 * 学习任务服务接口
 */
public interface LearningTaskService {
    
    /**
     * 获取用户的学习任务列表
     *
     * @param userId 用户ID
     * @param type 任务类型：train-培训，map-学习地图，默认全部
     * @param status 状态筛选：all-全部，required-必修，elective-选修，默认全部
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 学习任务列表响应
     */
    LearningTasksResponse getLearningTasks(Long userId, String type, String status, Integer pageNum, Integer pageSize);
}
