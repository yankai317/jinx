package com.learn.service.banner.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.infrastructure.repository.entity.Banner;
import com.learn.infrastructure.repository.mapper.BannerMapper;
import com.learn.service.banner.BannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页轮播图服务实现类
 */
@Service
@Slf4j
public class BannerServiceImpl implements BannerService {
    
    @Autowired
    private BannerMapper bannerMapper;
    
    /**
     * 获取首页轮播图列表
     *
     * @return 轮播图列表
     */
    @Override
    public List<Banner> getBannerList(String type) {
        // 构建查询条件
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        // 按照排序字段升序排列
        queryWrapper.eq(Banner::getType, type);
        queryWrapper.orderByAsc(Banner::getSort);
        
        // 查询轮播图列表
        return bannerMapper.selectList(queryWrapper);
    }
}
