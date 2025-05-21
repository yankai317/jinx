package com.learn.controller.auth;

import com.learn.common.dto.RbacPermissionResponse;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限控制器，需要参考 AuthController 中登陆态的校验,
 * 用于获取当前用户的所有权限
 * 具体调用: com.learn.service.auth.RbacService
 */
@RestController
@RequestMapping("/api/permission")
@CrossOrigin
@Slf4j
public class RbacController {

    @Autowired
    private com.learn.service.auth.RbacService rbacService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取当前用户的所有权限
     *
     * @param request HTTP请求
     * @return 权限响应
     */
    @GetMapping("/getUserPermissions")
    public ApiResponse<RbacPermissionResponse> getUserPermissions(HttpServletRequest request) {
        // 从session或token中获取用户ID
        Long userId = userTokenUtil.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error(400, "用户未登录");
        }
        
        try {
            // 调用rbacService获取用户权限
            RbacPermissionResponse permissions = rbacService.getUserPermissions(userId);
//            List<String> permissionCodes = new ArrayList<>();
//            permissionCodes.add("course:publish");
//            permissions = new RbacPermissionResponse();
//            permissions.setPermissionCodes(permissionCodes);
            return ApiResponse.success(permissions);
        } catch (Exception e) {
            log.error("获取用户权限失败", e);
            return ApiResponse.error("获取用户权限失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查用户是否有特定权限
     *
     * @param permissionCode 权限编码
     * @param request HTTP请求
     * @return 检查结果
     */
    @GetMapping("/checkPermission")
    public ApiResponse<Boolean> checkPermission(@RequestParam String permissionCode, HttpServletRequest request) {
        // 从session或token中获取用户ID
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        
        try {
            // 调用rbacService检查用户是否有权限
            Boolean hasPermission = rbacService.checkPermission(userId, permissionCode);
            return ApiResponse.success("检查成功", hasPermission);
        } catch (Exception e) {
            log.error("检查用户权限失败", e);
            return ApiResponse.error("检查用户权限失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有权限列表
     *
     * @return 权限列表
     */
    @GetMapping("/getAllPermissions")
    public ApiResponse<List<RbacPermissionResponse.PermissionDTO>> getAllPermissions() {
        try {
            // 调用rbacService获取所有权限
            List<RbacPermissionResponse.PermissionDTO> permissions = rbacService.getAllPermissions();
            return ApiResponse.success(permissions);
        } catch (Exception e) {
            log.error("获取所有权限失败", e);
            return ApiResponse.error("获取所有权限失败: " + e.getMessage());
        }
    }
}
