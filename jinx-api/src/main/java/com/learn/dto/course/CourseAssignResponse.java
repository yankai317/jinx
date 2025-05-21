package com.learn.dto.course;

import lombok.Data;

import java.util.List;

/**
 * 课程指派响应类
 */
@Data
public class CourseAssignResponse {
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
}
