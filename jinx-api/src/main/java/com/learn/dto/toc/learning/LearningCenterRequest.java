package com.learn.dto.toc.learning;

import lombok.Data;

import java.util.List;

/**
 * 学习中心请求参数
 */
@Data
public class LearningCenterRequest {
    /**
     * 内容类型：course-课程，train-培训，map-学习地图，exam-考试，practice-练习，默认全部
     */
    private String type;

    /**
     * 分类ID，默认全部
     */
    private List<Long> categoryIds;


    /**
     * 内容类型筛选：video-视频，document-文档，live-直播，多个用逗号分隔，默认全部
     */
    private String contentTypes;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;

    /**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;
}
