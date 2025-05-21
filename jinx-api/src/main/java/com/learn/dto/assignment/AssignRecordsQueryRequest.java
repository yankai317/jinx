package com.learn.dto.assignment;

import lombok.Data;

/**
 * 指派记录查询请求
 */
@Data
public class AssignRecordsQueryRequest {
    /**
     * 业务类型
     */
    private String type;
    
    /**
     * 业务ID
     */
    private Long typeId;
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页记录数
     */
    private Integer pageSize = 10;
}
