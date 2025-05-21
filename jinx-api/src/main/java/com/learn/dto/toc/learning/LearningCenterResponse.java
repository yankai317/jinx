package com.learn.dto.toc.learning;

import lombok.Data;

import java.util.List;

/**
 * 学习中心响应参数
 */
@Data
public class LearningCenterResponse {
    /**
     * 分类列表
     */
    private List<CategoryItemDTO> categories;

    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 学习内容列表
     */
    private List<LearningCenterItemDTO> list;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;
}
