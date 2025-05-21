package com.learn.dto.cateogry;

import lombok.Data;

/**
 * 更新分类请求DTO
 */
@Data
public class CategoryUpdateRequest {
    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 同级分类排序
     */
    private Integer sort;
}
