package com.learn.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务标题和图片信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSendInfo {
    /**
     * 标题
     */
    private String title;
    
    /**
     * 图片URL
     */
    private String picUrl;
    /**
     *  跳转pc地址
     */
    private String pcUrl;

    /**
     *  跳转地址
     */
    private String mobileUrl;
    /**
     * 指派人 + 时间信息
     */
    private String createInfo;
    /**
     * 详情
     */
    private String desc;
}
