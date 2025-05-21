package com.learn.dto.toc.home;

import lombok.Data;

/**
 * 首页轮播图响应DTO
 */
@Data
public class BannerResponseDTO {
    
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

    private String type;
}
