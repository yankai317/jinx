package com.learn.service.category;

import com.learn.infrastructure.repository.entity.Category;

import java.util.List;

/**
 * @author yujintao
 * @date 2025/4/21
 */
public interface CategoryQueryService {

    /**
     * 获取分类列表
     *
     * @param parentId 父分类ID，不传则查询所有分类
     * @param tree 是否返回树形结构，默认false
     * @return 分类列表
     */
    List<Category> getCategoryList(Long parentId, Boolean tree);

    /**
     * 获取分类列表
     *
     * @param parentId 父分类ID，不传则获取顶级分类
     * @param recursive 是否递归获取子分类，默认false
     * @return 分类列表
     */
    List<Category> getCategoryListWithRecursive(Long parentId, Boolean recursive);


    /**
     * 根据searchCategoryIds 通过sql全文检索，查询path中包含的searchCategoryId的
     *
     * @param searchCategoryIds
     * @return
     */
    List<Long> fullSearchCategoryId(List<Long> searchCategoryIds);
}
