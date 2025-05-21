package com.learn.dto.train;

import lombok.Data;

import java.util.List;

/**
 * 培训列表查询响应
 */
@Data
public class TrainListResponse {
    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 培训列表
     */
    private List<TrainListDTO> list;
}
