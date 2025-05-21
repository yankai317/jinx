package com.learn.dto.course;

import lombok.Data;

import java.util.List;

/**
 * 课程列表数据传输对象
 */
@Data
public class CourseListDTO {
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
     * 学分
     */
    private Integer credit;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 查看数
     */
    private Integer viewCount;
    
    /**
     * 完成人数
     */
    private Integer completeCount;
    
    /**
     * 创建人
     */
    private String creatorName;
    
    /**
     * 创建时间
     */
    private String gmtCreate;

    private String gmtModified;
    
    /**
     * 分类名称列表
     */
    private List<String> categoryNames;

    private String categoryIds;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
}
