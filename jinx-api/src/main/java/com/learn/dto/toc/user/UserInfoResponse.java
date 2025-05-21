package com.learn.dto.toc.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * C端用户信息响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 用户信息数据
     */
    private UserInfoData data;
    
    /**
     * 用户信息数据类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoData {
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户昵称
         */
        private String nickname;
        
        /**
         * 头像URL
         */
        private String avatar;
        
        /**
         * 部门名称
         */
        private String departmentName;
        
        /**
         * 用户等级
         */
        private Integer level;
        
        /**
         * 总学分
         */
        private Integer totalCredit;
        
        /**
         * 总学习时长（分钟）
         */
        private Integer totalStudyTime;
        
        /**
         * 已完成培训数量
         */
        private Integer completedTrainCount;
        
        /**
         * 已完成学习地图数量
         */
        private Integer completedMapCount;
        
        /**
         * 获得证书数量
         */
        private Integer certificateCount;
    }
    
    /**
     * 创建成功响应
     *
     * @param data 用户信息数据
     * @return 用户信息响应
     */
    public static UserInfoResponse success(UserInfoData data) {
        return UserInfoResponse.builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }
    
    /**
     * 创建错误响应
     *
     * @param message 错误消息
     * @return 用户信息响应
     */
    public static UserInfoResponse error(String message) {
        return UserInfoResponse.builder()
                .code(500)
                .message(message)
                .build();
    }
    
    /**
     * 创建未登录错误响应
     *
     * @return 用户信息响应
     */
    public static UserInfoResponse unauthorized() {
        return UserInfoResponse.builder()
                .code(401)
                .message("未登录或登录已过期")
                .build();
    }
}
