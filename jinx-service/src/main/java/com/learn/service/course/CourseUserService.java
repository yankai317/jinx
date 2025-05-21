package com.learn.service.course;

import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.dto.course.CourseAssignRequest;
import com.learn.dto.course.CourseAssignResponse;
import com.learn.dto.course.CourseLearnerRequest;
import com.learn.dto.course.CourseLearnerResponse;

/**
 * @author yujintao
 * @Description 定义课程的用户操作，例如用户课程进度，记录课程日志
 * @date 2025/4/21
 */
public interface CourseUserService {
    
    /**
     * 获取课程学习人员列表
     *
     * @param courseId 课程ID
     * @param request 查询条件
     * @return 学习人员列表
     */
    CourseLearnerResponse getCourseLearners(Long courseId, CourseLearnerRequest request);
    
    /**
     * 指派课程给用户
     *
     * @param request 指派课程请求
     * @return 指派结果
     */
    AssignResponse assignCourse(AssignRequest request);
}
