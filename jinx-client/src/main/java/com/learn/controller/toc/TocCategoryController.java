package com.learn.controller.toc;

import com.learn.common.converter.Converter;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.cateogry.CategoryDTO;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.Category;
import com.learn.service.category.CategoryQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * C端分类控制器
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin
@Slf4j
public class TocCategoryController {

    @Autowired
    private CategoryQueryService categoryQueryService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取分类列表，支持树形结构
     *
     * @param parentId 父分类ID，默认0表示获取顶级分类
     * @param tree 是否返回树形结构，默认false
     * @param request HTTP请求
     * @return 分类列表
     */
    @GetMapping
    public ApiResponse<List<CategoryDTO>> getCategories(
            @RequestParam(required = false, defaultValue = "0") Integer parentId,
            @RequestParam(required = false, defaultValue = "false") Boolean tree,
            HttpServletRequest request) {
        log.info("C端获取分类列表请求，parentId:{}, tree:{}", parentId, tree);
        
        // 验证token
        UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(request);
        if (userInfo == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        
        // 调用服务获取分类列表
        List<Category> categoryList = categoryQueryService.getCategoryList(parentId.longValue(), tree);
        
        // 转换为DTO
        List<CategoryDTO> categoryDTOList = convertToCategoryDTOList(categoryList);
        
        return ApiResponse.success(categoryDTOList);
    }
    
    /**
     * 将Category实体列表转换为CategoryDTO列表
     *
     * @param categoryList Category实体列表
     * @return CategoryDTO列表
     */
    private List<CategoryDTO> convertToCategoryDTOList(List<Category> categoryList) {
        if (categoryList == null || categoryList.isEmpty()) {
            return new ArrayList<>();
        }
        
        return categoryList.stream()
                .map(this::convertToCategoryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将Category实体转换为CategoryDTO
     *
     * @param category Category实体
     * @return CategoryDTO
     */
    private CategoryDTO convertToCategoryDTO(Category category) {
        if (category == null) {
            return null;
        }
        
        CategoryDTO dto = Converter.fromEntity(category);
        
        // 处理子分类
        List<Category> children = category.getChildren();
        if (children != null && !children.isEmpty()) {
            dto.setChildren(convertToCategoryDTOList(children));
        }
        
        return dto;
    }
}
