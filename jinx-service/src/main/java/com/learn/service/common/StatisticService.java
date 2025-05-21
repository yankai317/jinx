package com.learn.service.common;

import com.learn.common.dto.query.StatisticsQueryBO;
import com.learn.dto.train.StatisticLearnersResponse;
import com.learn.dto.train.StatisticsDTO;

/**
 * @author yujintao
 * @date 2025/5/13
 */
public interface StatisticService {
    /**
     * 获取统计数据
     *
     * @param queryBO 统计查询参数
     * @return 培训统计数据
     */
    <T extends StatisticsDTO> T getStatistics(StatisticsQueryBO queryBO, Class<T> type);


    /**
     * 获取学习人员列表
     *
     * @return 学习人员列表
     */
    StatisticLearnersResponse getLearners(StatisticsQueryBO queryBO);
}
