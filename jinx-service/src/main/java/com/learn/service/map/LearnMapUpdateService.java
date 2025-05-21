package com.learn.service.map;

import com.learn.dto.map.LearningMapUpdateRequest;

import java.util.List;

/**
 * @author yujintao
 * @description 定义地图的编辑相关操作
 * @date 2025/4/21
 */
public interface LearnMapUpdateService {
    
    /**
     * 更新学习地图
     *
     * @param request 更新学习地图请求
     * @param userId 用户ID
     * @param userName 用户名称
     * @return 是否更新成功
     */
    Boolean updateLearningMap(LearningMapUpdateRequest request, Long userId, String userName);


    /**
     * 批量更新创建人
     * @param courseIds
     * @param newCreatorId
     * @param newCreatorName
     * @param operatorId
     * @param operatorName
     * @return
     */
    boolean batchUpdateCreator(List<Long> courseIds, Long newCreatorId, String newCreatorName, Long operatorId, String operatorName);
}
