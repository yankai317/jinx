package com.learn.dto.common;

import lombok.Data;

/**
 * 指派记录查询请求类
 */
@Data
public class AssignRecordRequest {
    /**
     * 页码，从1开始
     */
    private Integer pageNum = 1;
    
    /**
     * 每页记录数
     */
    private Integer pageSize = 10;
    
    /**
     * 搜索关键字，可用于搜索操作人名称
     */
    private String keyword;
    
    /**
     * 状态筛选：success-指派成功，failed-指派失败
     */
    private String status;
    
    /**
     * 开始时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String startTime;
    
    /**
     * 结束时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String endTime;
}
