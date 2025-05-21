package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.learning.LearningTasksResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.toc.LearningTaskService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 学习任务控制器
 */
@RestController
@RequestMapping("/api/learning")
@CrossOrigin
@Slf4j
public class LearningTaskController {

    @Autowired
    private LearningTaskService learningTaskService;

    /**
     * 获取用户的学习任务列表
     *
     * @param type 任务类型：train-培训，map-学习地图
     * @param required 是否必修：0-全部，1-必修，2-选修
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @param request HTTP请求
     * @return 学习任务列表响应
     */
    @GetMapping("/tasks")
    public ApiResponse<LearningTasksResponse> getLearningTasks(
            @RequestParam(required = true) String type,
            @RequestParam(required = false, defaultValue = "0") Integer required,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        
        log.info("获取学习任务列表请求参数: type={}, required={}, pageNum={}, pageSize={}", 
                type, required, pageNum, pageSize);
        
        // 参数校验
        if (!type.equalsIgnoreCase(BizType.TRAIN) && !type.equalsIgnoreCase(BizType.LEARNING_MAP)) {
            return ApiResponse.error(400, "任务类型参数错误，只能为train或map");
        }
        
        if (required != null && (required < 0 || required > 2)) {
            return ApiResponse.error(400, "是否必修参数错误，只能为0、1或2");
        }
        
        if (pageNum != null && pageNum < 1) {
            return ApiResponse.error(400, "页码必须大于等于1");
        }
        
        if (pageSize != null && pageSize < 1) {
            return ApiResponse.error(400, "每页条数必须大于等于1");
        }
        
        // 获取当前登录用户信息
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录或登录已过期");
        }
        
        // 转换required参数为status
        String status = "all";
        if (required == 1) {
            status = "required";
        } else if (required == 2) {
            status = "elective";
        }
        
        try {
            // 调用服务获取学习任务列表
            LearningTasksResponse response = learningTaskService.getLearningTasks(
                    userId, type, status, pageNum, pageSize);
            
            return ApiResponse.success("获取成功", response);
        } catch (Exception e) {
            log.error("获取学习任务列表失败", e);
            return ApiResponse.error(500, "获取学习任务列表失败: " + e.getMessage());
        }
    }
}
