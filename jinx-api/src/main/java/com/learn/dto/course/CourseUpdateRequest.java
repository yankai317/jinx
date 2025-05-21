package com.learn.dto.course;

import com.learn.dto.common.RangeBaseRequest;
import com.learn.dto.course.sub.CollaboratorsDTO;
import com.learn.dto.course.sub.VisibilityDTO;
import lombok.Data;

import java.util.List;

/**
 * 更新课程请求
 */
@Data
public class CourseUpdateRequest extends RangeBaseRequest {
    /**
     * 课程ID
     */
    private Long id;

    /**
     * 课程名称
     */
    private String title;
    
    /**
     * 封面图URL
     */
    private String coverImage;
    
    /**
     * 讲师ID
     */
    private Integer instructorId;

    /**
     * 分享嘉宾
     */
    private String guestName;
    
    /**
     * 课程简介
     */
    private String description;/**
     * 学分
     */
    private Integer credit;

    /**
     * 分类ID列表
     */
    private String categoryIds;
    
    /**
     * 是否允许评论
     */
    private Boolean allowComments;
    
    /**
     * 是否置顶
     */
    private Boolean isTop;
    
    /**
     * 文章内容(仅文章类型)
     */
    private String article;
    
    /**
     * 附件类型
     */
    private String appendixType;
    
    /**
     * 附件文件列表，系列课类型
     */
    private List<com.learn.dto.course.sub.SeriesCourseFile> appendixFiles;
    
    /**
     * 附件路径
     */
    private String appendixPath;
    
    /**
     * 状态：draft, published
     */
    private String status;
    
    /**
     * 更新人ID
     */
    private Long updaterId;
    
    /**
     * 更新人名称
     */
    private String updaterName;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    /**
     * 是否可引用
     */
    private Boolean ifIsCitable;
}
