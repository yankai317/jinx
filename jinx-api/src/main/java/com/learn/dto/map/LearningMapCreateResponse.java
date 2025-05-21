package com.learn.dto.map;

import lombok.Data;

/**
 * 创建学习地图响应
 */
@Data
public class LearningMapCreateResponse {
    /**
     * 学习地图ID
     */
    private Long id;
    
    /**
     * 学习地图名称
     */
    private String name;
}
