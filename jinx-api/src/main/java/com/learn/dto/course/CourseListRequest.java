package com.learn.dto.course;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 课程列表查询请求
 */
@Data
public class CourseListRequest {
    /**
     * 课程名称，模糊匹配
     */
    private String title;
    
    /**
     * 课程类型：video,document, series, article
     */
    private String type;

    /**
     * 过滤支持引用的类型
     */
    private Boolean ifIsCitable;
    
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
     * 当前用户ID，用于可见范围判断
     */
    private Long userId;
    
    /**
     * 目标类型和目标ID的映射，用于可见范围判断
     * key为目标类型（如部门、角色、人员），value为目标ID列表
     */
    private Map<String, List<Long>> targetTypeAndIds;
}
