package com.learn.dto.course;

import lombok.Data;

import java.util.List;

/**
 * 批量更新课程创建人请求
 */
@Data
public class CourseCreatorUpdateRequest {
    /**
     * 课程ID列表
     */
    private List<Long> courseIds;
    
    /**
     * 新的创建人ID
     */
    private Long newCreatorId;
} 