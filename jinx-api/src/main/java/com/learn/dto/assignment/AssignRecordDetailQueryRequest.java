package com.learn.dto.assignment;

import lombok.Data;

/**
 * 指派记录明细查询请求
 */
@Data
public class AssignRecordDetailQueryRequest {
    /**
     * 指派记录ID
     */
    private Long assignRecordId;
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页记录数
     */
    private Integer pageSize = 10;
}
