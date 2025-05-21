package com.learn.service.course;

import com.learn.dto.course.CourseDetailDTO;
import com.learn.dto.course.CourseListRequest;
import com.learn.dto.course.CourseListResponse;
import com.learn.dto.course.CourseStatisticsDTO;

/**
 * @author yujintao
 * @description 定义课程查询接口，例如课程列表查询
 * @date 2025/4/21
 */
public interface CourseQueryService {
    
    /**
     * 获取课程列表
     *
     * @param request 查询条件
     * @return 课程列表响应
     */
    CourseListResponse getCourseList(CourseListRequest request);
    
    /**
     * 获取课程详情
     *
     * @param id 课程ID
     * @return 课程详情
     */
    CourseDetailDTO getCourseDetail(Long id);
    
    /**
     * 获取课程统计数据
     *
     * @param id 课程ID
     * @return 课程统计数据
     */
    CourseStatisticsDTO getCourseStatistics(Long id);
}
