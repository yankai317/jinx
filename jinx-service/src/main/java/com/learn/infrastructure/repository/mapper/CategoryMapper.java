package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author cys
* @description 针对表【category(分类表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.Category
*/
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 通过全文索引查询path中包含指定ID列表中任一ID的分类
     *
     * @param categoryIds 要查询的分类ID列表
     * @return 包含这些ID的分类列表
     */
    List<Category> findCategoriesByPathContainsId(@Param("categoryIds") List<Long> categoryIds);
}




