package com.learn.common.util;

import com.learn.common.converter.Converter;
import com.learn.dto.cateogry.CategoryDTO;
import com.learn.infrastructure.repository.entity.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yujintao
 * @date 2025/4/29
 */
public class CategoryTreeUtil {

    public static void buildCategoryTree(List<CategoryDTO> allDtoList) {
        // 创建一个Map，key为分类ID，value为对应的CategoryDTO对象
        Map<Long, CategoryDTO> dtoMap = allDtoList.stream()
                .collect(Collectors.toMap(CategoryDTO::getId, dto -> dto));


        // 遍历所有分类，将子分类添加到父分类的children列表中
        for (CategoryDTO dto : allDtoList) {
            // 如果不是顶级分类（parentId不为0或null），则将当前分类添加到父分类的children列表中
            if (dto.getParentId() != null && dto.getParentId() != 0) {
                CategoryDTO parentDto = dtoMap.get(dto.getParentId());
                if (parentDto != null) {
                    // 确保父分类的children列表已初始化
                    if (parentDto.getChildren() == null) {
                        parentDto.setChildren(new ArrayList<>());
                    }
                    // 将当前分类添加到父分类的children列表中
                    parentDto.getChildren().add(dto);
                }
            }
        }
    }

}
