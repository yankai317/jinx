package com.learn.service.train;

import com.learn.dto.train.TrainVisibilityRequest;

/**
 * 培训可见范围设置服务接口
 */
public interface TrainVisibilityService {

    /**
     * 获取培训可见范围
     *
     * @param trainId 培训ID
     * @return 可见范围设置
     */
    TrainVisibilityRequest getTrainVisibility(Long trainId);
}
