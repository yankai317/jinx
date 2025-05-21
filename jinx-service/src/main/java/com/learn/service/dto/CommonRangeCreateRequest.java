package com.learn.service.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 通用范围创建请求对象
 */
@Data
public class CommonRangeCreateRequest {
    /**
     * 功能模块类型（如可见范围、协同管理、任务指派）
     */
    private String modelType;
    
    /**
     * 业务模块类型（如培训、课程、学习地图）
     */
    private String type;
    
    /**
     * 业务模块ID
     */
    private Long typeId;
    
    /**
     * 目标范围类型和IDs的映射，key为目标类型，value为目标IDs列表
     */
    private Map<String, List<Long>> targetTypeAndIds;
    
    /**
     * 创建人ID
     */
    private Long creatorId;
    
    /**
     * 创建人名称
     */
    private String creatorName;
}
