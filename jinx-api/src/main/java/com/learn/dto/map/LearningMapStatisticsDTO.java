package com.learn.dto.map;

import com.learn.dto.train.StatisticsDTO;
import lombok.Data;

import java.util.List;

/**
 * 学习地图统计数据DTO
 */
@Data
public class LearningMapStatisticsDTO extends StatisticsDTO {
    /**
     * 各阶段完成率
     */
    private List<StageCompletionRate> stageCompletionRates;

    /**
     * 阶段完成率
     */
    @Data
    public static class StageCompletionRate {
        /**
         * 阶段名称
         */
        private String name;

        /**
         * 完成率
         */
        private Float rate;
    }
}
