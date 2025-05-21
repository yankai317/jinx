package com.learn.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.common.dto.UserTokenInfo;
import com.learn.common.util.JwtUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.user.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT Token拦截器
 * 拦截所有请求，检查JWT token并将用户信息存储到ThreadLocal中
 */
@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserInfoService userInfoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头中获取token
        String authHeader = request.getHeader("Authorization");
        
        // 如果请求头中包含JWT token
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            try {
                // 提取token
                String token = authHeader.substring(7);
                
                // 解析token获取用户信息
                UserTokenInfo userInfo = jwtUtil.extractUserId(token);
                
                // 将用户信息存储到ThreadLocal中
                UserContextHolder.setUserInfo(userInfo);
                
                log.debug("JWT token解析成功，用户ID: {}, 企业ID: {}", userInfo.getUserId(), userInfo.getCorpId());
            } catch (Exception e) {
                log.error("JWT token解析失败", e);
                // 设置响应状态码和内容类型
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");
                
                // 创建统一的错误响应
                ApiResponse<Object> errorResponse = ApiResponse.builder()
                        .code(401)
                        .message("未登录或登录已过期")
                        .data(null)
                        .build();
                
                // 将错误响应写入响应体
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                
                return false;
            }
        } else {
            log.debug("请求未携带JWT token");
        }
        
        // 如果token有效或不需要token，允许请求继续处理
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除ThreadLocal中的用户信息，防止内存泄漏
        UserContextHolder.clear();
    }
}
