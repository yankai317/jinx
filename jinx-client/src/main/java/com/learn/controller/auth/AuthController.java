package com.learn.controller.auth;

import com.learn.common.dto.RbacPermissionResponse;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.auth.LoginRequest;
import com.learn.dto.auth.LoginResponse;
import com.learn.dto.common.ApiResponse;
import com.learn.service.auth.AuthService;
import com.learn.service.auth.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import shade.com.alibaba.fastjson2.JSONObject;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录请求: info={}", JSONObject.toJSONString(request));

        try {
            // 参数校验
            if (StringUtils.isEmpty(request.getUsername()) && StringUtils.isEmpty(request.getPassword()) &&
                    (StringUtils.isEmpty(request.getThirdPartyType()) || StringUtils.isEmpty(request.getThirdPartyCode()))) {
                return ApiResponse.error(400, "用户名密码或第三方登录信息不能为空");
            }

            // 用户名密码登录校验
            if (!StringUtils.isEmpty(request.getUsername()) && StringUtils.isEmpty(request.getPassword())) {
                return ApiResponse.error(400, "密码不能为空");
            }

            // 第三方登录校验
            if (!StringUtils.isEmpty(request.getThirdPartyType()) && StringUtils.isEmpty(request.getThirdPartyCode())) {
                return ApiResponse.error(400, "第三方授权码不能为空");
            }

            // 调用服务进行登录
            LoginResponse response = authService.login(request);

            // 获取用户权限
            if (response != null && response.getUserInfo() != null && response.getUserInfo().getUserId() != null) {
                RbacPermissionResponse permissions = rbacService.getUserPermissions(response.getUserInfo().getUserId());
                if (permissions != null && permissions.getPermissionCodes() != null) {
                    response.setPermissions(permissions.getPermissionCodes());
                }
            }

            return ApiResponse.success("登录成功", response);
        } catch (Exception e) {
            log.error("用户登录异常", e);
            return ApiResponse.error(500, "登录失败: " + e.getMessage());
        }
    }
}
