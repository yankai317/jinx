package com.learn.dto.train;

import lombok.Data;

import java.util.List;

/**
 * 培训指派响应类
 */
@Data
public class TrainAssignResponse {
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
