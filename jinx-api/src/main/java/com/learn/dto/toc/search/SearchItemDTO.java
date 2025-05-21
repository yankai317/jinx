package com.learn.dto.toc.search;

import lombok.Data;

/**
 * 搜索结果项DTO
 */
@Data
public class SearchItemDTO {
    /**
     * 内容ID
     */
    private Long id;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 内容类型：course-课程，train-培训，map-学习地图
     */
    private String type;
    
    /**
     * 课程内容类型：video-视频，document-文档，series-系列课，article-文章
     * 仅当type=course时有效
     */
    private String contentType;
    
    /**
     * 封面图片
     */
    private String coverImage;
    
    /**
     * 学分
     */
    private Integer credit;
    
    /**
     * 描述/简介
     */
    private String description;
    
    /**
     * 学习进度（百分比）
     */
    private Integer progress;
    
    /**
     * 分类ID列表，格式：1,2,3
     */
    private String categoryIds;
    
    /**
     * 分类名称列表，格式：分类1,分类2,分类3
     */
    private String categoryNames;
}
