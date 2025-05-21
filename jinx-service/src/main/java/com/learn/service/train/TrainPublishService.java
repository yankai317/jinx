package com.learn.service.train;

import com.learn.common.dto.UserTokenInfo;

/**
 * @author yujintao
 * @description 培训发布服务接口
 * @date 2025/4/21
 */
public interface TrainPublishService {
    /**
     * 发布培训
     *
     * @param id 培训ID
     * @return 是否发布成功
     */
    boolean publishTrain(Long id, UserTokenInfo userInfo);
}
