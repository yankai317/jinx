package com.learn.dto.common;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 指派响应类
 */
@Data
public class AssignResponse {
    /**
     * 成功指派数量
     */
    private Integer success;
    
    /**
     * 指派失败数量
     */
    private Integer failed;
    
    /**
     * 指派失败的用户ID
     */
    private List<Long> failedUsers;
    
    /**
     * 指派记录ID
     */
    private Long assignRecordId;
    
    /**
     * 范围ID列表
     */
    private List<Long> rangeIds;
    /**
     * 指派结束时间
     */
    private Date assignEndTime;
}
