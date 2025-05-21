package com.learn.service.toc;

import com.learn.dto.toc.rankings.RankingsResponse;

/**
 * 排行榜服务接口
 */
public interface RankingsService {
    
    /**
     * 获取学习排行榜数据
     *
     * @param type 排行榜类型：all-全员，department-部门，默认全员
     * @param limit 返回数量限制，默认10
     * @param userId 当前用户ID
     * @return 排行榜数据
     */
    RankingsResponse getRankings(String type, Integer limit, Long userId);
    
    /**
     * 获取学习排行榜数据
     *
     * @param type 排行榜类型：all-全员，department-部门，默认全员
     * @param limit 返回数量限制，默认10
     * @param userId 当前用户ID
     * @param departmentId 部门ID，type=department时使用
     * @return 排行榜数据
     */
    RankingsResponse getRankings(String type, Integer limit, Long userId, Long departmentId);
}
