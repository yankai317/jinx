package com.learn.controller.course;

import com.learn.dto.common.ApiResponse;
import com.learn.dto.course.CourseStatisticsDTO;
import com.learn.dto.course.CourseLearnerRequest;
import com.learn.dto.course.CourseLearnerResponse;
import com.learn.service.course.CourseStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 课程统计控制器
 */
@RestController
@RequestMapping("/api/course")
@CrossOrigin
@Slf4j
public class CourseStatisticsController {

    @Autowired
    private CourseStatisticsService courseStatisticsService;

    /**
     * 获取课程统计数据
     *
     * @param id 课程ID
     * @return 课程统计数据
     */
    @GetMapping("/statistics/{id}")
    public ApiResponse<CourseStatisticsDTO> getCourseStatistics(@PathVariable Long id) {
        log.info("获取课程统计数据, courseId: {}", id);
        
        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }
        
        // 调用服务获取课程统计数据
        CourseStatisticsDTO statistics = courseStatisticsService.getCourseStatistics(id);
        
        if (statistics == null) {
            return ApiResponse.error(404, "课程不存在");
        }
        
        return ApiResponse.success("获取成功", statistics);
    }
    
    /**
     * 获取课程学习人员列表
     *
     * @param id 课程ID
     * @param request 请求参数
     * @return 学习人员列表
     */
    @PostMapping("/statistics/learners/{id}")
    public ApiResponse<CourseLearnerResponse> getCourseLearners(@PathVariable Long id, @RequestBody CourseLearnerRequest request) {
        log.info("获取课程学习人员列表, courseId: {}", id);
        
        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }
        
        // 调用服务获取课程学习人员列表
        CourseLearnerResponse response = courseStatisticsService.getCourseLearners(id, request);
        
        return ApiResponse.success("获取成功", response);
    }
}
