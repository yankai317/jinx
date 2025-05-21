package com.learn.dto.assignment;

import lombok.Data;

/**
 * 指派回显请求
 */
@Data
public class AssignmentDetailRequest {

    /**
     * 业务类型（培训、学习地图）
     */
    private String bizType;

    /**
     * 业务ID（培训ID或学习地图ID）
     */
    private Long bizId;

    /**
     * 指派类型（单次指派、周期指派）
     */
    private String assignType;
} 
