package com.learn.dto.cateogry;

import lombok.Data;

import java.util.List;

/**
 * 分类DTO，用于返回给前端
 */
@Data
public class CategoryDTO {
    /**
     * 自增id
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类id，顶级分类为0
     */
    private Long parentId;

    /**
     * 分类层级，从1开始
     */
    private Integer level;

    /**
     * 同级分类排序
     */
    private Integer sort;

    /**
     * 子分类列表
     */
    private List<CategoryDTO> children;
}
