package com.learn.service.learning;

import java.util.List;
import java.util.Map;

/**
 * 学习统计服务接口
 */
public interface LearningStatisticsService {
    
    /**
     * 获取用户课程学习数量
     *
     * @param userId 用户ID
     * @return 课程学习数量
     */
    Long getUserCourseCount(Long userId);
    
    /**
     * 获取用户培训完成率
     *
     * @param userId 用户ID
     * @return 培训完成率（百分比）
     */
    double getUserTrainCompletionRate(Long userId);
    
    /**
     * 获取用户学习地图数量
     *
     * @param userId 用户ID
     * @return 学习地图数量
     */
    Long getUserMapCount(Long userId);
    
    /**
     * 获取用户证书数量
     *
     * @param userId 用户ID
     * @return 证书数量
     */
    Long getUserCertificateCount(Long userId);
    
    /**
     * 获取用户最近学习的课程
     *
     * @param userId 用户ID
     * @return 最近学习的课程列表
     */
    List<Map<String, Object>> getUserRecentCourses(Long userId);
    
    /**
     * 获取用户最近学习的培训
     *
     * @param userId 用户ID
     * @return 最近学习的培训列表
     */
    List<Map<String, Object>> getUserRecentTrains(Long userId);
    
    /**
     * 获取用户最近学习的地图
     *
     * @param userId 用户ID
     * @return 最近学习的地图列表
     */
    List<Map<String, Object>> getUserRecentMaps(Long userId);
    
    /**
     * 设置学习地图自动指派状态
     *
     * @param mapId 学习地图ID
     * @param enableAutoAssign 是否启用自动指派
     * @return 操作结果
     */
    boolean setLearningMapAutoAssign(Long mapId, Boolean enableAutoAssign);
}
