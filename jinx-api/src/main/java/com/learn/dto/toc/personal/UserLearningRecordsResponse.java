package com.learn.dto.toc.personal;

import lombok.Data;

import java.util.List;

/**
 * 获取用户学习记录响应
 */
@Data
public class UserLearningRecordsResponse {
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 学习记录列表
     */
    private List<LearningRecordDTO> list;
    
    /**
     * 当前页码
     */
    private Integer pageNum;
    
    /**
     * 每页条数
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 学习记录DTO
     */
    @Data
    public static class LearningRecordDTO {
        /**
         * 记录ID
         */
        private Long id;

        private String source;
        
        /**
         * 内容ID
         */
        private Long contentId;
        
        /**
         * 内容名称
         */
        private String contentName;

        /**
         * 类型
         */
        private String type;
        
        /**
         * 内容类型：course-课程，train-培训，map-地图
         */
        private String contentType;
        
        /**
         * 开始学习时间
         */
        private String startTime;
        
        /**
         * 学习时长（分钟）
         */
        private Integer studyDuration;
        
        /**
         * 学习进度（百分比）
         */
        private Integer progress;
        
        /**
         * 完成状态
         * @see com.learn.constants.LearningStatus
         */
        private String status;
        
        /**
         * 封面图URL
         */
        private String coverUrl;

        /**
         * 描述
         */
        private String description;

        private Integer credit;
        
        /**
         * 最后学习时间
         */
        private String lastStudyTime;
        
        /**
         * 完成时间
         */
        private String completionTime;
    }
}
