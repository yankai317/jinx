package com.learn.service.train;

import com.learn.common.dto.UserTokenInfo;

/**
 * @author yujintao
 * @description 培训取消发布服务接口
 * @date 2025/4/21
 */
public interface TrainUnpublishService {
    /**
     * 取消发布培训
     *
     * @param id 培训ID
     * @return 是否取消发布成功
     */
    boolean unpublishTrain(Long id, UserTokenInfo userInfo);
}
