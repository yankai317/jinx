package com.learn.common.converter;

import com.learn.dto.cateogry.CategoryDTO;
import com.learn.infrastructure.repository.entity.Category;

/**
 * @author yujintao
 * @date 2025/4/20
 */
public class Converter {


    /**
     * 从Category实体转换为CategoryDTO
     *
     * @param category Category实体
     * @return CategoryDTO
     */
    public static CategoryDTO fromEntity(Category category) {
        if (category == null) {
            return null;
        }

        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setLevel(category.getLevel());
        dto.setSort(category.getSort());

        return dto;
    }
}
