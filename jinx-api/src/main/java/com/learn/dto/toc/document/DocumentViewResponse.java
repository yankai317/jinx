package com.learn.dto.toc.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档查看响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentViewResponse {
    /**
     * 课程ID
     */
    private Long courseId;
    
    /**
     * 课程标题
     */
    private String title;
    
    /**
     * 文档URL
     */
    private String documentUrl;
    
    /**
     * 文档类型
     */
    private String documentType;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 当前页
     */
    private Integer currentPage;
    
    /**
     * 学习进度（百分比）
     */
    private Integer progress;
    
    /**
     * 学习时长（分钟）
     */
    private Integer studyDuration;
    
    /**
     * 是否已完成
     */
    private Boolean completed;
    
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
    
    /**
     * 分享链接
     */
    private String shareUrl;
    
    /**
     * 分享二维码
     */
    private String shareQrCode;
}
