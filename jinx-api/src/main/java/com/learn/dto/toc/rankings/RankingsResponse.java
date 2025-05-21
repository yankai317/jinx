package com.learn.dto.toc.rankings;

import lombok.Data;

import java.util.List;

/**
 * 排行榜响应DTO
 */
@Data
public class RankingsResponse {
    /**
     * 排行榜类型：all-全员，department-部门
     */
    private String type;
    /**
     * 排行榜列表
     */
    private List<RankingItem> list;
    
    /**
     * 当前用户排名信息
     */
    private UserRank userRank;
    /**
     * 排行榜项
     */
    @Data
    public static class RankingItem {
        /**
         * 排名
         */
        private Integer rank;
        
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
         * 部门
         */
        private String department;/**
         * 学习积分
         */
        private Integer credit;
    }/**
     * 用户排名信息
     */
    @Data
    public static class UserRank {
        /**
         * 排名
         */
        private Integer rank;
        
        /**
         * 学习积分
         */
        private Integer credit;
    }
}
