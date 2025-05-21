package com.learn.service.map;

import com.learn.dto.map.LearningMapBatchDeleteResponse;

import java.util.List;

/**
 * @author yujintao
 * @description 定义地图的删除和批量删除
 * @date 2025/4/21
 */
public interface LearnMapDeleteService {
    /**
     * 删除学习地图
     * 
     * @param id 学习地图ID
     * @param userId 操作用户ID
     * @param userName 操作用户名称
     * @return 是否删除成功
     */
    Boolean deleteLearningMap(Long id, Long userId, String userName);
    
    /**
     * 批量删除学习地图
     * 
     * @param ids 学习地图ID列表
     * @param userId 操作用户ID
     * @param userName 操作用户名称
     * @return 批量删除结果
     */
    LearningMapBatchDeleteResponse batchDeleteLearningMap(List<Long> ids, Long userId, String userName);
}
