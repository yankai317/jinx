package com.learn.dto.course;

import com.learn.dto.common.RangeBaseRequest;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建课程请求
 */
@Data
public class CourseCreateRequest extends RangeBaseRequest {
    /**
     * 课程名称
     */
    private String title;
    
    /**
     * 课程类型：video, document, series, article
     */
    private String type;
    
    /**
     * 封面图URL
     */
    private String coverImage;
    
    /**
     * 讲师ID（仅视频/系列课）
     */
    private Integer instructorId;

    /**
     * 嘉宾名称
     */
    private String guestName;
    
    /**
     * 课程简介
     */
    private String description;
    
    /**
     * 学分，默认1
     */
    private Integer credit;
    
    /**
     * 分类ID列表
     */
    private String categoryIds;
    
    /**
     * 是否允许评论，默认true
     */
    private Boolean allowComments;
    
    /**
     * 是否置顶，默认false
     */
    private Boolean isTop;
    
    /**
     * 文章内容(仅文章类型)
     */
    private String article;

    /**
     * 附件类型，非文章类型必填
     */
    private String appendixType;

    /**
     * 附件文件列表，系列课类型
     */
    private List<com.learn.dto.course.sub.SeriesCourseFile> appendixFiles;
    
    /**
     * 附件路径，非文章类型必填
     */
    private String appendixPath;
    
    /**
     * 状态：draft, published，默认draft
     */
    private String status;
    
    /**
     * 创建人ID
     */
    private Long creatorId;
    
    /**
     * 创建人名称
     */
    private String creatorName;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    /**
     * 是否可评
     */
    private Boolean ifIsCitable;
}
