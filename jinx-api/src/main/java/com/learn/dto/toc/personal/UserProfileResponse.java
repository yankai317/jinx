package com.learn.dto.toc.personal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户个人中心信息响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 用户个人中心数据
     */
    private UserProfileData data;
    
    /**
     * 用户个人中心数据类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileData {
        /**
         * 用户基本信息
         */
        private UserInfo userInfo;
        
        /**
         * 学习统计数据
         */
        private Statistics statistics;
        
        /**
         * 最近获得的证书
         */
        private List<Certificate> certificates;
        
        /**
         * 最近学习记录
         */
        private List<LearningRecord> recentLearning;
    }
    
    /**
     * 用户基本信息类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户昵称
         */
        private String nickname;
        
        /**
         * 用户头像
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
    }
    
    /**
     * 学习统计数据类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        /**
         * 总学分
         */
        private Long totalCredit;
        
        /**
         * 总学习时长(分钟)
         */
        private Integer totalStudyTime;
        
        /**
         * 已完成课程数
         */
        private Integer completedCourseCount;
        
        /**
         * 已完成培训数
         */
        private Integer completedTrainCount;
        
        /**
         * 已完成学习地图数
         */
        private Integer completedMapCount;
        
        /**
         * 证书数量
         */
        private Integer certificateCount;
    }
    
    /**
     * 证书类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certificate {
        /**
         * 证书ID
         */
        private Long certificateId;
        
        /**
         * 证书名称
         */
        private String certificateName;
        
        /**
         * 证书图片URL
         */
        private String certificateImageUrl;
        
        /**
         * 获得时间
         */
        private String issueTime;
        
        /**
         * 证书编号
         */
        private String certificateNo;
    }
    
    /**
     * 学习记录类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningRecord {
        /**
         * 记录ID
         */
        private Long id;
        
        /**
         * 内容ID
         */
        private Long contentId;
        
        /**
         * 内容名称
         */
        private String contentName;
        
        /**
         * 内容类型：course-课程，train-培训，map-学习地图
         */
        private String contentType;
        
        /**
         * 封面图URL
         */
        private String coverUrl;
        
        /**
         * 学习时长(分钟)
         */
        private Integer studyDuration;
        
        /**
         * 开始学习时间
         */
        private String startTime;
        
        /**
         * 学习进度(0-100)
         */
        private Integer progress;
    }
    
    /**
     * 创建成功响应
     *
     * @param data 用户个人中心数据
     * @return 用户个人中心响应
     */
    public static UserProfileResponse success(UserProfileData data) {
        return UserProfileResponse.builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }
    
    /**
     * 创建错误响应
     *
     * @param message 错误消息
     * @return 用户个人中心响应
     */
    public static UserProfileResponse error(String message) {
        return UserProfileResponse.builder()
                .code(500)
                .message(message)
                .build();
    }
    
    /**
     * 创建未登录错误响应
     *
     * @return 用户个人中心响应
     */
    public static UserProfileResponse unauthorized() {
        return UserProfileResponse.builder()
                .code(401)
                .message("未登录或登录已过期")
                .build();
    }
}
