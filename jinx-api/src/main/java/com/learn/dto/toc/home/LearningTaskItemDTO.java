package com.learn.dto.toc.home;

import lombok.Data;

import java.util.Date;

/**
 * 学习任务项DTO
 */
@Data
public class LearningTaskItemDTO {
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 任务名称
     */
    private String name;
    
    /**
     * 任务类型：COURSE-课程 TRAIN-培训 MAP-学习地图
     */
    private String type;
    
    /**
     * 封面图片
     */
    private String cover;

    /**
     * 学分
     */
    private Integer credit;
    
    /**
     * 学习进度（百分比）
     */
    private Integer progress;
    
    /**
     * 学习状态
     * @see com.learn.constants.LearningStatus
     */
    private String status;
    
    /**
     * 截止时间
     */
    private Date deadline;
    
    /**
     * 学习来源：ASSIGN-指派 SELF-自学
     */
    private String source;
}
