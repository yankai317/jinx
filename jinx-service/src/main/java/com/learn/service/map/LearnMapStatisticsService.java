package com.learn.service.map;

import com.learn.dto.map.LearningMapStatisticsDTO;

/**
 * @author yujintao
 * @Description 定义学习地图的统计接口
 * @date 2025/4/21
 */
public interface LearnMapStatisticsService {
    
    /**
     * 获取学习地图统计数据
     *
     * @param statisticsDTO 学习地图
     * @return 学习地图统计数据
     */
    void setLearningMapStatistics(LearningMapStatisticsDTO statisticsDTO);
}
