package com.learn.dto.map;

import lombok.Data;

/**
 * 学习地图批量删除响应
 */
@Data
public class LearningMapBatchDeleteResponse {
    /**
     * 成功删除的数量
     */
    private Integer successCount;

    /**
     * 失败删除的数量
     */
    private Integer failCount;
}
