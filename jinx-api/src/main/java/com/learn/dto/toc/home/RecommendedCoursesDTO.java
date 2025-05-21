package com.learn.dto.toc.home;

import lombok.Data;

import java.util.List;

/**
 * 推荐课程DTO
 */
@Data
public class RecommendedCoursesDTO {
    /**
     * 推荐课程列表
     */
    private List<CourseItemDTO> courses;
    
    /**
     * 推荐培训列表
     */
    private List<CourseItemDTO> trainings;
    
    /**
     * 推荐学习地图列表
     */
    private List<CourseItemDTO> learningMaps;
}
