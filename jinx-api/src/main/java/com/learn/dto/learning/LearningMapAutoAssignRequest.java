package com.learn.dto.learning;

import lombok.Data;

/**
 * 学习地图自动指派请求
 */
@Data
public class LearningMapAutoAssignRequest {
    
    /**
     * 学习地图ID
     */
    private Long mapId;
    
    /**
     * 是否启用自动指派
     */
    private Boolean enableAutoAssign;
}
