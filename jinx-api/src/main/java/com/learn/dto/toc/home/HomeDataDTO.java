package com.learn.dto.toc.home;

import lombok.Data;

/**
 * 首页数据DTO
 */
@Data
public class HomeDataDTO {
    /**
     * 用户信息
     */
    private HomeUserCardInfoDTO userInfo;
    
    /**
     * 学习任务数据
     */
    private LearningTasksDTO learningTasks;
    
    /**
     * 推荐课程数据
     */
    private RecommendedCoursesDTO recommendedCourses;
    
    /**
     * 排行榜数据
     */
    private RankingsDTO rankings;
}
