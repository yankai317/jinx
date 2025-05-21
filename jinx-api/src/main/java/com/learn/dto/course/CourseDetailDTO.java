package com.learn.dto.course;

import com.learn.dto.course.sub.CollaboratorsDTO;
import com.learn.dto.course.sub.VisibilityDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 课程详情DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailDTO {
    /**
     * 课程ID
     */
    private Long id;
    
    /**
     * 课程名称
     */
    private String title;
    
    /**
     * 课程类型
     */
    private String type;
    
    /**
     * 封面图URL
     */
    private String coverImage;
    
    /**
     * 讲师ID
     */
    private Integer instructorId;
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
     * 分类名称列表
     */
    private List<String> categoryNames;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 是否允许评论
     */
    private Boolean allowComments;
    
    /**
     * 是否置顶
     */
    private Boolean isTop;
    
    /**
     * 查看数
     */
    private Integer viewCount;
    
    /**
     * 完成人数
     */
    private Integer completeCount;
    
    /**
     * 文章内容(仅文章类型)
     */
    private String article;
    
    /**
     * 附件类型
     */
    private String appendixType;
    
    /**
     * 附件路径
     */
    private String appendixPath;
    
    /**
     * 发布时间
     */
    private String publishTime;
    
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
     * 可见范围信息
     */
    private VisibilityDTO visibility;
    
    /**
     * 协同管理信息
     */
    private CollaboratorsDTO collaborators;
    
    /**
     * 是否可引用
     */
    private Boolean ifIsCitable;
}
