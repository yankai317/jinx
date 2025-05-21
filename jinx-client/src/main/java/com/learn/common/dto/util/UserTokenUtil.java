package com.learn.common.dto.util;

import com.google.common.collect.Lists;
import com.learn.common.dto.UserTokenInfo;
import com.learn.common.util.JwtUtil;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.user.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yujintao
 * @date 2025/4/23
 */
@Component
@Slf4j
public class UserTokenUtil {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 从请求中获取当前用户ID
     *
     * @param request HTTP请求
     * @return 用户ID，如果未登录则返回null
     */
    public Long getCurrentUserId(HttpServletRequest request) {
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

    /**
     * 从请求中获取当前企业ID
     *
     * @param request HTTP请求
     * @return 企业ID，如果未登录或不存在则返回null
     */
    public String getCurrentCorpId(HttpServletRequest request) {
        // 从请求头中获取token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        // 提取token
        String token = authHeader.substring(7);

        // 从token中获取企业ID
        try {
            UserTokenInfo userInfo = jwtUtil.extractUserId(token);
            return userInfo.getCorpId();
        } catch (Exception e) {
            log.error("从token中提取企业ID失败", e);
            return null;
        }
    }

    /**
     * 从请求中获取当前用户信息
     *
     * @param request HTTP请求
     * @return 用户ID，如果未登录则返回null
     */
    public UserInfoResponse getCurrentUserInfo(HttpServletRequest request) {
        // 从请求头中获取token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("获取登陆信息失败");
        }

        // 提取token
        String token = authHeader.substring(7);

        // 从token中获取用户ID
        try {
            UserTokenInfo userInfo = jwtUtil.extractUserId(token);
            return userInfoService.getUserInfo(userInfo.getUserId());
        } catch (Exception e) {
            log.error("从token中提取用户ID失败", e);
            throw new RuntimeException("获取登陆信息失败");
        }
    }

    public Map<String, List<Long>> getRange(Long userId) {
        Map<String, List<Long>> targetTypeAndIds = new HashMap<>();
        // 添加角色ID
        targetTypeAndIds.put("user", Lists.newArrayList(userId));
        UserInfoResponse userInfo = userInfoService.getUserInfo(userId);
        if (userInfo.getRoles() != null && !userInfo.getRoles().isEmpty()) {
            List<Long> roleIds = userInfo.getRoles().stream()
                    .map(UserInfoResponse.RoleInfo::getId)
                    .collect(Collectors.toList());
            targetTypeAndIds.put("role", roleIds);
        }
        if (userInfo.getDepartments() != null && !userInfo.getDepartments().isEmpty()) {
            List<Long> departmentIds = userInfo.getDepartments().stream()
                    .map(UserInfoResponse.DepartmentInfo::getId)
                    .collect(Collectors.toList());
            targetTypeAndIds.put("department", departmentIds);
        }
        return targetTypeAndIds;
    }
}
