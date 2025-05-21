package com.learn.service.train;

import com.learn.dto.train.TrainDetailDTO;
import com.learn.dto.train.TrainListRequest;
import com.learn.dto.train.TrainListResponse;

/**
 * @author yujintao
 * @description 定义培训查询接口，例如培训的列表查询
 * @date 2025/4/21
 */
public interface TrainQueryService {
    
    /**
     * 获取培训列表
     * @param request 培训列表查询请求
     * @return 培训列表查询响应
     */
    TrainListResponse getTrainList(TrainListRequest request);
    
    /**
     * 获取培训详情
     * @param id 培训ID
     * @return 培训详情
     */
    TrainDetailDTO getTrainDetail(Long id);
}
