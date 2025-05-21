package com.learn.infrastructure.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.infrastructure.repository.entity.Banner;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页轮播图Mapper接口
 */
@Mapper
public interface BannerMapper extends BaseMapper<Banner> {
}
