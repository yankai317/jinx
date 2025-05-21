package com.learn.service.dto;

import lombok.Data;

/**
 * 通用范围删除请求对象
 */
@Data
public class CommonRangeDeleteRequest {
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
}
