package com.learn.dto.train;

import lombok.Data;

import java.util.List;

/**
 * 培训批量删除请求
 */
@Data
public class TrainBatchDeleteRequest {
    /**
     * 培训ID列表
     */
    private List<Long> ids;
}
