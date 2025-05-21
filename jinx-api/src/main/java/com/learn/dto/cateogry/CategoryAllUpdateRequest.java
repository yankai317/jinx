package com.learn.dto.cateogry;

import lombok.Data;

import java.util.List;

/**
 * 更新分类请求DTO
 */
@Data
public class CategoryAllUpdateRequest {
    /**
     * 分类ID
     */
    private Long id;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 子类目
     */
    private List<CategoryAllUpdateRequest> children;
}
