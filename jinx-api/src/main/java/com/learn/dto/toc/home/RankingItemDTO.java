package com.learn.dto.toc.home;

import lombok.Data;

/**
 * 排行榜项DTO
 */
@Data
public class RankingItemDTO {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像URL
     */
    private String avatar;
    
    /**
     * 部门名称
     */
    private String departmentName;
    
    /**
     * 排名
     */
    private Integer rank;
    
    /**
     * 学习时长（分钟）
     */
    private Integer studyDuration;
    
    /**
     * 完成课程数
     */
    private Integer completedCourseCount;
    
    /**
     * 获得证书数
     */
    private Integer certificateCount;
}
