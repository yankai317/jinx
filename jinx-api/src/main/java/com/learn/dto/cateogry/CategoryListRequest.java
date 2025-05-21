package com.learn.dto.cateogry;

import lombok.Data;

/**
 * 获取分类列表请求
 */
@Data
public class CategoryListRequest {
    /**
     * 父分类ID，不传则获取顶级分类
     */
    private Long parentId;

    /**
     * 是否递归获取子分类，默认false
     */
    private Boolean recursive = false;
}
