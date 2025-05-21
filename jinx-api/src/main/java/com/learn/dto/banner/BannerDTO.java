package com.learn.dto.banner;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页轮播图DTO
 */
@Data
public class BannerDTO {
    
    /**
     * 轮播图ID
     */
    private Long id;
    
    /**
     * 轮播图标题
     */
    private String title;
    
    /**
     * 轮播图片URL
     */
    private String imageUrl;
    
    /**
     * 链接URL
     */
    private String linkUrl;
    
    /**
     * 排序序号
     */
    private Integer sort;
}
