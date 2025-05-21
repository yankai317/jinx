package com.learn.dto.toc.learning;

import lombok.Data;

/**
 * 分类项目DTO
 */
@Data
public class CategoryItemDTO {
    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 该分类下的内容数量
     */
    private Integer count;
}
