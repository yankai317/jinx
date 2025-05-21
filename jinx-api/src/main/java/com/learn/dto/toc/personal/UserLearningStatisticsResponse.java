package com.learn.dto.toc.personal;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户学习统计响应
 */
@Data
public class UserLearningStatisticsResponse {
    /**
     * 总学习时长（分钟）
     */
    private Long totalStudyDuration;
    
    /**
     * 完成培训数量
     */
    private Long completedTrainCount;
    
    /**
     * 完成地图数量
     */
    private Long completedMapCount;
    
    /**
     * 通过考试数量
     */
    private Long passedExamCount;
    
    /**
     * 获得证书数量
     */
    private Long certificateCount;
    
    /**
     * 学习时长趋势（最近7天）
     * key: 日期，格式：MM-dd
     * value: 学习时长（分钟）
     */
    private Map<String, Integer> studyDurationTrend;
    
    /**
     * 学习内容分布
     */
    private List<ContentDistribution> contentDistribution;
    
    /**
     * 学习内容分布
     */
    @Data
    public static class ContentDistribution {
        /**
         * 内容类型
         */
        private String type;
        
        /**
         * 内容类型名称
         */
        private String typeName;
        
        /**
         * 数量
         */
        private Long count;
        
        /**
         * 占比（百分比）
         */
        private Double percentage;
    }
}
