package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.toc.personal.UserProfileResponse;
import com.learn.dto.toc.user.UserInfoResponse;
import com.learn.service.toc.PersonalCenterService;
import com.learn.service.toc.CUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端用户控制器
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class TocUserController {

    @Autowired
    private CUserService CUserService;
    
    @Autowired
    private PersonalCenterService personalCenterService;

    @Autowired
    private UserTokenUtil userTokenUtil;
    
    /**
     * 获取用户个人中心信息
     *
     * @param request HTTP请求
     * @return 用户个人中心信息响应
     */
    @GetMapping("/profile")
    public UserProfileResponse getUserProfile(HttpServletRequest request) {
        try {
            // 1. 验证token，获取用户ID
            Long userId = userTokenUtil.getCurrentUserId(request);
            if (userId == null) {
                log.warn("用户未登录或登录已过期");
                return UserProfileResponse.unauthorized();
            }

            // 2. 调用服务获取用户个人中心信息
            return personalCenterService.getUserProfile(userId);
        } catch (Exception e) {
            log.error("获取用户个人中心信息异常", e);
            return UserProfileResponse.error("获取用户个人中心信息失败: " + e.getMessage());
        }
    }
}
