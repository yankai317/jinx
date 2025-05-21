package com.learn.service.train;

import com.learn.dto.train.TrainLearnerDetailDTO;

/**
 * @author yujintao
 * @Description 定义培训的用户操作，例如用户培训进度，记录培训日志
 * @date 2025/4/21
 */
public interface TrainUserService {

    /**
     * 获取培训学员学习详情
     *
     * @param trainId 培训ID
     * @param userId 用户ID
     * @return 学习详情
     */
    TrainLearnerDetailDTO getTrainLearnerDetail(Long trainId, Long userId);
    /**
     * 发送学习提醒
     *
     * @param trainId 培训ID
     * @param userId 用户ID
     * @return 是否发送成功
     */
    boolean sendLearningReminder(Long trainId, Long userId);
}
