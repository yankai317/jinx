package com.learn.dto.train;

import lombok.Data;

/**
 * 获取培训学习人员列表请求
 */
@Data
public class TrainLearnersRequest {
    /**
     * 部门ID
     */
    private Long departmentId;
    
    /**
     * 完成状态：all, completed, learning
     */
    private String status;
    
    /**
     * 学习来源：all, assign, self
     */
    private String source;
    
    /**
     * 用户id
     */
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
