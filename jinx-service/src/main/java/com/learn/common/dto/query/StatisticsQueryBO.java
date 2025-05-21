package com.learn.common.dto.query;

import lombok.Data;

/**
 * @author yujintao
 * @date 2025/5/13
 */
@Data
public class StatisticsQueryBO {

    /**
     * @see com.learn.constants.BizType
     */
    private String bizType;

    private Long bizId;

    private String status;

    private Long userId;

    private Integer pageSize = 10;

    private Integer pageNum = 1;
}
