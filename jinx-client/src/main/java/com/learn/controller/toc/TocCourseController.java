package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.course.CourseDetailResponse;
import com.learn.service.toc.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端课程控制器
 */
@RestController
@RequestMapping("/api/toc/course")
@Slf4j
public class TocCourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取课程详情
     *
     * @param id 课程ID
     * @param request HTTP请求
     * @return 课程详情响应
     */
    @GetMapping("/detail")
    public ApiResponse<CourseDetailResponse> getCourseDetail(@RequestParam Integer id, HttpServletRequest request) {
        try {
            // 1. 验证token，获取用户ID
            Long userId = userTokenUtil.getCurrentUserId(request);
            if (userId == null) {
                log.warn("用户未登录或登录已过期");
                return ApiResponse.error("用户未登录或登录已过期");
            }

            // 2. 检查参数合法性
            if (id == null || id <= 0) {
                log.warn("课程ID参数不合法: {}", id);
                return ApiResponse.error("课程ID不能为空且必须大于0");
            }

            // 3. 调用服务获取课程详情
            CourseDetailResponse courseDetail = courseService.getCourseDetail(id.longValue(), userId);
            
            // 4. 返回结果
            return ApiResponse.success(courseDetail);
        } catch (Exception e) {
            log.error("获取课程详情异常, 课程ID: {}", id, e);
            return ApiResponse.error("获取课程详情失败: " + e.getMessage());
        }
    }
}
