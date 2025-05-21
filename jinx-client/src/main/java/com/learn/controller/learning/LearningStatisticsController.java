package com.learn.controller.learning;

import com.learn.common.dto.UserTokenInfo;
import com.learn.common.util.JwtUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.learning.LearningMapAutoAssignRequest;
import com.learn.service.learning.LearningStatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习统计控制器
 */
@RestController
@RequestMapping("/api/learning")
@CrossOrigin
@Slf4j
public class LearningStatisticsController {

    @Autowired
    private LearningStatisticsService learningStatisticsService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取课程学习统计
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 课程学习统计
     */
    @GetMapping("/statistics/course")
    public ApiResponse<Map<String, Object>> getCourseStatistics(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(userId, request);
            if (currentUserId == null) {
                return ApiResponse.error(401, "未授权，请先登录");
            }
            
            // 获取课程学习统计
            Long courseCount = learningStatisticsService.getUserCourseCount(currentUserId);
            Map<String, Object> result = new HashMap<>();
            result.put("count", courseCount);
            
            return ApiResponse.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取课程学习统计异常", e);
            return ApiResponse.error(500, "获取课程学习统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取培训完成率统计
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 培训完成率统计
     */
    @GetMapping("/statistics/train")
    public ApiResponse<Map<String, Object>> getTrainStatistics(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(userId, request);
            if (currentUserId == null) {
                return ApiResponse.error(401, "未授权，请先登录");
            }
            
            // 获取培训完成率统计
            double completionRate = learningStatisticsService.getUserTrainCompletionRate(currentUserId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("completionRate", completionRate);
            
            return ApiResponse.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取培训完成率统计异常", e);
            return ApiResponse.error(500, "获取培训完成率统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取学习地图统计
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 学习地图统计
     */
    @GetMapping("/statistics/map")
    public ApiResponse<Map<String, Object>> getMapStatistics(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(userId, request);
            if (currentUserId == null) {
                return ApiResponse.error(401, "未授权，请先登录");
            }
            
            // 获取学习地图统计
            Long mapCount = learningStatisticsService.getUserMapCount(currentUserId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("count", mapCount);
            
            return ApiResponse.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取学习地图统计异常", e);
            return ApiResponse.error(500, "获取学习地图统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取证书统计
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 证书统计
     */
    @GetMapping("/statistics/certificate")
    public ApiResponse<Map<String, Object>> getCertificateStatistics(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(userId, request);
            if (currentUserId == null) {
                return ApiResponse.error(401, "未授权，请先登录");
            }
            
            // 获取证书统计
            Long certificateCount = learningStatisticsService.getUserCertificateCount(currentUserId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("count", certificateCount);
            
            return ApiResponse.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取证书统计异常", e);
            return ApiResponse.error(500, "获取证书统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近学习的课程
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 最近学习的课程列表
     */
    @GetMapping("/recent/course")
    public ApiResponse<List<Map<String, Object>>> getRecentCourses(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(userId, request);
            if (currentUserId == null) {
                return ApiResponse.error(401, "未授权，请先登录");
            }
            
            // 获取最近学习的课程
            List<Map<String, Object>> recentCourses = learningStatisticsService.getUserRecentCourses(currentUserId);
            
            return ApiResponse.success("获取成功", recentCourses);
        } catch (Exception e) {
            log.error("获取最近学习的课程异常", e);
            return ApiResponse.error(500, "获取最近学习的课程失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近学习的培训
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 最近学习的培训列表
     */
    @GetMapping("/recent/train")
    public ApiResponse<List<Map<String, Object>>> getRecentTrains(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(userId, request);
            if (currentUserId == null) {
                return ApiResponse.error(401, "未授权，请先登录");
            }
            
            // 获取最近学习的培训
            List<Map<String, Object>> recentTrains = learningStatisticsService.getUserRecentTrains(currentUserId);
            
            return ApiResponse.success("获取成功", recentTrains);
        } catch (Exception e) {
            log.error("获取最近学习的培训异常", e);
            return ApiResponse.error(500, "获取最近学习的培训失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近学习的地图
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 最近学习的地图列表
     */
    @GetMapping("/recent/map")
    public ApiResponse<List<Map<String, Object>>> getRecentMaps(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(userId, request);
            if (currentUserId == null) {
                return ApiResponse.error(401, "未授权，请先登录");
            }
            
            // 获取最近学习的地图
            List<Map<String, Object>> recentMaps = learningStatisticsService.getUserRecentMaps(currentUserId);
            
            return ApiResponse.success("获取成功", recentMaps);
        } catch (Exception e) {
            log.error("获取最近学习的地图异常", e);
            return ApiResponse.error(500, "获取最近学习的地图失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置学习地图自动指派状态
     *
     * @param request 请求对象
     * @param httpServletRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/map/autoAssign")
    public ApiResponse<Boolean> setLearningMapAutoAssign(@RequestBody LearningMapAutoAssignRequest request, HttpServletRequest httpServletRequest) {
        try {
            // 参数校验
            if (request == null || request.getMapId() == null || request.getEnableAutoAssign() == null) {
                return ApiResponse.error(400, "参数错误，请检查请求参数");
            }
            
            // 设置学习地图自动指派状态
            boolean result = learningStatisticsService.setLearningMapAutoAssign(request.getMapId(), request.getEnableAutoAssign());
            
            if (result) {
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error(500, "设置失败，请检查学习地图是否存在");
            }
        } catch (Exception e) {
            log.error("设置学习地图自动指派状态异常", e);
            return ApiResponse.error(500, "设置学习地图自动指派状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 从请求中获取用户ID
     *
     * @param userId 请求参数中的用户ID
     * @param request HTTP请求
     * @return 用户ID
     */
    private Long getUserIdFromRequest(Long userId, HttpServletRequest request) {
        // 如果请求参数中有用户ID，则直接返回
        if (userId != null) {
            return userId;
        }
        
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
