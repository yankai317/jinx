package com.learn.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 课程统计数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseStatisticsDTO {
    
    /**
     * 查看数
     */
    private Integer viewCount;
    
    /**
     * 完成人数
     */
    private Integer completeCount;
    
    /**
     * 平均学习时长(分钟)
     */
    private Integer avgDuration;
    
    /**
     * 总学习时长(分钟)
     */
    private Integer totalDuration;
    
    /**
     * 部门统计
     */
    private List<DepartmentStatDTO> departmentStats;
    
    /**
     * 时间分布
     */
    private List<TimeDistributionDTO> timeDistribution;
    
    /**
     * 部门统计DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentStatDTO {
        /**
         * 部门名称
         */
        private String name;
        
        /**
         * 人数
         */
        private Integer count;
    }
    
    /**
     * 时间分布DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeDistributionDTO {
        /**
         * 日期
         */
        private String date;
        
        /**
         * 人数
         */
        private Integer count;
    }
}
