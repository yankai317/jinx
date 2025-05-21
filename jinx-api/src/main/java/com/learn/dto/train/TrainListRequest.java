package com.learn.dto.train;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 培训列表查询请求
 */
@Data
public class TrainListRequest {
    /**
     * 培训名称，模糊匹配
     */
    private String title;

    /**
     * 状态：draft, published
     */
    private String status;

    /**
     * 分类ID
     */
    private List<Long> categoryIds;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建开始时间
     */
    private String startTime;

    /**
     * 创建结束时间
     */
    private String endTime;

    /**
     * 只看我创建的
     */
    private Boolean onlyMine;

    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;

    /**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式：asc, desc
     */
    private String sortOrder;

    /**
     * 是否可引用：1-可引用，0-不可引用
     */
    private Boolean ifIsCitable;

    private Map<String, List<Long>> targetTypeAndIds;
}
