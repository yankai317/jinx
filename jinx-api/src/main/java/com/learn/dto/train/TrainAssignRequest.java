package com.learn.dto.train;

import lombok.Data;

import java.util.List;

/**
 * 培训指派请求类
 */
@Data
public class TrainAssignRequest {
    /**
     * 培训ID
     */
    private Long trainId;
    
    /**
     * 用户ID列表
     */
    private List<Long> userIds;
    
    /**
     * 截止时间
     */
    private String deadline;
    
    /**
     * 是否发送通知，默认true
     */
    private Boolean sendNotification;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;
}
