package com.learn.dto.common;

import lombok.Data;

/**
 * 类目变更请求
 * 
 * @author yujintao
 * @date 2025/5/10
 */
@Data
public class CategoryChangeRequest {
    
    /**
     * 业务ID
     */
    private Long bizId;
    
    /**
     * 业务类型，包括COURSE, LEARNING_MAP, TRAIN
     */
    private String bizType;
    
    /**
     * 类目ID列表，格式为"1,2,3"
     */
    private String categoryIds;
}
