package com.learn.dto.map;

import com.learn.dto.common.LearnerDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学习地图学习人员列表响应
 */
@Data
public class LearningMapLearnersResponse {
    /**
     * 总记录数
     */
    private Integer total;
    
    /**
     * 学习人员列表
     */
    private List<LearnerDTO> list;

}
