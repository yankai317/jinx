package com.learn.service.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 通用范围查询请求对象
 */
@Data
@Accessors(chain = true)
public class CommonRangeQueryRequest {

    /**
     * 范围ids
     */
    private List<Long> ids;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 功能模块类型（如可见范围、协同管理、任务指派）
     */
    private String modelType;
    
    /**
     * 业务模块类型（如培训、课程、学习地图）
     */
    private String type;
    
    /**
     * 业务模块ID，用于查询特定业务的范围配置
     */
    private Long typeId;
    /**
     * 业务模块ids
     */
    private List<Long> typeIds;
    
    /**
     * 目标类型和目标ID的映射，用于查询特定目标是否在范围内
     * key为目标类型（如部门、角色、人员），value为目标ID列表
     */
    private Map<String, List<Long>> targetTypeAndIds;
}
