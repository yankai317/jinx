package com.learn.service.dto;

import lombok.Data;

/**
 * 通用范围更新响应对象
 */
@Data
public class CommonRangeUpdateResponse {
    /**
     * 更新的记录数
     */
    private Integer updatedCount;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
