package com.learn.controller.course;

import com.learn.common.dto.UserTokenInfo;
import com.learn.common.util.JwtUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.course.CourseAssignRequest;
import com.learn.dto.course.CourseAssignResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.course.CourseUserService;
import com.learn.service.user.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程指派控制器
 */
@RestController
@RequestMapping("/api/course")
@Slf4j
public class CourseAssignController {

    @Autowired
    private CourseUserService courseUserService;
    
    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 从请求中获取当前用户ID
     *
     * @param request HTTP请求
     * @return 用户ID，如果未登录则返回null
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 从请求头中获取token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        // 提取token
        String token = authHeader.substring(7);
        
        // 从token中获取用户ID
        try {
            UserTokenInfo userInfo = jwtUtil.extractUserId(token);
            return userInfo.getUserId();
        } catch (Exception e) {
            log.error("从token中提取用户ID失败", e);
            return null;
        }
    }
}
