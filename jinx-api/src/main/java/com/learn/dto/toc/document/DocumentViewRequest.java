package com.learn.dto.toc.document;

import lombok.Data;

/**
 * 文档查看请求
 */
@Data
public class DocumentViewRequest {
    /**
     * 课程ID
     */
    private Long courseId;
    
    /**
     * 来源类型：course-课程，train-培训，map-学习地图
     */
    private String sourceType;
    
    /**
     * 来源ID（培训ID或学习地图ID）
     */
    private Long sourceId;
    
    /**
     * 任务ID（培训或地图中的具体任务）
     */
    private Long taskId;
    
    /**
     * 阶段ID（学习地图中的阶段）
     */
    private Long stageId;
}
