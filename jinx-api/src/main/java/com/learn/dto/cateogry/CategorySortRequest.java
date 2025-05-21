package com.learn.dto.cateogry;

import lombok.Data;

import java.util.List;

/**
 * 分类排序请求
 */
@Data
public class CategorySortRequest {
    /**
     * 分类列表
     */
    private List<CategorySortItem> categoryList;

    /**
     * 分类排序项
     */
    @Data
    public static class CategorySortItem {
        /**
         * 分类ID
         */
        private Long id;

        /**
         * 父分类ID
         */
        private Long parentId;

        /**
         * 排序序号
         */
        private Integer sort;
    }
}
