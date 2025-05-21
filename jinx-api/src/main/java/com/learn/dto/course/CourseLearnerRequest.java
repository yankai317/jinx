package com.learn.dto.course;

import lombok.Data;

/**
 * 获取课程学习人员列表请求
 */
@Data
public class CourseLearnerRequest {
    /**
     * 部门ID
     */
    private Long departmentId;
    
    /**
     * 完成状态：all, completed, learning
     */
    private String status;
    
    /**
     * 关键词(用户名/工号)
     */
    private String keyword;

    private Long userId;
    
    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;
}
