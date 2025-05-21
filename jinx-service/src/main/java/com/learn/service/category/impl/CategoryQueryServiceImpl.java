package com.learn.service.category.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.learn.infrastructure.repository.entity.Category;
import com.learn.infrastructure.repository.mapper.CategoryMapper;
import com.learn.service.category.CategoryQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yujintao
 * @description 类目查询服务
 * @date 2025/4/21
 */
@Service
@Slf4j
public class CategoryQueryServiceImpl implements CategoryQueryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> getCategoryListWithRecursive(Long parentId, Boolean recursive) {
        LambdaQueryWrapper<Category> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Category::getIsDel, 0); // 只查询未删除的分类

        // 如果指定了父分类ID，则只查询该父分类下的分类
        if (parentId != null) {
            queryWrapper.eq(Category::getParentId, parentId);
        } else {
            // 如果没有指定父分类ID，则查询顶级分类
            queryWrapper.eq(Category::getParentId, 0L);
        }

        // 按照排序字段升序排列
        queryWrapper.orderByAsc(Category::getSort);

        List<Category> categoryList = categoryMapper.selectList(queryWrapper);

        // 如果需要递归获取子分类
        if (recursive != null && recursive) {
            for (Category category : categoryList) {
                // 递归获取子分类
                List<Category> children = getChildCategories(category.getId());
                if (!children.isEmpty()) {
                    category.setChildren(children);
                }
            }
        }

        return categoryList;
    }

    /**
     * 递归获取子分类
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    private List<Category> getChildCategories(Long parentId) {
        LambdaQueryWrapper<Category> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Category::getIsDel, 0); // 只查询未删除的分类
        queryWrapper.eq(Category::getParentId, parentId);
        queryWrapper.orderByAsc(Category::getSort);

        List<Category> children = categoryMapper.selectList(queryWrapper);
        
        // 递归获取每个子分类的子分类
        for (Category child : children) {
            List<Category> grandChildren = getChildCategories(child.getId());
            if (!grandChildren.isEmpty()) {
                child.setChildren(grandChildren);
            }
        }
        
        return children;
    }


    @Override
    public List<Category> getCategoryList(Long parentId, Boolean tree) {
        LambdaQueryWrapper<Category> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Category::getIsDel, 0); // 只查询未删除的分类

        // 如果指定了父分类ID，则只查询该父分类下的分类
        if (parentId != null) {
            queryWrapper.eq(Category::getParentId, parentId);
        }

        // 按照排序字段升序排列
        queryWrapper.orderByAsc(Category::getSort);

        List<Category> categoryList = categoryMapper.selectList(queryWrapper);

        // 如果需要返回树形结构
        if (tree != null && tree) {
            return buildCategoryTree(categoryList);
        }

        return categoryList;
    }


    /**
     * 构建分类树形结构
     *
     * @param categoryList 分类列表
     * @return 树形结构的分类列表
     */
    private List<Category> buildCategoryTree(List<Category> categoryList) {
        // 将分类列表按照parentId分组
        Map<Long, List<Category>> categoryMap = categoryList.stream()
                .collect(Collectors.groupingBy(Category::getParentId));

        // 遍历分类列表，设置子分类
        categoryList.forEach(category -> {
            List<Category> children = categoryMap.get(category.getId());
            if (children != null) {
                category.setChildren(children);
            }
        });

        // 返回顶级分类
        return categoryList.stream()
                .filter(category -> category.getParentId() == 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> fullSearchCategoryId(List<Long> searchCategoryIds) {
        if (searchCategoryIds == null || searchCategoryIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用自定义的Mapper方法，通过全文索引查询path中包含指定ID的分类
        List<Category> resultCategories = new ArrayList<>();
        
        // 对每个ID进行查询，并合并结果
        List<Category> categories = categoryMapper.findCategoriesByPathContainsId(searchCategoryIds);
        resultCategories.addAll(categories);
        
        // 去重并提取ID列表
        return resultCategories.stream()
                .map(Category::getId)
                .distinct()
                .collect(Collectors.toList());
    }
}
