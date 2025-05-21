package com.learn.service.map;

import com.learn.dto.map.LearningMapDetailDTO;
import com.learn.dto.map.LearningMapListRequest;
import com.learn.dto.map.LearningMapListResponse;

/**
 * @author yujintao
 * @description 定义学习地图查询接口，例如培训的列表查询
 * @date 2025/4/21
 */
public interface LearnMapQueryService {/**
     * 获取学习地图列表
     *
     * @param request 查询请求
     * @param userId 当前用户ID
     * @return 学习地图列表响应
     */
    LearningMapListResponse getLearningMapList(LearningMapListRequest request, Long userId);
    
    /**
     * 获取学习地图详情
     *
     * @param id 学习地图ID
     * @param userId 当前用户ID
     * @return 学习地图详情
     */
    LearningMapDetailDTO getLearningMapDetail(Long id, Long userId);
}
