package com.learn.service.toc;

import com.learn.dto.toc.train.TrainDetailResponse;

/**
 * C端培训服务接口
 */
public interface TrainService {
    
    /**
     * 获取培训详情
     *
     * @param id 培训ID
     * @param userId 当前用户ID
     * @return 培训详情
     */
    TrainDetailResponse getTrainDetail(Long id, Long userId);
}
