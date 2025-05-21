package com.learn.service.category;

import com.learn.infrastructure.repository.entity.Category;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryManageService {
    /**
     * 创建分类
     *
     * @param category 分类信息
     * @return 创建的分类
     */
    Category createCategory(Category category);
    
    /**
     * 更新分类
     *
     * @param category 分类信息
     * @return 更新后的分类
     */
    void updateCategory(Category category);

    /**
     * 查询类目列表
     * @return
     */
    List<Category> listAll();

    /**
     * 批量更新分类
     * 只更新parentId,path,level,sort四个字段
     *
     * @param categories 需要更新的分类列表
     */
    void batchUpdateCategory(List<Category> categories);

    /**
     * 删除分类
     *
     * @param id 分类ID
     * @return 是否删除成功
     */
    Boolean deleteCategory(Long id);
}
