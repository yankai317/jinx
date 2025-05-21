package com.learn.service.map;

import com.learn.dto.map.LearningMapLearnerDetailDTO;
import com.learn.dto.map.LearningMapLearnersRequest;
import com.learn.dto.map.LearningMapLearnersResponse;

/**
 * @author yujintao
 * @Description 定义学习地图的操作，例如用户学习地图进度，记录培训日志
 * @date 2025/4/21
 */
public interface LearnMapUserService {

    /**
     * 获取学习地图学员学习详情
     *
     * @param mapId 学习地图ID
     * @param userId 用户ID
     * @return 学习详情
     */
    LearningMapLearnerDetailDTO getLearningMapLearnerDetail(Long mapId, Long userId);
}
