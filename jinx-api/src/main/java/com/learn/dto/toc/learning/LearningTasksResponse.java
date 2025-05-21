package com.learn.dto.toc.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学习任务列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningTasksResponse {
    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 学习任务列表
     */
    private List<LearningTaskDTO> list;

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
