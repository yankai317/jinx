package com.learn.dto.toc.learning;

import lombok.Data;

/**
 * 记录学习进度请求
 */
@Data
public class RecordLearningProgressRequest {
    /**
     * 父内容类型：course-课程，train-培训内容，map-学习地图任务
     */
    private String parentType;

    /**
     * 子内容类型：course-课程，train-培训内容，map-学习地图任务
     */
    private String contentType;

    /**
     * 内容ID
     */
    private Long contentId;

    /**
     * 学习进度(百分比)
     */
    private Integer progress;

    /**
     * 本次学习时长(秒)
     */
    private Integer duration;

    /**
     * 父内容ID(培训ID或学习地图阶段ID)
     */
    private Long parentId;

    private boolean initByAssign;
}
