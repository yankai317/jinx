package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.search.SearchContentRequest;
import com.learn.dto.toc.search.SearchContentResponse;
import com.learn.service.toc.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * C端搜索控制器
 */
@RestController
@RequestMapping("/api/search")
@Slf4j
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 搜索课程、培训和学习地图内容
     *
     * @param keyword 搜索关键词
     * @param type 内容类型：all-全部，course-课程，train-培训，map-学习地图，exam-考试，practice-练习，默认all
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @param request HTTP请求
     * @return 搜索结果响应
     */
    @GetMapping
    public ApiResponse<SearchContentResponse> searchContent(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        try {
            // 1. 验证token，获取用户ID
            Long userId = userTokenUtil.getCurrentUserId(request);
            if (userId == null) {
                log.warn("用户未登录或登录已过期");
                return ApiResponse.error("用户未登录或登录已过期");
            }

            // 2. 检查参数合法性
            if (!StringUtils.hasText(keyword)) {
                log.warn("搜索关键词不能为空");
                return ApiResponse.error("搜索关键词不能为空");
            }

            // 验证类型参数
            if (!StringUtils.hasText(type) || 
                    !("all".equals(type) || BizType.COURSE.equals(type) || BizType.TRAIN.equals(type) || BizType.LEARNING_MAP.equals(type) ||
                      "exam".equals(type) || "practice".equals(type))) {
                log.warn("内容类型参数不合法: {}", type);
                return ApiResponse.error("内容类型参数不合法，可选值：all, course, train, map, exam, practice");
            }

            // 验证分页参数
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize == null || pageSize < 1 || pageSize > 100) {
                pageSize = 10;
            }

            // 3. 构建搜索请求
            SearchContentRequest searchRequest = new SearchContentRequest();
            searchRequest.setKeyword(keyword);
            searchRequest.setType(type);
            searchRequest.setPageNum(pageNum);
            searchRequest.setPageSize(pageSize);

            // 4. 调用服务执行搜索
            SearchContentResponse response = searchService.searchContent(userId, searchRequest);
            // 5. 返回结果
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("搜索内容异常, 关键词: {}, 类型: {}", keyword, type, e);
            return ApiResponse.error("搜索内容失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索课程、培训和学习地图内容（POST方法）
     *
     * @param request 搜索请求
     * @param httpRequest HTTP请求
     * @return 搜索结果响应
     */
    @PostMapping
    public ApiResponse<SearchContentResponse> searchContentPost(
            @RequestBody SearchContentRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 1. 验证token，获取用户ID
            Long userId = userTokenUtil.getCurrentUserId(httpRequest);
            if (userId == null) {
                log.warn("用户未登录或登录已过期");
                return ApiResponse.error("用户未登录或登录已过期");
            }

            // 2. 检查参数合法性
            if (request == null) {
                log.warn("搜索请求不能为空");
                return ApiResponse.error("搜索请求不能为空");
            }

            if (!StringUtils.hasText(request.getKeyword())) {
                log.warn("搜索关键词不能为空");
                return ApiResponse.error("搜索关键词不能为空");
            }

            // 验证类型参数
            String type = request.getType();
            if (!StringUtils.hasText(type) || 
                    !("all".equals(type) || BizType.COURSE.equals(type) || BizType.TRAIN.equals(type) || BizType.LEARNING_MAP.equals(type) || 
                      "exam".equals(type) || "practice".equals(type))) {
                log.warn("内容类型参数不合法: {}", type);
                return ApiResponse.error("内容类型参数不合法，可选值：all, course, train, map, exam, practice");
            }

            // 验证分页参数
            if (request.getPageNum() == null || request.getPageNum() < 1) {
                request.setPageNum(1);
            }
            if (request.getPageSize() == null || request.getPageSize() < 1 || request.getPageSize() > 100) {
                request.setPageSize(10);
            }

            // 3. 调用服务执行搜索
            SearchContentResponse response = searchService.searchContent(userId, request);
            
            // 4. 返回结果
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("搜索内容异常, 请求: {}", request, e);
            return ApiResponse.error("搜索内容失败: " + e.getMessage());
        }
    }
}
