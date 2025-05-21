package com.learn.service.toc;

import com.learn.dto.toc.map.MapDetailResponse;
import com.learn.dto.toc.map.MapStageDetailResponse;

/**
 * C端学习地图服务接口
 */
public interface MapService {
    
    /**
     * 获取学习地图详情
     *
     * @param id 学习地图ID
     * @param userId 当前用户ID
     * @return 学习地图详情响应
     */
    MapDetailResponse getMapDetail(Long id, Long userId);
    
    /**
     * 获取学习地图阶段详情
     *
     * @param mapId 学习地图ID
     * @param stageId 阶段ID
     * @param userId 当前用户ID
     * @return 学习地图阶段详情响应
     */
    MapStageDetailResponse getMapStageDetail(Long mapId, Long stageId, Long userId);
}
