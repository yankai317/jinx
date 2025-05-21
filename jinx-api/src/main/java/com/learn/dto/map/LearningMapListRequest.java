package com.learn.dto.map;

import lombok.Data;

import java.util.List;

/**
 * 学习地图列表查询请求
 */
@Data
public class LearningMapListRequest {
    /**
     * 地图名称，模糊匹配
     */
    private String name;
    
    /**
     * 分类ID
     */
    private List<Long> categoryIds;
    
    /**
     * 创建人ID
     */
    private Long creatorId;/**
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
}
