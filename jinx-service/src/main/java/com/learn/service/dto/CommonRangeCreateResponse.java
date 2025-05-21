package com.learn.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用范围创建响应对象
 */
@Data
public class CommonRangeCreateResponse {
    /**
     * 创建的范围ID列表
     */
    private List<Long> rangeIds;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
