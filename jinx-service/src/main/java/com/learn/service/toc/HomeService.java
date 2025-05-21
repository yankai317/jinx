package com.learn.service.toc;

import com.learn.dto.toc.home.HomeDataDTO;

/**
 * 首页服务接口
 */
public interface HomeService {
    
    /**
     * 获取首页数据
     *
     * @param userId 用户ID
     * @return 首页数据
     */
    HomeDataDTO getHomeData(Long userId);
}
