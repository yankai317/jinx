package com.learn.dto.train;

import com.learn.dto.common.LearnerDTO;
import lombok.Data;

import java.util.List;

/**
 * 培训学习人员列表响应
 */
@Data
public class StatisticLearnersResponse {
    /**
     * 总记录数
     */
    private Integer total;
    
    /**
     * 学习人员列表
     */
    private List<LearnerDTO> list;
}
