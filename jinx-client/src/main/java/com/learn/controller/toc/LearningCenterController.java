package com.learn.controller.toc;

import com.google.common.collect.Lists;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.learning.LearningCenterRequest;
import com.learn.dto.toc.learning.LearningCenterResponse;
import com.learn.service.category.CategoryQueryService;
import com.learn.service.toc.LearningCenterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端学习中心控制器
 */
@RestController
@RequestMapping("/api/learning")
@Slf4j
public class LearningCenterController {

    @Autowired
    private LearningCenterService learningCenterService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    @Autowired
    private CategoryQueryService categoryQueryService;

    /**
     * 获取学习中心数据
     *
     * @param request HTTP请求
     * @param type 内容类型：course-课程，train-培训，map-学习地图，exam-考试，practice-练习
     * @param categoryId 分类ID
     * @param contentTypes 内容类型，多个用逗号分隔：video,document,series
     * @param keyword 搜索关键词
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 学习中心数据响应
     */
    @GetMapping("/center")
    public ApiResponse<LearningCenterResponse> getLearningCenter(
            HttpServletRequest request,
            String type,
            Integer categoryId,
            String contentTypes,
            String keyword,
            Integer pageNum,
            Integer pageSize) {
        try {
            // 1. 验证token，获取用户ID
            Long userId = userTokenUtil.getCurrentUserId(request);
            if (userId == null) {
                log.warn("用户未登录或登录已过期");
                return ApiResponse.error(401, "用户未登录或登录已过期");
            }

            // 2. 构建请求参数
            LearningCenterRequest learningCenterRequest = new LearningCenterRequest();
            learningCenterRequest.setType(type);
            learningCenterRequest.setContentTypes(contentTypes);
            learningCenterRequest.setKeyword(keyword);
            
            // 设置默认值
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 10;
            }
            learningCenterRequest.setPageNum(pageNum);
            learningCenterRequest.setPageSize(pageSize);
            if (null != categoryId) {
                List<Long> categoryIds = categoryQueryService.fullSearchCategoryId(Lists.newArrayList(Long.valueOf(categoryId)));
                learningCenterRequest.setCategoryIds(categoryIds);
            }
            
            // 记录请求参数
            log.info("获取学习中心数据，userId:{}, type:{}, categoryId:{}, contentTypes:{}, keyword:{}, pageNum:{}, pageSize:{}",
                    userId, type, categoryId, contentTypes, keyword, pageNum, pageSize);

            // 3. 调用服务获取学习中心数据
            LearningCenterResponse response = learningCenterService.getLearningCenter(userId, learningCenterRequest);

            // 4. 返回结果
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取学习中心数据异常", e);
            return ApiResponse.error("获取学习中心数据失败: " + e.getMessage());
        }
    }
}
