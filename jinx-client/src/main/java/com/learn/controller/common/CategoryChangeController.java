package com.learn.controller.common;

import com.learn.dto.common.ApiResponse;
import com.learn.dto.common.CategoryChangeRequest;
import com.learn.service.category.CategoryChangeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类目变更控制器
 * 
 * @author yujintao
 * @date 2025/5/10
 */
@RestController
@RequestMapping("/api/common/category")
@Slf4j
public class CategoryChangeController {

    @Autowired
    private CategoryChangeService categoryChangeService;

    /**
     * 更新业务对象的类目信息
     *
     * @param request 类目变更请求
     * @return 更新结果
     */
    @PostMapping("/change")
    public ApiResponse<Object> changeCategory(@RequestBody CategoryChangeRequest request) {
        log.info("更新类目信息请求，业务ID：{}，业务类型：{}，类目：{}",
                request.getBizId(), request.getBizType(), request.getCategoryIds());

        // 参数校验
        if (request.getBizId() == null || request.getBizId() <= 0) {
            return ApiResponse.error(400, "业务ID不能为空且必须大于0");
        }

        if (StringUtils.isBlank(request.getBizType())) {
            return ApiResponse.error(400, "业务类型不能为空");
        }

        if (StringUtils.isBlank(request.getCategoryIds())) {
            return ApiResponse.error(400, "类目信息不能为空");
        }

        try {
            // 调用服务层方法进行类目变更
            boolean updated = categoryChangeService.changeCategory(request);

            if (updated) {
                return ApiResponse.success("更新类目信息成功");
            } else {
                return ApiResponse.error(404, "未找到对应的业务对象或不支持的业务类型");
            }
        } catch (Exception e) {
            log.error("更新类目信息失败", e);
            return ApiResponse.error(500, "更新类目信息失败：" + e.getMessage());
        }
    }
}
    
