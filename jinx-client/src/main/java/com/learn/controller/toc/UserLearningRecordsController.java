package com.learn.controller.toc;

import com.learn.common.dto.UserTokenInfo;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.personal.UserLearningRecordsRequest;
import com.learn.dto.toc.personal.UserLearningRecordsResponse;
import com.learn.dto.toc.personal.UserLearningTotalResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.toc.PersonalCenterService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.text.SimpleDateFormat;

/**
 * 用户学习记录控制器
 */
@RestController
@RequestMapping("/api/user/learning")
@Slf4j
public class UserLearningRecordsController {

    @Autowired
    private PersonalCenterService personalCenterService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取用户学习记录列表
     *
     * @param request HTTP请求
     * @param recordsRequest 学习记录请求参数
     * @return 学习记录响应
     */
    @GetMapping("/records")
    public ApiResponse<UserLearningRecordsResponse> getUserLearningRecords(UserLearningRecordsRequest recordsRequest) {
        
        // 验证token，获取用户信息
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        
        // 设置默认值
        if (recordsRequest.getPageNum() == null) {
            recordsRequest.setPageNum(1);
        }
        if (recordsRequest.getPageSize() == null) {
            recordsRequest.setPageSize(10);
        }
        
        try {
            // 调用服务获取学习记录
            UserLearningRecordsResponse response = personalCenterService.getUserLearningRecords(userId, recordsRequest);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取用户学习记录失败", e);
            return ApiResponse.error("获取用户学习记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户学习记录列表
     *
     * @return 学习记录响应
     */
    @GetMapping("/total")
    public ApiResponse<UserLearningTotalResponse> getTotal(boolean countSelective) {
        // 验证token，获取用户信息
        UserTokenInfo userInfo =  UserContextHolder.getUserInfo();
        if (userInfo == null || userInfo.getUserId() == null) {
            return ApiResponse.error("用户未登录");
        }
        try {
            // 调用服务获取学习记录
            UserLearningTotalResponse response = personalCenterService.getUserLearningTotal(userInfo.getUserId(), countSelective);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取用户学习记录失败", e);
            return ApiResponse.error("获取用户学习记录失败: " + e.getMessage());
        }
    }
}
