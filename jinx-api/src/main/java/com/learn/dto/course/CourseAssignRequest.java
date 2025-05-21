package com.learn.dto.course;

import lombok.Data;

import java.util.List;

/**
 * 课程指派请求类
 */
@Data
public class CourseAssignRequest {
    /**
     * 课程ID
     */
    private Integer courseId;
    
    /**
     * 用户ID列表
     */
    private List<Integer> userIds;
    
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
