package com.learn.dto.map;

import lombok.Data;

import java.util.List;

/**
 * 学习地图批量删除请求
 */
@Data
public class LearningMapBatchDeleteRequest {
    /**
     * 学习地图ID列表
     */
    private List<Long> ids;
}
