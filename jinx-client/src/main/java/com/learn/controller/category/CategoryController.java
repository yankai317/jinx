package com.learn.controller.category;

import com.learn.common.converter.Converter;
import com.learn.common.util.CategoryTreeUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.dto.cateogry.*;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.Category;
import com.learn.service.category.CategoryManageService;
import com.learn.service.category.CategoryQueryService;
import com.learn.service.user.UserInfoService;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/api/category")
@CrossOrigin
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryQueryService categoryQueryService;

    @Autowired
    private CategoryManageService categoryManageService;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 创建分类
     *
     * @param request 创建分类请求
     * @return 创建的分类信息
     */
    @PostMapping("/create")
    public ApiResponse<CategoryDTO> createCategory(@RequestBody CategoryCreateRequest request) {
        log.info("创建分类请求入参:{}", Json.toJson(request));
        // 参数校验
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ApiResponse.error(400, "分类名称不能为空");
        }if (request.getParentId() == null) {
            return ApiResponse.error(400, "父分类ID不能为空");
        }
        
        // 构建Category实体
        Category category = new Category();
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setSort(request.getSort());
        // 调用服务创建分类
        Category createdCategory = categoryManageService.createCategory(category);

        // 转换为DTO
        CategoryDTO categoryDTO = convertToCategoryDTO(createdCategory);

        return ApiResponse.success("创建成功", categoryDTO);
    }

    /**
     * 获取分类列表
     *
     * @param parentId 父分类ID，不传则查询所有分类
     * @param tree 是否返回树形结构，默认false
     * @return 分类列表
     */
    @GetMapping("/list")
    public ApiResponse<List<CategoryDTO>> getCategoryList(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false, defaultValue = "false") Boolean tree) {
        // 调用服务获取分类列表
        List<Category> categoryList = categoryQueryService.getCategoryList(parentId, tree);
        
        // 转换为DTO
        List<CategoryDTO> categoryDTOList = convertToCategoryDTOList(categoryList);
        
        return ApiResponse.success("获取成功", categoryDTOList);
    }
    
    /**
     * 更新分类
     *
     * @param request 更新分类请求
     * @return 更新结果
     */
    @PostMapping("/update")
    public ApiResponse<Boolean> updateCategory(@RequestBody CategoryUpdateRequest request) {
        log.info("更新分类请求入参:{}", Json.toJson(request));
        // 参数校验
        if (request.getId() == null) {
            return ApiResponse.error(400, "分类ID不能为空");
        }
        
        // 构建Category实体
        Category category = new Category();
        category.setId(request.getId());
        category.setName(request.getName());
        category.setSort(request.getSort());
        category.setParentId(request.getParentId());
        // 调用服务更新分类
        categoryManageService.updateCategory(category);
        return ApiResponse.success("更新成功", true);
    }
    
    /**
     * 更新分类信息
     *
     * @param requests 更新分类请求
     * @return 更新结果
     */
    @PostMapping("/sort/update")
    public ApiResponse<Boolean> updateCategoryPut(@RequestBody List<CategoryAllUpdateRequest> requests) {
        log.info("更新分类请求入参:{}", Json.toJson(requests));
        // 参数校验
        if (CollectionUtils.isEmpty(requests)) {
            return ApiResponse.error(500, "分类ID不能为空");
        }

        Long userId = UserContextHolder.getUserId();
        if (null == userId) {
            log.error("未登录！！");
            throw new RuntimeException("当前用户未登录");
        }
        UserInfoResponse userInfo = userInfoService.getUserInfo(userId);
        if (null == userInfo) {
            log.error("用户没有权限更新类目！！用户信息不存在");
            throw new RuntimeException("用户没有权限更新类目");
        }
        try {
            Date now = new Date();
            // 将requests转为单层的List<Category>
            List<Category> categories = new ArrayList<>();
            processCategoryRequests(requests, categories, 0L, "", 1);

            // 当前的category
            List<Category> categoryList = categoryManageService.listAll();
            Map<Long, Category> categoryMap = categoryList.stream()
                    .collect(Collectors.toMap(Category::getId, category -> category));
            
            // 筛选需要更新的分类
            List<Category> needUpdateCategories = new ArrayList<>();
            for (Category newCategory : categories) {
                // 在现有分类中查找对应的分类
                Category existingCategory = categoryMap.get(newCategory.getId());
                
                if (existingCategory != null) {
                    // 如果parentId或sort发生变化，则需要更新
                    if (!existingCategory.getParentId().equals(newCategory.getParentId()) ||
                        !existingCategory.getSort().equals(newCategory.getSort())) {
                        // 只更新这四个字段
                        existingCategory.setParentId(newCategory.getParentId());
                        existingCategory.setSort(newCategory.getSort());
                        existingCategory.setPath(newCategory.getPath());
                        existingCategory.setLevel(newCategory.getLevel());
                        existingCategory.setGmtModified(now);
                        existingCategory.setUpdaterId(userId);
                        existingCategory.setUpdaterName(userInfo.getNickname());
                        needUpdateCategories.add(existingCategory);
                    }
                }
            }

            // 调用服务批量更新分类
            if (!needUpdateCategories.isEmpty()) {
                categoryManageService.batchUpdateCategory(needUpdateCategories);
            }

            return ApiResponse.success("更新成功", true);
        } catch (Exception e) {
            log.error("批量更新分类失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 递归处理分类请求，构建分类层级关系
     *
     * @param requests 分类请求列表
     * @param categories 结果分类列表
     * @param parentId 父分类ID
     * @param parentPath 父分类路径
     * @param level 当前层级
     */
    private void processCategoryRequests(List<CategoryAllUpdateRequest> requests, List<Category> categories, 
            Long parentId, String parentPath, int level) {
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }

        for (int i = 0; i < requests.size(); i++) {
            CategoryAllUpdateRequest request = requests.get(i);
            Category category = new Category();
            category.setId(request.getId());
            category.setParentId(parentId);
            category.setSort(request.getSort()); // 设置排序号，从1开始
            category.setLevel(level);

            // 构建分类路径
            // 如果是第一级类目（parentId为0），path就是类目本身的id
            // 否则，path是父类目path加上当前类目id
            String currentPath = (parentId == 0L) ? 
                    String.valueOf(category.getId()) : 
                    parentPath + "," + category.getId();
            category.setPath(currentPath);

            categories.add(category);

            // 递归处理子分类
            if (!CollectionUtils.isEmpty(request.getChildren())) {
                processCategoryRequests(request.getChildren(), categories, category.getId(), currentPath, level + 1);
            }
        }
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
        
        // 先将所有分类转换为DTO
        List<CategoryDTO> allDtoList = categoryList.stream()
                .map(category -> {
                    CategoryDTO dto = Converter.fromEntity(category);
                    // 初始化子分类列表，避免空指针
                    dto.setChildren(new ArrayList<>());
                    return dto;
                })
                .collect(Collectors.toList());

        // 构建完整的类目树结构
        CategoryTreeUtil.buildCategoryTree(allDtoList);
        
        // 只返回顶级分类（parentId为0或null的分类）
        // 子分类会通过children属性关联到父分类中
        return allDtoList.stream()
                .filter(dto -> dto.getParentId() == null || dto.getParentId() == 0)
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

        // 处理子分类
        CategoryDTO dto = Converter.fromEntity(category);
        List<Category> children = category.getChildren();
        if (children != null && !children.isEmpty()) {
            // 递归转换子分类
            List<CategoryDTO> childrenDTOs = children.stream()
                    .map(this::convertToCategoryDTO)
                    .collect(Collectors.toList());
            dto.setChildren(childrenDTOs);
        } else {
            // 初始化空的子分类列表，避免空指针
            dto.setChildren(new ArrayList<>());
        }
        
        return dto;
    }
    
    /**
     * 删除分类
     *
     * @param id 分类ID
     * @return 删除结果
     */
    @PostMapping("/delete")
    public ApiResponse<Boolean> deleteCategory(@RequestParam Long id) {
        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "分类ID不能为空");
        }try {
            // 调用服务删除分类
            Boolean result = categoryManageService.deleteCategory(id);
            
            return ApiResponse.success("删除成功", result);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }
    
    /**
     * 获取分类列表（POST方法）
     *
     * @param request 获取分类列表请求
     * @return 分类列表
     */
    @PostMapping("/list")
    public ApiResponse<List<CategoryDTO>> getCategoryList(@RequestBody CategoryListRequest request) {
        log.info("获取分类列表请求入参:{}", Json.toJson(request));
        
        // 调用服务获取分类列表
        List<Category> categoryList = categoryQueryService.getCategoryListWithRecursive(
                request.getParentId(), 
                request.getRecursive());
        
        // 转换为DTO
        List<CategoryDTO> categoryDTOList = convertToCategoryDTOList(categoryList);
        
        return ApiResponse.success("获取成功", categoryDTOList);
    }
    
    /**
     * 删除分类
     *
     * @param id 分类ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Boolean> deleteCategoryById(@PathVariable Long id) {
        log.info("删除分类请求入参, id:{}", id);
        
        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "分类ID不能为空");
        }
        
        try {
            // 调用服务删除分类
            Boolean result = categoryManageService.deleteCategory(id);
            
            return ApiResponse.success("删除成功", result);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }
}
