package com.learn.service.course;

import com.learn.dto.course.CourseRelateDTO;

import java.util.List;

/**
 * @author yujintao
 * @description 负责课程的外部关联，例如课程信息的关联
 * @date 2025/4/21
 */
public interface RelateService {
    
    /**
     * 查询课程关联的内容
     *
     * @param courseId 课程ID
     * @return 关联内容列表
     */
    List<CourseRelateDTO> queryContentRelate(Long courseId, String type);
}
