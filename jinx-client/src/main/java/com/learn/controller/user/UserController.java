package com.learn.controller.user;

import com.learn.common.dto.UserTokenInfo;
import com.learn.common.util.JwtUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.dto.UserCreateRequest;
import com.learn.service.dto.UserCreateResponse;
import com.learn.service.dto.UserDeleteRequest;
import com.learn.service.dto.UserDeleteResponse;
import com.learn.service.dto.UserQueryRequest;
import com.learn.service.dto.UserQueryResponse;
import com.learn.service.user.UserInfoService;
import com.learn.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin
@Slf4j
public class UserController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserInfoService userInfoService;


    /**
     * 获取用户列表
     *
     * @param request 查询用户请求
     * @return 用户列表
     */
    @PostMapping("/list")
    public ApiResponse<List<UserQueryResponse>> listUser(@RequestBody UserQueryRequest request) {
        try {
            if (Boolean.TRUE.equals(request.getQueryAdmin())) {
                return ApiResponse.success(userService.queryAdministrators(request));
            }
            List<UserQueryResponse> response = userService.queryUsers(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ApiResponse.error("获取用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户列表
     *
     * @param request 查询用户请求
     * @return 用户列表
     */
    @PostMapping("/list/administrators")
    public ApiResponse<List<UserQueryResponse>> listAdministrators(@RequestBody UserQueryRequest request) {
        try {
            List<UserQueryResponse> response = userService.queryAdministrators(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ApiResponse.error("获取用户列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量查询用户
     *
     * @param request 查询用户请求
     * @return 查询用户响应
     */
    @PostMapping("/query")
    public ApiResponse<List<UserQueryResponse>> queryUsers(@RequestBody UserQueryRequest request) {
        try {
            List<UserQueryResponse> response = userService.queryUsers(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("查询用户失败", e);
            return ApiResponse.error("查询用户失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除用户
     *
     * @param request 删除用户请求
     * @return 删除用户响应
     */
    @PostMapping("/delete")
    public ApiResponse<UserDeleteResponse> deleteUser(@RequestBody UserDeleteRequest request) {
        try {
            UserDeleteResponse response = userService.deleteUser(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ApiResponse.error("删除用户失败：" + e.getMessage());
        }
    }
    
//    /**
//     * 获取用户信息
//     *
//     * @param request HTTP请求
//     * @return 用户信息响应
//     */
//    @GetMapping("/info")
//    public Response<UserInfoResponse> getUserInfo(HttpServletRequest request) {
//        try {
//            // 从session或token中获取用户ID
//            Long userId = (Long) request.getAttribute("userId");
//            if (userId == null) {
//                return Response.fail("用户未登录");
//            }
//
//            UserInfoResponse response = userInfoService.getUserInfo(userId);
//            return Response.success(response);
//        } catch (Exception e) {
//            log.error("获取用户信息失败", e);
//            return Response.fail("获取用户信息失败：" + e.getMessage());
//        }
//    }
    
    /**
     * 根据ID获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息响应
     */
    @GetMapping("/info/{userId}")
    public ApiResponse<UserInfoResponse> getUserInfoById(@PathVariable Long userId) {
        try {
            if (userId == null) {
                return ApiResponse.error("用户ID不能为空");
            }
            
            UserInfoResponse response = userInfoService.getUserInfo(userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ApiResponse.error("获取用户信息失败：" + e.getMessage());
        }
    }

    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> getUserInfo(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
                return ApiResponse.error(401, "未授权，请先登录");
            }
            // 提取token
            String token = authHeader.substring(7);

            // 从token中获取用户信息
            UserTokenInfo userTokenInfo = jwtUtil.extractUserId(token);
            if (userTokenInfo == null || userTokenInfo.getUserId() == null) {
                return ApiResponse.error(401, "无效的token");
            }

            // 获取用户信息
            UserInfoResponse userInfo = userInfoService.getUserInfo(userTokenInfo.getUserId());

            return ApiResponse.success("获取成功", userInfo);
        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            return ApiResponse.error(500, "获取用户信息失败: " + e.getMessage());
        }
    }
} 
