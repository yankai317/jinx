package com.learn.service.course;

import com.learn.dto.course.CourseStatisticsDTO;
import com.learn.dto.course.CourseLearnerRequest;
import com.learn.dto.course.CourseLearnerResponse;

/**
 * 课程统计服务接口
 */
public interface CourseStatisticsService {
    /**
     * 获取课程统计数据
     *
     * @param courseId 课程ID
     * @return 课程统计数据
     */
    CourseStatisticsDTO getCourseStatistics(Long courseId);
    
    /**
     * 获取课程学习人员列表
     *
     * @param courseId 课程ID
     * @param request 请求参数
     * @return 学习人员列表
     */
    CourseLearnerResponse getCourseLearners(Long courseId, CourseLearnerRequest request);
}
