package com.learn.dto.toc.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 课程详情响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailResponse {
    /**
     * 课程ID
     */
    private Long id;
    
    /**
     * 课程名称
     */
    private String title;
    
    /**
     * 课程类型
     */
    private String type;
    
    /**
     * 封面图URL
     */
    private String coverImage;
    
    /**
     * 课程简介
     */
    private String description;
    
    /**
     * 学分
     */
    private Integer credit;
    
    /**
     * 讲师信息
     */
    private InstructorDTO instructor;
    
    /**
     * 附件类型
     */
    private String appendixType;
    
    /**
     * 附件路径
     */
    private String appendixPath;
    
    /**
     * 课程时长（秒）
     */
    private Integer duration;
    
    /**
     * 用户学习进度
     */
    private UserProgressDTO userProgress;
    
    /**
     * 学习人员列表
     */
    private List<LearnerDTO> learners;
    
    /**
     * 课程章节列表
     */
    private List<CourseItemDTO> courseItems;
    
    /**
     * 讲师DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorDTO {
        /**
         * 讲师ID
         */
        private Integer id;
        
        /**
         * 讲师姓名
         */
        private String name;
        
        /**
         * 讲师头像
         */
        private String avatar;
    }
    
    /**
     * 用户学习进度DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProgressDTO {
        /**
         * 学习状态：not_started-未开始，learning-学习中，completed-已完成
         */
        private String status;
        
        /**
         * 学习进度（百分比）
         */
        private Integer progress;
        
        /**
         * 学习时长（秒）
         */
        private Integer studyDuration;
        
        /**
         * 最后学习时间
         */
        private String lastStudyTime;
    }
    
    /**
     * 学习人员DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearnerDTO {
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
         * 部门
         */
        private String department;
        
        /**
         * 学习状态：not_started-未开始，learning-学习中，completed-已完成
         */
        private String status;
    }
    
    /**
     * 课程章节DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseItemDTO {
        /**
         * 内容ID
         */
        private Long id;
        
        /**
         * 标题
         */
        private String title;
        
        /**
         * 内容类型
         */
        private String type;
        
        /**
         * 封面图
         */
        private String coverImage;
        
        /**
         * 时长（秒）
         */
        private Integer duration;
        
        /**
         * 文件大小（KB）
         */
        private Integer size;
        
        /**
         * 学习进度（百分比）
         */
        private Integer progress;
        
        /**
         * 是否必修：true-必修，false-选修
         */
        private Boolean required;
        
        /**
         * 学习状态：not_started-未开始，learning-学习中，completed-已完成
         */
        private String status;
        
        /**
         * 排序序号
         */
        private Integer sortOrder;

        /**
         * 附件路径
         */
        private String appendixPath;
    }
}
