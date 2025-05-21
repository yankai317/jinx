package com.learn.dto.train;

import lombok.Data;

/**
 * 培训批量删除响应
 */
@Data
public class TrainBatchDeleteResponse {
    /**
     * 成功删除的数量
     */
    private Integer successCount;

    /**
     * 失败删除的数量
     */
    private Integer failCount;
}
