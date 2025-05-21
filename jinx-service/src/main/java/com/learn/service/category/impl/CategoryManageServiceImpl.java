package com.learn.service.category.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.learn.common.exception.CommonException;
import com.learn.infrastructure.repository.entity.Category;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.mapper.CategoryMapper;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.service.category.CategoryManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yujintao
 * @description 类目管理服务
 * @date 2025/4/21
 */
@Service
@Slf4j
public class CategoryManageServiceImpl implements CategoryManageService {

    @Autowired
    private CategoryMapper categoryMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Category createCategory(Category category) {
        // 检查分类名称是否重复
        LambdaQueryWrapper<Category> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Category::getName, category.getName())
                .eq(Category::getParentId, category.getParentId())
                .eq(Category::getIsDel, 0);

        Long count = categoryMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new CommonException("分类名称已存在");
        }

        // 设置默认值
        if (category.getSort() == null) {
            category.setSort(0);
        }

        // 设置分类层级和路径
        if (category.getParentId() == 0) {
            // 顶级分类
            category.setLevel(1);
            // 路径暂时为空，插入后再更新
            category.setPath("");
        } else {
            // 子分类，需要查询父分类信息
            Category parentCategory = categoryMapper.selectById(category.getParentId());
            if (parentCategory == null) {
                throw new CommonException("父分类不存在");
            }

            category.setLevel(parentCategory.getLevel() + 1);
            
            // 设置路径为父分类路径，插入后再更新
            if (parentCategory.getPath() == null || parentCategory.getPath().isEmpty()) {
                category.setPath(parentCategory.getId().toString());
            } else {
                category.setPath(parentCategory.getPath() + "," + parentCategory.getId());
            }
        }

        // 设置创建时间和更新时间
        Date now = new Date();
        category.setGmtCreate(now);
        category.setGmtModified(now);
        category.setIsDel(0);

        // 插入数据库
        categoryMapper.insert(category);

        // 更新分类路径，加上自己的ID
        if (category.getParentId() == 0) {
            // 顶级分类，路径为自己的ID
            category.setPath(category.getId().toString());
        } else {
            // 子分类，路径为已有路径 + 自己的ID
            category.setPath(category.getPath() + "," + category.getId());
        }

        // 更新数据库
        categoryMapper.updateById(category);

        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Category category) {
        // 检查分类是否存在
        Category existingCategory = categoryMapper.selectById(category.getId());
        if (existingCategory == null || existingCategory.getIsDel() == 1) {
            throw new CommonException("分类不存在");
        }

        // 判断是否修改了父分类
        boolean isParentChanged = category.getParentId() != null && !category.getParentId().equals(existingCategory.getParentId());
        
        // 如果修改了父分类，需要检查新的父分类是否存在
        if (isParentChanged) {
            // 检查新的父分类是否存在
            if (category.getParentId() != 0) {
                Category newParentCategory = categoryMapper.selectById(category.getParentId());
                if (newParentCategory == null || newParentCategory.getIsDel() == 1) {
                    throw new CommonException("父分类不存在");
                }
                
                // 检查新的父分类是否是当前分类的子分类，避免循环引用
                if (newParentCategory.getPath() != null && newParentCategory.getPath().contains(category.getId().toString())) {
                    throw new CommonException("不能选择当前分类的子分类作为父分类");
                }
            }
        }

        // 检查分类名称是否重复（排除自身）
        if (category.getName() != null) {
            LambdaQueryWrapper<Category> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(Category::getName, category.getName())
                    .eq(Category::getParentId, isParentChanged ? category.getParentId() : existingCategory.getParentId())
                    .eq(Category::getIsDel, 0)
                    .ne(Category::getId, category.getId());

            Long count = categoryMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new CommonException("分类名称已存在");
            }
        }

        // 更新分类信息
        Category updateCategory = new Category();
        updateCategory.setId(category.getId());
        
        // 如果名称不为空，则更新名称
        if (StringUtils.isNotBlank(category.getName())) {
            updateCategory.setName(category.getName());
        }

        // 如果排序字段不为空，则更新排序字段
        if (category.getSort() != null) {
            updateCategory.setSort(category.getSort());
        }
        
        // 如果修改了父分类，则更新父分类ID、路径和层级
        if (isParentChanged) {
            updateCategory.setParentId(category.getParentId());
            
            // 计算新的路径和层级
            if (category.getParentId() == 0) {
                // 顶级分类
                updateCategory.setLevel(1);
                updateCategory.setPath(category.getId().toString());
            } else {
                // 子分类，需要查询父分类信息
                Category parentCategory = categoryMapper.selectById(category.getParentId());
                updateCategory.setLevel(parentCategory.getLevel() + 1);
                
                // 设置路径为父分类路径 + 自己的ID
                if (parentCategory.getPath() == null || parentCategory.getPath().isEmpty()) {
                    updateCategory.setPath(parentCategory.getId() + "," + category.getId());
                } else {
                    updateCategory.setPath(parentCategory.getPath() + "," + category.getId());
                }
            }
            
            // 更新子分类的路径和层级
            updateChildrenPathAndLevel(category.getId(), updateCategory.getPath(), updateCategory.getLevel());
        }

        // 设置更新时间
        updateCategory.setGmtModified(new Date());

        // 更新数据库
        categoryMapper.updateById(updateCategory);
    }

    @Override
    public List<Category> listAll() {
        LambdaQueryWrapper<Category> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Category::getIsDel, 0);

        return categoryMapper.selectList(queryWrapper);
    }

    /**
     * 更新子分类的路径和层级
     *
     * @param parentId 父分类ID
     * @param parentPath 父分类路径
     * @param parentLevel 父分类层级
     */
    private void updateChildrenPathAndLevel(Long parentId, String parentPath, Integer parentLevel) {
        // 查询所有子分类
        LambdaQueryWrapper<Category> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Category::getParentId, parentId)
                .eq(Category::getIsDel, 0);
        
        List<Category> children = categoryMapper.selectList(queryWrapper);
        
        for (Category child : children) {
            // 更新子分类的路径和层级
            Category updateChild = new Category();
            updateChild.setId(child.getId());
            updateChild.setLevel(parentLevel + 1);
            updateChild.setPath(parentPath + "," + child.getId());
            updateChild.setGmtModified(new Date());
            
            categoryMapper.updateById(updateChild);
            
            // 递归更新子分类的子分类
            updateChildrenPathAndLevel(child.getId(), updateChild.getPath(), updateChild.getLevel());
        }
    }



    @Autowired
    private CoursesMapper coursesMapper;
    
    @Autowired
    private TrainMapper trainMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCategory(Long id) {
        // 检查分类是否存在
        Category category = categoryMapper.selectById(id);
        if (category == null || category.getIsDel() == 1) {
            throw new CommonException("分类不存在");
        }

        // 检查是否有子分类
        LambdaQueryWrapper<Category> childQueryWrapper = Wrappers.lambdaQuery();
        childQueryWrapper.eq(Category::getParentId, id)
                .eq(Category::getIsDel, 0);
        Long childCount = categoryMapper.selectCount(childQueryWrapper);
        if (childCount > 0) {
            throw new CommonException("该分类下存在子分类，无法删除");
        }

        // 构建精确匹配分类ID的条件
        String idStr = id.toString();

        // 检查是否有关联的课程
        LambdaQueryWrapper<Courses> coursesQueryWrapper = Wrappers.lambdaQuery();
        coursesQueryWrapper.eq(Courses::getIsDel, 0)
                .and(wrapper -> wrapper
                        .like(Courses::getCategoryIds,  idStr)
                );
        Long coursesCount = coursesMapper.selectCount(coursesQueryWrapper);
        if (coursesCount > 0) {
            throw new CommonException("该分类下存在关联的课程，无法删除");
        }

        // 检查是否有关联的培训
        LambdaQueryWrapper<Train> trainQueryWrapper = Wrappers.lambdaQuery();
        trainQueryWrapper.eq(Train::getIsDel, 0)
                .and(wrapper -> wrapper
                        .like(Train::getCategoryIds,  idStr)
                );
        Long trainCount = trainMapper.selectCount(trainQueryWrapper);
        if (trainCount > 0) {
            throw new CommonException("该分类下存在关联的培训，无法删除");
        }

        // 检查是否有关联的学习地图
        LambdaQueryWrapper<LearningMap> learningMapQueryWrapper = Wrappers.lambdaQuery();
        learningMapQueryWrapper.eq(LearningMap::getIsDel, 0)
                .and(wrapper -> wrapper
                        .like(LearningMap::getCategoryIds, "," + idStr + ",")
                );
        Long learningMapCount = learningMapMapper.selectCount(learningMapQueryWrapper);
        if (learningMapCount > 0) {
            throw new CommonException("该分类下存在关联的学习/地图/培训，无法删除");
        }

        // 逻辑删除分类
        Category updateCategory = new Category();
        updateCategory.setId(id);
        updateCategory.setIsDel(1);
        updateCategory.setGmtModified(new Date());

        int result = categoryMapper.updateById(updateCategory);

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateCategory(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }

        log.info("批量更新分类，数量：{}", categories.size());
        
        for (Category category : categories) {
            LambdaUpdateWrapper<Category> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Category::getId, category.getId())
                    .set(Category::getParentId, category.getParentId())
                    .set(Category::getPath, category.getPath())
                    .set(Category::getLevel, category.getLevel())
                    .set(Category::getSort, category.getSort());
            
            categoryMapper.update(null, updateWrapper);
        }
        
        log.info("批量更新分类完成");
    }
}
