package com.learn.service.train;

import com.learn.dto.train.StatisticsDTO;

/**
 * @author yujintao
 * @Description 定义培训的统计接口
 * @date 2025/4/21
 */
public interface TrainStatisticsService {
    
    /**
     * 获取培训统计数据
     *
     * @param trainId 培训ID
     * @return 培训统计数据
     */
    StatisticsDTO getTrainStatistics(Long trainId);
}
