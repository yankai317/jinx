package com.learn.service.banner;

import com.learn.infrastructure.repository.entity.Banner;

import java.util.List;

/**
 * 首页轮播图服务接口
 */
public interface BannerService {
    
    /**
     * 获取首页轮播图列表
     *
     * @return 轮播图列表
     */
    List<Banner> getBannerList(String type);
}
