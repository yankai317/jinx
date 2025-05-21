package com.learn.config;

import com.learn.common.interceptor.JwtTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.util.Arrays;
import java.util.List;

/**
 * Web MVC 配置类
 * 处理前端单页面应用(SPA)的路由配置，确保刷新页面时不会报404错误
 * 配置全局拦截器，处理JWT token
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtTokenInterceptor jwtTokenInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源映射，并添加自定义资源解析器处理SPA路由
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false)
                .addResolver(new PushStateResourceResolver());
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加JWT token拦截器，应用于所有请求
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/**");
    }
    
    /**
     * 自定义资源解析器，用于处理SPA前端路由
     * 将所有非API请求且不存在对应静态资源的请求都重定向到index.html
     */
    private static class PushStateResourceResolver implements ResourceResolver {
        // 需要排除的路径，通常是API路径，这些路径不会被重定向到index.html
        private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api", 
            "/v1", 
            "/v2", 
            "/auth", 
            "/swagger-ui", 
            "/swagger-resources", 
            "/webjars"
        );
        
        private Resource index = new ClassPathResource("/static/index.html");
        
        @Override
        public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
            // 首先尝试正常解析资源（检查是否是静态资源）
            Resource resource = chain.resolveResource(request, requestPath, locations);

            // 如果资源存在或者是排除的API路径，直接返回
            if (resource != null || isExcluded(requestPath)) {
                return resource;
            }

            // 对于所有其他请求（前端路由），返回index.html
            return index;
        }
        
        @Override
        public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
            // 正常解析URL路径
            String resolvedPath = chain.resolveUrlPath(resourcePath, locations);

            // 如果能够解析或者是排除的路径，直接返回
            if (resolvedPath != null || isExcluded(resourcePath)) {
                return resolvedPath;
            }

            // 否则返回null，表示使用默认的index.html
            try {
                if (index.exists()) {
                    return "index.html";
                }
            } catch (Exception e) {
                // 忽略异常
            }

            return null;
        }
        
        /**
         * 检查请求路径是否应该被排除（不重定向到index.html）
         * 主要用于区分API请求和前端路由请求
         */
        private boolean isExcluded(String path) {
            return EXCLUDED_PATHS.stream().anyMatch(prefix -> path.startsWith(prefix));
        }
    }
} 
