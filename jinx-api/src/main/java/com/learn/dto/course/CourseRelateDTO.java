package com.learn.dto.course;

import lombok.Data;

import java.io.Serializable;

/**
 * 课程关联DTO
 */
@Data
public class CourseRelateDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 关联ID
     */
    private Long id;
    
    /**
     * 关联内容ID
     */
    private Long contentId;
    
    /**
     * 关联内容名称
     */
    private String name;
    
    /**
     * 关联类型：train-培训, map-学习地图
     */
    private String type;
    
    /**
     * 创建人ID
     */
    private Long creatorId;
    
    /**
     * 创建人名称
     */
    private String creatorName;
    
    /**
     * 创建时间
     */
    private String gmtCreate;
}
