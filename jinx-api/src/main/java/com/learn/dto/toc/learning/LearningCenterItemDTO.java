package com.learn.dto.toc.learning;

import lombok.Data;

/**
 * 学习中心内容项DTO
 */
@Data
public class LearningCenterItemDTO {
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
     * 内容形式：video-视频，document-文档，live-直播
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
     * 是否必修：1-是，0-否
     */
    private Integer isRequired;

    /**
     * 学习进度（百分比）
     */
    private Integer progress;

    /**
     * 是否置顶：1-是，0-否
     */
    private Integer isTop;
}
