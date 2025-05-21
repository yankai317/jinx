package com.learn.service.common.strategy;

import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.service.dto.CommonRangeQueryRequest;

/**
 * 指派策略接口
 * 定义不同业务类型（课程、培训、学习地图）的指派策略
 */
public interface AssignStrategy {

    /**
     * 执行指派操作
     * 
     * @param request 指派请求
     * @return 指派结果
     */
    AssignResponse assign(AssignRequest request);
    
    /**
     * 获取业务类型
     * 
     * @return 业务类型
     */
    String getBizType();
}
