package com.learn.dto.toc.home;

import lombok.Data;

import java.util.List;

/**
 * 学习任务DTO
 */
@Data
public class LearningTasksDTO {
    /**
     * 课程学习任务列表
     */
    private List<LearningTaskItemDTO> courses;
    
    /**
     * 培训学习任务列表
     */
    private List<LearningTaskItemDTO> trainings;
    
    /**
     * 学习地图任务列表
     */
    private List<LearningTaskItemDTO> learningMaps;
}
