package com.learn.dto.cateogry;

import lombok.Data;

/**
 * 创建分类请求DTO
 */
@Data
public class CategoryCreateRequest {
    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类id，顶级分类为0
     */
    private Long parentId;

    /**
     * 同级分类排序
     */
    private Integer sort;
}
