package com.learn.dto.map;

import lombok.Data;

import java.util.List;

/**
 * 学习地图列表查询响应
 */
@Data
public class LearningMapListResponse {
    /**
     * 总记录数
     */
    private Integer total;
    
    /**
     * 学习地图列表
     */
    private List<LearningMapDTO> list;
}
