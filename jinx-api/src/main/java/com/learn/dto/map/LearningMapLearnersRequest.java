package com.learn.dto.map;

import lombok.Data;

/**
 * 获取学习地图学习人员列表请求
 */
@Data
public class LearningMapLearnersRequest {
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
     * 关键词(用户名/工号)
     */
    private String keyword;
    
    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;
}
