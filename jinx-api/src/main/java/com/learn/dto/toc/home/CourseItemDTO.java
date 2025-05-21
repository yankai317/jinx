package com.learn.dto.toc.home;

import lombok.Data;

/**
 * 课程项DTO
 */
@Data
public class CourseItemDTO {
    /**
     * 课程ID
     */
    private Long id;
    
    /**
     * 课程名称
     */
    private String name;
    
    /**
     * 课程类型：COURSE-课程 TRAIN-培训 MAP-学习地图
     */
    private String type;
    
    /**
     * 封面图片
     */
    private String cover;
    
    /**
     * 简介
     */
    private String introduction;
    
    /**
     * 学分
     */
    private Integer credit;
    
    /**
     * 查看数
     */
    private Integer viewCount;
    
    /**
     * 完成人数
     */
    private Integer completeCount;
}
